# Phase 2.13 — 학습/테스트 다중 유형 + 정답/문제 derive

학습/테스트 흐름에서 사용자가 **여러 testType 을 동시에 선택**할 수 있도록 확장하고, 카드 응답의 **문제(question)** 와 **정답(answer)** 을 서버가 derive 해서 내려준다. Phase 2.10 의 단일 testType 가정을 정정.

상세 결정 배경은 v2-roadmap.md 2.13 항목, 학습하기 Home 디자인 + iOS handoff 의견.

---

## 1. 배경

### 1.1 Phase 2.10 의 잘못된 가정 (정정 대상)

- 한 테스트 = 단일 testType 가정
- `SubmitRequest.testType` 단수
- `GlossaryCardsRequest.testType`, `NoteCardsRequest.testType` 단수
- `DistractorsRequest.testType + termIds` 단일 testType + termId 리스트
- 카드 응답에 testType 없음. 정답은 클라가 derive

### 1.2 실제 정책 (디자인 확정)

클라이언트 화면 (테스트 유형 선택) 에서 사용자는 5가지 옵션 중 다중 선택 가능. 한 테스트 안에 여러 testType 의 카드가 섞임.

클라이언트 선택지:
1. 용어 → 의미 (영어 단어 보고 한글 뜻 작성)
2. 의미 → 용어 (한글 뜻 보고 영어 단어 작성)
3. 듣고 쓰기 (음성 듣고 영어 단어 작성)
4. 약어 쓰기 (영어 단어 보고 약어 작성)
5. 다지선다 (통합) — 서버가 내부적으로 3종 변형 균등 분배

5번 선택 시 한 카드에 다음 3종이 균등 분배:
- 5-1. 영어 단어 → 한글 뜻 선택
- 5-2. 약어 → 영어 단어 선택
- 5-3. 한글 뜻 → 영어 단어 선택

5-1/5-2/5-3 은 클라에 개별 선택지로 노출하지 않음 (서버 내부 분배만).

---

## 2. TestType enum 재정의

기존 7종 → **신규 7종**. 클라 화면 5선택지에 다지선다 3변형 (서버 내부) 합쳐서 7종.

이름 패턴: `WRITE_{정답}_FROM_{입력}` (작성형) / `LISTEN_AND_WRITE` (음성) / `MC_{정답}_FROM_{입력}` (다지선다).

```
WRITE_MEANING_FROM_TERM            작성형, 영어 보고 한글 뜻 작성 (용어 → 의미)
WRITE_TERM_FROM_MEANING            작성형, 한글 뜻 보고 영어 작성 (의미 → 용어)
LISTEN_AND_WRITE                   작성형, 영어 발음 듣고 영어 작성 (듣고 쓰기)
WRITE_ABBREVIATION_FROM_TERM       작성형, 영어 보고 약어 작성 (약어 쓰기)
MC_MEANING_FROM_TERM               다지선다, 영어 보고 한글 뜻 선택 (5-1)
MC_TERM_FROM_ABBREVIATION          다지선다, 약어 보고 영어 선택 (5-2)
MC_TERM_FROM_MEANING               다지선다, 한글 뜻 보고 영어 선택 (5-3)
```

### 2.1 변경 내역

| 기존 | 신규 | 비고 |
|---|---|---|
| `MEANING_TO_TERM` | `WRITE_TERM_FROM_MEANING` | rename + 변형 명시 |
| `TERM_TO_MEANING` | `WRITE_MEANING_FROM_TERM` | rename |
| `WRITE_ABBREVIATION` | `WRITE_ABBREVIATION_FROM_TERM` | rename + 입력 명시 (용어 → 약어 한 변형만, 화면 "용어의 약어 작성") |
| `LISTEN_AND_WRITE` | `LISTEN_AND_WRITE` | 그대로 |
| `MC_PICK_TERM` | `MC_TERM_FROM_ABBREVIATION` 또는 `MC_TERM_FROM_MEANING` | 입력 종류로 세분 |
| `MC_PICK_MEANING` | `MC_MEANING_FROM_TERM` | rename + 입력 명시 |
| `MC_PICK_ABBR` | (제거) | 기획상 약어를 답으로 선택하는 다지선다 없음 |
| — | `MC_*_FROM_*` 3종 | 신규 (다지선다 5-1/5-2/5-3 변형) |

