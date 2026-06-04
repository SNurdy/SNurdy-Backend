# Phase 2.8 — 테스트 분리

학습 flow 를 카드 phase 와 테스트 phase 로 명확히 분리하고, 테스트 결과 submit endpoint 를 신설한다.
디자인 PNG (학습하기, 학습-1~4, 학습완료, 테스트 유형 선택, 테스트 뷰_시안1~다지선다, 테스트결과_틀린문제O/X/레벨업) 분석 결과 반영.

---

## 1. 화면 flow 와 backend 호출 매핑

| phase | 화면 | backend 호출 |
|---|---|---|
| 학습 시작 | 학습하기(Home) | 단어장/카드수/테스트유형 사전 설정. backend 호출 없음 |
| 카드 학습 | 학습-1~4 | 순수 클라이언트 화면. backend 호출 없음 |
| 테스트 유형 모달 | 테스트 유형 선택 | 진입 시 default test type 조회. "다음부터 묻지 않기" 시 PUT default |
| 테스트 진행 | 테스트 뷰 | 클라이언트가 단어 리스트 가지고 자체 진행 (1문제마다 backend 호출 없음) |
| 테스트 결과 | 테스트결과_틀린문제O/X/레벨업 | 한 번에 submit, response 로 결과/레벨업 받음 |

진도/streak/totalLearningTerms 갱신은 테스트 submit 시점에만 발생한다.
카드만 보고 이탈 / 테스트 중 종료 시 학습 진도 반영되지 않는다 (테스트 종료 모달 카피 일관).

---

## 2. 테스트 유형 enum (7종)

```kotlin
enum class TestType {
    TERM_TO_MEANING,      // 용어 보고 의미 작성 (한글)
    MEANING_TO_TERM,      // 의미 보고 용어 작성 (영문)
    LISTEN_AND_WRITE,     // TTS 듣고 영문 작성
    WRITE_ABBREVIATION,   // 용어 보고 약어 작성
    MC_PICK_TERM,         // 의미 보고 용어 4지선다
    MC_PICK_MEANING,      // 용어 보고 의미 4지선다
    MC_PICK_ABBR          // 용어 보고 약어 4지선다
}
```

UI 노출은 클라이언트가 5개로 묶거나 7개 그대로 노출할 수 있으나, backend 계약은 7종 분리.

### 출제 가능 조건

| TestType | 필수 조건 |
|---|---|
| TERM_TO_MEANING | 단어장 단어 1개 이상 |
| MEANING_TO_TERM | 단어장 단어 1개 이상 |
| LISTEN_AND_WRITE | 단어장 단어 1개 이상 |
| WRITE_ABBREVIATION | 단어장에 `abbreviation` 가진 단어 1개 이상 |
| MC_PICK_TERM | 단어장 단어 1개 이상 |
| MC_PICK_MEANING | 단어장 단어 1개 이상 |
| MC_PICK_ABBR | 단어장에 `abbreviation` 가진 단어 1개 이상 |

조건 미충족 시 출제 불가 에러 반환.

다지선다 보기는 같은 단어장에서 우선 수집하고, distinct 처리 후 4개 (정답 1 + 오답 3) 가 채워지지 않으면 다른 단어장의 단어 풀에서 random 으로 보충한다 (MC_PICK_ABBR 은 abbreviation 가진 단어로 한정).

---

## 3. API 명세

### 3.1 PUT /v2/users/me/preferences/test-type — 사용자 default 테스트 유형 변경

**요청**
```
{ "defaultTestType": "MEANING_TO_TERM" }
```

null 허용 (선택 해제 = 다음 진입 시 모달 강제).

**응답**
```
{ "defaultTestType": "MEANING_TO_TERM" }
```

**SuccessCode**
- USER_S030 UPDATE_DEFAULT_TEST_TYPE

**ErrorCode**
- USER_E010 ACCOUNT_NOT_FOUND

UserEntity 에 `default_test_type ENUM NULL` 컬럼 추가 (별도 user_preferences 테이블 도입 보류).
GET /v2/users/me 응답에 `defaultTestType` 필드 포함.

---

### 3.2 POST /v2/test/start — 테스트 시작 (문제 출제)

stateless. 클라이언트가 문제 리스트를 받아 자체 진행한 뒤 submit 한다.

**요청**
```
{
  "glossaryId": 12,
  "testType": "MEANING_TO_TERM",
  "questionCount": 30
}
```

- `questionCount` 1 ~ 50 범위 (학습하기 Home 의 카드 수 설정 UI 와 동일 범위)
- 단어장 단어 수 < questionCount → 가능한 만큼만 출제 (예: 18개면 18문제)

**응답**
```
{
  "glossaryId": 12,
  "testType": "MEANING_TO_TERM",
  "questions": [
    {
      "termId": 101,
      "prompt": {
        "type": "MEANING_TO_TERM",
        "text": "심전도"
      },
      "choices": null
    },
    {
      "termId": 205,
      "prompt": {
        "type": "MC_PICK_TERM",
        "text": "심전도"
      },
      "choices": ["Electrocardiogram", "Heartbeat", "Hemogram", "Erythrocyte"]
    }
  ]
}
```

