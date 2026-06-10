# iOS 전달 - 커스텀 단어장 / 오답노트 DTO 이름 변경 안내

## 요약

`/v2/custom-notes/**`, `/v2/wrong-answer-notes/**` 의 **DTO 클래스명 + request/response 필드명 + path variable 명**이 변경되었습니다. URL path 자체와 ErrorCode/SuccessCode 코드 문자열은 변경 없음 - **호출 자체는 그대로 작동**, 모델 클래스만 재생성/수정 필요합니다.

영향 도메인: Custom Note, Wrong Answer Note.

---

## 변경 이유

단어장 도메인은 **커스텀 단어장 (custom note)** 과 **오답노트 (wrong answer note)** 두 종류가 있고, 향후 추가 종류 (예: 즐겨찾기 노트) 도 같은 패턴으로 확장될 예정입니다. 두 도메인이 별도 entity / 별도 테이블 / 별도 endpoint 로 분리되어 있는데, 응답 모양에서는 `notes`, `noteId`, `noteIds`, `id` 처럼 단어장 종류를 명시하지 않아 다음 문제가 있었습니다.

1. **응답 JSON 만 보고 도메인 식별 불가** - 예) `{ "noteIds": [1, 5, 12] }` 만으로는 커스텀 단어장 id 인지 오답노트 id 인지 알 수 없음. iOS 가 모델 매핑 시 dto 클래스명을 거꾸로 확인해야 함.
2. **단순 `id` 가 모호** - 같은 응답 안에 단어장 id 와 단어 id 가 섞여 있어도 둘 다 `id` 로 노출되면 어느 쪽인지 의미 추적이 필요.
3. **다른 종류 단어장 추가 시 충돌** - 향후 `WrongAnswerNoteResponse` 같은 클래스가 추가되었을 때 `notes` 라는 필드명을 두 도메인이 모두 쓰면 자동 생성된 swift 모델 / 디버깅 로그가 혼동됨.
4. **공유 단어장 import 같은 흐름에서 두 도메인 id 가 한 요청에 섞일 가능성** - 어느 id 가 어느 도메인인지 prefix 로 자명해야 함.

해결책: 응답/요청에 노출되는 **모든 도메인 식별자에 도메인 prefix 를 명시**합니다.
- 단어장 id -> `customNoteId` (도메인 명시)
- 단어 id -> `termId` (글로벌 식별자라 도메인 prefix 불필요, 단 단순 `id` 보다 명확)
- 단어장 리스트 -> `customNotes`
- 단어장 ID 배열 -> `customNoteIds`

이 원칙은 향후 신규 단어장 도메인 추가 시 동일하게 적용되며, dto 이름과 필드 이름만 보고도 어느 도메인 데이터인지 즉시 분간되게 하는 것이 목적입니다.

`termIds` 처럼 단어 id 리스트는 모든 도메인 공통 식별자라 prefix 안 붙입니다 (오답노트 / 커스텀 단어장 / 사전 모두에서 같은 의미).

---

## 1. DTO 클래스 이름 변경

| 기존 | 신규 |
|---|---|
| `AddTermItem` | `CustomNoteTermInput` |
| `AddTermsResponse` | `AddTermsToCustomNoteResponse` |
| `NotesForTermResponse` | `CustomNotesForTermResponse` |
| `NoteForTermItem` | `CustomNoteForTermItem` |

`AddTermsToCustomNoteRequest`, `AddTermToCustomNotesRequest`, `AddTermToCustomNotesResponse`, `DeleteCustomNotesRequest`, `DeleteTermsFromCustomNoteRequest`, `CustomNoteResponse`, `CustomNoteListResponse`, `CustomNoteTermsResponse`, `CustomNoteTermItem` 은 이름 변경 없음.

오답노트 도메인 DTO 클래스 이름 변경 없음.

---

## 2. Request / Response 필드 변경

### 2.1 Custom Note

| DTO | 기존 필드 | 신규 필드 |
|---|---|---|
| `CustomNoteResponse` | `id` | `customNoteId` |
| `CustomNoteListResponse` | `notes` | `customNotes` |
| `CustomNoteTermsResponse` | `noteId` | `customNoteId` |
| `CustomNoteTermItem` | `id` | `termId` |
| `CustomNotesForTermResponse` | `notes` | `customNotes` |
| `CustomNoteForTermItem` | `id` | `customNoteId` |
| `AddTermToCustomNotesRequest` | `noteIds` | `customNoteIds` |
| `AddTermToCustomNotesResponse` | `addedNoteIds` | `addedCustomNoteIds` |
| `DeleteCustomNotesRequest` | `noteIds` | `customNoteIds` |

`CustomNoteTermsResponse.terms`, `CustomNoteTermItem.glossaryId` 등 그 외 필드는 변경 없음.

### 2.2 Wrong Answer Note

| DTO | 기존 필드 | 신규 필드 |
|---|---|---|
| `WrongAnswerTermItem` | `id` | `termId` |

그 외 (`glossaryId`, `spelling`, `meaning`, `abbreviation`, `totalIncorrectCount`, `terms`, `termIds` 등) 변경 없음.

