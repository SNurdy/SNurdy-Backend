# CustomNote 재설계 + 공유노트 통합 명세 (Step 2)

기획팀 요구 (공유노트 기능) + 도메인 일관성 (CustomNote 별도 entity 분리) 통합 명세.

DB 에 사용자 학습 데이터 없는 시점이라 자유로운 재설계 가능 (데이터 손실 위험 0).

**상태: 구현 완료** (commit 79d3a66, ecc8724, 5e4348a, 96e46ec 외 다수)

구현 중 추가 정책 변경:
- 공유 코드 별도 테이블 폐기 → `custom_notes.share_code` 컬럼 통합 (V38/V39)
- revoked_at 컬럼 폐기 → 비활성화 시 `shareCode = null` 처리 (감사 추적 불필요)

---

## 1. 결정 사항 (사용자 합의 완료)

| 항목 | 결정 | 근거 |
|---|---|---|
| CustomNote 모델 | 별도 entity `custom_notes` 테이블 | WrongAnswerTerm 별도 테이블 패턴 일관 |
| CustomNote 단어 매핑 | 별도 `custom_note_terms` 테이블 | glossary_terms 와 분리 |
| ShareCode 모델 | `custom_notes.share_code` 컬럼 통합 (구현 중 변경) | 1:1 관계 + revoked 추적 불필요로 별도 테이블 과설계로 판정 |
| glossaries 테이블 cleanup | type/owner_id/share_code 컬럼 모두 제거 | DEFAULT 만 남으니 자기 자신만 가리키는 컬럼 무의미 |
| GlossaryType enum | 폐기 | DEFAULT 만 남으면 enum 무의미 |
| CUSTOM/WRONG_ANSWER 학습 SRS | 갱신 안 함 (term_progresses 는 DICTIONARY 전용) | 진척률 노출 안 하고, SM-2 의 단어 난이도는 사전 학습 기준 유지 |
| **CUSTOM/WRONG_ANSWER 학습 시 daily_progresses + streak** | **갱신** (streak 채움) | UX 일관 — 어디서 학습하든 streak 유지 |
| CUSTOM/WRONG_ANSWER 학습 시 오답 처리 | wrong_answer_terms 갱신 | 기존 흐름 유지 |
| 북마크 적용 범위 | DEFAULT 사전 단어만 (기존 유지) | 기획팀 확언 |
| CustomNote 별 진척률 노출 | 없음 (단어 개수만) | 기획 결정 |
| glossaryProgress 추적 범위 | DEFAULT 사전 단어장만 | CUSTOM 진척률 안 보여주니 추적 불필요 |
| 공유 매개체 | URL + 코드 둘 다 백엔드 응답 | iOS 의 두 진입 경로 (카톡 링크 / 앱 내 코드) 지원 |
| 공유 코드 길이 | 8자 영숫자 (모호 문자 제외) | 손 입력 가능성 고려 |
| CustomNote 삭제 | hard delete (cascade) | 현재 코드와 일치, 데이터 없으니 변경 불필요 |
| **CustomNote 자동 title 생성** | **폐기, 사용자 필수 입력** | 자동 생성된 "단어장 N" 가치 낮음 |
| **import 시 title 충돌 처리** | **숫자 suffix 자동 증가 — `외과 단어장 (2)`, `(3)`, `(4)` ...** | UX 단순 |
| **공유 코드 재발급 정책** | 활성 코드 있으면 `SHARE_CODE_ALREADY_EXISTS` throw, 비활성화 후 재발급 시 새 코드 | 감사 추적 불필요 시점 |
| **shareUrl 도메인** | **`snurdy.org` (보유 도메인)** | 환경별 설정 가능 |

---

## 2. 데이터 모델

### 2.1 신규 테이블