- 같은 테스트 안 모든 question 의 testType 은 동일 (1회 테스트 단일 유형 원칙)
- `choices` 는 다지선다(MC_*) 에서만 4개 채움, 작성형은 null
- 다지선다 보기 4개 중 1개가 정답, 순서 random
- 출제 단어 선정: 단어장 내 random sampling (중복 없이)

**SuccessCode**
- LEARNING_S020 START_TEST

**ErrorCode**
- LEARNING_E020 TEST_TYPE_NOT_AVAILABLE — 단어장이 해당 testType 출제 조건 미충족
- DICTIONARY_E001 GLOSSARY_NOT_FOUND

---

### 3.3 POST /v2/test/submit — 테스트 결과 일괄 submit

**요청**
```
{
  "glossaryId": 12,
  "testType": "MEANING_TO_TERM",
  "results": [
    { "termId": 101, "correct": true,  "userAnswer": "Electrocardiogram" },
    { "termId": 205, "correct": false, "userAnswer": "elec" }
  ]
}
```

- `correct` 는 클라이언트가 판정한 결과
  - 작성형 정답 비교 규칙은 백엔드가 모르므로 클라이언트가 판정
  - 보안 측면에서 추후 서버 측 재검증 도입 가능성 열어둠
- `userAnswer` 는 오답노트 표시용

**응답**
```
{
  "correctCount": 25,
  "incorrectCount": 5,
  "todayGoalCompleted": true,
  "glossary": {
    "glossaryId": 12,
    "progressRateBefore": 80.0,
    "progressRateAfter": 85.0,
    "levelBefore": 4,
    "levelUp": {
      "from": 4,
      "to": 5
    }
  }
}
```

- `levelUp` null 이면 클라이언트는 일반 결과 화면, non-null 이면 레벨업 화면 노출
- `todayGoalCompleted` 는 오늘 첫 테스트 완료 여부 (streak/totalLearningTerms 갱신 트리거)

**SuccessCode**
- LEARNING_S021 SUBMIT_TEST

**ErrorCode**
- LEARNING_E020 TEST_TYPE_NOT_AVAILABLE
- LEARNING_E021 TEST_RESULT_INVALID — termId 가 단어장 외부 / results 비어있음
- DICTIONARY_E001 GLOSSARY_NOT_FOUND

### 처리 부수 효과

| 대상 | 변경 |
|---|---|
| TermProgressEntity | 각 term 의 SRS 상태 갱신 (정답 시 nextReviewAt 연장, 오답 시 단축) |
| WrongAnswerTermEntity | 오답 term 을 upsert (incorrectAt 갱신, 누적값 +1) |
| UserEntity.totalLearningTerms | 오늘 첫 테스트면 단어장 진척 새로 학습한 termCount 만큼 +N |
| StreakEntity | 오늘 첫 테스트면 streak 갱신 |
| GlossaryProgressEntity | learnedCount/lastLearnedAt 갱신 |

---

## 4. 기존 v1 endpoint 처리

기존 `POST /v1/learning/submit` 은 카드 학습 + 자체 정오 판정이 섞여 있었음. v2 로 명확히 분리하면서:

- v1 endpoint 유지 (이미 배포된 클라이언트 호환)
- v2 출시 후 deprecation 일정 별도 결정

---

## 5. 추가 결정 사항

| 항목 | 결정 |
|---|---|
| 테스트 1회 단일 유형 | 30문제 전부 같은 testType |
| 출제 개수 | 클라이언트가 결정 (학습하기 Home UI), 1~50 |
| 출제 단어 선정 | Random sampling, SRS due 우선 알고리즘 도입은 보류 |
| 정오 판정 위치 | 클라이언트 (서버 재검증 보류) |
| default test type 저장 | UserEntity 컬럼 (user_preferences 분리 보류) |
| 카드 phase backend 호출 | 없음 |
| 진도 갱신 시점 | 테스트 submit 시점만 |
| 다지선다 보기 출처 | 같은 단어장 내 다른 term (단어장 단어 4개 이상) |
| stateless | sessionId 도입 안 함, glossaryId + testType 만으로 submit |

---

## 6. 구현 순서

1. UserEntity `default_test_type` 컬럼 추가 + Flyway V40
2. TestType enum + DTO (StartTestRequest/Response, SubmitTestRequest/Response)
3. PUT /v2/users/me/preferences/test-type endpoint
4. POST /v2/test/start endpoint (단어 sampling + 다지선다 보기 생성)
5. POST /v2/test/submit endpoint (SRS/오답노트/streak/level 부수 효과)
6. SuccessCode/ErrorCode 신규 추가
7. v2-roadmap.md 2.8 상태 done 으로 갱신

---

## 7. 미확정 / 후속 검토

- 서버 측 정오 재검증 (부정행위 방지)
- SRS due 우선 sampling 알고리즘
- 자주 틀린 단어 (3회+ 오답, 홈 화면 daily 1) 별도 endpoint
- 테스트 도중 종료 시 일부 결과 저장 여부 (현재는 종료 = 진도 미반영)