화면 미지원으로 enum 만들지 않은 변형:
- `WRITE_MEANING_FROM_ABBREVIATION` (약어 → 한글 뜻 작성) — 화면 없음
- `WRITE_ABBREVIATION_FROM_MEANING` (한글 뜻 → 약어 작성) — 화면 없음

### 2.2 클라 선택지 ↔ enum 매핑

| 클라 선택지 | 요청 testType |
|---|---|
| 1. 용어 → 의미 | `WRITE_MEANING_FROM_TERM` |
| 2. 의미 → 용어 | `WRITE_TERM_FROM_MEANING` |
| 3. 듣고 쓰기 | `LISTEN_AND_WRITE` |
| 4. 약어 쓰기 | `WRITE_ABBREVIATION_FROM_TERM` |
| 5. 다지선다 | **3종을 한 번에 List 에 추가** — `MC_MEANING_FROM_TERM` + `MC_TERM_FROM_ABBREVIATION` + `MC_TERM_FROM_MEANING` |

작성형 4종은 사용자 선택과 1:1. 다지선다는 클라가 선택지 5번 누르면 internal 매핑으로 3종 모두 testTypes 에 추가 → 서버 균등 분배 정책 (testTypes.size 로 나누기) 에 따라 자동으로 5-1/5-2/5-3 균등 출제.

---

## 3. cards endpoint

### 3.1 Request

#### 3.1.1 Body 구조 변경

**기존 (Phase 2.10)**:
```json
{ "testType": "MEANING_TO_TERM", "count": 30 }
```

**신규** — 사용자가 #2(의미→용어) + #4(약어 쓰기) + 다지선다 선택 시:
```json
{
  "testTypes": [
    "WRITE_TERM_FROM_MEANING",
    "WRITE_ABBREVIATION_FROM_TERM",
    "MC_MEANING_FROM_TERM",
    "MC_TERM_FROM_ABBREVIATION",
    "MC_TERM_FROM_MEANING"
  ],
  "count": 30
}
```

- `testTypes`: 클라 매핑 결과 testType 리스트. 1~N 개. 같은 testType 중복 허용 (중복 시 분배 비중 두 배)
- 다지선다 선택 = 클라가 `MC_*_FROM_*` 3종 모두 List 에 추가. 서버는 이 3종을 **1 슬롯 (다지선다 그룹)** 으로 묶어 분배 계산
- `count`: 출제할 총 카드 수 (기본 단어장만, 노트는 count 없음 — Phase 2.10 정책 유지)

#### 3.1.2 testType 의 자기완결성

enum 이름 자체에 정답과 입력이 모두 포함되어 있어 별도 보조 필드 (`questionField` 등) 불필요. wrapper 객체 없이 `List<TestType>` 직접 사용. 다지선다도 변형까지 enum 으로 표현되어 카드별 distractors 생성 시 추가 정보 없이 testType 만으로 자명.

#### 3.1.3 분배 정책 (서버) — 다지선다 그룹 1 슬롯 처리

화면 1 선택지 = 1 슬롯 원칙. 다지선다는 클라가 `MC_*_FROM_*` 3종을 모두 testTypes 에 추가하지만 서버는 1 슬롯으로 묶어 분배.

**알고리즘**:
1. testTypes 를 슬롯으로 그룹화 — `MC_*_FROM_*` 3종은 함께 등장하면 1 슬롯 (다지선다 그룹). 그 외 testType 은 각자 1 슬롯
2. `slots.size = S` 로 `count` 균등 분배. 나머지는 앞 슬롯부터 +1
3. 다지선다 슬롯에 배정된 양은 내부적으로 `MC_*_FROM_*` 3변형에 균등 재분배 (나머지는 5-1 → 5-2 → 5-3 순)

