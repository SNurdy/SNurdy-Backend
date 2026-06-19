# Phase 2.5 — 오답노트 명세 (v2)

기획 디자인 png 13장 + 기획안(2026-05-25) + 사용자 합의 결정 반영.

본 명세는 **옵션 C** 채택 — WRONG_ANSWER 를 GlossaryEntity 의 type 으로 모델링하지 않고, 별도 도메인 (`wrong_answer_terms` 테이블) 으로 분리. 매핑 테이블(`glossary_terms`) 은 순수 mapping 책임만 유지. 노트 title 은 디자인상 정적이라 별도 컬럼/endpoint 없음. 누적 오답 횟수는 컬럼 저장 + soft delete 로 매번 SUM 회피.

---

## 1. 화면 흐름 요약 (디자인 기준)

```
[메인] -> [오답노트 탭 선택]
              |- 기간별 탭 -> 카드 3개 (오늘/이번 주/전체)
              |              -> 카드 탭 -> [기간별 상세] -> 단어 리스트 + 테스트하기 CTA
              |
              |- 과별 탭   -> DEFAULT glossary 별 카드 (n개)
                              -> 카드 탭 -> [과별 상세] -> 단어 리스트 + 테스트하기 CTA

[상세] 우측 상단 휴지통 -> [편집 모드] -> 다중 선택 -> "단어 삭제" -> 확인 dialog -> 토스트
[상세] 단어 탭 -> 단어 카드 (기존 dictionary term detail 재사용)
[상세] 하단 검색 바 -> 노트 내 단어 검색 (사전 검색과 동일 정책)
[상세] 빈 상태 -> "오답 단어가 없어요! 학습하러 가기" CTA
```

화면 title 은 모두 정적:
- 메인: "오답노트" (고정)
- 기간별 상세: "오늘 틀린 단어" / "이번 주 틀린 단어" / "전체 틀린 단어" (백엔드 label 반환)
- 과별 상세: DEFAULT `glossaries.title` 활용

---

## 2. 도메인 위치

CLAUDE.md 11.1 원칙대로 `customNote` 와 평평한 형제 도메인.

```
domain/user/
├── customNote/         (GlossaryType.CUSTOM)
└── wrongAnswerNote/    (오답노트 - 단어장 type 이 아닌 별도 도메인)
    ├── controller/
    │   ├── WrongAnswerNoteApi
    │   └── WrongAnswerNoteController
    ├── dto/WrongAnswerNoteDto
    ├── entity/
    │   ├── WrongAnswerTermEntity            (user_id, term_id PK)
    │   └── WrongAnswerTermId                (복합키)
    ├── repository/WrongAnswerTermRepository
    └── service/WrongAnswerNoteService
```

ErrorCode/SuccessCode prefix: `WRONG_ANSWER_NOTE_*`

---

## 3. 데이터 모델

### 3.1 핵심 결정 — WRONG_ANSWER glossary 모델 폐기

- `GlossaryType.WRONG_ANSWER` enum value 는 V27 에서 도입되어 enum 에는 남아있으나 **v2 코드에서 사용하지 않음**
- WRONG_ANSWER glossary 인스턴스를 만들지 않음 (lazy create 로직 불필요)
- `glossary_terms` 에는 WRONG_ANSWER 행이 들어가지 않음 (순수 mapping 유지)
- enum value 보존 (클라이언트 계약, 미사용 enum value 보존 원칙)

### 3.2 오답 단어 — `wrong_answer_terms` 테이블 신설

| 컬럼 | 타입 | 의미 |
|---|---|---|
| `user_id` | BINARY(16) NOT NULL | 사용자 UUID |
| `term_id` | BIGINT NOT NULL | 단어 ID |
| `source_glossary_id` | BIGINT NULL | 마지막 오답 발생 시점의 학습 단어장 ID (과별 그루핑용) |
| `incorrect_at` | TIMESTAMP NOT NULL | 마지막 오답 발생 시각 (기간 필터/정렬용) |
| `total_incorrect_count` | INT NOT NULL DEFAULT 0 | 누적 오답 횟수 (자체 누적, SUM 회피) |
| `deleted_at` | TIMESTAMP NULL | 사용자 수동 삭제 시각 (NULL = 활성). 코드베이스 soft delete 컨벤션 |

