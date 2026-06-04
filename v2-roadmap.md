# v2 API 로드맵

v2 기능 구현 단계별 진행 상황과 의존 관계를 추적한다.

---

## Phase 1 — 도메인 모델 변경 (완료)

| 단계 | 작업 | 상태 |
|---|---|---|
| 1.1 | UserEntity 확장 (profileImageUrl, marketingConsent, marketingConsentAt, currentStreak/longestStreak/totalLearningTerms) + V26 | done |
| 1.2 | GlossaryEntity 확장 (type, ownerId, shareCode) + V27 | done |
| 1.3 | GlossaryEntity CHECK 제약 (type-ownerId 정합성) + V29 | done |
| 1.4 | UserTermMemoEntity + UserTermMemoId 신규 (termMemo 패키지) + V28 | done |
| 1.5 | UserEntity 단일 level/exp 컬럼 폐기 (V30/V31) — 단어장별 레벨로 전환 | done |

---

## Phase 2 — v2 API 구현

### 단계 요약

| 단계 | 도메인 | 핵심 API | 의존 단계 | 상태 |
|---|---|---|---|---|
| 2.1 | 마이페이지 (User) | 프로필 조회/수정, 마케팅 동의 | Phase 1.1 | done |
| 2.2 | 마이페이지 대시보드 | 화면별 3개 endpoint (메인 + 과 변경 바텀시트 + 과 지표 리스트) | Phase 1.1 | done (재설계 완료) |
| 2.3 | ~~레벨 (다마고치)~~ | ~~사용자 레벨/경험치~~ | - | **폐기** (단어장별 레벨로 전환) |
| 2.4 | 커스텀 단어장 + 단어 관리 | CRUD + 단어 추가/삭제/조회 | Phase 1.2 | done |
| 2.5 | 오답노트 | 기간별/과별 조회, 다중 해제 | Phase 1.2 | done |
| 2.6 | 단어 카드 메모 | upsert/조회/삭제 | Phase 1.4 | done |
| 2.7 | 공유 단어장 | 코드 + URL 듀얼 공유, 본인 단어장으로 복제 | Phase 1.2, 2.4 | done |
| 2.8 | 테스트 분리 | 단어장 종류별 submit | 2.4, 2.5 | 보류 (재검토) |
| 2.9 | ~~홈화면 대시보드~~ | - | - | **폐기** (2.2 마이페이지로 통합) |

---

## 단계별 상세

### 2.1 마이페이지 (User) — done

구현된 endpoint:
- `GET /v2/users/me` — 본인 프로필 조회
- `PATCH /v2/users/me` — 프로필 부분 수정 (닉네임, 이미지 URL)
- `PUT /v2/users/me/marketing-consent` — 마케팅 동의 변경

위치: `domain/user/profile/`

프로필 이미지 업로드 방식 결정 보류 (현재 URL 직접 입력만 지원).

---

### 2.2 마이페이지 대시보드 — done (재설계 완료)

상세 명세: `v2-phase-2.2-spec.md`

구현된 endpoint (`/v2/users/me/dashboard*`):
- `GET /dashboard?startDate=YYYY-MM-DD` — 메인 화면 (streak + selectedGlossary + 7일치 막대 그래프)
- `GET /dashboard/glossaries` — 과 변경 바텀시트 (전체 27개 + 사용자 진척률)
- `GET /dashboard/glossary-statistics?period=WEEK|ALL` — 과 지표 리스트 (학습 시작한 과만, lastLearnedAt 최근순)

자연주 정책: 월요일 시작. WEEK = 이번 주 월요일 ~ 오늘. 메인 막대 그래프만 자연주 예외 (사용자 임의 7일 슬라이딩).

위치: `domain/user/dashboard/`

---

### 2.3 ~~레벨 (다마고치)~~ — 폐기

당초 계획: 사용자별 단일 level/exp 컬럼 + 경험치 적립 시스템.