**예시 A** — 사용자 #2(의미→용어) + #4(약어 쓰기) + 다지선다, count=30
- testTypes = `[WRITE_TERM_FROM_MEANING, WRITE_ABBREVIATION_FROM_TERM, MC_MEANING_FROM_TERM, MC_TERM_FROM_ABBREVIATION, MC_TERM_FROM_MEANING]`
- 슬롯 3개 (작성형 2 + 다지선다 그룹 1)
- 30 / 3 = 10 → 작성형 10, 작성형 10, 다지선다 10
- 다지선다 10 → 4 / 3 / 3 (5-1, 5-2, 5-3)

**예시 B** — 사용자 다지선다만 선택, count=30
- testTypes = `[MC_MEANING_FROM_TERM, MC_TERM_FROM_ABBREVIATION, MC_TERM_FROM_MEANING]`
- 슬롯 1개 (다지선다 그룹)
- 30 / 1 = 30 → 다지선다 30
- 내부 분배 → 10 / 10 / 10

**그룹 조건**: `MC_*_FROM_*` 3종이 testTypes 에 **모두** 들어있을 때만 그룹화. 일부만 있는 경우는 클라 약속 위반으로 간주 — 각자 1 슬롯 fallback (실제로는 화면이 막아 발생 안 함)

각 testType 슬롯에 배정된 count 만큼 해당 testType 에 맞는 단어 풀에서 sampling.

#### 3.1.4 풀 부족 정책

어느 한 testType 에 배정된 count 를 못 채우면 (예: `WRITE_ABBREVIATION_FROM_TERM` + abbreviation 보유 단어 부족) **전체 거부** `TEST_E001`. 기존 정책과 일관.

#### 3.1.5 단어 중복 정책

같은 termId 가 다른 testType 으로 두 번 출제될 수 있음. 단어 부족 시 학습 효과 측면에서 허용.

같은 testType 안에서는 termId 유일.

#### 3.1.6 노트 cards 분배 정책 (custom note, wrong answer note)

노트는 count 없음. 전체 단어를 testType 별 균등 분배 (각 단어는 한 testType 으로만 출제).

**알고리즘**:
1. 노트의 출제 대상 단어 N 개 추출 (기존 Phase 2.10 정책)
2. 슬롯 그룹화 (3.1.3 과 동일) → 슬롯 수 S
3. N 을 S 로 균등 분배. 나머지는 앞 슬롯부터 +1
4. 각 슬롯 안에서 단어 셔플 → testType 매핑
5. 다지선다 슬롯은 내부 5-1/5-2/5-3 균등 재분배

**같은 단어 중복 없음** — 카드 수 = N. 각 단어는 정확히 한 카드로 출제.

abbreviation 보유 안 한 단어가 `WRITE_ABBREVIATION_FROM_TERM` / `MC_TERM_FROM_ABBREVIATION` 슬롯에 배정되면 안 됨 → testType 별 가능 단어 필터링 후 분배 (분배 보정 로직). 노트는 풀이 작아 슬롯별 단어 부족 시 가능한 만큼만 출제 (전체 거부 X — 노트는 진단성 학습 도구).

### 3.2 Response

#### 3.2.1 카드별 필드

기존 cards 응답의 단어 정보 (spelling, meaning, abbreviation, explanation, bookmarked, morphemes) 그대로 + 신규 4 필드 추가:

| 필드 | 타입 | 의미 |
|---|---|---|
| `testType` | enum | 카드 type — 7종 중 하나 (다지선다도 변형까지 자명) |
| `question` | String | 화면에 노출할 문제 텍스트. 서버가 derive |
| `answer` | String | 정답 텍스트. 클라가 채점에 사용 |
| `distractors` | List\<String\>? | 다지선다 카드 (`MC_*_FROM_*`) 만 채움. 작성형 / LISTEN 은 null 또는 미포함. 정답과 중복 안 되는 보기 3개 |

#### 3.2.2 응답 예시

