# Phase 2.8 — 학습/테스트 분리 최종 구현 명세

학습 flow 를 **카드 phase (인지 학습)** 와 **테스트 phase (정오 평가)** 로 분리.
card / test / progress 도메인 세 패키지로 책임 분담.

---

## 1. 핵심 원칙

| 원칙 | 내용 |
|---|---|
| 카드 phase 중 backend 호출 없음 | 시작 시 1회 (`/v2/cards`) 받아 N장 자체 진행 |
| 테스트 phase 중 backend 호출 없음 | MC 만 1회 (`/v2/test/distractors`), 정오 판정은 클라이언트 |
| 진도 갱신은 submit 시점만 | streak / totalLearningTerms / SRS / 오답노트 모두 submit 에서 처리 |
| 중도 종료 = 진도 반영 없음 | 부분 submit 없음, 다음 진입 시 Home 부터 새로 시작 |
| 카드/테스트 단어 일치 | client 가 cards 받은 termIds 를 distractors / submit 에 전달 |
| stateless | sessionId 없음 |

---

## 2. API 호출 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│ 학습하기 (Home)                                                      │
│  단어장 + 카드수 + 테스트유형 선택, API 호출 없음                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ "학습 시작"
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 테스트 유형 선택 모달 (defaultTestType 없을 때만)                     │
│  - GET  /v2/users/me                  (defaultTestType 확인)         │
│  - PUT  /v2/users/me/preferences/test-type  ("다음부터 묻지 않기")    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 카드 phase 시작                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ POST /v2/cards                                           │ │
│  │   { glossaryId, testType, count }                              │ │
│  │      ↓                                                          │ │
│  │   { cards: [{ termId, spelling, meaning, abbreviation }, ...] }│ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ N장 카드 자체 진행 (호출 없음)
                               │ 중도 이탈 가능 (부수효과 없음)
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 학습 완료 화면 → "테스트 시작하기"                                    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
        작성형 testType               다지선다 testType
                │                             │
                │            ┌────────────────▼─────────────────────┐
                │            │ POST /v2/test/distractors            │
                │            │   { testType, termIds }              │
                │            │      ↓                                │
                │            │   { distractorsByTermId: {            │
                │            │       101: ["...", "...", "..."],    │
                │            │       102: ["...", "...", "..."] } } │
                │            └────────────────┬─────────────────────┘
                │                             │
                └──────────────┬──────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 테스트 phase 진행                                                     │
│   - 작성형: cards 단어로 prompt / correctAnswer 자체 derive           │
│   - 다지선다: cards 의 정답 + distractors 3개 shuffle → choices 4개  │
│   - 정오 판정 클라이언트                                              │
│   - 진행 중 호출 없음                                                 │
│   - 중도 종료 = 어떤 API 도 호출 안 함                                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 모든 문제 완료
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 테스트 결과 일괄 제출                                                 │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ POST /v2/test/submit                                           │ │
│  │   { glossaryId, testType, results: [{ termId, correct,         │ │
│  │                                       userAnswer }, ...] }     │ │
│  │      ↓                                                          │ │
│  │   { correctCount, incorrectCount, todayGoalCompleted,          │ │
│  │     glossary: { progressRateBefore/After, levelBefore,         │ │
│  │                 levelUp } }                                    │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ glossary.levelUp != null → 레벨업 화면
                               │ glossary.levelUp == null → 일반 결과 화면
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 결과 / 레벨업 화면 → "테스트 종료하기" → Home                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 호출 수 정리

| 시나리오 | 호출 |
|---|---|
| 작성형 테스트 1회 | cards + submit (+ 옵션 users/me, preferences) = 2 ~ 4 |
| 다지선다 테스트 1회 | cards + distractors + submit (+ 옵션) = 3 ~ 5 |

---

## 3. TestType enum (7종)

```kotlin
enum class TestType {
    TERM_TO_MEANING,       // 용어 → 의미 작성 (한글)
    MEANING_TO_TERM,       // 의미 → 용어 작성 (영문)
    LISTEN_AND_WRITE,      // TTS → 영문 작성
    WRITE_ABBREVIATION,    // 용어 → 약어 작성
    MC_PICK_TERM,          // 의미 → 용어 4지선다
    MC_PICK_MEANING,       // 용어 → 의미 4지선다
    MC_PICK_ABBR           // 용어 → 약어 4지선다
}

// 분류 헬퍼
fun TestType.isMultipleChoice(): Boolean = this in listOf(
    MC_PICK_TERM, MC_PICK_MEANING, MC_PICK_ABBR
)
fun TestType.requiresAbbreviation(): Boolean = this in listOf(
    WRITE_ABBREVIATION, MC_PICK_ABBR
)
```