**`custom_notes`** — 사용자 커스텀 단어장
```sql
CREATE TABLE custom_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BINARY(16) NOT NULL,
    title VARCHAR(14) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_note_owner_title (owner_id, title),
    INDEX idx_custom_note_owner (owner_id, created_at DESC),
    CONSTRAINT fk_custom_note_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**`custom_note_terms`** — CustomNote ↔ Term 매핑
```sql
CREATE TABLE custom_note_terms (
    note_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id, term_id),
    INDEX idx_custom_note_term_term (term_id),
    CONSTRAINT fk_custom_note_term_note FOREIGN KEY (note_id) REFERENCES custom_notes(id) ON DELETE CASCADE,
    CONSTRAINT fk_custom_note_term_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**공유 코드는 `custom_notes.share_code` 컬럼 통합** (V38 에서 추가, V39 에서 별도 테이블 폐기)
```sql
-- V38
ALTER TABLE custom_notes ADD COLUMN share_code VARCHAR(12) NULL;
ALTER TABLE custom_notes ADD UNIQUE INDEX uk_custom_notes_share_code (share_code);

-- V39
DROP TABLE custom_note_share_codes;
```

- 활성화 상태 `share_code IS NOT NULL`
- 비활성화 시 `share_code = NULL` (감사 추적 없음)
- VARCHAR(12) — 추후 길이 확장 대비 여유 (현재 8자)

### 2.2 `term_progresses` 변경 없음

PK `(user_id, glossary_id, term_id)` 그대로 유지.
- glossary_id 는 사전 단어장만 참조 (cleanup 후 자명)
- CUSTOM/WRONG_ANSWER 학습은 term_progresses 갱신 안 함

### 2.3 `glossaries` 테이블 cleanup

```sql
-- V37
ALTER TABLE glossaries DROP CHECK chk_glossary_owner;
ALTER TABLE glossaries DROP INDEX uk_glossary_share_code;
ALTER TABLE glossaries DROP COLUMN type;
ALTER TABLE glossaries DROP COLUMN owner_id;
ALTER TABLE glossaries DROP COLUMN share_code;
```

→ 결과: `glossaries(id, category, title, created_at, updated_at)` — 순수 사전 카탈로그

### 2.4 `wrong_answer_terms.source_glossary_id`

그대로. glossary_id 가 사전 단어장만 가리킴.

### 2.5 `bookmarks` / `glossary_progresses`

변경 없음. 사전 단어장만 다룸.

---

## 3. 도메인 위치 / 패키지 구조

```
domain/
├── dictionary/
│   └── glossary/
│       └── entity/
│           ├── GlossaryEntity        (cleanup 후 사전 카탈로그 전용)
│           ├── GlossaryTermEntity
│           └── GlossaryTermId
│           (GlossaryType enum 폐기)
│
├── user/
│   ├── customNote/
│   │   ├── controller/
│   │   │   ├── CustomNoteApi             (기존 CRUD)
│   │   │   ├── CustomNoteController
│   │   │   ├── CustomNoteShareApi        (신규 - 공유 관련)
│   │   │   └── CustomNoteShareController
│   │   ├── dto/
│   │   │   ├── CustomNoteDto             (기존)
│   │   │   └── CustomNoteShareDto        (신규)
│   │   ├── entity/
│   │   │   ├── CustomNoteEntity          (신규)
│   │   │   ├── CustomNoteTermEntity
│   │   │   ├── CustomNoteTermId
│   │   │   └── CustomNoteShareCodeEntity (신규)
│   │   ├── repository/
│   │   │   ├── CustomNoteRepository
│   │   │   ├── CustomNoteTermRepository
│   │   │   └── CustomNoteShareCodeRepository
│   │   └── service/
│   │       ├── CustomNoteService          (재작성)
│   │       ├── CustomNoteShareService     (신규)
│   │       ├── CustomNoteImportService    (신규 - title 충돌 처리 포함)
│   │       └── ShareCodeGenerator         (8자 nanoid 스타일)
│   │
│   └── wrongAnswerNote/  (그대로)
│
└── learning/  (변경 거의 없음)
```