```json
{
  "cards": [
    {
      "termId": 101,
      "testType": "WRITE_TERM_FROM_MEANING",
      "question": "심전도",
      "answer": "Electrocardiogram",
      "spelling": "Electrocardiogram",
      "meaning": "심전도",
      "abbreviation": "ECG",
      "explanation": "심장 활동의 전기적 변화를 기록한 그래프",
      "bookmarked": false,
      "morphemes": []
    },
    {
      "termId": 102,
      "testType": "WRITE_MEANING_FROM_TERM",
      "question": "Hypertension",
      "answer": "고혈압",
      "spelling": "Hypertension",
      "meaning": "고혈압",
      "abbreviation": "HTN",
      "explanation": "혈압이 정상 범위보다 높은 상태",
      "bookmarked": true,
      "morphemes": []
    },
    {
      "termId": 105,
      "testType": "WRITE_ABBREVIATION_FROM_TERM",
      "question": "Cardiopulmonary resuscitation",
      "answer": "CPR",
      "spelling": "Cardiopulmonary resuscitation",
      "meaning": "심폐소생술",
      "abbreviation": "CPR",
      "explanation": "심정지 환자에게 시행하는 응급 처치",
      "bookmarked": false,
      "morphemes": []
    },
    {
      "termId": 103,
      "testType": "LISTEN_AND_WRITE",
      "question": "Acute kidney injury",
      "answer": "Acute kidney injury",
      "spelling": "Acute kidney injury",
      "meaning": "급성 신부전",
      "abbreviation": "AKI",
      "explanation": "수 시간에서 수일 이내에 신기능이 급격히 저하되는 상태",
      "bookmarked": false,
      "morphemes": []
    },
    {
      "termId": 120,
      "testType": "MC_MEANING_FROM_TERM",
      "question": "Hypertension",
      "answer": "고혈압",
      "distractors": ["당뇨병", "심근경색", "급성 신부전"],
      "spelling": "Hypertension",
      "meaning": "고혈압",
      "abbreviation": "HTN",
      "explanation": "혈압이 정상 범위보다 높은 상태",
      "bookmarked": false,
      "morphemes": []
    },
    {
      "termId": 121,
      "testType": "MC_TERM_FROM_ABBREVIATION",
      "question": "AKI",
      "answer": "Acute kidney injury",
      "distractors": ["Diabetes mellitus", "Myocardial infarction", "Hypertension"],
      "spelling": "Acute kidney injury",
      "meaning": "급성 신부전",
      "abbreviation": "AKI",
      "explanation": "수 시간에서 수일 이내에 신기능이 급격히 저하되는 상태",
      "bookmarked": false,
      "morphemes": []
    },
    {
      "termId": 122,
      "testType": "MC_TERM_FROM_MEANING",
      "question": "심근경색",
      "answer": "Myocardial infarction",
      "distractors": ["Cardiopulmonary resuscitation", "Electrocardiogram", "Hypertension"],
      "spelling": "Myocardial infarction",
      "meaning": "심근경색",
      "abbreviation": "MI",
      "explanation": "관상동맥이 막혀 심근의 일부가 괴사하는 상태",
      "bookmarked": false,
      "morphemes": []
    }
  ]
}
```

각 카드의 `testType` 별 `question` / `answer` 매핑은 3.2.3 표 참조.

- termId 101: `WRITE_TERM_FROM_MEANING` — 한글 뜻 보고 영어 작성
- termId 102: `WRITE_MEANING_FROM_TERM` — 영어 보고 한글 뜻 작성
- termId 105: `WRITE_ABBREVIATION_FROM_TERM` — 영어 보고 약어 작성
- termId 103: `LISTEN_AND_WRITE` — 영어 발음 듣고 영어 작성 (클라가 question 텍스트로 TTS 재생, 화면 노출 X)
- termId 120: `MC_MEANING_FROM_TERM` (5-1) — 영어 보고 한글 뜻 선택
- termId 121: `MC_TERM_FROM_ABBREVIATION` (5-2) — 약어 보고 영어 선택
- termId 122: `MC_TERM_FROM_MEANING` (5-3) — 한글 뜻 보고 영어 선택

#### 3.2.3 question / answer 매핑

서버가 derive 하는 규칙. testType 만 보면 자명:

