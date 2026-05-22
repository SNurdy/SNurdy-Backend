# Phase 2.5 — 오답노트 명세 (v2)

기획 디자인 png 13장 분석 후 재작성. 디자인에 명시된 모든 UX 흐름을 백엔드 API 로 매핑.

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
[상세] 하단 검색 바 -> 노트 내 단어 검색
[상세] 빈 상태 -> "오답 단어가 없어요! 학습하러 가기" CTA
```

---

## 2. 도메인 위치

CLAUDE.md 11.1 원칙에 따라 `customNote` 와 평평한 형제 도메인.

```
domain/user/
├── customNote/         (GlossaryType.CUSTOM)
└── wrongAnswerNote/    (GlossaryType.WRONG_ANSWER)  <- 신규
    ├── controller/
    │   ├── WrongAnswerNoteApi
    │   └── WrongAnswerNoteController
    ├── dto/WrongAnswerNoteDto
    └── service/WrongAnswerNoteService
```

ErrorCode/SuccessCode prefix: `WRONG_ANSWER_NOTE_*`

---

## 3. 데이터 모델

### 3.1 사용자당 WRONG_ANSWER glossary 1개 (lazy 생성)
- 사용자가 처음 오답을 낸 순간 자동 생성
- `type=WRONG_ANSWER`, `ownerId=사용자UUID`, `title="오답노트"` (고정), `category=null`

### 3.2 단어가 속한 카테고리 = DEFAULT glossary 의 category
- 단어 자체에는 카테고리가 없음
- 과별 뷰는 `glossary_terms` 매핑을 거쳐 **단어가 속한 DEFAULT glossary 들**을 lookup
- 한 단어가 여러 DEFAULT glossary 에 속할 수 있음 -> 첫 번째 매칭 또는 우선순위 정책 필요 (3.3)

### 3.3 단어의 "대표 과" 결정 정책
디자인 과별 카드에 단어 수가 표시되므로, 한 단어가 두 과에 속하면 두 과 모두 카운트할지 결정 필요.
- **추천 A**: 단어가 속한 모든 DEFAULT glossary 카운트 (한 단어가 정형외과+심장내과 둘 다면 양쪽 모두 +1) -> 합계가 실제 오답 단어 수보다 클 수 있음
- **추천 B**: 단어당 첫 번째 매칭 glossary 1개만 (안정적이나 사용자 혼란 가능)
- **추천 C**: 단어가 마지막으로 오답난 시점의 학습 컨텍스트 glossary 사용 (가장 정확하나 컬럼 추가 필요)

**제안**: C 채택. `glossary_terms` 에 `source_glossary_id` 컬럼 추가 (오답 발생 시점의 학습 단어장 ID).

### 3.4 신규 컬럼 (V32)
| 테이블 | 컬럼 | 의미 |
|---|---|---|
| `glossary_terms` | `added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` | 단어 추가 시각 (period 필터용) |
| `glossary_terms` | `source_glossary_id BIGINT NULL` | 오답 발생 시점의 학습 단어장 ID (과별 그루핑용, WRONG_ANSWER 행만 사용) |

CustomNote 행은 `source_glossary_id` NULL.

---

## 4. 자동 추가 로직

`LearningCommandService.submitLearning` 안에서 각 이벤트 처리 시:
- `correct=false` 이면:
  1. 사용자 WRONG_ANSWER glossary 조회 (없으면 생성)
  2. `glossary_terms` 에 `(wrongAnswerGlossaryId, termId)` upsert
     - 이미 있으면 `added_at`, `source_glossary_id` 갱신 (재오답이면 시점/소스 최신화)
  3. `TermProgress.incorrectCount` 증가 (이미 구현되어 있음)
- `correct=true` 이면:
  - 자동 졸업 **없음** (사용자 결정 10-1: 현재 졸업 불가능)
  - WRONG_ANSWER glossary 에서 단어 제거 안 함

---

## 5. API endpoint

### 5.1 기간별 카드 목록
**`GET /v2/wrong-answer-note/by-period`**

응답:
```json
{
  "periods": [
    { "period": "TODAY", "label": "오늘 틀린 단어", "termCount": 12 },
    { "period": "WEEK",  "label": "이번 주 틀린 단어", "termCount": 12 },
    { "period": "ALL",   "label": "전체 틀린 단어", "termCount": 12 }
  ]
}
```

- 카드 3개 고정. label 은 백엔드가 채워서 iOS 가 별도 i18n 불필요.

### 5.2 과별 카드 목록
**`GET /v2/wrong-answer-note/by-glossary`**

응답:
```json
{
  "glossaries": [
    { "glossaryId": 1, "title": "정형외과", "category": "외과", "termCount": 12 },
    { "glossaryId": 2, "title": "심장내과", "category": "내과", "termCount": 12 }
  ]
}
```

- 사용자가 오답을 낸 적 있는 DEFAULT glossary 만 노출 (termCount > 0).
- 그루핑 기준: `glossary_terms.source_glossary_id` (3.3 C 안).

### 5.3 기간별 상세 (단어 목록)
**`GET /v2/wrong-answer-note/by-period/{period}/terms?page=0&size=20&query=`**

- `period`: `TODAY` | `WEEK` | `ALL`
- `query`: 노트 내 검색어 (검색 바)
- 응답 항목에 `incorrectCount` 포함

```json
{
  "page": 0, "size": 20, "totalElements": 100, "totalPages": 5, "hasNext": true,
  "terms": [
    {
      "id": 586,
      "spelling": "Acute Kidney Injury",
      "meaning": "급성부비동염",
      "abbreviation": "AKI",
      "incorrectCount": 18
    }
  ]
}
```

### 5.4 과별 상세 (단어 목록)
**`GET /v2/wrong-answer-note/by-glossary/{glossaryId}/terms?page=0&size=20&query=`**

- `glossaryId`: DEFAULT glossary ID
- 응답 구조는 5.3 과 동일

### 5.5 노트 메타 (title 수정 가능 — 사용자 결정 10-4)
**`PATCH /v2/wrong-answer-note`** body `{ "title": "..." }`

- 사용자 결정 10-4 "가능하도록" 반영. 단 디자인 png 어디에도 제목 편집 UI 없음 — 차후 디자인 추가 시 사용.
- title 미수정 시 default "오답노트" 유지.
- 단일 WRONG_ANSWER glossary 대상이므로 path 에 ID 없음.

### 5.6 단어 다중 삭제
**`DELETE /v2/wrong-answer-note/terms`** body `{ "termIds": [123, 456] }`

- 최대 100개, CustomNote 와 일관된 검증 정책 (하나라도 없으면 전체 거부)
- ErrorCode: `WRONG_ANSWER_NOTE_E002 TERM_NOT_IN_NOTE`
- 단일 삭제 endpoint **만들지 않음** (사용자 결정 10-3) — 디자인의 편집 모드는 항상 다중 패턴

### 5.7 단어 상세
**별도 endpoint 없음**. 기존 `GET /v1/dictionary/terms/{termId}` 재사용 (북마크/메모/어원 결합 BFF).

---

## 6. 빈 상태 처리

iOS 가 `termCount==0` 으로 "오답 단어가 없어요! 학습하러 가기" UI 렌더. 백엔드 별도 응답 분기 불필요.

기간별 상세 진입 시 `terms=[]` & `totalElements=0` 이면 빈 상태.

---

## 7. ErrorCode / SuccessCode

### SuccessCode (`WrongAnswerNoteSuccess`)
| Code | Name | Message |
|---|---|---|
| `WRONG_ANSWER_NOTE_S001` | `LIST_BY_PERIOD` | 기간별 오답노트 카드 조회 성공 |
| `WRONG_ANSWER_NOTE_S002` | `LIST_BY_GLOSSARY` | 과별 오답노트 카드 조회 성공 |
| `WRONG_ANSWER_NOTE_S003` | `LIST_PERIOD_TERMS` | 기간별 오답 단어 목록 조회 성공 |
| `WRONG_ANSWER_NOTE_S004` | `LIST_GLOSSARY_TERMS` | 과별 오답 단어 목록 조회 성공 |
| `WRONG_ANSWER_NOTE_S005` | `UPDATE_NOTE` | 오답노트 정보 수정 성공 |
| `WRONG_ANSWER_NOTE_S006` | `DELETE_TERMS` | 오답 단어 다중 삭제 성공 |

### ErrorCode (`WrongAnswerNoteError`)
| Code | Name | Message |
|---|---|---|
| `WRONG_ANSWER_NOTE_E001` | `GLOSSARY_NOT_FOUND` | 해당 과 단어장이 존재하지 않습니다 |
| `WRONG_ANSWER_NOTE_E002` | `TERM_NOT_IN_NOTE` | 오답노트에 없는 단어입니다 |
| `WRONG_ANSWER_NOTE_E003` | `INVALID_PERIOD` | 유효하지 않은 기간입니다 |

---

## 8. Period 정의

| Period | 의미 | 쿼리 조건 |
|---|---|---|
| `TODAY` | KST 오늘 00:00 이후 | `added_at >= startOfTodayKst` |
| `WEEK` | 최근 7일 (오늘 포함) | `added_at >= now - 7 days` |
| `ALL` | 전체 | (조건 없음) |

`added_at` 은 **마지막 오답 발생 시점** (재오답 시 갱신). 즉 한 단어가 어제+오늘 둘 다 틀렸으면 TODAY 에도 표시.

---

## 9. DB 마이그레이션 V32

```sql
-- 1. glossary_terms.added_at (idempotent)
SET @add_added_at = (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE()
             AND table_name = 'glossary_terms'
             AND column_name = 'added_at'),
    'SELECT 1',
    'ALTER TABLE glossary_terms ADD COLUMN added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP'
  )
);
PREPARE stmt FROM @add_added_at; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. glossary_terms.source_glossary_id (idempotent, nullable)
SET @add_source = (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE()
             AND table_name = 'glossary_terms'
             AND column_name = 'source_glossary_id'),
    'SELECT 1',
    'ALTER TABLE glossary_terms ADD COLUMN source_glossary_id BIGINT NULL'
  )
);
PREPARE stmt FROM @add_source; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 인덱스 (조회 성능)
CREATE INDEX IF NOT EXISTS idx_glossary_terms_source
  ON glossary_terms(source_glossary_id, added_at);