---

## 4. ErrorCode / SuccessCode 변경

### 4.1 신규 (`CustomNoteSuccess` 확장)
| Code | Name | Message |
|---|---|---|
| `CUSTOM_NOTE_S0xx` | `ISSUE_SHARE_CODE` | 공유 코드 발급 성공 |
| `CUSTOM_NOTE_S0xx` | `GET_SHARED_NOTE` | 공유 단어장 조회 성공 |
| `CUSTOM_NOTE_S0xx` | `IMPORT_SHARED_NOTE` | 공유 단어장 불러오기 성공 |
| `CUSTOM_NOTE_S0xx` | `REVOKE_SHARE_CODE` | 공유 비활성화 성공 |

### 4.2 신규 (`CustomNoteError` 확장)
| Code | Name | Message |
|---|---|---|
| `CUSTOM_NOTE_E0xx` | `SHARE_CODE_NOT_FOUND` | 유효하지 않거나 만료된 공유 코드입니다 |
| `CUSTOM_NOTE_E0xx` | `SHARE_CODE_GENERATION_FAILED` | 공유 코드 발급에 실패했습니다 |
| `CUSTOM_NOTE_E0xx` | `TITLE_REQUIRED` | 단어장 제목은 필수입니다 |

기존 재사용:
- `CUSTOM_NOTE_E0xx NOTE_LIMIT_EXCEEDED` (20개 한도) — import 도 같은 에러 사용
- `CUSTOM_NOTE_E0xx DUPLICATED_TITLE` — create 시 동일 title 충돌 (import 시는 자동 suffix 라 발생 X)

### 4.3 폐기
- `GlossaryType` 관련 ErrorCode (있다면)

실제 번호는 기존 enum 의 마지막 +1 부터 부여. CLAUDE.md 의 코드 안정성 원칙 준수.

---

## 5. API endpoint (공유노트)

### 5.1 공유 코드 발급/조회
**`POST /v2/custom-notes/{noteId}/share`**

응답:
```json
{
  "noteId": 12,
  "title": "외과 단어장",
  "ownerNickname": "지윤",
  "termCount": 42,
  "shareCode": "k7Mx9pQ2",
  "shareUrl": "https://snurdy.org/shared/k7Mx9pQ2"
}
```

- 권한: 본인 CustomNote 만
- 멱등 — 활성 코드 있으면 같은 코드 반환
- `revoked_at` 채워진 상태에서 재호출 시 같은 note_id row 덮어쓰기 (회전)

### 5.2 코드로 단어장 조회
**`GET /v2/custom-notes/by-share-code/{code}`**

응답:
```json
{
  "shareCode": "k7Mx9pQ2",
  "title": "외과 단어장",
  "ownerNickname": "지윤",
  "termCount": 42,
  "createdAt": "2026-05-20T10:15:30Z",
  "page": 0, "size": 20, "totalPages": 3, "hasNext": true,
  "terms": [...]
}
```

- 권한: 로그인 필요. 누구나 조회
- 단어 목록 페이지네이션
- 코드가 `revoked_at` NOT NULL 또는 존재 안 함 → `CUSTOM_NOTE_E0xx SHARE_CODE_NOT_FOUND`
- 원본 CustomNote 삭제 시 cascade → 자연 만료

### 5.3 본인 단어장으로 복제
**`POST /v2/custom-notes/from-share/{code}`**

응답: `CustomNoteResponse` (신규 생성된 본인 단어장)

- 권한: 로그인 필요. 본인 단어장 코드 복제도 허용
- 새 `CustomNoteEntity` 생성 (`owner_id` = 복제자)
- title 결정:
  - 원본 title 그대로 시도
  - 본인 단어장 중 동일 title 있으면 `원본 (2)` 시도
  - 또 있으면 `원본 (3)`, `(4)` ... 증가
  - 최대 한도 (예: 100) 까지 시도, 그래도 충돌이면 에러