| testType | question (서버가 노출할 단어) | answer (정답) |
|---|---|---|
| `WRITE_TERM_FROM_MEANING` | meaning | spelling |
| `WRITE_MEANING_FROM_TERM` | spelling | meaning |
| `WRITE_ABBREVIATION_FROM_TERM` | spelling | abbreviation |
| `LISTEN_AND_WRITE` | spelling (클라가 TTS 재생) | spelling |
| `MC_MEANING_FROM_TERM` (5-1) | spelling | meaning |
| `MC_TERM_FROM_ABBREVIATION` (5-2) | abbreviation | spelling |
| `MC_TERM_FROM_MEANING` (5-3) | meaning | spelling |

#### 3.2.4 LISTEN_AND_WRITE 의 question 처리

`question` 에 영어 단어 텍스트가 들어가지만 **클라는 화면에 노출하지 않음**. iOS 측 TTS (AVSpeechSynthesizer) 로 발음만 재생.

테스트 화면 노출 시 정답 누설 방지 — 클라가 `testType == LISTEN_AND_WRITE` 분기로 처리.

---

## 4. distractors endpoint — **폐기**

cards 응답의 카드별 `distractors` 필드에 통합됨. distractors 4종 endpoint 와 관련 service / DTO 일괄 폐기.

### 4.1 폐기 대상 endpoint
- `POST /v2/test/glossaries/{glossaryId}/distractors`
- `POST /v2/test/custom-notes/{customNoteId}/distractors`
- `POST /v2/test/wrong-answer-notes/by-period/{period}/distractors`
- `POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/distractors`

### 4.2 폐기 대상 코드
- `TestService.listGlossaryDistractors`, `listCustomNoteDistractors`, `listWrongAnswerDistractors`
- `DistractorsRequest`, `DistractorsResponse`, `DistractorQuestion` DTO
- 3개 controller 의 distractors endpoint
- `TEST_S001 LIST_DISTRACTORS` SuccessCode

### 4.3 CardService 책임 확장
- 다지선다 카드 sampling 시 그 자리에서 distractor 도 생성
- 기존 `TestService.buildDistractors` 로직 그대로 이전 — 같은 단어장/노트 우선 + 글로벌 보충, 정답 텍스트 중복 제외
- 작성형 / LISTEN 카드는 `distractors = null`

### 4.4 distractor 생성 규칙 (cards 응답 내장)

각 `MC_*_FROM_*` 카드에 대해 서버가 다음 절차로 distractor 3개를 derive:
1. `testType` 으로 정답 필드 derive (예: `MC_TERM_FROM_ABBREVIATION` → 정답 필드 = spelling)
2. distractor 풀에서 같은 필드의 텍스트 추출 (정답 텍스트 중복 제외)
3. 1차 풀 (같은 단어장 / 노트) 우선 + 글로벌 보충 (Phase 2.10 정책 유지)
4. 풀 부족 시 3개 미만 가능. 0개여도 카드 자체는 출제 (`distractors = []` 가능)

cards / distractors 일관성 문제는 한 응답 안에 합쳐 사라짐. 서버 stateless 유지.

---

## 5. submit endpoint

### 5.1 Request 변경

**기존**:
```json
{
  "testType": "MEANING_TO_TERM",
  "results": [
    { "termId": 101, "correct": true, "userAnswer": "Electrocardiogram" }
  ]
}
```

**신규** — `testType` 을 body 의 단수 필드에서 results 항목별로 이동:
```json
{
  "results": [
    { "termId": 101, "testType": "WRITE_TERM_FROM_MEANING", "correct": true,  "userAnswer": "Electrocardiogram" },
    { "termId": 121, "testType": "MC_TERM_FROM_ABBREVIATION", "correct": false, "userAnswer": "..." }
  ]
}
```

각 결과 항목이 어느 testType 인지 자체 표현.

### 5.2 Response

기존 그대로 (Phase 2.10):
- `SubmitGlossaryTestResponse` — 기본 단어장
- `SubmitNoteTestResponse` — 커스텀/오답

---

## 6. availableCounts 활용 정책

기존 `selectedGlossary.availableCounts` 는 testType 별 가용 단어 수. 클라가 selector max 결정 시:

### 6.1 정책