PK: `(user_id, term_id)` — 사용자당 단어 1개는 단일 row. 재오답 시 컬럼 갱신만.

soft delete 채택 이유: 사용자가 단어를 삭제한 뒤 같은 단어를 또 틀린 경우 누적값 (예: 18 -> 19) 을 보존해야 하기 때문. hard delete 면 재 insert 시 1 부터 시작해 기획안과 모순.

### 3.3 노트 title

별도 컬럼 없음. 디자인상 모든 title 이 정적이거나 기존 `glossaries.title` 활용. 사용자 자유 수정 UI 없음.

### 3.4 단어의 "대표 과" 결정 정책

`wrong_answer_terms.source_glossary_id` = 가장 최근 오답 발생 시점의 학습 단어장 ID.

사용자 결정 — "어차피 해당 학습 단어장의 오답노트로 넣는 것" 이라 학습 컨텍스트 단어장 ID 가 자연스러운 source.

- 단어 A 를 정형외과 학습 중 처음 틀림: `source_glossary_id = 정형외과 ID`
- 같은 단어 A 를 심장내과 학습 중 또 틀림: `source_glossary_id = 심장내과 ID` 로 갱신
- 단어는 항상 1개의 대표 과로 그루핑

### 3.5 totalIncorrectCount 산출

응답의 단어별 `totalIncorrectCount` = `wrong_answer_terms.total_incorrect_count` 컬럼 직접 사용 (SUM 불필요).

- 자동 추가 (4번 로직) 시 `+= 1` 자체 누적
- 사용자 삭제 시 row hard delete 하지 않고 `deleted_at` 설정 → 누적값 보존
- 재오답 시 `deleted_at = NULL` + `total_incorrect_count += 1` → 18 -> 19 자연 누적 (기획안 일치)
- 단일 source of truth = `wrong_answer_terms.total_incorrect_count`

---

## 4. 자동 추가 로직

`LearningCommandService.submitLearning` 안 각 이벤트 처리 시:

`correct=false` 이면:
1. `wrong_answer_terms` upsert `(user_id, term_id)`
   - 없으면 insert
     ```
     incorrect_at = now()
     source_glossary_id = 학습 단어장 ID
     total_incorrect_count = 1
     deleted_at = NULL
     ```
   - 있으면 update
     ```
     incorrect_at = now()
     source_glossary_id = 학습 단어장 ID
     total_incorrect_count = total_incorrect_count + 1
     deleted_at = NULL    -- 삭제된 상태였다면 복구
     ```
2. `term_progresses.incorrect_count` 증가 (기존 구현 그대로)

`correct=true` 이면:
- `wrong_answer_terms` 변경 없음 (자동 졸업/제거 없음 - 기획안 명시)
- `term_progresses` 만 기존대로 갱신

WRONG_ANSWER glossary lazy create 로직 **없음**. race condition 처리 불필요.

---

## 5. API endpoint (5개)

모든 조회 쿼리는 `wrong_answer_terms.deleted_at IS NULL` 필터 포함.

### 5.1 기간별 카드 목록
**`GET /v2/wrong-answer-note/by-period`**

응답:
```json
{
  "periods": [
    { "period": "TODAY", "label": "오늘 틀린 단어",   "incorrectTermCount": 12 },
    { "period": "WEEK",  "label": "이번 주 틀린 단어", "incorrectTermCount": 12 },
    { "period": "MONTH", "label": "이번 달 틀린 단어", "incorrectTermCount": 12 },
    { "period": "ALL",   "label": "전체 틀린 단어",   "incorrectTermCount": 12 }
  ]
}
```

- 카드 4개 고정. label 은 백엔드가 채워서 iOS 가 별도 i18n 불필요.
- 기간 정의:
  - `TODAY`: 오늘 00:00 KST 이후
  - `WEEK`: **자연주** (이번 주 월요일 00:00 KST 이후)
  - `MONTH`: **자연월** (이번 달 1일 00:00 KST 이후)
  - `ALL`: 전체 (조건 없음)