```

(IF NOT EXISTS 인덱스 패턴은 MySQL 8.0.29+ 지원. 미지원 환경이면 conditional pattern 적용.)

WRONG_ANSWER glossary 의 사용자당 유일성은 application 레벨 처리 (race condition: try insert -> catch dup -> re-select).

---

## 10. Migration 영향

`MigrationUploadRequest` 에 오답노트 데이터 추가 안 함. v1 에 개념 없음.

---

## 11. 작업 순서 (commit 단위)

1. V32 마이그레이션 (`added_at` + `source_glossary_id`)
2. WrongAnswerNoteError/Success enum 신설
3. WrongAnswerGlossary lazy create 헬퍼
4. `LearningCommandService` 훅 — 오답 시 추가 (source_glossary_id 채움)
5. `WrongAnswerNoteService` — 기간별/과별 조회 + 다중 삭제 + title 수정
6. `WrongAnswerNoteApi`/`Controller` — 6개 endpoint
7. Swagger Request example 부착 (DeleteTermsFromWrongAnswerNoteRequest, UpdateWrongAnswerNoteRequest)

---

## 12. 결정 사항 (사용자 합의 완료)

| 항목 | 결정 |
|---|---|
| 자동 졸업 | 불가능 (수동 삭제만) |
| period 필터 default | `ALL` |
| 단일 삭제 endpoint | 만들지 않음 (다중만) |
| title 사용자 수정 | 가능 (PATCH endpoint) |
| 메타 endpoint (별도 요약 API) | 우선 만들지 않음 |
| WRONG_ANSWER glossary 응답 노출 | `GET /v2/glossaries?type=CUSTOM` 에는 제외, 별도 endpoint 로만 |

---

## 13. 추가 확인 필요 (재작성 후 신규 발견)

1. **단어의 "대표 과" 결정 (3.3)** — A/B/C 중 추천 C (source_glossary_id 컬럼 추가) 선택해도 되는지
2. **테스트하기 CTA** — Phase 2.8 (테스트 분리) 의존. 본 Phase 에서는 endpoint 만들지 않고 기존 `/v1/learning/submit` 재사용? 아니면 2.8 까지 미루기?
3. **상세 화면 검색** — 5.3/5.4 의 `query` 파라미터 — spelling/meaning/abbreviation 어디까지 매칭할지 (사전 검색과 동일 정책 사용 추천)
4. **재오답 시 added_at 갱신** — 4번 정책 (재오답이면 시점 최신화) 확정?
