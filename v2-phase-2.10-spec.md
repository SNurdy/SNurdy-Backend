## Phase 2.10 — 학습/테스트 기능 확장 (커스텀노트 / 오답노트)

Phase 2.8 의 학습 카드 + 다지선다 오답 생성 + 테스트 결과 제출 흐름을 기본 단어장에서 커스텀노트, 오답노트로 확장한다. 정통 REST sub-resource 패턴으로 URL 을 노트 타입별 분리.

상세 결정 배경은 v2-roadmap.md 2.10 항목, 디자인 PNG (테스트하기_오답 폴더) 분석 결과 반영.

---

## 1. URL 전략 (옵션 B 채택)

Phase 2.8 의 `POST /v2/cards` 단일 endpoint 가 noteType 별 polymorphic body 로 분기되는 RPC 스타일을 폐기하고, 리소스별 sub-resource URL 로 분리. 기존 `/v2/wrong-answer-note/by-period`, `/by-glossary` 패턴과 일관.

### 이유
- URL 자체로 컨텍스트 명확 (어떤 노트 타입인지)
- Request body 가 단순 (`{ testType, count }` 만 공통)
- Swagger 문서가 각 endpoint 별 명확, 분기 없음
- iOS 호출 코드가 URL 만 바꾸면 됨, body 분기 없음
- 기존 오답노트 API 컨벤션과 동일

### 트레이드오프
- endpoint 수 증가 (cards 4개 + distractors 4개 + test-results 4개 = 12개)
- Controller 는 thin, Service 내부에 sampling 분기 로직 모음

---

## 2. 전체 endpoint 일람

### 학습 카드 조회

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/cards` |
| 커스텀노트 | `POST /v2/custom-notes/{customNoteId}/cards` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/cards` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/cards` |

### 다지선다 오답 보기 생성

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/distractors` |
| 커스텀노트 | `POST /v2/custom-notes/{customNoteId}/distractors` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/distractors` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/distractors` |

