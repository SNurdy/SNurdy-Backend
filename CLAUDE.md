# SNurdy Backend — Claude 코드 작성 가이드

이 문서는 Claude 가 이 프로젝트에서 코드를 작성/수정할 때 반드시 따라야 할 컨벤션과 원칙을 정리한다.
지금까지 사용자가 의문/불만 제기한 사례를 원칙으로 만들었으므로, 매 변경 전에 이 가이드와 충돌하지 않는지 자체 점검한다.

---

## 1. 네이밍 컨벤션

### 1.1 줄임말 금지
변수, 클래스, 필드, 메서드, 패키지명에 영어 줄임말을 사용하지 않는다. 풀네임으로 작성한다.

| 금지 | 사용 |
|---|---|
| `stats`, `stat` | `statistics`, `statistic` |
| `req`, `res` | `request`, `response` |
| `msg`, `ctx`, `cfg` | `message`, `context`, `config` |

**예외 — 약어로 굳어진 도메인 용어**: `URL`, `JWT`, `OAuth`, `API`, `ID`, `UUID`, `JSON`, `XML`, `HTML`, `SQL`, `DTO`, `auth` (Spring Security 관용).

짧은 로컬 변수 (`it`, 람다 인자) 와 표준 단어 (`id`, `userId`) 는 허용.

### 1.2 클래스명 접두어/접미어 컨벤션
- **응답 DTO**: `*Response` (예: `StudyNoteResponse`, `AddTermsResponse`)
- **요청 DTO**: `*Request` (예: `CreateStudyNoteRequest`)
- **`Result`, `Reply`, `Dto`, `Vo` 접미어 금지** (응답이면 Response, 요청이면 Request)
- **내부 service 모델** (외부 노출 아님) 은 `*Result` 허용 (예: `ValidationResult`)
- **Entity**: `*Entity` (예: `UserEntity`, `GlossaryEntity`)

### 1.3 메서드명 — 동작 + 대상 명시
메서드명에서 "무엇을 하는지" 만 적고 "무엇에 대해" 를 생략하면 안 된다. 패키지/클래스 컨텍스트가 명확해도 메서드 단독으로 의미가 통해야 한다.

| 금지 | 사용 |
|---|---|
| `delete()`, `deleteMany()` | `deleteNote()`, `deleteNotes()` |
| `removeTerm()` 와 `delete()` 혼재 | 동사 통일 — `deleteTerm()`, `deleteNote()` |
| `create()`, `list()`, `get()` | `createNote()`, `listNotes()`, `getNote()` |

**원칙**: 같은 도메인 안에서 동사를 한 가지로 통일 (`delete` 또는 `remove` 중 하나, 섞지 말 것).

### 1.4 SuccessCode / ErrorCode enum 네이밍
- 코드 **문자열 prefix 는 도메인을 정확히 반영**해야 한다. 예: `STATS_S001` 이 calendar 도메인에 속하면 `CALENDAR_S001` 로 신규 발급 (단, **기존 클라이언트 계약 변경 시 사용자 합의 필요**).
- enum class 이름 = 도메인명 + Success/Error (예: `StudyNoteSuccess`, `StudyNoteError`).
- enum value 이름 = 동작 + 대상 (예: `CREATE_NOTE`, `LIST_NOTES`, `DELETE_TERM`).

---

## 2. 도메인 책임 분리

### 2.1 도메인 패키지 원칙
- `domain/{도메인}/` 안에 해당 도메인의 책임만 둔다.
- 한 도메인이 다른 도메인의 entity 를 직접 import 하는 건 가능. 단 "다른 도메인의 데이터를 응답에 결합" 하는 BFF 책임은 명확히 표시.

### 2.2 사용자 시점 도메인 vs 공용 데이터 도메인
- 공용 데이터 (사전, 단어장 카탈로그 등): `dictionary/`
- 사용자 본인 시점 데이터: `user/{서브도메인}/` (account/profile/streak/dashboard/studyNote)
- 관리자 권한 그룹: `admin/{서브도메인}/`

### 2.3 BFF (Backend for Frontend) 가 다른 도메인 데이터를 결합할 때
- 명시적으로 `Client*` 접두어 사용 (예: `ClientDictionaryService` — 사전 데이터 + 사용자 컨텍스트).
- 또는 별도 사용자 시점 도메인으로 분리.
- service/dto 가 user 정보를 다룬다는 점을 이름으로 표현.

