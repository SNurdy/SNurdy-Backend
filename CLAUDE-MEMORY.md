# Claude 작업 컨텍스트 (다른 기기 이어작업용)

이 문서는 Claude 가 다른 기기에서 작업을 이어갈 때 참고해야 할 사용자 정보, 작업 스타일, 누적된 규칙을 정리한 것이다.
`CLAUDE.md` 와 중복되는 항목도 일부 있으나, 메모리에 저장된 원문 그대로를 보존한다.

새 기기 (집 노트북) 에서 작업 시작 시 Claude 에게 이 파일을 읽혀서 컨텍스트를 복원시킨다.

---

## 1. 사용자 배경

- Java + Spring Boot 경험 풍부
- Kotlin 은 AI 도움으로 작성된 상태이며, 본인이 직접 이해하는 것이 목표
- 코드 설명 시 Java/Spring 대응 개념과 비교해주면 효과적

---

## 2. 코드 설명 형식 (파일 설명 요청 시)

코드 파일을 설명할 때 아래 순서를 반드시 지킨다.

1. **전체 역할 요약** — 이 파일이 어떤 책임을 지는지 한 줄로
2. **핵심 로직 설명** — 전체가 아닌 중요한 부분 위주. Java+Spring 대응 개념이 있으면 비교해서 설명
3. **import 목적** — 각 import 가 왜 필요한지, 어디서 쓰이는지
4. **플로우** — 요청이 들어왔을 때 이 파일 기점으로 흐름이 어떻게 이어지는지
5. **보안 및 버그 검토** — 아래 4가지 기준만:
   - 인증/인가 처리 누락 또는 취약점
   - 외부 입력값 검증 여부
   - 설정값/시크릿 노출 위험
   - 추후 유지보수에서 문제될 수 있는 코드 패턴

**How to apply**: 파일을 올리면 질문 없이 바로 이 형식으로 설명. 코드나 구조에 의문점이 생기면 설명 전에 먼저 물어볼 것. 설명은 핵심 위주로 간결하게.

---

## 3. ErrorCode / SuccessCode 안정성

ErrorCode 및 SuccessCode enum 의 `code` 필드 문자열 (예: `USER_E005`, `ADMIN_E027`) 은 클라이언트(모바일 앱 등)가 분기 로직에 사용하는 **공개 계약**이다.

- 새 enum 항목 추가는 OK (다음 비어있는 번호 사용)
- 기존 항목의 code 문자열 변경, 재번호, 항목 삭제는 제안하지 말 것. 사용자가 명시적으로 "클라이언트 같이 배포할 거니 정리해줘" 라고 할 때만 수행
- 분석 단계에서 "넘버링 일관성" 은 단순 정보 제공으로만 언급하고, 자동 수정 후보로 올리지 않는다
- message 필드는 클라이언트가 분기에 쓰지 않으므로 다듬어도 무방

**Why**: 클라이언트 배포 주기와 서버 배포 주기가 다르므로, 서버에서 코드 문자열을 바꾸면 구버전 클라이언트의 에러 분기가 깨진다.

---

## 4. 비즈니스 로직 메시지 하드코딩 금지

Service/Controller/도메인 클래스에서 예외를 던질 때, **메시지 문자열을 인라인으로 적지 않는다**. `ApiException(ErrorCode)` 형태로 ErrorCode enum 이 보유한 message 만 사용한다.

금지:
```kotlin
throw ApiException(CommonError.BAD_REQUEST, "해당 용어는 용어장에 속하지 않습니다")
throw ApiException(DictionaryError.TERM_NOT_FOUND, "term id=$id")
```

권장:
```kotlin
throw ApiException(DictionaryError.TERM_NOT_IN_GLOSSARY)
throw ApiException(DictionaryError.TERM_NOT_FOUND)
```

**How to apply**:
- 새로운 에러 케이스가 필요하면 `ErrorCode.kt` 의 적절한 enum 에 새 항목 추가, 그 enum 을 던진다
- 기존 enum 의 message 가 부정확하면 enum 의 message 를 수정하지 말고 (클라이언트 계약), 더 구체적인 새 enum 을 추가한다
- 동적 값 (id 등) 이 필요한 케이스라도 비즈니스 로직에서는 메시지에 박지 말고 로그로만 남긴다

**적용 범위 예외**:
- `global/exception/ExceptionExtensions.kt` 같은 글로벌 예외 어댑터 계층은 이 규칙에서 제외 (Bean Validation 메시지 동적 전달 허용)
- 위 규칙은 Service / Controller / 도메인 클래스에 한정

---

## 5. Git Commit 메시지 규칙

1. **한 줄 subject 만 작성**, 상세 body 절대 금지
2. **`Co-Authored-By: Claude ...` 라인 절대 금지**
3. Conventional Commits 접두어 (`refactor:`, `fix:`, `chore:`, `feat:` 등) 사용. **scope 괄호/도메인명 금지**
   - 금지: `feat(user): ...`, `fix(infra): ...`
   - 권장: `feat: ...`, `fix: ...`
4. 한국어 한 줄 요약. 도메인명은 메시지 본문에 자연스럽게 포함

예시: `feat: 사용자 프로필 이미지 컬럼 추가`