### 테스트 결과 제출

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/test-results` |
| 커스텀노트 | `POST /v2/custom-notes/{customNoteId}/test-results` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/test-results` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/test-results` |

### Path 파라미터 정의
- `glossaryId`: Long, 기본 단어장 id
- `customNoteId`: Long, 커스텀노트 id (사용자 소유 검증 필수)
- `period`: enum `TODAY | WEEK | MONTH | ALL` (오답노트 by-period 기간)

---

## 3. Request / Response 모양

### 3.1 cards 요청 / 응답

모든 4종이 동일한 body / response 모양. URL 만 다름.

**요청 body 공통**
```json
{ "testType": "MEANING_TO_TERM", "count": 30 }
```

**응답 공통** (Phase 2.8 의 CardsResponse 그대로)
```json
{
  "glossaryId": 12,        // 기본 단어장 일 때만 채움. 그 외는 noteId 또는 null
  "cards": [
    {
      "termId": 101,
      "spelling": "Electrocardiogram",
      "meaning": "심전도",
      "abbreviation": "ECG",
      "explanation": "...",
      "bookmarked": false,
      "morphemes": [...]
    }
  ]
}
```

응답 envelope 의 `glossaryId` 는 backward compat 위해 유지하되, 노트 타입별로 다음 의미:
- 기본 단어장: glossaryId
- 커스텀노트: customNoteId
- 오답노트 (기간): null, 별도 `period` 필드로 echo (또는 응답 envelope 재설계)
- 오답노트 (과): sourceGlossaryId

(또는 envelope 를 `noteContext` 같은 generic 필드로 통합. 결정 보류, 구현 시 확정)

### 3.2 distractors 요청 / 응답

**요청 body 공통**
```json
{ "testType": "MC_PICK_TERM", "termIds": [101, 102, 205] }
```

**응답 공통**
```json
{
  "distractorsByTermId": {
    "101": ["...", "...", "..."],
    "102": ["...", "...", "..."]
  }
}
```

### 3.3 test-results 요청 / 응답

**요청 body 공통**
```json
{
  "testType": "MEANING_TO_TERM",
  "results": [
    { "termId": 101, "correct": true, "userAnswer": "Electrocardiogram" }
  ]
}
```

**응답 공통**
```json
{
  "correctCount": 25,
  "incorrectCount": 5,
  "todayGoalCompleted": true,
  "glossary": {
    "glossaryId": 12,         // 기본 단어장 일 때만 의미 있음
    "progressRateBefore": 80.0,
    "progressRateAfter": 85.0,
    "levelBefore": 4,
    "levelUp": { "from": 4, "to": 5 }    // 커스텀/오답은 항상 null
  }
}
```

커스텀노트/오답노트의 경우 `glossary` 필드 의미가 없으니 null 또는 별도 envelope 로 분리 결정 (구현 시 확정).

---

## 4. noteType 별 동작 차이

### 4.1 cards 단어 sampling

| 노트 타입 | 데이터 출처 | NEW 우선 정렬 |
|---|---|---|
| 기본 단어장 | `glossary_terms` JOIN `term_progresses` | ✅ (기존 정책) |
| 커스텀노트 | `custom_note_terms` (소유 검증) | ❌ (SRS 무관) |
| 오답노트 (기간) | `wrong_answer_terms` (user + 기간 필터, deletedAt IS NULL) | ❌ |
| 오답노트 (과) | `wrong_answer_terms` (user + sourceGlossaryId 필터, deletedAt IS NULL) | ❌ |

#### testType 별 필터링 (공통)
- WRITE_ABBREVIATION / MC_PICK_ABBR → abbreviation 가진 단어만
- 그 외 → 필터 없음

#### count 부족 케이스
- 데이터 0개 → `TEST_E001 TEST_TYPE_NOT_AVAILABLE`
- 데이터 < count → 가능한 만큼만 반환 (cards.size 로 client 인지)

### 4.2 distractors 출처

| 노트 타입 | 1차 출처 | 2차 보충 |
|---|---|---|
| 기본 단어장 | 같은 glossary 의 다른 termId | 글로벌 random |
| 커스텀노트 | 같은 customNote 의 다른 termId | 글로벌 random |
| 오답노트 (기간/과) | 각 termId 의 `sourceGlossaryId` 기준 같은 과 우선 | 글로벌 random |

오답노트의 경우 1차 출처가 단어별로 달라짐 (각 단어가 다른 sourceGlossaryId 일 수 있음).

#### 정답 중복 보장
모든 타입 공통. server 가 정답 텍스트 derive 후 distractors 에서 제외.

#### 풀 부족 시
글로벌에서도 3개를 못 채우면 `TEST_E001`.

### 4.3 test-results 부수 효과

| 부수 효과 | 기본 단어장 | 커스텀노트 | 오답노트 |
|---|---|---|---|
| TermProgressEntity (SRS) | ✅ 갱신 | ❌ 갱신 안 함 | ❌ 갱신 안 함 |
| WrongAnswerTermEntity (오답 자동 기록) | ✅ 정답 시 무시, 오답 시 upsert | ❌ 자동 기록 안 함 | ❌ 자동 기록 안 함 (이미 오답노트 단어, 졸업 없음) |
| GlossaryProgressEntity (단어장 진척) | ✅ learnedCount + lastLearnedAt | ❌ | ❌ |
| DailyProgressEntity (일별 활동) | ✅ | ✅ | ✅ |
| UserEntity.totalLearningTerms | ✅ 신규 학습 단어 수만큼 | ✅ 단순 +결과 수 (SRS 없으니 NEW 전환 불가, 정답 수로 가산) | ✅ 정답 수로 가산 |
| UserEntity.currentStreak / longestStreak | ✅ | ✅ | ✅ |
| `todayGoalCompleted` | 오늘 첫 활동 여부 | 동일 | 동일 |
| `levelUp` | 변동 시 채움 | 항상 null | 항상 null |
| `progressRate*` | 단어장 진척률 | 항상 0 (의미 없음) | 항상 0 |
| `levelBefore` | 단어장 레벨 | 항상 0 | 항상 0 |

#### totalLearningTerms 정책 (커스텀/오답)
SRS 가 없어 NEW → LEARNING 전환 추적 불가. 가장 단순한 정책:
- 옵션 A: 정답 수만큼 +N (학습 결과 인정)
- 옵션 B: 0 (SRS 가 없으니 학습 카운트 안 함)

옵션 A 추천 (사용자 입장에서 학습 활동 인정).

---

## 5. validation / 권한 검증

### 권한
- 기본 단어장: 누구나 접근 가능 (공용 catalog)
- 커스텀노트: `customNoteId.ownerId == userId` 검증, 아니면 `CUSTOM_NOTE_E002 FORBIDDEN`
- 오답노트: 본인 (userId) 의 wrong_answer_terms 만 자동 필터

### 필수 path param 검증
- glossaryId / customNoteId / period 모두 path 에 있음. 누락 시 404.
- period enum 외 값 → 400

### testType 제약 (distractors)
- 작성형 testType 으로 distractors 호출 → `TEST_E001`
- 모든 4종 endpoint 에 동일

---

## 6. ErrorCode

기존 + 신규 없음. Phase 2.8 코드 재활용:
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND` — 기본 단어장 미존재
- `CUSTOM_NOTE_E001 NOT_FOUND` / `CUSTOM_NOTE_E002 FORBIDDEN` — 커스텀노트 미존재 / 권한 없음
- `WRONG_ANSWER_NOTE_E002 INVALID_PERIOD` — period 값 유효성
- `TEST_E001 TEST_TYPE_NOT_AVAILABLE` — 단어 부족 / 작성형으로 distractors 호출
- `TEST_E002 TEST_RESULT_INVALID` — termId 가 노트에 속하지 않음