### testType 별 prompt / correctAnswer 매핑 (client 측 derive)

| testType | prompt | correctAnswer | choices |
|---|---|---|---|
| TERM_TO_MEANING | `spelling` | `meaning` | (없음) |
| MEANING_TO_TERM | `meaning` | `spelling` | (없음) |
| LISTEN_AND_WRITE | `spelling` (TTS) | `spelling` | (없음) |
| WRITE_ABBREVIATION | `spelling` | `abbreviation` | (없음) |
| MC_PICK_TERM | `meaning` | `spelling` | 정답 + distractors 3 shuffled |
| MC_PICK_MEANING | `spelling` | `meaning` | 정답 + distractors 3 shuffled |
| MC_PICK_ABBR | `spelling` | `abbreviation` | 정답 + distractors 3 shuffled |

---

## 4. API 명세

### 4.1 PUT /v2/users/me/preferences/test-type

**요청**
```json
{ "defaultTestType": "MEANING_TO_TERM" }
```
- `defaultTestType` null 허용 (선택 해제 → 다음 진입 시 모달 강제)

**응답 (200)**
```json
{
  "code": "USER_S030",
  "message": "기본 테스트 유형을 변경했습니다",
  "data": { "defaultTestType": "MEANING_TO_TERM" }
}
```

**ErrorCode**: `USER_E010 ACCOUNT_NOT_FOUND`, `USER_E005 ALREADY_DELETED`

---

### 4.2 POST /v2/cards

카드 phase 데이터 제공. testType 받아서 출제 가능한 단어만 sampling (도메인 누수 임시 허용, 추후 리팩토링 예정).

**요청**
```json
{
  "glossaryId": 12,
  "testType": "MEANING_TO_TERM",
  "count": 30
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| glossaryId | Long | positive |
| testType | TestType | 7종 중 하나 |
| count | Int | 1 ~ 50 |

**응답 (200)**
```json
{
  "code": "CARD_S001",
  "message": "학습 카드를 조회했습니다",
  "data": {
    "glossaryId": 12,
    "cards": [
      {
        "termId": 101,
        "spelling": "Electrocardiogram",
        "meaning": "심전도",
        "abbreviation": "ECG"
      },
      {
        "termId": 102,
        "spelling": "Hemoglobin",
        "meaning": "혈색소",
        "abbreviation": null
      }
    ]
  }
}
```

#### sampling 알고리즘 (단일 SQL, 최고 성능)

```sql
SELECT t.id AS termId, t.spelling, t.meaning, t.abbreviation
FROM glossary_terms gt
JOIN terms t ON t.id = gt.term_id
LEFT JOIN term_progresses tp
       ON tp.user_id = :userId
      AND tp.glossary_id = :glossaryId
      AND tp.term_id = t.id
WHERE gt.glossary_id = :glossaryId
  AND t.deleted_at IS NULL
  AND (:requireAbbreviation = FALSE
       OR (t.abbreviation IS NOT NULL AND t.abbreviation <> ''))
ORDER BY
  (tp.status IS NULL OR tp.status = 'NEW') DESC,
  RAND()