- `custom_note_terms` 매핑 복사
- 본인 CustomNote 20개 한도 도달 시 `NOTE_LIMIT_EXCEEDED`
- **복제 후 원본과 완전 독립**

### 5.4 공유 비활성화
**`DELETE /v2/custom-notes/{noteId}/share`**

- `custom_note_share_codes.revoked_at = now()` 처리
- 권한: 본인 CustomNote 만
- 재발급 시 같은 row 덮어쓰기 (회전)

---

## 6. 공유 코드 정책

### 6.1 코드 포맷
- 8자, 영문 + 숫자 혼합
- 모호 문자 제외 (`0OIl1`)
- 사용 문자 풀: `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz` (54자)
- 조합 수: 54^8 ≈ 7.2조
- DB UNIQUE 충돌 검출 시 재발급 retry 최대 3회

### 6.2 URL 생성 — Universal Link
- 형식: `{prefix}{code}` (예: `https://snurdy.org/shared/k7Mx9pQ2`)
- prefix 는 application.yml 환경별 설정 (보유 도메인 `*.snurdy.org`):
  ```yaml
  sharing:
    glossary-url-prefix: ${SHARING_GLOSSARY_URL_PREFIX:https://snurdy.org/shared/}
  ```
- `@ConfigurationProperties("sharing")` 으로 바인딩

### 6.3 진입 경로 두 가지
1. **카톡 등 외부 메신저** — URL 클릭 → iOS Universal Link → 앱이 URL 에서 code 추출 → import 자동 호출
2. **앱 내 코드 입력** — 사용자가 8자 코드 직접 타이핑 → 같은 endpoint 호출

백엔드 입장: 단일 endpoint. iOS 측 진입 흐름만 다름.

---

## 7. 학습 처리 정책

### 7.1 사전 (DICTIONARY) 학습 — 기존 그대로
- `term_progresses` 갱신 (SM-2 알고리즘)
- `glossary_progresses.learned_count` 갱신
- 오답 시 `wrong_answer_terms` 자동 추가
- `daily_progresses` 갱신
- streak 갱신
- endpoint: `POST /v1/learning/submit` (기존)

### 7.2 CUSTOM 학습 — Phase 2.8 신설
- term_progresses 갱신 **안 함**
- glossary_progresses 갱신 **안 함**
- `daily_progresses.completed_count` **+1 per 이벤트** (streak 위해)
- streak 갱신 (currentStreak / longestStreak)
- 오답 시 `wrong_answer_terms` 자동 추가
- endpoint: `POST /v2/tests/custom-notes/{noteId}/submit` (Phase 2.8 시 신설)

### 7.3 WRONG_ANSWER 학습 — Phase 2.8 신설
- 같은 정책 (CUSTOM 과 동일)
- endpoint: `POST /v2/tests/wrong-answer-note/submit` (Phase 2.8 시 신설)

### 7.4 LearningCommandService 영향
- 현재 `/v1/learning/submit` 은 사전 학습 전용으로 유지
- Phase 2.8 endpoint 들은 별도 service 또는 분기 처리
- streak/daily_progresses 갱신 로직은 공통 헬퍼로 추출 권장

### 7.5 Step 2 와 Phase 2.8 의 관계
- **Step 2 (본 명세)** = CustomNote/공유노트/glossaries cleanup
- **Phase 2.8 (별도)** = CUSTOM/WRONG_ANSWER 학습 endpoint 신설
- Step 2 진행 시점에는 디자인의 "테스트하기" 버튼 백엔드 미지원
- Phase 2.8 까지는 iOS 와 협의 (버튼 비활성 또는 alert 노출)
- Step 2 완료 후 Phase 2.8 진행 권장 (모델 안정화 후 endpoint)

---

## 8. 마이그레이션 순서