사용자 선택지 슬롯 수 = `S` (다지선다 선택 시 `MC_*_FROM_*` 3종을 1 슬롯으로 묶음, 즉 작성형 4 + 다지선다 1 → S=5 가 최대).

- 각 slot 의 대표 `availableCount` 조회 — 다지선다 슬롯은 `min(MC_MEANING_FROM_TERM, MC_TERM_FROM_ABBREVIATION, MC_TERM_FROM_MEANING)`
- `min(availableCount[slot])` 의 S 배가 max
- selector (10/20/30/40/50) 에서 max 이하만 활성화

### 6.2 예시

사용자가 `[용어→의미, 약어 쓰기]` 선택 → S = 2
- availableCounts = { WRITE_MEANING_FROM_TERM: 100, WRITE_ABBREVIATION_FROM_TERM: 42 }
- min(100, 42) = 42, S = 2 → max = 84 → selector 50 모두 활성

사용자가 다지선다만 선택 → S = 1
- 다지선다 슬롯 availableCount = min(MC_MEANING_FROM_TERM=100, MC_TERM_FROM_ABBREVIATION=42, MC_TERM_FROM_MEANING=100) = 42
- max = 42 × 1 = 42 → selector 10/20/30/40 활성, 50 비활성

### 6.3 availableCounts 의 키

기존 7종 enum → 신규 7종으로 변경 (구성이 다름). 응답:
```json
"availableCounts": {
  "WRITE_TERM_FROM_MEANING": 100,
  "WRITE_MEANING_FROM_TERM": 100,
  "WRITE_ABBREVIATION_FROM_TERM": 42,
  "LISTEN_AND_WRITE": 100,
  "MC_MEANING_FROM_TERM": 100,
  "MC_TERM_FROM_ABBREVIATION": 42,
  "MC_TERM_FROM_MEANING": 100
}
```

- 정답/입력 중 한쪽이라도 abbreviation 인 enum → abbreviation 보유 단어 수
  - `WRITE_ABBREVIATION_FROM_TERM`, `MC_TERM_FROM_ABBREVIATION`
- 그 외 → 전체 단어 수

응답에는 7종 enum 모두 그대로 노출. selector max 계산 시 클라가 다지선다 슬롯을 1로 묶고 그 안의 3 변형 중 최솟값을 슬롯 대표값으로 사용 (6.1 정책).

---

## 7. 도메인 영향

### 7.1 enum 정리
- `TestType` enum 갱신 — 7종 (`WRITE_*_FROM_*` 3종 + `LISTEN_AND_WRITE` + `MC_*_FROM_*` 3종)
- 사용처: CardService, TestService, availableCounts 매핑, DTO 등
- 보조 필드 없음 — enum 이름에 정답/입력 변형 모두 포함

### 7.2 DTO 변경

#### Request
- `GlossaryCardsRequest` — `testType + count` → `testTypes: List<TestType> + count`
- `NoteCardsRequest` — `testType` → `testTypes: List<TestType>`
- `DistractorsRequest`, `DistractorQuestion`, `DistractorsResponse` — **전부 폐기** (cards 응답에 통합)
- `SubmitRequest.testType` 제거
- `TestResultItem.testType` 추가

#### Response
- `GlossaryCard / WrongAnswerCard / CustomNoteCard` 모두에 `testType + question + answer + distractors` 추가
- `distractors` 는 `MC_*_FROM_*` 카드만 채움. 작성형 / LISTEN 은 null

### 7.3 Service 갱신

#### CardService
- 슬롯 그룹화 — `MC_*_FROM_*` 3종이 모두 있으면 1 다지선다 슬롯, 그 외 testType 은 각자 1 슬롯
- `pickCardTermsForTest` (기본 단어장) — count 를 슬롯 균등 분배, 나머지 앞 슬롯 +1. 다지선다 슬롯 내부 3변형 균등 재분배
- `pickCardTermsForNote` (custom/wrong answer) — 노트 전체 단어 N 을 슬롯 균등 분배 (각 단어 1 testType). abbreviation 필터 적용 후 셔플 매핑
- 카드별 `question` / `answer` derive — 매핑 표 참조
- `MC_*_FROM_*` 카드에 대해 distractor 생성 — `TestService.buildDistractors` 로직 이전
- 풀 부족 시 기본 단어장은 `TEST_E001`, 노트는 가능한 만큼만 출제