폐기 사유:
- 다마고치 컨셉이 학습 동기와 직결성 약하다고 판단
- 단어장별 학습률 기반 레벨이 사용자에게 더 직관적
- `GlossaryProgressCalculator` 로 단어장 별 progressRate + level 자동 계산 (저장 컬럼 없음)
- V30/V31 마이그레이션으로 user.level/exp 컬럼 제거됨

대체 구현: 단어장 응답에 `progressRate`, `level` 필드 노출. 학습 submit 응답에 `levelUp` 이벤트 (해당 단어장의 level 이 오른 경우만).

---

### 2.4 커스텀 단어장 + 단어 관리 — done

구현된 endpoint (`/v2/custom-notes/`):
- `POST` — 생성 (자동 생성 title 지원)
- `GET` — 본인 목록
- `GET /{noteId}` — 단건
- `PATCH /{noteId}` — title 수정
- `DELETE /{noteId}` — 단일 삭제 (cascade)
- `DELETE` — 다중 삭제 (전체 또는 거부)
- `POST /{noteId}/terms` — 단어 추가
- `DELETE /{noteId}/terms/{termId}` — 단일 단어 삭제
- `DELETE /{noteId}/terms` — 다중 단어 삭제
- `GET /{noteId}/terms` — 단어 목록 (페이지)

제약: 사용자당 20개, 단어장당 250개, title 최대 14자.

위치: `domain/user/customNote/`

---

### 2.5 오답노트 — done

상세 명세: `v2-phase-2.5-spec.md`

구현된 endpoint (`/v2/wrong-answer-note/`):
- `GET /by-period` — 기간별 카드 (TODAY/WEEK/ALL)
- `GET /by-glossary` — 과별 카드 (source_glossary_id 그루핑)
- `GET /by-period/{period}/terms` — 기간별 단어 목록
- `GET /by-glossary/{glossaryId}/terms` — 과별 단어 목록
- `DELETE /terms` — 다중 soft delete

데이터 모델: 별도 `wrong_answer_terms` 테이블 (V35). PK `(user_id, term_id)`. `GlossaryType.WRONG_ANSWER` enum value 는 보존하되 인스턴스 만들지 않음.

자동 졸업 없음. 수동 삭제만 가능 (재오답 시 누적값 보존을 위한 soft delete).

위치: `domain/user/wrongAnswerNote/`

---

### 2.6 단어 카드 메모 — done

구현된 endpoint (`/v2/terms/{termId}/memo`):
- `GET` — 본인 메모 조회 (단어 상세에 통합되어 있음 — `GET /v1/dictionary/terms/{termId}` 안 `memo` 필드)
- `PUT` — upsert (작성/수정 통합)
- `DELETE` — 삭제

위치: `domain/user/termMemo/`. 본문 최대 500자.

---

### 2.7 공유 단어장 — done

상세 명세: `customNote-redesign-spec.md` 5장 (2.7 spec 은 흡수 후 폐기)

구현된 endpoint:
- `POST /v2/custom-notes/{noteId}/share` — 공유 코드 발급 (활성 있으면 SHARE_CODE_ALREADY_EXISTS)
- `GET /v2/custom-notes/{noteId}/share` — 본인 공유 코드 조회
- `DELETE /v2/custom-notes/{noteId}/share` — 비활성화 (shareCode = null)
- `GET /v2/custom-notes/by-share-code/{code}` — 코드로 공유 단어장 조회
- `POST /v2/custom-notes/from-share/{code}` — 본인 단어장으로 복제 (title 충돌 시 (2), (3) suffix)

특징:
- 8자 영숫자 코드 (모호 문자 제외), SecureRandom
- 공유 코드는 `custom_notes.share_code` 컬럼 통합 (별도 테이블 안 씀)
- 활성 코드 재발급 막음 (먼저 비활성화 필요)
- Universal Link prefix 환경 변수 `SHARING_CUSTOM_NOTE_URL_PREFIX`

---

### 2.8 테스트 분리 — 보류 (재검토)

당초 계획: 단어장 종류별로 별도 submit endpoint.

현재 상태: 기존 `POST /v1/learning/submit` 이 모든 단어장 종류 처리. 오답 발생 시 `WrongAnswerNoteService.recordIncorrect` 자동 호출. 단어장 종류별 분기가 service 안에 이미 흡수됨.