- `incorrectTermCount` = 해당 기간 안의 오답 단어 row 수 (`COUNT(*) WHERE user_id=? AND deleted_at IS NULL AND incorrect_at >= threshold`)

### 5.2 과별 카드 목록
**`GET /v2/wrong-answer-note/by-glossary`**

응답:
```json
{
  "glossaries": [
    { "glossaryId": 1, "title": "정형외과", "category": "외과", "incorrectTermCount": 12 },
    { "glossaryId": 2, "title": "심장내과", "category": "내과", "incorrectTermCount": 12 }
  ]
}
```

- 사용자가 오답을 낸 적 있는 DEFAULT glossary 만 노출 (`incorrectTermCount > 0`)
- 그루핑 기준: `wrong_answer_terms WHERE user_id=? AND deleted_at IS NULL GROUP BY source_glossary_id JOIN glossaries`
- `incorrectTermCount` = 해당 과의 오답 단어 row 수

### 5.3 기간별 상세 (단어 목록) — 4개 endpoint 로 분리
기간 옵션이 고정이라 path enum 대신 개별 endpoint 로 표현 (REST 컨벤션 일치).

- **`GET /v2/wrong-answer-note/today/terms?page=0&size=20&query=`** — 오늘 틀린 단어
- **`GET /v2/wrong-answer-note/week/terms?page=0&size=20&query=`** — 이번 주 틀린 단어 (자연주)
- **`GET /v2/wrong-answer-note/month/terms?page=0&size=20&query=`** — 이번 달 틀린 단어 (자연월)
- **`GET /v2/wrong-answer-note/all/terms?page=0&size=20&query=`** — 전체 틀린 단어

- `query`: 노트 내 검색어 (사전 검색과 동일 정책, 5.7 참조). 빈 값/null 이면 조건 무시
- 응답 항목에 `totalIncorrectCount` 포함

```json
{
  "page": 0, "size": 20, "totalElements": 100, "totalPages": 5, "hasNext": true,
  "terms": [
    {
      "id": 586,
      "spelling": "Acute Kidney Injury",
      "meaning": "급성신손상",
      "abbreviation": "AKI",
      "totalIncorrectCount": 18
    }
  ]
}
```

정렬:
- 빈 쿼리: `incorrect_at DESC` (최근 오답 우선)
- 쿼리 있을 때: relevance DESC, `incorrect_at DESC`

### 5.4 과별 상세 (단어 목록)
**`GET /v2/wrong-answer-note/by-glossary/{glossaryId}/terms?page=0&size=20&query=`**

- `glossaryId`: DEFAULT glossary ID
- `query`: 5.3 과 동일 정책
- 응답 구조는 5.3 과 동일
- 필터: `wrong_answer_terms WHERE user_id=? AND deleted_at IS NULL AND source_glossary_id=?`
- glossary 존재 검증 실패: `DictionaryError.GLOSSARY_NOT_FOUND` (DICTIONARY_E002) — 다른 도메인 자원이라 dictionary ErrorCode 사용

### 5.5 단어 다중 삭제 (soft delete)
**`DELETE /v2/wrong-answer-note/terms`** body `{ "termIds": [123, 456] }`

- 최대 100개. CustomNote 와 일관된 검증 정책 (하나라도 없으면 전체 거부)
- 단어장 컨텍스트 구분 없음 (사용자 x 단어 단위 단일 row 라 어느 화면에서 삭제하든 동일 동작)
- ErrorCode: `WRONG_ANSWER_NOTE_E002 TERM_NOT_IN_NOTE` (존재하지 않거나 이미 deleted_at IS NOT NULL 인 경우)
- 단일 삭제 endpoint **만들지 않음** (디자인 편집 모드는 항상 다중 패턴)
- 실제 수행:
  ```
  UPDATE wrong_answer_terms
     SET deleted_at = NOW()
   WHERE user_id = ? AND term_id IN (?, ?, ...) AND deleted_at IS NULL
  ```
- `term_progresses` 는 cascade 삭제하지 않음 (학습 진행도와 분리)