---

## 7. SuccessCode

Phase 2.8 의 `CARD_S001 LIST_CARDS`, `TEST_S001 LIST_DISTRACTORS`, `TEST_S002 SUBMIT_TEST` 재활용. 12개 endpoint 모두 같은 SuccessCode 사용.

(노트 타입별 코드 신설하지 않음 — 의미 같음)

---

## 8. 도메인 패키지 영향

### 신규
- `domain/user/customNote/controller/CustomNoteTestApi.kt` 또는 기존 CustomNoteApi 에 추가
- `domain/user/wrongAnswerNote/controller/` 에 by-period / by-glossary 별 endpoint 추가
- `domain/dictionary/glossary/controller/` 또는 기존 dictionary 측에 cards/distractors/test-results 추가

또는 노트 타입별 controller 신설하되 service 는 통합:
- 신설: `CardService` (이미 있음, 분기 추가)
- 신설: `TestService` (이미 있음, 분기 추가)
- 또는 service 도 노트 타입별 분리

#### 구조 제안

```
domain/
├── card/                         # 기본 단어장 + 커스텀 + 오답 통합 서비스
│   ├── controller/  GlossaryCardApi, CustomNoteCardApi, WrongAnswerCardApi  (각 4-3-3개 endpoint)
│   ├── dto/         CardsRequest (공통), CardsResponse, Card
│   ├── service/     CardSampler (interface + 3구현체 DefaultCardSampler/CustomCardSampler/WrongAnswerCardSampler)
│   │                  CardService (sampler 위임 + 공통 응답 조립)
│   └── ...
├── test/                         # 동일 패턴
│   ├── controller/  3종
│   ├── service/     DistractorBuilder, TestSubmitter (각 노트 타입별)
│   └── ...
└── progress/
    └── ProgressUpdateService (옵션 추가: includeSrs Boolean)
```