---

## 6. 구현 완료 후 자동 커밋

구현 단위가 끝나면 별도 "커밋해줘" 지시 없이 바로 git commit 까지 진행.

- 한 도메인 / 한 기능 / 명확히 마무리되는 작업 단위로 commit
- 컴파일 확인 후 커밋
- `src/` 외 파일 (`.claude/`, `v2-roadmap.md` 등) 은 commit 대상에서 제외 (단, 사용자가 명시적으로 commit 요청하면 예외)
- 한 번에 여러 도메인을 묶어야 할 만큼 광범위하면 사용자에게 묶기 vs 분리 확인
- 단순 탐색/분석/질문 응답은 커밋 불필요

---

## 7. 줄임말 금지 (네이밍)

변수/클래스/필드/메서드/패키지명에 영어 줄임말 사용 금지. 항상 풀네임.

- `stats` -> `statistics`, `stat` -> `statistic`
- `req` -> `request`, `res` -> `response`, `msg` -> `message`
- `ctx` -> `context`, `cfg` -> `config`
- `auth` 는 관용 허용
- 약어로 굳어진 도메인 용어 예외: URL, JWT, OAuth, API, ID, UUID, JSON, XML, HTML, SQL, DTO
- 짧은 로컬 변수 (`it`, 람다 인자) 관용 허용
- 기존 코드에서 줄임말 발견 시 별도 지시 없어도 풀네임으로 정정 제안

---

## 8. 주석 작성 스타일

### 8.1 ASCII + 한국어만
이모지/체크/별/경고/화살표 등 특수문자 금지.

대체 표기:
- 화살표 -> `->` 또는 `-->`
- 체크 -> `OK` / `[done]`
- 강조 -> `주의:` / `참고:` / `중요:`
- 예시 -> `ex)` / `예:`

### 8.2 주석 끝 마침표 금지
주석 문장 끝에 `.` 절대 금지. 여러 문장이 필요하면 줄 분리.

```kotlin
// 잘못된 예
// 단어장 종류 구분. DEFAULT 외에는 ownerId 가 반드시 채워진다.

// 올바른 예 (단문)
// 단어장 종류 구분

// 올바른 예 (장문은 여러 줄)
// 단어장 종류 구분
// DEFAULT 외에는 ownerId 가 반드시 채워진다
```

### 8.3 적용 범위
- 코드 파일 안의 주석에만 적용
- 채팅 응답에는 적용 안 됨

---

## 9. Swagger example 부착 범위

`@Schema(example = ...)` / `@Parameter(example = ...)` 는 **Request 영역에만** 부착.

- Request body DTO 의 각 필드: `@field:Schema(description = "...", example = "...")` 필수
- query param / path param: `@Parameter(description = "...", example = "...")` 필수
- Response DTO: `description` 만 부착 (필요 시), `example` 은 부착 안 함

**Why**: iOS 가 request payload 모양을 명확히 알아야 호출 가능. Response 는 type/schema 만으로 모델링 충분, example 은 과한 정보.

**How to apply**:
- 신규 endpoint 추가 시 Request DTO 의 모든 필드에 example 부착
- 기존 endpoint 보강 시 Request 만 작업, Response 는 건드리지 않음
- 사용자가 별도 지시할 때까지 이 정책 유지

---

## 10. 현재 진행 상황 스냅샷 (2026-05-22 기준)

### 최근 완료
- v2 dictionary 도메인 재정비 (Client 접두어 제거, controller 통합)
- 단어 검색에 category/glossaryId 필터 추가 (단일 dynamic native query)
- Swagger Request example 보강 (P1 — Auth/Learning/Migration/CustomNote)

### 다음 작업 (이 기기에서 결정 완료)
- **Phase 2.5 오답노트 구현** — `v2-phase-2.5-spec.md` 의 명세대로
- Phase 2.5 에서 사용자 확정 결정:
  - 자동 졸업: 불가능
  - period 필터 default: ALL
  - 단일 삭제 endpoint: 만들지 않음
  - title 사용자 수정: 가능
  - 메타 endpoint: 우선 만들지 않음
  - WRONG_ANSWER glossary 응답 노출: `GET /v2/glossaries?type=CUSTOM` 에는 제외, 별도 endpoint 로만

### Phase 2.5 미확정 (구현 시작 전 답 받아야 함)
1. 단어 "대표 과" 결정 정책 (3가지 옵션 A/B/C, 추천 C — `source_glossary_id` 컬럼 추가)
2. "테스트하기" CTA — Phase 2.5 에 포함 vs Phase 2.8 까지 미루기
3. 노트 내 검색 매칭 범위 (spelling/meaning/abbreviation)
4. 재오답 시 `added_at` 갱신 정책 확정 여부

### 그 외 미진행 (v2-roadmap.md 참조)
- Phase 2.1 마이페이지 (User 프로필)
- Phase 2.2 학습 리포트 대시보드
- Phase 2.3 레벨 (다마고치)
- Phase 2.7 공유 단어장
- Phase 2.8 테스트 분리
- Phase 2.9 홈 대시보드
- P2 Admin Swagger Request example 보강 (사용자가 안 해도 된다고 결정)