### 5.6 단어 상세
**별도 endpoint 없음**. 기존 `GET /v1/dictionary/terms/{termId}` 재사용 (북마크/메모/어원 결합 BFF).

### 5.7 노트 내 검색 정책

| 항목 | 정책 |
|---|---|
| 매칭 컬럼 | `terms.spelling`, `terms.meaning`, `terms.abbreviation` (사전 검색과 동일) |
| 매칭 방식 | `MATCH(spelling, meaning, abbreviation) AGAINST(:query IN BOOLEAN MODE)` |
| 인덱스 | V25 의 `ft_terms_search` FULLTEXT 인덱스 (ngram 파서) 재활용 - 추가 인덱스 불필요 |
| 빈 쿼리 (`null` 또는 `""`) | 조건 무시, 전체 반환 |
| 추가 필터 | `wrong_answer_terms.user_id = ? AND deleted_at IS NULL` + period 또는 source_glossary_id |

---

## 6. 빈 상태 처리

iOS 가 `incorrectTermCount == 0` 으로 "오답 단어가 없어요! 학습하러 가기" UI 렌더. 백엔드 별도 응답 분기 불필요.

기간별/과별 상세 진입 시 `terms=[]` & `totalElements=0` 이면 빈 상태.

---

## 7. ErrorCode / SuccessCode

### SuccessCode (`WrongAnswerNoteSuccess`)
| Code | Name | Message |
|---|---|---|
| `WRONG_ANSWER_NOTE_S001` | `LIST_BY_PERIOD` | 기간별 오답노트 카드 조회 성공 |
| `WRONG_ANSWER_NOTE_S002` | `LIST_BY_GLOSSARY` | 과별 오답노트 카드 조회 성공 |
| `WRONG_ANSWER_NOTE_S003` | `LIST_PERIOD_TERMS` | 기간별 오답 단어 목록 조회 성공 |
| `WRONG_ANSWER_NOTE_S004` | `LIST_GLOSSARY_TERMS` | 과별 오답 단어 목록 조회 성공 |
| `WRONG_ANSWER_NOTE_S005` | `DELETE_TERMS` | 오답 단어 다중 삭제 성공 |

### ErrorCode (`WrongAnswerNoteError`)
| Code | Name | Message |
|---|---|---|
| `WRONG_ANSWER_NOTE_E001` | `TERM_NOT_IN_NOTE` | 오답노트에 없는 단어입니다 |
| `WRONG_ANSWER_NOTE_E002` | `INVALID_PERIOD` | 유효하지 않은 기간입니다 |

glossary 존재 검증은 `DictionaryError.GLOSSARY_NOT_FOUND` 사용 (CLAUDE.md 2.4 — 다른 도메인 자원 검증은 그 도메인 ErrorCode).

---

## 8. Period 정의

| Period | 의미 | 쿼리 조건 |
|---|---|---|
| `TODAY` | KST 오늘 00:00 이후 | `incorrect_at >= startOfTodayKst AND deleted_at IS NULL` |
| `WEEK` | 최근 7일 (오늘 포함) | `incorrect_at >= now - 7 days AND deleted_at IS NULL` |
| `ALL` | 전체 | `deleted_at IS NULL` |

모든 조건은 기본적으로 `wrong_answer_terms.user_id = ?` 와 결합.

`incorrect_at` 은 **마지막 오답 발생 시점**. 어제 + 오늘 둘 다 틀렸으면 incorrect_at 이 오늘로 갱신되어 TODAY 에 표시.

---

## 9. DB 마이그레이션 V32