본 단계의 실제 가치 재검토 필요:
- 별도 endpoint 분리가 클라이언트에 명확성을 주는가 vs 통합이 단순한가
- 현재 통합 흐름이 정상 작동 중이라 추가 가치 불확실

결정: 추가 요구사항 발생 전까지 보류.

---

### 2.9 ~~홈화면 대시보드~~ — 폐기

디자인 png 분석 결과 마이페이지(2.2)와 통합된 단일 화면임이 확인됨. 별도 endpoint 불필요.

홈 = 마이페이지 메인 + 과 변경 바텀시트 + 과 지표 리스트 (2.2 의 3개 endpoint 로 모두 대응).

---

## 진행 체크리스트

각 단계 시작 시 `todo` -> `in-progress`, 끝나면 `done` 으로 위 표 상태 갱신.

### 남은 작업

1. **2.8 테스트 분리** — 보류. 사용자 피드백/디자인 요구 후 재검토

---

## 공통 작업 (Phase 2 전반)

### v2 컨트롤러 패키지 구조

도메인 패키지 안에 같이 두기로 결정 (`domain/user/profile/` 등). v1 과 v2 가 같은 도메인이면 같은 패키지에 다른 클래스명 (UserMeController 등).

### v1 deprecation 계획

forced update 가능한 환경이라 v2 안정화 후 v1 endpoint 들은 한 차례 정리. 다만 일정은 v2 출시 후 결정.

---

## 결정 누적

| 항목 | 결정 |
|---|---|
| v2 namespace | URL prefix `/v2` |
| Glossary 통합 모델 | type enum (DEFAULT/CUSTOM/WRONG_ANSWER) + ownerId + shareCode |
| 기존 Glossary 데이터 | type=DEFAULT 일괄 채움 |
| iOS 강제 업데이트 | 가능 |
| 메모 모델 | 별도 UserTermMemoEntity (옵션 B-1 복합키) |
| 메모 패키지 | `domain/user/termMemo/` |
| 공유 코드 길이 | 8자 영숫자 (모호 문자 제외) — 기획 변경됨 (당초 12자) |
| 공유 매개체 | 코드 + URL 듀얼 (Universal Link 로 카톡 1탭 import 지원) |
| 메모 본문 길이 | 500자 |
| 마케팅 동의 기본값 | false |
| 사용자 레벨/경험치 | 폐기 (단어장별 레벨로 대체) |
| 오답노트 데이터 모델 | 별도 `wrong_answer_terms` 테이블 (WRONG_ANSWER glossary 인스턴스 안 만듦) |
| 오답노트 졸업 정책 | 자동 졸업 없음, 수동 삭제만 |
| 오답노트 단어장 그루핑 | `source_glossary_id` (마지막 오답 발생 시 학습 단어장) |
| CustomNote 한도 | 사용자당 20개, 단어장당 단어 250개, title 14자 (code point 기준, 이모지 1자) |
| CustomNote 모델 | 별도 entity `custom_notes` 테이블 (Step 2 재설계) |
| CustomNote 삭제 | hard delete (cascade) |
| CustomNote 공유 코드 모델 | `custom_notes.share_code` 컬럼 통합 (별도 테이블 안 씀) |
| 자연주 정의 | 월요일 시작. WEEK = 이번 주 월요일 ~ 오늘 (오답노트/대시보드 일관) |
| 자연월 정의 | 매달 1일 ~ 오늘 |
| 단어장 레벨 범위 | 1 ~ 5 (기획 조이 표 기준, 0 폐기) |
| 단어장별 레벨 산출 | `GlossaryProgressCalculator` (학습률 기반, 저장 컬럼 없음) |
| DTO 매핑 컨벤션 | Kotlin extension function (`toResponse`/`toEntity`/의미 키워드) |
| 도메인 아키텍처 | 현재 Spring 표준 layered. DDD-Lite 도입 검토 중 (별도 명세 `ddd-lite-migration-spec.md`) |
