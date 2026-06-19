## Phase 2.10 — 학습/테스트 기능 확장 (커스텀노트 / 오답노트)

Phase 2.8 의 학습 카드 + 다지선다 오답 생성 + 테스트 결과 제출 흐름을 기본 단어장에서 커스텀노트, 오답노트로 확장한다. 정통 REST sub-resource 패턴 + URL prefix `/v2/test/` 통일.

상세 결정 배경은 v2-roadmap.md 2.10 항목, 디자인 PNG (테스트하기_오답 폴더) 분석 결과 반영.

---

## 1. URL 전략 (옵션 B 채택)

Phase 2.8 의 `POST /v2/cards` 단일 endpoint 가 noteType 별 polymorphic body 로 분기되는 RPC 스타일을 폐기하고, URL prefix `/v2/test/` 아래 노트 타입별 리소스 path 로 분리.

### 이유
- URL 자체로 컨텍스트 명확 (어떤 노트 타입인지)
- Request body 가 단순 (`{ testType, count }` 만 공통)
- Swagger 문서가 각 endpoint 별 명확, 분기 없음
- iOS 호출 코드가 URL 만 바꾸면 됨, body 분기 없음
- `/v2/test/` prefix 로 학습/테스트 책임이 한 곳에 모임

### Singular vs Plural
- 노트 타입 path 는 plural 통일 (`glossaries`, `custom-notes`, `wrong-answer-notes`)
- 동사 `/submit` 유지 (cards/distractors 는 조회성, submit 만 실제 mutation)
- 기존 `/v2/custom-notes/*` 는 이미 plural 이라 변경 없음 (breaking change 없음)

---

## 2. 전체 endpoint 일람

### 학습 카드 조회

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/test/glossaries/{glossaryId}/cards` |
| 커스텀노트 | `POST /v2/test/custom-notes/{customNoteId}/cards` |
| 오답노트 (기간) | `POST /v2/test/wrong-answer-notes/by-period/{period}/cards` |
| 오답노트 (과) | `POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/cards` |

### 다지선다 오답 보기 생성

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/test/glossaries/{glossaryId}/distractors` |
| 커스텀노트 | `POST /v2/test/custom-notes/{customNoteId}/distractors` |
| 오답노트 (기간) | `POST /v2/test/wrong-answer-notes/by-period/{period}/distractors` |
| 오답노트 (과) | `POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/distractors` |

### 테스트 결과 제출

| 노트 타입 | URL |
|---|---|
| 기본 단어장 | `POST /v2/test/glossaries/{glossaryId}/submit` |
| 커스텀노트 | `POST /v2/test/custom-notes/{customNoteId}/submit` |
| 오답노트 (기간) | `POST /v2/test/wrong-answer-notes/by-period/{period}/submit` |
| 오답노트 (과) | `POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/submit` |

### Path 파라미터 정의
- `glossaryId`: Long, 기본 단어장 id (오답노트 by-glossary 에서는 sourceGlossaryId 의미)
- `customNoteId`: Long, 커스텀노트 id (사용자 소유 검증 필수)
- `period`: enum `TODAY | WEEK | MONTH | ALL`

---

## 3. Request / Response 모양

context id (glossaryId / customNoteId / period) 는 path 전용. body 에서 제거하여 4종 endpoint 가 동일 body DTO 공유.

### 3.1 cards 요청 / 응답

**요청 body — 기본 단어장**
```json
{ "testType": "MEANING_TO_TERM", "count": 30 }
```
- `count` 는 selector 로 정함 (10/20/30/40/50)

**요청 body — 커스텀노트 / 오답노트**
```json
{ "testType": "MEANING_TO_TERM" }
```
- `count` 없음. 노트의 전체 단어를 반환 (단어 수 고정)

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
- `todayGoalCompleted` 의미는 Phase 2.11 (daily goal) 에서 확정. Phase 2.10 시점에는 현재 로직 (오늘 첫 활동 여부) 유지

커스텀노트 / 오답노트 submit 응답 — `SubmitNoteTestResponse`
```json
{
  "correctCount": 18,
  "incorrectCount": 2
}
```
- 커스텀/오답은 진도 트래킹 안 함 (방식 B 채택) 이라 `todayGoalCompleted`, `glossary` 필드 없음

---

## 4. noteType 별 동작 차이

### 4.1 cards 단어 sampling vs 전체 조회

기본 단어장만 진짜 sampling. 커스텀/오답은 노트의 전체 단어를 정렬해서 반환 (sampling 아님).