| 버전 | 작업 |
|---|---|
| V36 | `custom_notes`, `custom_note_terms`, `custom_note_share_codes` 신설 |
| V37 | `glossaries` cleanup (type/owner_id/share_code/CHECK 제거) |
| V38 | `custom_notes.share_code` 컬럼 추가 (UNIQUE) |
| V39 | `custom_note_share_codes` 테이블 폐기 |

**term_progresses 변경 없음.**

---

## 9. 코드 변경 영향 범위

### 9.1 신규 작성
- `CustomNoteEntity` (shareCode 컬럼 포함), `CustomNoteTermEntity` (+ Id 클래스)
- `CustomNoteRepository` (findByShareCode, existsByShareCode 포함), `CustomNoteTermRepository`
- `CustomNoteShareService`, `CustomNoteImportService`, `ShareCodeGenerator`
- `CustomNoteShareApi`, `CustomNoteShareController`
- `CustomNoteShareDto`
- `SharingProperties` (application.yml 바인딩)
- **`CodePointSize` custom validation annotation + `CodePointSizeValidator`** (`global/validation/`)
  - Bean Validation 표준 `@Size` 가 `String.length` (UTF-16 code unit) 기준이라 이모지를 2자로 카운트하는 문제 해결
  - code point 기준 검증 (이모지 1자 = 1 카운트)
  - title/nickname/memo 등 한글+이모지 혼합 입력 필드 모두 적용 권장

### 9.2 전면 재작성
- `CustomNoteService` — GlossaryRepository 의존 제거, CustomNoteRepository 사용
  - **자동 title (`단어장 N`) 로직 제거**
  - `resolveTitle` private 메서드 폐기
  - `CreateCustomNoteRequest.title` 이 nullable → required (Validation 변경)
- `CustomNoteApi`, `CustomNoteController` — 시그니처는 거의 같으나 entity 변경 반영
- `CustomNoteDto` — 매핑 extension 의 receiver 가 `GlossaryEntity` → `CustomNoteEntity`
- `CreateCustomNoteRequest` — `title: String?` → `title: String`
  - `@field:NotBlank` + `@field:CodePointSize(max = 14)` 적용 (기존 `@field:Size` 대신)
- `UpdateCustomNoteRequest` — `@field:Size(min=1, max=14)` → `@field:CodePointSize(min=1, max=14)`

### 9.3 부분 수정
- `BookmarkService.verifyGlossaryContainsTerm` — DEFAULT 만 받음 (이미 사실상 그러함)
- `GlossaryRepository` — type/owner 관련 메서드 모두 제거
- `WrongAnswerNoteService` — 변경 없음 (source_glossary_id 그대로)
- `dashboard` — glossary 목록이 모두 DEFAULT 라 변경 없음
- `LearningCommandService` — glossaryId 가 사전 단어장만 받음 (CUSTOM ID 거부)

### 9.4 폐기
- `GlossaryType` enum
- `GlossaryEntity` 의 type/ownerId/shareCode 필드
- `GlossaryRepository` 의 `findAllByOwnerIdAndTypeOrderByCreatedAtDesc`, `countByOwnerIdAndType`, `existsByOwnerIdAndTitle`, `findAutoGeneratedTitlesByOwner`, `findDefaultGlossaryTermCounts` 등
- `AdminGlossaryQueryService` 의 type 분기 (있다면)
- `CustomNoteService.resolveTitle` private 메서드 + 관련 상수 (`AUTO_TITLE_PREFIX`, `AUTO_TITLE_PATTERN`)

---

## 10. 작업 순서 (chunk)

각 chunk = 1 commit, 1~2시간.