#### TestService.listDistractors / listCustomNoteDistractors / listWrongAnswerDistractors
- **전부 폐기.** cards 응답 내 distractors 필드로 통합

#### TestService.submitGlossaryTest / submit*
- `results[].testType` 받음. termId 검증은 기본 단어장만 그대로 유지 (Phase 2.10 정책)
- progress 갱신은 testType 무관 (기존 동작)

### 7.4 availableCounts
- key 가 신규 7종 enum 으로 변경
- 정답/입력 중 abbreviation 포함된 enum 2종 (`WRITE_ABBREVIATION_FROM_TERM`, `MC_TERM_FROM_ABBREVIATION`) 은 abbreviation 보유 단어 수, 나머지 5종은 전체 단어 수

---

## 8. SuccessCode / ErrorCode

신규 없음. Phase 2.10 코드 재활용.

`TEST_S001 LIST_DISTRACTORS` SuccessCode 는 distractors endpoint 폐기로 더 이상 응답되지 않음 (enum value 는 그대로 유지 — 미사용 enum value 는 삭제하지 않는 원칙).

`TestType` enum value 문자열은 클라가 분기에 사용하는 계약이므로 변경 시 iOS 영향 있음. 변경 내역:
- `MEANING_TO_TERM` → `WRITE_TERM_FROM_MEANING`
- `TERM_TO_MEANING` → `WRITE_MEANING_FROM_TERM`
- `MC_PICK_TERM` → `MC_TERM_FROM_ABBREVIATION` 또는 `MC_TERM_FROM_MEANING` (변형 분리)
- `MC_PICK_MEANING` → `MC_MEANING_FROM_TERM`
- `MC_PICK_ABBR` → 제거 (기획상 미사용)

---

## 9. 결정 누적

| 항목 | 결정 |
|---|---|
| testTypes 다중 선택 | 지원. 한 테스트에 여러 testType 카드 혼합 |
| testType enum 정리 | 신규 7종 (`WRITE_*_FROM_*` 3종 + `LISTEN_AND_WRITE` + `MC_*_FROM_*` 3종). 화면 5선택지 + 다지선다 3변형 |
| enum 명명 기준 | `WRITE_{정답}_FROM_{입력}` / `MC_{정답}_FROM_{입력}` 패턴. 정답과 입력을 enum 이름에 모두 포함 |
| 화면 미지원 변형 enum 제외 | `WRITE_MEANING_FROM_ABBREVIATION`, `WRITE_ABBREVIATION_FROM_MEANING` 은 화면 선택지 없어 enum 에서 제외 (향후 필요 시 신규) |
| MC_PICK_* 3종 처리 | `MC_*_FROM_*` 3종으로 rename + 입력 명시 (`MC_PICK_ABBR` 제거) |
| 다지선다 분배 | 클라가 `MC_*_FROM_*` 3종 모두 testTypes 에 추가. **서버가 1 슬롯 (다지선다 그룹) 으로 묶어 분배 → 슬롯 내부 5-1/5-2/5-3 균등 재분배** |
| cards request body | `testTypes: List<TestType> + count`. wrapper 없이 enum 리스트 직접 |
| testTypes 중복 허용 | 허용. 같은 testType 두 번 들어가면 분배 비중 두 배 |
| 클라 ↔ enum 매핑 | 5 선택지. 작성형 4종 1:1 + 다지선다 1선택지 → MC 3종 일괄 추가 |
| 슬롯 그룹화 | `MC_*_FROM_*` 3종이 모두 testTypes 에 있으면 1 슬롯으로 묶음. 그 외 testType 은 각자 1 슬롯 |
| cards 분배 | 슬롯 균등. 나머지는 요청 앞 슬롯부터 +1. 다지선다 슬롯은 내부 3변형 균등 재분배 |
| 노트 cards 분배 | 노트 전체 단어 N → 슬롯 균등 분배 (각 단어 1 testType). abbreviation 미보유 단어는 abbreviation 슬롯 배정 안 함. 슬롯별 부족 시 가능한 만큼만 (전체 거부 X) |
| 풀 부족 시 | 기본 단어장 — 전체 `TEST_E001`. 노트 — 가능한 만큼만 출제 (없는 슬롯 카드 수 줄어듦) |
| 단어 중복 | 기본 단어장 — 같은 termId 가 다른 testType 으로 출제 허용. 노트 — 같은 단어 한 번만 (한 testType) |
| cards response | 카드별 `testType` + `question` + `answer` + `distractors` 신규 필드 |
| question / answer | 서버 derive. 매핑 표 참조 |
| LISTEN_AND_WRITE 의 question | spelling 텍스트. 클라가 TTS 재생만 (화면 노출 X) |
| submit request | `testType` 을 body 단수 → `results[].testType` 항목별 |
| availableCounts key | 신규 7종 enum |
| selector max 정책 | `min(availableCount[slot]) × slots.size` (다지선다 슬롯 = 1 슬롯, 대표값 = MC 3변형의 min) |
| distractors endpoint | **폐기.** cards 응답의 카드별 `distractors` 필드로 통합 (`MC_*_FROM_*` 카드만 값 보유) |