| 노트 타입 | 데이터 출처 | 정렬 | sampling 여부 |
|---|---|---|---|
| 기본 단어장 | `glossary_terms` JOIN `term_progresses` | NEW 우선 + RAND (Phase 2.8 정책) | sampling — `LIMIT count` |
| 커스텀노트 | `custom_note_terms` (소유 검증) | createdAt ASC | 전체 조회 — LIMIT 없음 |
| 오답노트 (기간) | `wrong_answer_terms` (user + 기간 필터, deletedAt IS NULL) | RAND | 전체 조회 — LIMIT 없음 |
| 오답노트 (과) | `wrong_answer_terms` (user + sourceGlossaryId 필터, deletedAt IS NULL) | RAND | 전체 조회 — LIMIT 없음 |

커스텀/오답의 "정렬" 은 카드 노출 순서를 정하는 의미 (학습 순서). 단어를 일부만 고르는 sampling 이 아님.

#### testType 별 필터링 (공통)
- `WRITE_ABBREVIATION` / `MC_PICK_ABBR` → abbreviation 가진 단어만 (4종 노트 타입 공통)
- 그 외 → 필터 없음

#### 단어 부족 케이스
- 기본 단어장: count > available (abbreviation 필터 적용 후) → `TEST_E001`. 데이터 0개 → `TEST_E001`
- 커스텀/오답: count 가 없으므로 비교 안 함. abbreviation 필터 적용 후 0개 → `TEST_E001` (작성형 abbreviation 테스트인데 단어가 없는 경우)

### 4.2 distractors 출처

| 노트 타입 | 1차 출처 | 2차 보충 |
|---|---|---|
| 기본 단어장 | 같은 glossary 의 다른 termId | 다른 단어장 random |
| 커스텀노트 | 같은 customNote 의 다른 termId | 다른 단어장 random |
| 오답노트 (기간) | 각 termId 의 `sourceGlossaryId` 기준 같은 과 우선 | 다른 단어장 random |
| 오답노트 (과) | 같은 sourceGlossaryId 의 다른 단어 우선 | 다른 단어장 random |

오답노트의 경우 1차 출처가 단어별로 달라짐 (각 단어가 다른 sourceGlossaryId 일 수 있음). `wrong_answer_terms.sourceGlossaryId` 는 non-null 보장.

#### 정답 중복 보장
모든 타입 공통. server 가 정답 텍스트 derive 후 distractors 에서 제외.

#### 풀 부족 시
다른 단어장 보충에서도 3개를 못 채우면 전체 `TEST_E001`. 한 단어만 부족해도 전체 거부 (전체 단어 약 3000 개라 실제 발생 거의 없음).

### 4.3 submit 부수 효과 (방식 B 채택)

커스텀/오답 테스트는 학습 진도 시스템 전반에 반영 안 함. 단 오답 자동 기록은 별개 트랙으로 4종 모두 갱신.

| 부수 효과 | 기본 단어장 | 커스텀노트 | 오답노트 |
|---|---|---|---|
| TermProgressEntity (SRS) | 갱신 | X | X |
| GlossaryProgressEntity | learnedCount + lastLearnedAt | X | X |
| WrongAnswerTermEntity | 정답 시 무시, 오답 시 신규 INSERT 또는 누적 | X | 기존 row totalIncorrectCount += 1, incorrectAt 갱신 |
| DailyProgressEntity | 갱신 | X | X |
| UserEntity.totalLearningTerms | NEW → LEARNING 전환분 | X | X |
| UserEntity.currentStreak / longestStreak | 갱신 | X | X |
| 응답 `todayGoalCompleted` | 의미 있음 (Phase 2.11 에서 daily goal 비교로 변경 예정) | 응답 없음 | 응답 없음 |
| 응답 `glossary` | 값 채움 | 응답 없음 | 응답 없음 |

#### 오답 자동 기록 세부
- **기본 단어장**: 신규 단어면 `firstIncorrect()` 로 INSERT, 기존이면 `recordIncorrect()` 로 누적. `sourceGlossaryId = 현재 glossaryId`
- **커스텀노트**: 오답 발생해도 wrong_answer_terms 에 기록 안 함. 커스텀노트는 사용자가 단어 출처를 선택할 수 없는 구조라 `sourceGlossaryId` 를 의미 있게 매핑할 수 없음
- **오답노트**: 이미 wrong_answer_terms 에 있는 단어. 기존 row 의 `totalIncorrectCount += 1`, `incorrectAt` 갱신. 졸업 정책 없음

#### termId 검증 (submit)
- 기본 단어장 submit 만 `validateTermIdsBelongToGlossary` 적용. termId 가 단어장에 속하지 않으면 `TEST_E002`
- 커스텀/오답 submit 은 검증 안 함. 클라가 cards 응답의 termIds 만 정확히 전달한다고 신뢰

### 4.4 가용 단어 수 제공

**기본 단어장 상세 응답에만** `availableCounts` 필드 추가.