또는 단순화 — 각 노트 도메인 안에 controller 만 두고 service 는 통합:
- `card/` 의 CardService 가 모든 sampling 처리 (path param 으로 분기)
- `test/` 의 TestService 가 모든 distractors / submit 처리

복잡도 vs 분리 트레이드오프. 구현 시 확정.

### ProgressUpdateService 시그니처 변경
```kotlin
@Transactional
fun updateProgress(
    userId: UUID,
    events: List<ProgressEvent>,
    includeSrs: Boolean = true,      // 커스텀/오답 호출 시 false
    glossaryContext: Long? = null    // GlossaryProgress 갱신 대상. 커스텀/오답은 null
)
```

또는 두 메서드 분리 (`updateProgress` + `updateProgressWithoutSrs`).

---

## 9. 결정 누적

| 항목 | 결정 |
|---|---|
| URL 전략 | 정통 REST sub-resource 패턴 (옵션 B). noteType polymorphic body 폐기 |
| Endpoint 수 | cards 4 + distractors 4 + test-results 4 = 12개 |
| cards body | `{ testType, count }` (공통, path 로 컨텍스트) |
| distractors body | `{ testType, termIds }` (공통) |
| test-results body | `{ testType, results }` (공통) |
| SRS 갱신 | 기본 단어장만 수행, 커스텀/오답 제외 |
| streak / DailyProgress / totalLearningTerms | 모든 노트 타입 갱신 (학습 활동 인정) |
| levelUp / progressRate / levelBefore | 기본 단어장만 의미 있음, 커스텀/오답 항상 null 또는 0 |
| 오답노트 졸업 | 없음. 정답 처리해도 wrong_answer_terms 그대로 유지 |
| 오답 자동 기록 | 기본 단어장만. 커스텀/오답 테스트에서는 오답 발생해도 wrong_answer_terms 갱신 안 함 |
| 커스텀노트 권한 | ownerId == userId 검증 |
| 오답노트 진입 | by-period 또는 by-glossary 필터 필수 (경로 분리) |
| distractors 출처 (오답노트) | 각 단어의 sourceGlossaryId 기준 같은 과 우선 |
| distractors 출처 (커스텀) | 같은 customNote 우선 + 부족 시 글로벌 보충 |
| Service 통합 vs 분리 | CardService / TestService 통합 후 path 별 분기 (구현 시 sampler 추출 가능) |

---

## 10. 구현 순서

1. ✓ spec md 작성
2. v2-roadmap.md 2.10 단계 등록 + 결정 누적 표 보강
3. Repository 신설 (custom_note_terms sampling, wrong_answer_terms sampling by period / by glossary)
4. ProgressUpdateService 시그니처 확장 (includeSrs / glossaryContext 옵션)
5. CardService 분기 로직 (3 sampler)
6. TestService distractors 분기 로직
7. TestService submit 분기 로직 (커스텀/오답 시 progress 갱신 정책)
8. Controller 신설 12개 endpoint (또는 기존 controller 확장)
9. Phase 2.8 의 기존 `/v2/cards`, `/v2/test/distractors`, `/v2/test/submit` 폐기 (기본 단어장도 sub-resource URL 로 통일)
10. SuccessCode/ErrorCode 점검 (변경 없음 예상)
11. iOS 전달 메시지 갱신
12. v2-roadmap.md 2.10 상태 done 처리

---

## 11. 미확정 / 후속 검토

- 응답 envelope 의 `glossary` 필드 처리 방식 (커스텀/오답에서 null vs 별도 필드)
- 커스텀노트 테스트의 totalLearningTerms 가산 정책 (옵션 A 정답 수만큼 가산 채택 가정)
- 오답노트 by-period 와 by-glossary 둘 다 동시 적용 (현재는 양자택일)
- 커스텀노트 sampling 의 정렬 (createdAt 순 vs random)
- 카드/테스트 분리 정책 (cards 가 N개 단어 sampling 한 뒤 termIds 를 distractors / test-results 에 전달하는 일관 정책은 유지)