LIMIT :count
```

- NEW 정의: `term_progresses` row 없음 OR `status = 'NEW'`
- NEW 우선 → 부족하면 학습된 단어 보충
- 같은 그룹 내 random
- 단어장 단어 < count → 가능한 만큼만

#### testType → SQL 파라미터 매핑

| testType | requireAbbreviation |
|---|---|
| WRITE_ABBREVIATION, MC_PICK_ABBR | true |
| 그 외 | false |

**SuccessCode**: `CARD_S001 LIST_CARDS`
**ErrorCode**: `DICTIONARY_E001 GLOSSARY_NOT_FOUND`, `TEST_E001 TEST_TYPE_NOT_AVAILABLE` (필터 후 0개)

---

### 4.3 POST /v2/test/distractors

다지선다 보기 풀 제공. **MC_* testType 일 때만 호출**.

**요청**
```json
{
  "glossaryId": 12,
  "testType": "MC_PICK_TERM",
  "termIds": [101, 102, 205]
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| glossaryId | Long | positive |
| testType | TestType | MC_PICK_TERM / MC_PICK_MEANING / MC_PICK_ABBR 만 허용 |
| termIds | List<Long> | non-empty |

**응답 (200)**
```json
{
  "code": "TEST_S001",
  "message": "다지선다 보기를 조회했습니다",
  "data": {
    "distractorsByTermId": {
      "101": ["Hemoglobin", "Hematocrit", "Erythrocyte"],
      "102": ["Electrocardiogram", "Blood Pressure", "Computed Tomography"],
      "205": ["Hemoglobin", "Bilirubin", "Creatinine"]
    }
  }
}
```

#### distractors 생성 알고리즘

1. termId 별로 server 가 그 단어의 정답 텍스트 derive (testType 따라 spelling/meaning/abbreviation)
2. **같은 단어장 우선** + 부족 시 다른 단어장에서 random 보충
   - 자기 termId 제외
   - 정답 텍스트와 중복되는 텍스트 제외 (정답 중복 보장)
   - distinct 처리
3. 3개 채울 때까지 sampling
4. 전체 사전에서도 3개를 못 채우면 `TEST_E001 TEST_TYPE_NOT_AVAILABLE`

#### 정답 중복 방지 범위
- 각 문제 단위로만 적용 (termId 101 의 distractors 에 101 의 정답 텍스트 없음)
- 다른 termId 의 정답이 distractors 에 포함되는 것은 허용 (101 의 정답 = "ECG" 가 102 의 distractor 에 나와도 OK)

**SuccessCode**: `TEST_S001 LIST_DISTRACTORS`
**ErrorCode**:
- `TEST_E001 TEST_TYPE_NOT_AVAILABLE` — 작성형 testType / distractors 풀 부족
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND`

---

### 4.4 POST /v2/test/submit

테스트 결과 일괄 제출. 모든 진도/SRS/오답노트/streak 갱신.

**요청**
```json
{
  "glossaryId": 12,
  "testType": "MEANING_TO_TERM",
  "results": [
    { "termId": 101, "correct": true,  "userAnswer": "Electrocardiogram" },
    { "termId": 102, "correct": false, "userAnswer": "Hemo" }
  ]
}
```

| 필드 | 제약 |
|---|---|
| results | non-empty, 순서 무관 |
| results[].termId | glossary 에 속한 termId (검증 통과 조건) |
| results[].correct | client 판정 결과 |
| results[].userAnswer | 빈 문자열 허용 (오답노트 표시용) |

#### client 정오 판정 규칙

작성형:
- 앞뒤 trim
- 영문 대소문자 무시
- 연속 공백 1개로 정규화
- correctAnswer 와 일치하면 정답

다지선다:
- 선택한 보기 텍스트 == correctAnswer 면 정답

**응답 (200)**
```json
{
  "code": "TEST_S002",
  "message": "테스트 결과를 반영했습니다",
  "data": {
    "correctCount": 25,
    "incorrectCount": 5,
    "todayGoalCompleted": true,
    "glossary": {
      "glossaryId": 12,
      "progressRateBefore": 80.0,
      "progressRateAfter": 85.0,
      "levelBefore": 4,
      "levelUp": { "from": 4, "to": 5 }
    }
  }
}
```

`glossary.levelUp` 은 레벨 상승 시 object, 변화 없으면 **응답에서 키 자체가 제외됨** (Jackson `default-property-inclusion: non_null` 글로벌 설정 유지). 클라이언트는 키 존재 여부로 레벨업 여부 판정.

#### 처리 부수 효과 (server 내부)

| 대상 | 변경 |
|---|---|
| TermProgressEntity | 각 term SRS 갱신 (정답 → nextReviewAt 연장 / 오답 → 단축) |
| WrongAnswerTermEntity | 오답 term upsert (incorrectAt 갱신, 누적 카운트 +1) |
| GlossaryProgressEntity | learnedCount 증가, lastLearnedAt 갱신 |
| DailyProgressEntity | 오늘 행의 learnedCount/completedCount 갱신 |
| UserEntity.totalLearningTerms | 신규 학습된 단어 수만큼 누적 |
| UserEntity.currentStreak/longestStreak | 오늘 첫 활동 + 어제 활성 여부 기반 갱신 |

`todayGoalCompleted`: 오늘 DailyProgress 가 비활성 → 활성 전환 시 true (사실상 "오늘 첫 테스트" 의미. 카드 phase 가 호출 없으므로 DailyProgress 활성 = 테스트 submit 발생 = 오늘 첫 테스트)

**SuccessCode**: `TEST_S002 SUBMIT_TEST`
**ErrorCode**:
- `TEST_E002 TEST_RESULT_INVALID` — termId 가 glossary 외부 / results 비어있음
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND`

---

## 5. 데이터 모델

### UserEntity 컬럼 (V40 마이그레이션)
```sql
ALTER TABLE users ADD COLUMN default_test_type VARCHAR(32) NULL;
```
- null 이면 다음 진입 시 모달 강제

---

## 6. 도메인 패키지

```
domain/
├── card/                           (신규)
│   ├── controller/  CardApi, CardController
│   ├── dto/         CardsRequest, CardsResponse, Card
│   └── service/     CardService
│
├── test/
│   ├── controller/  TestApi, TestController
│   ├── dto/         DistractorsRequest, DistractorsResponse,
│   │                SubmitTestRequest, TestResultItem,
│   │                SubmitTestResponse, TestGlossarySnapshot, LevelUpEvent
│   ├── entity/      TestType
│   └── service/     TestService
│
└── progress/                       (learning → progress rename)
    ├── dto/         LearningAnswerEvent  (test 위임용 internal)
    ├── entity/      TermProgressEntity, GlossaryProgressEntity,
    │                DailyProgressEntity, TermProgressStatus
    ├── repository/  TermProgressRepository, GlossaryProgressRepository,
    │                DailyProgressRepository
    └── service/     ProgressUpdateService (이전 LearningCommandService),
                     SrsCalculator
```

### 의존 방향
- `card` → `progress` (단어 sampling 시 status LEFT JOIN)
- `test` → `progress` (submit 시 위임)
- `card` 와 `test` 상호 의존 없음
- `progress` 는 인프라 (다른 도메인 의존 없음)

---

## 7. SuccessCode / ErrorCode

```kotlin
enum class UserSuccess(...) : SuccessCode {
    UPDATE_DEFAULT_TEST_TYPE(HttpStatus.OK, "USER_S030", "기본 테스트 유형을 변경했습니다")
}

enum class CardSuccess(...) : SuccessCode {
    LIST_CARDS(HttpStatus.OK, "CARD_S001", "학습 카드를 조회했습니다")
}

enum class TestSuccess(...) : SuccessCode {
    LIST_DISTRACTORS(HttpStatus.OK, "TEST_S001", "다지선다 보기를 조회했습니다"),
    SUBMIT_TEST(HttpStatus.OK, "TEST_S002", "테스트 결과를 반영했습니다")
}

enum class TestError(...) : ErrorCode {
    TEST_TYPE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "TEST_E001",
        "단어장이 해당 테스트 유형의 출제 조건을 충족하지 않습니다"),
    TEST_RESULT_INVALID(HttpStatus.BAD_REQUEST, "TEST_E002",
        "테스트 결과가 유효하지 않습니다")
}
```

이전 `LEARNING_S020`, `LEARNING_S021`, `LEARNING_E020`, `LEARNING_E021` 은 미배포라 폐기.
`LearningSuccess` / `LearningError` enum class 자체 폐기.

---

## 8. 글로벌 설정 정책

### Jackson non_null 유지
```yaml
# application.yml
spring:
  jackson:
    time-zone: UTC
    default-property-inclusion: non_null
```

null 필드는 JSON 응답에서 자동 제외. 응답 payload 크기 절감 + 기존 운영 정책 유지.

**클라이언트 영향**: nullable 필드는 응답에 키가 아예 없을 수 있음. iOS Codable 정의 시 nullable 필드는 모두 `Optional` 로 선언 필요 (decoding 시 키 없음 = nil).

응답 예시에서 null 인 필드는 표기 생략하거나 명시적으로 "키 자체 제외됨" 으로 주석 표기.

---

## 9. 결정 누적

| 항목 | 결정 |
|---|---|
| 카드/테스트 분리 | card, test 별도 도메인 패키지 |
| 카드와 테스트 단어 일치 | client 가 cards termIds 를 distractors/submit 에 전달 |
| 1회 테스트 단일 유형 | 모든 문제 동일 testType |
| 출제 개수 | client 결정 (1 ~ 50) |
| 카드 sampling 알고리즘 | NEW 우선 + RAND, 단일 SQL |
| cards 의 testType 노출 | 도메인 누수 임시 허용 (추후 generic filter 로 리팩토링) |
| 다지선다 보기 출처 | 같은 단어장 우선 + 부족 시 다른 단어장 random 보충 |
| 다지선다 정답 중복 보장 | server 가 정답 derive 후 distractors 에서 정답 텍스트 제외 |
| 정답 중복 방지 범위 | 각 문제 단위 (다른 문제 정답이 distractor 로 나오는 건 OK) |
| distractors 풀 부족 시 | 전 사전에서도 못 채우면 TEST_E001 에러 |
| 정오 판정 위치 | client (server 재검증 보류) |
| default testType 저장 | UserEntity 컬럼 |
| stateless | sessionId 없음 |
| 진도 갱신 시점 | 테스트 submit 시점만 |
| 중도 종료 재진입 | Home 부터 새로 시작, 이전 테스트 없던 일 |
| submit termId 검증 | glossary 소속만 (cards 일치 검증 없음) |
| 패키지 rename | learning → progress |
| Jackson non_null | 글로벌 유지, null 필드는 응답에서 자동 제외 |
| testType prefix | TEST_* (이전 LEARNING_S020 등 폐기) |

---

## 10. 구현 순서

| 순서 | 작업 | 비고 |
|---|---|---|
| 1 | 패키지 rename `domain/learning/` → `domain/progress/` | 모든 import 갱신 |
| 2 | LearningCommandService → ProgressUpdateService rename | 시그니처 유지 |
| 3 | (Jackson non_null 글로벌 설정은 유지) | - |
| 4 | SuccessCode/ErrorCode 정리 (LearningSuccess/Error 폐기, CardSuccess/TestSuccess/TestError 신설) | 미배포라 자유 |
| 5 | `POST /v2/cards` (card 도메인 신설) | NEW 우선 SQL |
| 6 | `POST /v2/test/distractors` (test 도메인 신설) | 정답 중복 방지 |
| 7 | `POST /v2/test/submit` 갱신 (기존 코드 재사용, 신 ErrorCode 적용) | |
| 8 | 기존 `POST /v2/test/start` 폐기 | TestQuestion, TestPrompt, StartTestRequest/Response 모두 제거 |
| 9 | iOS 전달 메시지 갱신 | API 흐름, 정답 derive 로직 명시 |
| 10 | v2-roadmap.md 결정 누적 갱신 | |

---

## 11. iOS 클라이언트 구현 가이드

### 데이터 흐름

```swift
// 1. cards 호출
let cardsRequest = CardsRequest(
    glossaryId: 12,
    testType: .meaningToTerm,
    count: 30
)
let cards = await api.fetchCards(cardsRequest)  // [Card]

// 2. 카드 phase 진행 (API 호출 없음)
//    cards[i].spelling / meaning / abbreviation 으로 통합 카드 표시
//    TTS(cards[i].spelling) 음성 재생

// 3. 다지선다일 때만 distractors 호출
let distractorsMap: [Long: [String]]
if testType.isMultipleChoice {
    let req = DistractorsRequest(
        glossaryId: 12,
        testType: testType,
        termIds: cards.map { $0.termId }
    )
    distractorsMap = await api.fetchDistractors(req).distractorsByTermId
}

// 4. 테스트 phase 진행
for card in cards {
    let prompt: String
    let correctAnswer: String
    let choices: [String]?

    switch testType {
    case .termToMeaning:
        prompt = card.spelling
        correctAnswer = card.meaning
        choices = nil
    case .meaningToTerm, .listenAndWrite:
        // ...
    case .mcPickTerm:
        prompt = card.meaning
        correctAnswer = card.spelling
        choices = (distractorsMap[card.termId]! + [correctAnswer]).shuffled()
    // ...
    }

    // 사용자 입력 받기
    let userAnswer: String = ...
    let correct: Bool = compare(userAnswer, correctAnswer)
    results.append(TestResultItem(termId: card.termId, correct: correct, userAnswer: userAnswer))
}

// 5. submit
let submitResponse = await api.submitTest(SubmitTestRequest(
    glossaryId: 12,
    testType: testType,
    results: results
))

// 6. 화면 분기
if let levelUp = submitResponse.glossary.levelUp {
    showLevelUpScreen(from: levelUp.from, to: levelUp.to)
} else {
    showResultScreen(correctCount: submitResponse.correctCount, ...)
}
```

### 중도 종료
- 뒤로가기 → "테스트를 종료하시겠습니까?" → 종료 → **어떤 API 도 호출 안 함**
- results 폐기, Home 복귀

---

## 12. 후속 검토 (별도 phase)

- cards 의 testType 파라미터를 generic `filters` 로 리팩토링 (도메인 누수 해소)
- server 측 정오 재검증 (부정행위 방지). 도입 시 cards 응답에서 정답 derive 차단 정책 변경
- SRS due 우선 sampling
- 자주 틀린 단어 (3회+ 오답) 별도 endpoint
- 오답노트 모드 테스트 (테스트하기_오답 폴더) 전용 진입 flow