| # | 작업 | 의존 |
|---|---|---|
| 1 | V36 마이그레이션 (custom_notes 계열 3개 테이블 신설) | - |
| 2 | CustomNoteEntity / 관련 entity + repository 신설 | 1 |
| 3 | CustomNoteService 재작성 (CustomNoteRepository 기반, 자동 title 제거) | 2 |
| 4a | `CodePointSize` annotation + Validator 신설 (`global/validation/`) | - |
| 4b | CustomNoteController/Api/Dto 갱신 (title required, `@CodePointSize` 적용) | 3, 4a |
| 5 | application.yml sharing 설정 + SharingProperties + ShareCodeGenerator | - |
| 6 | CustomNoteShareService 신설 (발급/조회/비활성화) | 2, 5 |
| 7 | CustomNoteImportService 신설 (suffix 자동 증가) | 2 |
| 8 | CustomNoteShareController/Api/Dto (공유 endpoint 4개) | 6, 7 |
| 9 | V37 마이그레이션 + GlossaryEntity 컬럼 제거 + GlossaryType enum 폐기 | 1~8 후 |
| 10 | GlossaryRepository / AdminGlossaryQueryService 정리 + 회귀 테스트 + Swagger 점검 | 9 |

순서 중요: **9번은 1~8번 후** (CUSTOM 이 새 entity 로 분리된 뒤에 GlossaryEntity 의 컬럼 제거해야 안전).

---

## 11. 검증 기준 (Definition of Done)

- [ ] V36, V37 마이그레이션 정상 실행
- [ ] `custom_notes`, `custom_note_terms`, `custom_note_share_codes` 테이블 존재
- [ ] `glossaries` 에 type/owner_id/share_code 컬럼 없음
- [ ] `GlossaryType` enum 코드베이스에 없음
- [ ] CustomNote 생성 시 title 필수 (NotBlank 검증)
- [ ] CustomNote 기존 endpoint 모두 정상 동작 (CRUD + 단어 추가/삭제/조회)
- [ ] 공유노트 endpoint 4개 모두 정상 동작
- [ ] 공유 코드 발급 — 활성 코드 재호출 시 같은 코드, revoked 후 재호출 시 새 코드 (덮어쓰기)
- [ ] import 시 title 충돌 → `(2)`, `(3)` ... 자동 suffix
- [ ] 학습 submit (`/v1/learning/submit`) 정상 동작 (사전 단어장만)
- [ ] 오답노트 자동 추가 정상 (WrongAnswerNoteService.recordIncorrect)
- [ ] Swagger 명세에 공유노트 4개 endpoint 노출 + Request example 부착
- [ ] 한 줄 한국어 commit, 도메인명/특수문자 없음

---

## 12. 잠재 위험 / 트레이드오프

| 위험 | 대응 |
|---|---|
| LearningCommandService 가 CUSTOM glossaryId 받으면 처리 불가 | Phase 2.8 시점 별도 endpoint 신설. 본 단계에서는 사전 단어장만 동작 |
| 디자인의 "테스트하기" 버튼 동작 미지원 (CUSTOM/WRONG_ANSWER) | iOS 와 협의 — Phase 2.8 까지 노출 X 또는 alert |
| 공유 URL prefix 환경 변수 누락 시 기본값 동작 | application.yml 에 `snurdy.org` 기본값 명시 |
| import 시 title suffix 무한 증가 | 최대 100 까지 시도, 그래도 충돌이면 `DUPLICATED_TITLE` throw |
| 기존 v2 단계에서 만든 CUSTOM glossary row (데이터 없음) | 무관 |

---

## 13. 영향 받는 기존 문서

이 명세 시행 시 갱신 필요:
- `CLAUDE.md` 11.1 — CustomNote 패키지 구조 (entity 분리 반영)
- `CLAUDE.md` 11.3 — `GLOSSARY_*` ErrorCode 가 cleanup 된 사실 반영
- `v2-roadmap.md` — 2.7 공유 단어장 진행 가능 상태 + 2.8 Phase 2.8 신설 endpoint 정책 명시
- `v2-phase-2.7-spec.md` — 이 명세로 흡수, 폐기