### 2.4 ErrorCode 의 도메인 일관성
**서비스 메서드가 던지는 ErrorCode 는 그 서비스의 도메인에 속해야 한다.**

| 잘못된 예 | 정정 |
|---|---|
| `StudyNoteService.deleteTerm` 이 `DictionaryError.TERM_NOT_IN_GLOSSARY` throw | `StudyNoteError.TERM_NOT_IN_NOTE` 신설 |
| `LearningCommandService` 가 `DictionaryError` 만 throw | 학습 도메인 검증은 `LearningError` 신설 |

**예외**: 다른 도메인의 자원 검증 (예: 사전에 없는 termId) 은 그 도메인의 ErrorCode 사용이 자연스러움 (`DICTIONARY_E002 TERM_NOT_FOUND`).

---

## 3. 매핑/DTO 변환

### 3.1 entity ↔ DTO 매핑은 Kotlin extension function 으로
service 메서드 안에 매핑 로직을 두지 않는다. companion `from()` 같은 자바 컨벤션도 사용하지 않는다. DTO 파일에 extension function 으로 분리한다.

```kotlin
// 금지 - service 안 매핑
class CustomNoteService {
    private fun toResponse(glossary: GlossaryEntity, ...): CustomNoteResponse { ... }
}

// 금지 - companion from
data class CustomNoteResponse(...) {
    companion object {
        fun from(glossary: GlossaryEntity, ...): CustomNoteResponse { ... }
    }
}

// 권장 - dto 파일 안 extension function
data class CustomNoteResponse(...)

fun GlossaryEntity.toResponse(learnedCount: Int, termCount: Long): CustomNoteResponse = ...
fun CreateCustomNoteRequest.toEntity(ownerId: UUID): GlossaryEntity = ...
```

함수 네이밍 규칙:
- **1:1 매핑** (한 receiver → 한 dto): `toResponse()` 통일
- **1:N 매핑** (한 receiver → 여러 dto): 의미 키워드로 구분 (`toSummary()`, `toDetail()`, `toAction()`, `toUserInfo()`, `toProfileResponse()` 등)
- **Page extension**: `Page<XxxEntity>.toResponse()` 통일 (receiver 가 다른 도메인과 충돌 없음)
- **dto → entity**: `toEntity()` 통일 (필요하면 추가 인자 받음)
- **합성형** (다중 source 조합, 외부 계산 결과 필요): service 가 직접 생성 (extension 강제 안 함)

의존 방향:
- DTO 가 entity 를 import 하는 단방향만 허용 (entity 가 dto 를 알면 안 됨)
- 다른 도메인 entity 와 매핑이 필요하면 그 도메인의 entity 만 import (역참조 금지)

### 3.2 계산 helper 는 별도 object 로 추출
같은 계산이 2곳 이상에 반복되면 즉시 helper 로 추출한다.

예: `progressRate` 와 `level` 계산을 3곳에서 복붙 → `GlossaryProgressCalculator` object 로 추출.

### 3.3 service 책임 — 비즈니스 로직만
service 가 비즈니스 로직 + 매핑 + 계산을 모두 가지면 비대해진다. 매핑은 DTO 로, 계산은 helper 로 분리.

---

## 4. 응답 설계

### 4.1 의미 없는 필드 노출 금지
응답에 클라이언트가 처리할 분기가 없는 필드를 두지 않는다.

**예 (금지)**: 단어장 안 단어만 선택 가능한 UX → `skipped` 필드는 항상 0 → 응답에 두지 말 것.

### 4.2 단일/다중 작업의 검증 정책 일관
단일 삭제가 "없으면 에러" 면 다중 삭제도 "하나라도 없으면 전체 에러". 다중만 묵시적 멱등으로 다르게 처리하지 않는다.

```kotlin
// 단일: 단어장에 없으면 STUDY_NOTE_E007
fun deleteTerm(...) { 
    if (!exists) throw ApiException(StudyNoteError.TERM_NOT_IN_NOTE)
}

// 다중: 일부 없으면 전체 거부 (일관)
fun deleteTerms(...) {
    if (existing.size != input.size) throw ApiException(StudyNoteError.TERM_NOT_IN_NOTE)
}
```