```sql
CREATE TABLE IF NOT EXISTS wrong_answer_terms (
    user_id BINARY(16) NOT NULL,
    term_id BIGINT NOT NULL,
    source_glossary_id BIGINT NULL,
    incorrect_at TIMESTAMP NOT NULL,
    total_incorrect_count INT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (user_id, term_id),
    INDEX idx_wrong_answer_user_incorrect
        (user_id, deleted_at, incorrect_at),
    INDEX idx_wrong_answer_user_source
        (user_id, deleted_at, source_glossary_id, incorrect_at),
    CONSTRAINT fk_wrong_answer_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wrong_answer_term
        FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

주: `source_glossary_id` 에는 FK 를 걸지 않음 (DEFAULT glossary 가 admin 에 의해 삭제되어도 오답 row 는 유지 - `source_glossary_id` 가 dangling 이 되면 과별 그루핑에서 자연스럽게 제외)

인덱스에 `deleted_at` 을 두 번째 컬럼으로 포함시켜 활성 row 만 효율 조회.

`glossary_terms`, `users` 등 기존 테이블 변경 없음.

---

## 10. Migration 영향 (v1->v2 사용자 데이터 마이그레이션)

`MigrationUploadRequest` 에 오답노트 데이터 추가 없음. v1 에 개념 없음.

---

## 11. 작업 순서 (commit 단위)

1. V32 마이그레이션 (`wrong_answer_terms` 테이블 + 인덱스 + FK)
2. `WrongAnswerTermEntity` / `WrongAnswerTermId` / `WrongAnswerTermRepository` 신설
3. `WrongAnswerNoteError` / `WrongAnswerNoteSuccess` enum 신설
4. `LearningCommandService` 훅 — 오답 시 `wrong_answer_terms` upsert (total_incorrect_count += 1, deleted_at = NULL)
5. `WrongAnswerNoteService` — 기간별/과별 조회 + 검색(FULLTEXT) + 다중 삭제 (soft delete)
6. `WrongAnswerNoteApi` / `Controller` — 5개 endpoint
7. Swagger Request example 부착 (`DeleteTermsFromWrongAnswerNoteRequest`)

---

## 12. 결정 사항 (사용자 합의 완료)

| 항목 | 결정 |
|---|---|
| 자동 졸업 | 없음 (수동 삭제만) |
| 정답 시 자동 제거 | 없음 (기획안 명시) |
| period 필터 default | `ALL` |
| 단일 삭제 endpoint | 만들지 않음 (다중만) |
| title 사용자 수정 | 없음 (디자인상 모든 title 정적) |
| 메타 endpoint (별도 요약 API) | 만들지 않음 |
| WRONG_ANSWER glossary 응답 노출 | 해당 사항 없음 - WRONG_ANSWER glossary 인스턴스 자체를 만들지 않음 |
| 단어 대표 과 결정 | `wrong_answer_terms.source_glossary_id` (재오답 시 갱신) |
| 시점 컬럼명 | `incorrect_at` (added_at 보다 의미 명확) |
| 재오답 시 시점 갱신 | 갱신 (`incorrect_at = now()`) |
| 컬럼 위치 | `glossary_terms` 가 아닌 별도 `wrong_answer_terms` 테이블 (mapping 책임 분리) |
| 노트 내 검색 | 사전 검색과 동일 정책 (FULLTEXT MATCH AGAINST spelling/meaning/abbreviation) |
| 검색 정렬 | 빈 쿼리 `incorrect_at DESC`, 쿼리 있을 때 relevance DESC, `incorrect_at DESC` |
| 삭제 시 누적값 보존 방식 | soft delete (`deleted_at TIMESTAMP NULL`, 코드베이스 컨벤션 일치) |
| totalIncorrectCount 산출 | `wrong_answer_terms.total_incorrect_count` 컬럼 직접 사용 (자체 누적, SUM 회피) |
| 카드 termCount 필드명 | `incorrectTermCount` (단어 개수, 다른 도메인 `termCount` 와 의미 충돌 방지) |
| 단어별 누적 필드명 | `totalIncorrectCount` (의미 명확화) |
| 단어 삭제 컬럼 구분 | hard delete 안 함 (soft delete 로 누적 보존) |
| term_progresses cascade 삭제 | 안 함 (학습 진행도와 분리) |
| ErrorCode E001 GLOSSARY_NOT_FOUND | 제거 (DictionaryError 사용, 도메인 일관성) |
| 테스트하기 CTA | Phase 2.8 까지 미룸 (본 Phase 에서는 endpoint 만들지 않음) |
| GlossaryType.WRONG_ANSWER enum value | enum 에는 남기되 v2 코드에서 미사용 (클라이언트 계약 보존) |

---

## 13. 추가 확인 필요

모두 해소.