---

## 10. iOS 영향

### Breaking
- `TestType` enum value 문자열 변경 — 모델 재생성 필요
- `GlossaryCardsRequest`, `NoteCardsRequest`, `SubmitRequest` body 구조 변경
- distractors endpoint 4종 **폐기** — cards 응답에 distractors 필드로 통합
- cards 응답 카드에 신규 필드 (`testType`, `question`, `answer`, `distractors`) 추가
- `availableCounts` 의 key 변경

### 변경 없음
- cards / submit URL / Method
- SuccessCode / ErrorCode 문자열
- 단어 카드의 다른 필드들 (spelling, meaning, abbreviation, explanation, morphemes, bookmarked)

---

## 11. 구현 순서

1. spec 작성 (본 문서)
2. `TestType` enum 갱신 — 7종 (`WRITE_*_FROM_*` 3종 + `LISTEN_AND_WRITE` + `MC_*_FROM_*` 3종). `MC_PICK_ABBR` 폐기
3. cards Request DTO 변경 — `GlossaryCardsRequest`, `NoteCardsRequest` (`testTypes: List<TestType> + count`)
4. cards Response DTO 변경 — `GlossaryCard`, `WrongAnswerCard`, `CustomNoteCard` 에 `testType` + `question` + `answer` + `distractors` 추가
5. CardService 갱신 — 슬롯 그룹화 (다지선다 1 슬롯) + 슬롯 균등 분배 + 다지선다 슬롯 내부 3변형 균등 재분배. 노트는 슬롯 균등 분배 (각 단어 1 testType). 카드별 question/answer derive, MC 카드 distractor 생성 (기존 `TestService.buildDistractors` 로직 이전)
6. distractors endpoint 4종 폐기 — 3개 controller, `DistractorsRequest`/`DistractorsResponse`/`DistractorQuestion` DTO, `TestService.listGlossaryDistractors` / `listCustomNoteDistractors` / `listWrongAnswerDistractors` 삭제
7. submit Request DTO 변경 — `SubmitRequest.testType` 제거 + `TestResultItem.testType` 추가
8. TestService.submitGlossaryTest 등 갱신 — termId 검증 정책 유지
9. availableCounts 매핑 갱신 — UserDashboardService.buildAvailableCounts (key 가 7종 enum)
10. SuccessCode / ErrorCode 점검 (변경 없음 예상, `LIST_DISTRACTORS` 미사용 enum value 그대로 유지)
11. v2-roadmap.md 결정 누적 갱신
12. iOS 전달 메시지

---

## 12. 미확정 / 후속 검토

- LISTEN_AND_WRITE 의 audio 처리가 향후 서버 측 음성 파일 제공으로 변경될 가능성 (현재는 iOS TTS)
- question / answer 외 추가 derive 필드 가능성 (예: hint, candidates)