기본 단어장만 학습 카드 개수 selector 노출 (10/20/30/40/50). 커스텀/오답은 단어 수 고정이라 selector 자체가 없으므로 노출 불필요.

```json
"availableCounts": {
  "MEANING_TO_TERM": 30,
  "TERM_TO_MEANING": 30,
  "LISTEN_AND_WRITE": 30,
  "WRITE_ABBREVIATION": 12,
  "MC_PICK_TERM": 30,
  "MC_PICK_MEANING": 30,
  "MC_PICK_ABBR": 12
}
```

- `MEANING_TO_TERM`, `TERM_TO_MEANING`, `LISTEN_AND_WRITE`, `MC_PICK_TERM`, `MC_PICK_MEANING` → 단어장 전체 단어 수
- `WRITE_ABBREVIATION`, `MC_PICK_ABBR` → 단어장의 abbreviation 보유 단어 수

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
- 모든 4종 endpoint 동일

### termId 검증 (submit)
- 기본 단어장 submit 만 적용 → `TEST_E002`
- 커스텀/오답은 검증 안 함 (클라 책임)

---

## 6. ErrorCode

기존 + 신규 없음. Phase 2.8 코드 재활용:
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND` — 기본 단어장 미존재
- `CUSTOM_NOTE_E001 NOT_FOUND` / `CUSTOM_NOTE_E002 FORBIDDEN` — 커스텀노트 미존재 / 권한 없음
- `WRONG_ANSWER_NOTE_E002 INVALID_PERIOD` — period 값 유효성
- `TEST_E001 TEST_TYPE_NOT_AVAILABLE` — 단어 부족 / 작성형으로 distractors / distractor 풀 부족
- `TEST_E002 TEST_RESULT_INVALID` — 기본 단어장 submit 에서 termId 가 단어장에 속하지 않음 (기본 단어장만)

---

## 7. SuccessCode

Phase 2.8 의 `CARD_S001 LIST_CARDS`, `TEST_S001 LIST_DISTRACTORS`, `TEST_S002 SUBMIT_TEST` 재활용. 12개 endpoint 모두 같은 SuccessCode 사용.

---

## 8. 도메인 패키지 영향

### 옵션 B 구조 채택

card 도메인 폐지 후 test 도메인 안에 흡수. URL prefix `/v2/test/` 와 도메인 경계 일치.

```
domain/test/
├── controller/
│   ├── GlossaryTestApi              POST /v2/test/glossaries/{glossaryId}/{cards|distractors|submit}
│   ├── CustomNoteTestApi            POST /v2/test/custom-notes/{customNoteId}/{cards|distractors|submit}
│   └── WrongAnswerNoteTestApi       POST /v2/test/wrong-answer-notes/{by-period/{period}|by-glossary/{glossaryId}}/{cards|distractors|submit}
├── dto/
│   ├── CardsRequest, CardsResponse, Card
│   ├── DistractorsRequest, DistractorsResponse
│   ├── SubmitRequest
│   ├── SubmitGlossaryTestResponse   기본 단어장 전용
│   └── SubmitNoteTestResponse       커스텀/오답 공용 (correctCount, incorrectCount 만)
├── entity/                          Phase 2.8 그대로
└── service/
    ├── CardService                  noteType path 분기 (3종 sampling 통합)
    └── TestService                  noteType path 분기 (distractors + submit)