### 4.3 필드 의미 명확화
필드 이름이 의미와 어긋나면 rename 또는 description 추가. 같은 이름이 두 곳에서 다른 의미면 한쪽을 변경.

예: `DailyProgress.reviewedCount` (총 처리수) vs `LearningSubmitResponse.reviewedCount` (복습만) → DB 컬럼을 `completedCount` 로 rename.

### 4.4 응답 필드 description
응답 DTO 의 각 필드에 `@field:Schema(description = "...")` 로 의미 명시. Swagger 명세에 그대로 노출.

---

## 5. ErrorCode / SuccessCode 사용

### 5.1 코드 문자열은 클라이언트 계약 — 변경 금지
한 번 발급된 `STUDY_NOTE_S001` 같은 코드 문자열은 **재번호/변경 금지**. enum class/value 이름은 자유 변경.

**미사용 enum value 는 그대로 둔다** (이전 응답으로 받은 클라이언트와 호환).

### 5.2 비즈니스 로직에서 메시지 하드코딩 금지
`throw ApiException(ErrorCode, message = "...")` 같이 메시지 직접 작성 금지. enum 의 message 만 사용.

### 5.3 인프라 장애 vs 비즈니스 실패 구분
- **인프라 장애** (Redis/DB 다운 등): `CommonError.SERVICE_UNAVAILABLE` (503). endpoint 어노테이션에 명시하지 않음 (글로벌 처리).
- **비즈니스 실패**: 도메인 ErrorCode + endpoint `@ApiErrorCodes` 어노테이션에 명시.

---

## 6. Swagger 응답 명세

### 6.1 endpoint 별 어노테이션 필수
모든 endpoint 에 `@ApiSuccessCode` + `@ApiErrorCodes` 어노테이션 부착. 신규 endpoint 추가 시 누락 금지.

### 6.2 인프라 장애는 명세 제외
모든 endpoint 잠재적이라 endpoint별 명시 안 함.

---

## 7. git / commit

### 7.1 commit 메시지
- **한 줄 subject 만** (한국어).
- `body`, `Co-Authored-By` 라인 **금지**.
- prefix: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `revert` 중 하나.

### 7.2 구현 완료 시 자동 commit
사용자가 별도 지시하지 않아도 구현 단위가 끝나면 컴파일 확인 후 즉시 commit. 의미 단위로 분리.

### 7.3 commit 제외 파일
`.claude/`, `v2-roadmap.md` 등 로컬/문서 파일은 commit 대상 아님.

---

## 8. 마이그레이션 (Flyway)

### 8.1 한 번 적용된 V## 파일은 수정 금지
Flyway checksum mismatch 발생. 새 V## 추가로 정정.

### 8.2 idempotent pattern 사용
컬럼/제약 추가/삭제 시 `information_schema` 로 존재 여부 체크 후 동적 ALTER. 재실행 안전.

