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
- endpoint 수 증가 (cards 4개 + distractors 4개 + submit 4개 = 12개)
- Controller 는 thin, Service 내부에 sampling 분기 로직 모음

### Singleton vs Collection 표현
- 커스텀노트, 오답노트 모두 singular URL 로 통일 — `/v2/custom-note`, `/v2/wrong-answer-note`
- 기존 `/v2/custom-notes` plural 도 본 phase 에서 singular 로 변경 (breaking change, iOS 호출 코드 전부 수정)
- 동사 `/submit` 유지 (cards/distractors 는 조회성, submit 만 실제 mutation 이라 동사가 의미 명확)

---

## 2. 전체 endpoint 일람

### 학습 카드 조회

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/cards` |
| 커스텀노트 | `POST /v2/custom-note/{customNoteId}/cards` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/cards` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/cards` |

### 다지선다 오답 보기 생성

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/distractors` |
| 커스텀노트 | `POST /v2/custom-note/{customNoteId}/distractors` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/distractors` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/distractors` |

### 테스트 결과 제출

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/glossaries/{glossaryId}/submit` |
| 커스텀노트 | `POST /v2/custom-note/{customNoteId}/submit` |
| 오답노트 (기간) | `POST /v2/wrong-answer-note/by-period/{period}/submit` |
| 오답노트 (과) | `POST /v2/wrong-answer-note/by-glossary/{glossaryId}/submit` |

### Path 파라미터 정의
- `glossaryId`: Long, 기본 단어장 id (오답노트 by-glossary 에서는 sourceGlossaryId)
- `customNoteId`: Long, 커스텀노트 id (사용자 소유 검증 필수)
- `period`: enum `TODAY | WEEK | MONTH | ALL` (오답노트 by-period 기간)

---

## 3. Request / Response 모양

context id (glossaryId / customNoteId / period) 는 path 전용. body 에서 제거하여 4종 endpoint 가 동일 body DTO 공유.

### 3.1 cards 요청 / 응답

**요청 body (4종 공통)**
```json
{ "testType": "MEANING_TO_TERM", "count": 30 }
```

**응답 body (4종 공통)**
```json
{
  "cards": [
    {
      "termId": 101,
      "spelling": "Electrocardiogram",
      "meaning": "심전도",
      "abbreviation": "ECG",
      "explanation": "심장 활동의 전기적 변화를 기록한 그래프",
      "bookmarked": false,
      "morphemes": [
        { "id": 11, "spelling": "electro", "meaning": "전기", "order": 0, "highlight": { "start": 0, "length": 7 } }
      ]
    }
  ]
}
```
- 응답 envelope 에 context id (glossaryId 등) 노출하지 않음. 클라이언트가 호출한 URL 로 컨텍스트 인지

### 3.2 distractors 요청 / 응답

**요청 body (4종 공통)**
```json
{ "testType": "MC_PICK_TERM", "termIds": [101, 102, 205] }
```

**응답 body (4종 공통)**
```json
{
  "distractorsByTermId": {
    "101": ["Hemoglobin", "Hematocrit", "Erythrocyte"],
    "102": ["Heart Rate", "Blood Pressure", "Body Temperature"],
    "205": ["Magnetic Resonance Imaging", "Computed Tomography", "Positron Emission Tomography"]
  }
}
```

### 3.3 submit 요청 / 응답

**요청 body (4종 공통)**
```json
{
  "testType": "MEANING_TO_TERM",
  "results": [
    { "termId": 101, "correct": true, "userAnswer": "Electrocardiogram" },
    { "termId": 102, "correct": false, "userAnswer": "Hemo" }
  ]
}
```

**응답 DTO 2개 분리**

기본 단어장 submit 응답 — `SubmitGlossaryTestResponse`
```json
{
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
```

커스텀노트 / 오답노트 submit 응답 — `SubmitNoteTestResponse`
```json
{
  "correctCount": 18,
  "incorrectCount": 2,
  "recordedIncorrectCount": 2
}
```
- `recordedIncorrectCount`: 이번 제출로 wrong_answer_terms 에 신규 기록 또는 누적된 단어 수
- 커스텀/오답은 진도 트래킹 안 함 (방식 B 채택) 이라 `todayGoalCompleted`, `glossary` 필드 없음

---

## 4. noteType 별 동작 차이

### 4.1 cards 단어 sampling

| 노트 타입 | 데이터 출처 | NEW 우선 정렬 |
|---|---|---|
| 기본 단어장 | `glossary_terms` JOIN `term_progresses` | ✅ (기존 SRS 정책) |
| 커스텀노트 | `custom_note_terms` (소유 검증) | ❌ (SRS 무관) |
| 오답노트 (기간) | `wrong_answer_terms` (user + 기간 필터, deletedAt IS NULL) | ❌ |
| 오답노트 (과) | `wrong_answer_terms` (user + sourceGlossaryId 필터, deletedAt IS NULL) | ❌ |

#### testType 별 필터링 (공통)
- `WRITE_ABBREVIATION` / `MC_PICK_ABBR` → abbreviation 가진 단어만
- 그 외 → 필터 없음

#### count 부족 케이스 (방식 B, 4종 노트 타입 공통)
- 데이터 0개 → `TEST_E001 TEST_TYPE_NOT_AVAILABLE`
- 데이터 < count → `TEST_E001` (서버 안전망. 정상 흐름에서는 클라이언트가 사전 제한)
- 데이터 ≥ count → 정확히 count 개 반환

"가능한 만큼만 반환" 정책은 폐기. 클라이언트는 사전에 가용 단어 수를 알아야 함 (4.4 참고).

### 4.2 distractors 출처

| 노트 타입 | 1차 출처 | 2차 보충 |
|---|---|---|
| 기본 단어장 | 같은 glossary 의 다른 termId | 글로벌 random |
| 커스텀노트 | 같은 customNote 의 다른 termId | 글로벌 random |
| 오답노트 (기간) | 각 termId 의 `sourceGlossaryId` 기준 같은 과 우선 (sourceGlossaryId 가 null 이면 글로벌만) | 글로벌 random |
| 오답노트 (과) | 같은 sourceGlossaryId 의 다른 단어 우선 | 글로벌 random |

오답노트의 경우 1차 출처가 단어별로 달라짐 (각 단어가 다른 sourceGlossaryId 일 수 있음).

#### 정답 중복 보장
모든 타입 공통. server 가 정답 텍스트 derive 후 distractors 에서 제외.

#### 풀 부족 시
글로벌에서도 3개를 못 채우면 `TEST_E001`.

### 4.3 submit 부수 효과 (방식 B 채택)

커스텀/오답 테스트는 학습 진도 시스템 전반에 반영 안 함. 단 오답 자동 기록은 별개 트랙으로 4종 모두 갱신.

| 부수 효과 | 기본 단어장 | 커스텀노트 | 오답노트 |
|---|---|---|---|
| TermProgressEntity (SRS) | ✅ 갱신 | ❌ | ❌ |
| GlossaryProgressEntity (단어장 진척) | ✅ learnedCount + lastLearnedAt | ❌ | ❌ |
| WrongAnswerTermEntity (오답 자동 기록) | ✅ 정답 시 무시, 오답 시 신규 INSERT 또는 누적 | ✅ 동일 (sourceGlossaryId = null) | ✅ 기존 row 의 totalIncorrectCount += 1, incorrectAt 갱신 |
| DailyProgressEntity (일별 활동) | ✅ | ❌ | ❌ |
| UserEntity.totalLearningTerms | ✅ NEW → LEARNING 전환분 | ❌ | ❌ |
| UserEntity.currentStreak / longestStreak | ✅ | ❌ | ❌ |
| 응답 `todayGoalCompleted` | 의미 있음 (오늘 첫 활동 여부) | 응답 없음 | 응답 없음 |
| 응답 `glossary` | 값 채움 | 응답 없음 | 응답 없음 |
| 응답 `levelUp` / `progressRate` / `levelBefore` | 의미 있음 | 응답 없음 | 응답 없음 |
| 응답 `recordedIncorrectCount` | 응답 없음 | 의미 있음 | 의미 있음 |

#### 오답 자동 기록 세부 (4종 공통 트랙)
- **기본 단어장**: 신규 단어면 `firstIncorrect()` 로 INSERT, 기존이면 `recordIncorrect()` 로 누적. `sourceGlossaryId = 현재 glossaryId`
- **커스텀노트**: 신규/기존 동일 처리. **`sourceGlossaryId = null`** — 기획 의도상 사용자가 과를 선택할 수 없는 단어장이므로 출처 과를 강제로 채우지 않음. 결과적으로 오답노트 by-glossary 필터에는 잡히지 않고 by-period 에만 노출
- **오답노트**: 이미 wrong_answer_terms 에 있는 단어. 기존 row 의 `totalIncorrectCount += 1`, `incorrectAt` 갱신. 졸업 정책 없음 — 정답이어도 row 유지

### 4.4 가용 단어 수 제공 (방식 B 전제)

클라이언트가 cards 호출 전에 testType 별 가용 단어 수를 알아야 출제 개수 selector 의 max 값을 결정할 수 있음. 단어장/노트 상세 endpoint 응답에 `availableCounts` 필드 신규 추가.

```json
"availableCounts": {
  "MEANING_TO_TERM": 30,
  "TERM_TO_MEANING": 30,
  "WRITE_ABBREVIATION": 12,
  "MC_PICK_TERM": 30,
  "MC_PICK_MEANING": 30,
  "MC_PICK_ABBR": 12
}
```

영향 받는 endpoint
- 기본 단어장 상세
- 커스텀노트 상세
- 오답노트 by-period / by-glossary 진입 응답 (동적 카운트)

---

## 5. validation / 권한 검증

### 권한
- 기본 단어장: 누구나 접근 가능 (공용 catalog)
- 커스텀노트: `customNoteId.ownerId == userId` 검증, 아니면 `CUSTOM_NOTE_E002 FORBIDDEN`
- 오답노트: 본인 (userId) 의 wrong_answer_terms 만 자동 필터

### 필수 path param 검증
- glossaryId / customNoteId / period 모두 path 에 있음. 누락 시 404
- period enum 외 값 → `WRONG_ANSWER_NOTE_E002`

### testType 제약 (distractors)
- 작성형 testType 으로 distractors 호출 → `TEST_E001`
- 모든 4종 endpoint 에 동일

---

## 6. ErrorCode

기존 + 신규 없음. Phase 2.8 코드 재활용:
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND` — 기본 단어장 미존재
- `CUSTOM_NOTE_E001 NOT_FOUND` / `CUSTOM_NOTE_E002 FORBIDDEN` — 커스텀노트 미존재 / 권한 없음
- `WRONG_ANSWER_NOTE_E002 INVALID_PERIOD` — period 값 유효성
- `TEST_E001 TEST_TYPE_NOT_AVAILABLE` — 단어 부족 (0개 또는 count 미달) / 작성형으로 distractors 호출 / distractor 풀 부족
- `TEST_E002 TEST_RESULT_INVALID` — termId 가 노트에 속하지 않음

---

## 7. SuccessCode

Phase 2.8 의 `CARD_S001 LIST_CARDS`, `TEST_S001 LIST_DISTRACTORS`, `TEST_S002 SUBMIT_TEST` 재활용. 12개 endpoint 모두 같은 SuccessCode 사용.

(노트 타입별 코드 신설하지 않음 — 의미 같음)

---

## 8. 도메인 패키지 영향

### 신규 / 변경 controller
- `domain/dictionary/glossary/controller/GlossaryCardApi` 등 — 기본 단어장 sub-resource endpoint (cards / distractors / submit)
- `domain/user/customNote/controller/CustomNoteApi` — URL plural → singular 전체 변경, 신규 cards/distractors/submit endpoint 추가
- `domain/user/customNote/controller/CustomNoteShareApi` — URL plural → singular 변경
- `domain/user/wrongAnswerNote/controller/WrongAnswerNoteApi` — 신규 by-period / by-glossary 별 cards / distractors / submit endpoint 추가

### 신규 service / sampler
구조 제안 (sampler 인터페이스 + 3구현체):
```
domain/
├── card/
│   ├── controller/         GlossaryCardApi, CustomNoteCardApi, WrongAnswerCardApi
│   ├── dto/                CardsRequest (공통), CardsResponse (공통), Card
│   ├── service/
│   │   ├── CardSampler (interface)
│   │   ├── GlossaryCardSampler
│   │   ├── CustomNoteCardSampler
│   │   ├── WrongAnswerCardSampler
│   │   └── CardService (sampler 위임 + 공통 응답 조립)
├── test/
│   ├── controller/         3종 + 4종
│   ├── service/
│   │   ├── DistractorBuilder (노트 타입별)
│   │   ├── GlossaryTestSubmitter
│   │   ├── NoteTestSubmitter (커스텀/오답 공용)
│   │   └── TestService
```

또는 단순화 — 각 노트 도메인 안에 controller 만 두고 service 는 통합:
- `card/` 의 CardService 가 모든 sampling 처리 (path param 으로 분기)
- `test/` 의 TestService 가 모든 distractors / submit 처리

복잡도 vs 분리 트레이드오프. 구현 시 확정.

### ProgressUpdateService 변경
- 메서드 1개만 유지: `updateProgressWithSrs(userId, events, glossaryId): SrsProgressSnapshot`
- 커스텀/오답 submit 흐름은 ProgressUpdateService 호출 안 함 — 진도 시스템 전반에 안 닿음
- `updateProgressActivityOnly` 같은 별도 메서드 불필요

### WrongAnswerNoteService 변경
- `recordIncorrects(userId, termIds, sourceGlossaryId: Long?, occurredAt)` 시그니처
- sourceGlossaryId 가 nullable 이라 커스텀노트 호출 시 null 전달 가능
- 기본 단어장: sourceGlossaryId = 현재 glossaryId
- 커스텀노트: sourceGlossaryId = null
- 오답노트 테스트: 기존 row 누적만 (recordIncorrect 호출, sourceGlossaryId 는 기존 값 유지)

### 영향 받는 기존 응답
- 단어장 상세, 커스텀노트 상세, 오답노트 진입 응답 → `availableCounts` 필드 추가

---

## 9. 결정 누적

| 항목 | 결정 |
|---|---|
| URL 전략 | 정통 REST sub-resource 패턴 (옵션 B). noteType polymorphic body 폐기 |
| URL 동사 | submit 유지 (test-results 폐기). cards/distractors 는 명사 |
| URL 단/복수 | 커스텀노트 도메인 전부 singular 통일 (`/v2/custom-note`). 오답노트는 기존 singular 유지 |
| Endpoint 수 | cards 4 + distractors 4 + submit 4 = 12개 |
| cards body | `{ testType, count }` (공통, path 로 컨텍스트) |
| distractors body | `{ testType, termIds }` (공통) |
| submit body | `{ testType, results }` (공통) |
| 응답 envelope (cards) | context id 노출 안 함, `{ cards: [...] }` 만 |
| 응답 envelope (submit) | DTO 분리 — `SubmitGlossaryTestResponse` (기본 전용) / `SubmitNoteTestResponse` (커스텀/오답 공용) |
| 진도 트래킹 (SRS / GlossaryProgress / DailyProgress / totalLearningTerms / streak) | 기본 단어장만 갱신. 커스텀/오답은 진도 시스템 전반에 반영 X (방식 B) |
| 오답 자동 기록 | 4종 모두 갱신 (진도와 별개 트랙). 커스텀노트는 sourceGlossaryId = null |
| 오답노트 졸업 | 없음. 정답 처리해도 wrong_answer_terms 그대로 유지, totalIncorrectCount 만 += 1 |
| 커스텀노트 권한 | ownerId == userId 검증 |
| 오답노트 진입 | by-period 또는 by-glossary 필터 필수 (경로 분리) |
| distractors 출처 (오답노트) | 각 단어의 sourceGlossaryId 기준 같은 과 우선 |
| distractors 출처 (커스텀) | 같은 customNote 우선 + 부족 시 글로벌 보충 |
| cards 단어 부족 정책 | 4종 공통 — count > available 시 `TEST_E001` (방식 B). 가능한 만큼만 반환 정책 폐기 |
| 가용 단어 수 제공 | 단어장/노트 상세 응답에 `availableCounts` 필드 추가. testType 별 카운트 |
| ProgressUpdateService 메서드 | `updateProgressWithSrs` 1개만 유지. 커스텀/오답은 호출 안 함 |
| Service 통합 vs 분리 | CardService / TestService 통합 후 path 별 분기 (구현 시 sampler 추출 가능) |

---

## 10. 구현 순서

1. ✓ spec md 작성
2. ✓ v2-roadmap.md 2.10 단계 등록 + 결정 누적 표 보강
3. Repository 신설 — `custom_note_terms` sampling, `wrong_answer_terms` sampling by period / by glossary
4. Repository — testType 별 availableCount 집계 쿼리 (각 노트 상세에서 사용)
5. 단어장 / 커스텀노트 / 오답노트 상세 응답에 `availableCounts` 필드 추가
6. `CustomNoteApi`, `CustomNoteShareApi` URL plural → singular 변경 (기존 endpoint 전체)
7. CardService 분기 로직 (sampler 3종)
8. TestService distractors 분기 로직
9. WrongAnswerNoteService.recordIncorrects 시그니처 확장 (sourceGlossaryId nullable)
10. TestService submit 분기 로직 — 기본 단어장은 ProgressUpdateService.updateProgressWithSrs 호출, 커스텀/오답은 채점 집계 + 오답 기록만
11. 응답 DTO 분리 — `SubmitGlossaryTestResponse` / `SubmitNoteTestResponse`
12. Controller 신설 12개 endpoint (또는 기존 controller 확장)
13. Phase 2.8 의 기존 `/v2/cards`, `/v2/test/distractors`, `/v2/test/submit` 폐기 (기본 단어장도 sub-resource URL 로 통일)
14. SuccessCode / ErrorCode 점검 (변경 없음 예상)
15. iOS 전달 메시지 갱신
16. v2-roadmap.md 2.10 상태 done 처리

---

## 11. 미확정 / 후속 검토

- 커스텀노트 sampling 의 정렬 (createdAt 순 vs random)
- 오답노트 by-period 와 by-glossary 동시 적용 (현재는 양자택일)
- `availableCounts` 응답 필드를 testType 별 풀 노출할지, 단순화 (총 단어 수 + abbreviation 보유 수) 로 갈지 (현재는 풀 노출 선택)
- Service 구조 — sampler 인터페이스 분리 vs 단일 service 안 path 분기 (구현 시 확정)
- 카드/테스트 분리 정책 (cards 가 N개 단어 sampling 한 뒤 termIds 를 distractors / submit 에 전달하는 일관 정책은 유지)