---

## 3. Path variable 이름 변경

URL path 문자열 자체는 변경 없음. swagger 명세상 placeholder 이름만 `{noteId}` -> `{customNoteId}` 변경. iOS 가 URL 만들 때는 위치만 보므로 호출 코드 영향 없음.

| 기존 표기 | 신규 표기 |
|---|---|
| `GET /v2/custom-notes/{noteId}` | `GET /v2/custom-notes/{customNoteId}` |
| `PATCH /v2/custom-notes/{noteId}` | `PATCH /v2/custom-notes/{customNoteId}` |
| `DELETE /v2/custom-notes/{noteId}` | `DELETE /v2/custom-notes/{customNoteId}` |
| `POST /v2/custom-notes/{noteId}/terms` | `POST /v2/custom-notes/{customNoteId}/terms` |
| `GET /v2/custom-notes/{noteId}/terms` | `GET /v2/custom-notes/{customNoteId}/terms` |
| `DELETE /v2/custom-notes/{noteId}/terms` | `DELETE /v2/custom-notes/{customNoteId}/terms` |
| `DELETE /v2/custom-notes/{noteId}/terms/{glossaryId}/{termId}` | `DELETE /v2/custom-notes/{customNoteId}/terms/{glossaryId}/{termId}` |

---

## 4. Before / After 예시

### 4.1 `GET /v2/custom-notes` 응답
**Before**
```json
{
  "notes": [
    { "id": 3, "title": "외과 단어장", "termCount": 50, "createdAt": "..." }
  ]
}
```
**After**
```json
{
  "customNotes": [
    { "customNoteId": 3, "title": "외과 단어장", "termCount": 50, "createdAt": "..." }
  ]
}
```

### 4.2 `GET /v2/custom-notes/{customNoteId}/terms` 응답
**Before**
```json
{
  "noteId": 3,
  "title": "...",
  "terms": [{ "id": 101, "glossaryId": 12, "spelling": "...", "meaning": "..." }],
  "page": 0, "size": 20, "totalElements": 50, "totalPages": 3, "hasNext": true
}
```
**After**
```json
{
  "customNoteId": 3,
  "title": "...",
  "terms": [{ "termId": 101, "glossaryId": 12, "spelling": "...", "meaning": "..." }],
  "page": 0, "size": 20, "totalElements": 50, "totalPages": 3, "hasNext": true
}
```

### 4.3 `POST /v2/custom-notes/terms` 요청 / 응답
**Request Before**
```json
{ "glossaryId": 12, "termId": 101, "noteIds": [1, 5, 12] }
```
**Request After**
```json
{ "glossaryId": 12, "termId": 101, "customNoteIds": [1, 5, 12] }
```

**Response Before**
```json
{ "addedNoteIds": [1, 12] }
```
**Response After**
```json
{ "addedCustomNoteIds": [1, 12] }
```

### 4.4 `GET /v2/custom-notes/terms/{glossaryId}/{termId}` 응답
**Before**
```json
{
  "notes": [
    { "id": 3, "title": "...", "termCount": 50, "hasTerm": false }
  ]
}
```
**After**
```json
{
  "customNotes": [
    { "customNoteId": 3, "title": "...", "termCount": 50, "hasTerm": false }
  ]
}
```

### 4.5 `DELETE /v2/custom-notes` 요청
**Before**
```json
{ "noteIds": [7, 12] }
```
**After**
```json
{ "customNoteIds": [7, 12] }
```

### 4.6 오답노트 단어 응답 (`GET /v2/wrong-answer-notes/...`)
**Before**
```json
{ "terms": [{ "id": 101, "glossaryId": 12, "spelling": "...", ... }] }
```
**After**
```json
{ "terms": [{ "termId": 101, "glossaryId": 12, "spelling": "...", ... }] }
```

---

## 5. 변경 없음 (안심 항목)

- URL path 문자열 (예: `/v2/custom-notes`, `/v2/custom-notes/terms`)
- HTTP method
- SuccessCode / ErrorCode 코드 문자열 (`CUSTOM_NOTE_S001`, `CUSTOM_NOTE_E001` 등) - 클라이언트 분기 그대로
- HttpStatus
- request 의 `glossaryId`, `termId` 필드명 (단어 id 자체는 글로벌 식별자라 prefix 안 붙임)
- 오답노트 도메인 대부분 (WrongAnswerTermItem.id -> termId 만 변경)
- `terms`, `termIds` 같은 단어 리스트 필드명

---

## 6. 작업 권장 순서

1. swagger codegen 으로 모델 재생성 또는 변경된 DTO 수동 갱신
2. 위 7개 endpoint 응답 파싱 부분 필드명 수정
3. `POST /v2/custom-notes/terms` 와 `DELETE /v2/custom-notes` 요청 body 필드명 수정
4. 빌드 - 컴파일 에러 발생한 곳이 영향 받은 호출처 (이름 변경된 필드)

호출 URL / 헤더 / 코드 문자열 분기 로직은 손댈 필요 없습니다.