```sql
SET @add_col = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE ...),
        'SELECT 1',
        'ALTER TABLE ... ADD COLUMN ...'
    )
);
PREPARE stmt FROM @add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 8.3 MySQL 호환 SQL
`ALTER TABLE DROP COLUMN IF EXISTS` 같은 MariaDB-only 구문 사용 금지. 위 conditional pattern 사용.

---

## 9. 코드 주석

### 9.1 ASCII + 한국어만
이모지, 화살표, 체크 표시 등 특수문자(온점 포함) 사용 금지. 일반 ASCII + 한국어 만.

### 9.2 WHY 만, WHAT 은 금지
코드가 무엇을 하는지는 코드 자체가 표현. 주석은 그렇게 한 이유 (제약, 의도, 비즈니스 규칙) 만 작성.

### 9.3 짧고 정확하게
한 줄 충분하면 한 줄. 여러 줄 docstring/block comment 지양.

---

## 10. 의사 결정 패턴

### 10.1 큰 변경 전에 옵션 제시
도메인 분리, URL 변경, 응답 구조 변경 등 영향 범위 큰 작업은:
1. 영향 범위 분석
2. 2~3개 옵션 제시 (장단점 명시)
3. 추천 + 이유
4. 사용자 확인 후 진행

### 10.2 도그마틱 분리 주의
"도메인 책임 분리" 원칙이라도 본질에 어긋나면 분리 안 함. 예: 사전 화면이 본질적으로 user-aware BFF 라면 `dictionary` 도메인이 user 데이터 결합하는 게 자연스러움.

### 10.3 이름 결정 시 한국 개발자 직관 고려
영어 단어 후보 중 한국 개발자에게 직관적인 것 선택 (예: `library` 는 "도서관" 연상으로 거부).

### 10.4 추상 super 도메인 금지 — 타입별 평평한 분리
여러 타입을 한 추상 super 패키지 (`studyNote/`) 로 묶으면 안 됨. 타입별 별도 도메인 패키지 (`customNote/`, `wrongAnswerNote/`) 로 평평하게 분리. 타입 명이 클래스/필드 이름에 명시되어야 책임이 자명.

### 10.5 부정확한 작업은 솔직히 인정 후 정정
이전 답변/결정이 잘못되었으면 정직하게 인정. 합리화하지 않음. 사용자가 의문 제기 = 거의 항상 사용자가 옳음.

---

## 11. 사용자 도메인 컨텍스트

### 11.1 사용자 시점 도메인 구조 (v2)
```
domain/user/
├── account/         UserEntity, 탈퇴
├── profile/         프로필, 마케팅 동의
├── streak/          학습 streak
├── dashboard/       학습 리포트 BFF
├── customNote/      사용자 소유 커스텀 단어장 (GlossaryType.CUSTOM)
└── (wrongAnswerNote/) 향후 — 오답노트 (GlossaryType.WRONG_ANSWER)
```

각 단어장 type 별로 별도 도메인 패키지를 둔다 (`studyNote` 같은 추상 super 도메인 금지).
이름 접미사는 `Note` 로 통일 (`CustomNote`, `WrongAnswerNote`).

### 11.2 사전 도메인 (공용, 단 사용자 시점 BFF 포함)
```
domain/dictionary/
├── glossary/  term/  morpheme/   (entity, repository — pure)
├── controller/ClientDictionaryApi  (사용자 컨텍스트 — Client 접두어로 의도 표시)
├── controller/MorphemeSearchApi    (사용자 무관)
└── service/
    ├── DictionaryQueryService     (pure, admin 도 사용)
    └── ClientDictionaryService    (BFF, bookmark/progress 결합)
```

### 11.3 SuccessCode / ErrorCode prefix 정책
도메인별로 코드 prefix 사용:
- `COMMON_*` — 공통 (400/401/403/404/500/503 등)
- `AUTH_*` — 인증
- `USER_*` — 사용자 (account/profile/streak/dashboard 통합)
- `CUSTOM_NOTE_*` — 사용자 커스텀 단어장
- (`WRONG_ANSWER_NOTE_*` — 향후 오답노트)
- `DICTIONARY_*` — 사전
- `CALENDAR_*` — 캘린더
- `LEARNING_*` — 학습 활동
- `BOOKMARK_*` — 북마크
- `MIGRATION_*` — 마이그레이션
- `ADMIN_*` — 관리자
- `VERSION_*` — 버전 체크

---

## 12. 변경 전 자체 점검 체크리스트

코드를 작성/수정하기 전 다음을 자체 점검:

- [ ] 클래스/메서드/필드 이름이 줄임말 없이 풀네임인가
- [ ] DTO 접미어가 Request/Response 인가
- [ ] 메서드명에 동작+대상이 모두 있는가
- [ ] entity→DTO 매핑이 service 가 아닌 DTO companion `from()` 인가
- [ ] 같은 계산이 2곳 이상 반복되면 helper 로 추출했는가
- [ ] ErrorCode 가 해당 service 의 도메인에 속하는가
- [ ] 응답에 의미 없는 필드를 넣지 않았는가
- [ ] 단일/다중 작업의 검증 정책이 일관되는가
- [ ] 코드 문자열 (SuccessCode/ErrorCode value) 을 변경하지 않았는가
- [ ] 큰 변경이면 옵션 제시 + 사용자 확인 받았는가
- [ ] 이미 적용된 Flyway 마이그레이션 파일을 수정하지 않았는가
- [ ] 신규 endpoint 에 `@ApiSuccessCode` + `@ApiErrorCodes` 어노테이션을 붙였는가
- [ ] 구현 완료 후 한국어 한 줄 commit 메시지로 commit 했는가