```

오답노트 controller 는 by-period + by-glossary 6 개 endpoint 를 한 파일에 묶음 (기존 `WrongAnswerNoteApi` 와 동일 정책).

### card 도메인 폐지
Phase 2.8 의 `domain/card/` 패키지 전체 삭제. controller / dto / service 전부 test 도메인으로 이전.

### progress 도메인
Phase 2.8 그대로. `ProgressUpdateService.updateProgress` → `updateProgressWithSrs(userId, events, glossaryId): SrsProgressSnapshot` 으로 rename (의도 명시).

### WrongAnswerNoteService 변경
- `recordIncorrects(userId, termIds, sourceGlossaryId: Long, occurredAt)` — 기본 단어장 submit 에서 호출. 신규 INSERT 또는 누적. sourceGlossaryId non-null
- `incrementExistingIncorrects(userId, termIds, occurredAt)` 신설 — 오답노트 submit 에서 호출. 기존 row totalIncorrectCount += 1, incorrectAt 갱신

커스텀노트 submit 은 WrongAnswerNoteService 호출 안 함.

### 영향 받는 기존 응답
- 기본 단어장 상세 응답 → `availableCounts` 필드 추가
- 커스텀노트 / 오답노트 상세 응답 → 변경 없음

---

## 9. 결정 누적

| 항목 | 결정 |
|---|---|
| URL 전략 | 정통 REST sub-resource + `/v2/test/` prefix 통일 (옵션 B) |
| URL 동사 | submit 유지. cards/distractors 는 명사 |
| URL 단/복수 | 노트 타입 plural 통일 (glossaries, custom-notes, wrong-answer-notes) |
| Endpoint 수 | cards 4 + distractors 4 + submit 4 = 12개 |
| cards body | 기본 단어장 `{ testType, count }`, 커스텀/오답 `{ testType }` (단어 수 고정, count 불필요) |
| cards 데이터 정책 | 기본 단어장만 sampling (NEW 우선 + RAND + `LIMIT count`). 커스텀/오답은 노트 전체를 정렬 (createdAt ASC / RAND) 해서 반환 |
| distractors body | `{ testType, termIds }` |
| submit body | `{ testType, results }` |
| 응답 envelope (cards) | context id 노출 안 함 |
| 응답 envelope (submit) | `SubmitGlossaryTestResponse` / `SubmitNoteTestResponse` 분리 |
| 진도 트래킹 | 기본 단어장만. 커스텀/오답 전반 제외 (방식 B) |
| 오답 자동 기록 | 기본 단어장 + 오답노트만. 커스텀노트 기록 안 함 |
| 오답노트 재오답 | totalIncorrectCount += 1. 졸업 없음 |
| 커스텀노트 cards 정렬 | createdAt 순 |
| distractors 보충 | 같은 컨텍스트 + 다른 단어장 random (Phase 2.8 정책 4종 통일) |
| distractors 풀 부족 | 한 단어만 못 채워도 전체 `TEST_E001` |
| termId 검증 (submit) | 기본 단어장 submit 만 적용. 커스텀/오답은 클라 책임 |
| 가용 단어 수 (`availableCounts`) | 기본 단어장 상세 응답에만. 커스텀/오답은 단어 수 고정이라 불필요 |
| ProgressUpdateService 메서드 | `updateProgressWithSrs` 1개만 유지 |
| Service 구조 | CardService / TestService 두 개 (책임 분리), path 별 분기 통합 |
| Controller 구조 | 노트 타입별 3 파일 (Glossary/CustomNote/WrongAnswerNote), 각 파일에 cards/distractors/submit 묶음 |
| card 도메인 | 폐지. test 도메인 안에 흡수 |
| `wrong_answer_terms.sourceGlossaryId` | non-null |

---

## 10. 구현 순서

1. spec 갱신 (commit X) — 본 문서
2. Repository — `custom_note_terms`, `wrong_answer_terms` (by-period / by-glossary) sampling 쿼리
3. Repository — testType 별 availableCount 집계 쿼리 (기본 단어장만)
4. 기본 단어장 상세 응답에 `availableCounts` 필드 추가
5. CardService 분기 로직 — noteType path 별 3종 sampling 통합
6. TestService distractors 분기 로직 — 4종 출처
7. WrongAnswerNoteService.recordIncorrects 시그니처 확장 (sourceGlossaryId non-null)
8. WrongAnswerNoteService.incrementExistingIncorrects 신설
9. Submit response DTO 분리 — `SubmitGlossaryTestResponse` / `SubmitNoteTestResponse`
10. TestService submit 분기 — 기본은 ProgressUpdateService 호출, 커스텀/오답은 채점 집계 (+ 오답 갱신은 오답노트만)
11. Controller 12개 endpoint 신설 — 옵션 B 구조 (`domain/test/controller/` 3 파일). 기본 단어장 submit 의 `validateTermIdsBelongToGlossary` 신규 endpoint 로 이동
12. Phase 2.8 의 `/v2/cards`, `/v2/test/distractors`, `/v2/test/submit` controller 삭제 + `domain/card/` 패키지 전체 삭제
13. SuccessCode / ErrorCode 점검 — 변경 없음 예상
14. iOS 전달 메시지 갱신
15. v2-roadmap.md 2.10 상태 done 처리 (commit X)

---

## 11. 미확정 / 후속 검토

- 오답노트 by-period 와 by-glossary 동시 적용 (현재는 양자택일)
- `availableCounts` 응답 필드 — 풀 노출 vs 단순화 (현재는 풀 노출)

---

## 12. Phase 2.11 연계

`todayGoalCompleted` 정의는 Phase 2.11 에서 확정. Phase 2.10 시점에는 현재 로직 (오늘 첫 활동 여부) 유지. Phase 2.11 에서:
- `users.daily_goal` 컬럼 신설 (default 10)
- `PUT /v2/users/me/preferences/daily-goal` API
- 마이페이지 응답에 `dailyGoal` 추가
- `todayGoalCompleted` 로직 변경 — 오늘 누적 학습 단어 수가 dailyGoal 이상이고 이전에는 미만이었던 경우 true
