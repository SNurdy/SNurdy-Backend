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
| 2.8 | 테스트 분리 | 학습/테스트 phase 분리, 7종 testType, 시작/제출 endpoint | 2.4, 2.5 | done |
| 2.9 | ~~홈화면 대시보드~~ | - | - | **폐기** (2.2 마이페이지로 통합) |
| 2.10 | 학습/테스트 기능 확장 | 커스텀노트/오답노트로 cards/distractors/submit 확장. 정통 REST sub-resource | 2.4, 2.5, 2.8 | done |
| 2.11 | Daily goal | 사용자별 일일 학습 목표 단어 수 (10/20/30/40/50). todayGoalCompleted 로직 정정 | 2.10 | todo |

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

### 2.8 학습/테스트 분리 — done

상세 명세: `v2-phase-2.8-spec.md`

학습 flow 를 카드 phase (인지 학습) 와 테스트 phase (정오 평가) 로 분리. card / test / progress 3개 도메인 패키지로 책임 분담. 디자인 PNG 의 "테스트를 완료해야 오늘의 학습 진도가 반영돼요" 카피와 일관.

구현된 endpoint:
- `PUT /v2/users/me/preferences/test-type` — 사용자 기본 테스트 유형 변경
- `POST /v2/cards` — 학습 카드 조회 (NEW 우선 + RAND 단일 SQL, requireAbbreviation 자동 매핑)
- `POST /v2/test/distractors` — 다지선다 오답 생성 (MC_* testType 일 때만 호출, distractors 3개)
- `POST /v2/test/submit` — 테스트 결과 일괄 제출 (응답에 progressRate before/after + levelUp)

도메인 패키지:
- `domain/card/` — 학습 카드 조회 책임
- `domain/test/` — 다지선다 보기 + 테스트 제출 책임
- `domain/progress/` — SRS / TermProgress / GlossaryProgress / DailyProgress (이전 learning 패키지 rename)
  - `ProgressUpdateService.updateProgress` — test 가 submit 시 위임 호출
  - `ProgressBatchCache` — events batch prefetch 캐시 (N+1 회피)
  - `SrsCalculator` — SM-2 변형 알고리즘

핵심 설계:
- TestType enum 7종 (TERM_TO_MEANING, MEANING_TO_TERM, LISTEN_AND_WRITE, WRITE_ABBREVIATION, MC_PICK_TERM, MC_PICK_MEANING, MC_PICK_ABBR)
- 카드/테스트 단어 일치는 client 책임 (cards 응답 termIds 를 distractors/submit 에 전달)
- 카드 phase 진행 중 backend 호출 없음 (시작 시 cards 1회만)
- 테스트 phase 진행 중 backend 호출 없음 (시작 시 distractors 1회만 MC 인 경우)
- 정오 판정은 클라이언트 책임 (테스트 본문 prompt 와 correctAnswer 모두 클라가 cards 데이터에서 derive)
- 다지선다 보기는 같은 단어장 우선 + 부족 시 다른 단어장 random 보충, 정답 텍스트와 중복 안 되게 server 가 보장
- submit 시 SRS/오답노트/streak/totalLearningTerms 갱신은 `ProgressUpdateService.updateProgress` 위임
- dailyProgress 활성 = 오늘 첫 테스트 완료 (카드 phase 가 호출 없으므로)

UserEntity.defaultTestType 컬럼 추가 (V40). null 이면 다음 진입 시 모달 강제.

SuccessCode/ErrorCode:
- `USER_S030 UPDATE_DEFAULT_TEST_TYPE`
- `CARD_S001 LIST_CARDS`
- `TEST_S001 LIST_DISTRACTORS`, `TEST_S002 SUBMIT_TEST`
- `TEST_E001 TEST_TYPE_NOT_AVAILABLE`, `TEST_E002 TEST_RESULT_INVALID`

---

### 2.9 ~~홈화면 대시보드~~ — 폐기

디자인 png 분석 결과 마이페이지(2.2)와 통합된 단일 화면임이 확인됨. 별도 endpoint 불필요.

홈 = 마이페이지 메인 + 과 변경 바텀시트 + 과 지표 리스트 (2.2 의 3개 endpoint 로 모두 대응).

---

### 2.10 학습/테스트 기능 확장 — spec 완료, 구현 대기

상세 명세: `v2-phase-2.10-spec.md`

Phase 2.8 의 학습 카드 / 다지선다 오답 생성 / 테스트 결과 제출 흐름을 기본 단어장에서 커스텀노트, 오답노트로 확장.

정통 REST sub-resource 패턴 채택 — Phase 2.8 의 `POST /v2/cards`, `/v2/test/distractors`, `/v2/test/submit` 단일 endpoint 가 noteType polymorphic body 로 분기되는 RPC 스타일을 폐기하고, 노트 타입별 URL 분리. 기본 단어장도 새 URL 로 이전. 커스텀노트 도메인은 URL 을 singular 로 통일 (`/v2/custom-notes` → `/v2/custom-note`, breaking).

구현 예정 endpoint (12개, URL prefix `/v2/test/` 통일):
- 기본 단어장: `POST /v2/test/glossaries/{glossaryId}/cards|distractors|submit`
- 커스텀노트: `POST /v2/test/custom-notes/{customNoteId}/cards|distractors|submit`
- 오답노트 (기간): `POST /v2/test/wrong-answer-notes/by-period/{period}/cards|distractors|submit`
- 오답노트 (과): `POST /v2/test/wrong-answer-notes/by-glossary/{glossaryId}/cards|distractors|submit`

핵심 정책:
- 진도 트래킹 (SRS / GlossaryProgress / DailyProgress / totalLearningTerms / streak) 은 기본 단어장만 갱신. 커스텀/오답은 진도 시스템 / 스트릭 전반에 영향 X (방식 B 채택)
- 오답 자동 기록 (WrongAnswerTerm) 은 기본 단어장 + 오답노트만 갱신. 커스텀노트는 기록 안 함 (과 선택 불가 구조라 sourceGlossaryId 매핑 불가, 오답노트에 누적해도 활용 불가)
- 오답노트 재오답은 totalIncorrectCount += 1, incorrectAt 갱신. 졸업 없음, 정답이어도 row 유지
- 응답 DTO 분리 — 기본 단어장은 `SubmitGlossaryTestResponse` (todayGoalCompleted, glossary 진척 포함), 커스텀/오답은 `SubmitNoteTestResponse` (`{ correctCount, incorrectCount }` 만)
- distractors 출처: 기본 단어장은 같은 glossary 우선, 커스텀은 같은 customNote 우선, 오답은 각 단어의 sourceGlossaryId 기준 같은 과 우선
- cards 단어 부족 정책 (방식 B): 4종 공통 — count > available 시 `TEST_E001`. 클라이언트가 사전에 단어장/노트 상세 응답의 `availableCounts` 로 출제 개수 selector max 결정

---

## 진행 체크리스트

각 단계 시작 시 `todo` -> `in-progress`, 끝나면 `done` 으로 위 표 상태 갱신.

### 남은 작업

- Phase 2.11 daily goal 신설 (spec 작성 중)
- 마이페이지 설정 화면 동의 항목 (기획 확인 후 spec 작성)
- 커스텀노트 북마크 흐름 (단어를 커스텀 단어장에 저장) — 기존 북마크와의 관계 결정 후 spec

후속 검토 항목 (별도 phase):
- 테스트 정오 판정 서버 측 재검증 (부정행위 방지)
- SRS due 우선 출제 sampling 알고리즘
- 자주 틀린 단어 (3회+ 오답, 홈 daily 1) 별도 endpoint

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
| 학습/테스트 phase 분리 | 카드 phase 는 backend 호출 없음. 진도/streak/totalLearningTerms 갱신은 테스트 submit 시점만 |
| TestType enum | 7종 분리 (작성형 4 + 다지선다 3 variant). UI 5종 표시 시 클라이언트가 매핑 |
| 테스트 출제 개수 | 클라이언트가 결정 (학습하기 Home UI), 1 부터 50. 부족 케이스 처리는 Phase 2.10 에서 방식 B 로 변경 — 클라이언트가 가용 단어 수 이하로 사전 제한, 서버는 count > available 시 `TEST_E001` |
| 다지선다 보기 출처 | 같은 단어장 우선 + 부족 시 다른 단어장 random 보충 |
| 테스트 정오 판정 | 클라이언트 책임 (응답에 correctAnswer 동봉). 추후 서버 재검증 도입 가능성 열어둠 |
| 테스트 sessionId | 도입 안 함 (stateless cards/distractors/submit). 부정행위 방지는 후속 검토 |
| default test type 저장 | `user_preferences.default_test_type` (V42 에서 UserEntity 컬럼에서 이관, V43 에서 users.default_test_type DROP) |
| 도메인 패키지 분리 | `domain/card/` + `domain/test/` + `domain/progress/` 3개로 책임 분담 (learning 패키지는 progress 로 rename) |
| ProgressUpdateService 책임 | internal API. 호출자가 termId-glossary 매핑 검증 책임, 본 서비스는 user 존재 확인 + SRS/streak/오답노트 갱신만 |
| ProgressBatchCache | events batch prefetch 캐시 (N+1 회피용). 별도 파일로 추출 |
| cards 의 testType 노출 | 도메인 누수 임시 허용 (추후 generic filter 로 리팩토링) |
| 카드 sampling 알고리즘 | NEW 우선 + RAND, 단일 SQL (term_progresses LEFT JOIN) |
| 다지선다 정답 중복 보장 | server 가 정답 derive 후 distractors 에서 정답 텍스트 제외. 각 문제 단위로만 적용 |
| 카드/테스트 단어 일치 | client 가 cards termIds 를 distractors/submit 에 전달 (server stateless 유지) |
| 중도 종료 재진입 | Home 부터 새로 시작. 이전 테스트 없던 일 처리 |
| 테스트 코드 prefix | TEST_* (이전 LEARNING_S020/E020 등 미배포 폐기) |
| 학습 카드 코드 prefix | CARD_* |
| Jackson non_null | 글로벌 제거. null 필드도 응답에 명시적으로 노출 (응답 스키마 일관성 우선) |
| 학습/테스트 확장 URL 전략 | 정통 REST sub-resource + `/v2/test/` prefix 통일 (Phase 2.10, 옵션 B). noteType polymorphic body 폐기, 12개 endpoint 로 분리 |
| 학습/테스트 확장 URL 동사 | submit 유지 (test-results 폐기). cards/distractors 는 명사 |
| 학습/테스트 확장 URL 단/복수 | 노트 타입 plural 통일 (glossaries, custom-notes, wrong-answer-notes). 기존 `/v2/custom-notes/*` 그대로 (breaking 없음) |
| 학습/테스트 확장 도메인 구조 | card 도메인 폐지 후 test 도메인으로 흡수. controller 노트 타입별 3 파일, service 는 CardService / TestService 두 개로 책임 분리 |
| 커스텀/오답 테스트 진도 트래킹 | 전반 제외 (방식 B). SRS / GlossaryProgress / DailyProgress / totalLearningTerms / streak 모두 갱신 안 함 |
| 커스텀노트 오답 자동 기록 | 기록 안 함. 과 선택 불가 구조라 sourceGlossaryId 매핑 불가, 오답노트에 누적해도 활용 불가 |
| 오답노트 재오답 처리 | totalIncorrectCount += 1, incorrectAt 갱신. 졸업 없음, 정답이어도 row 유지 |
| 커스텀/오답 응답 DTO | 기본 단어장과 분리 — `SubmitGlossaryTestResponse` / `SubmitNoteTestResponse` (커스텀/오답은 `{ correctCount, incorrectCount }` 만) |
| 카드 단어 부족 정책 | 4종 공통 방식 B. count > available 시 `TEST_E001`. 가능한 만큼만 반환 정책 폐기 |
| 가용 단어 수 노출 | **기본 단어장 상세 응답에만** `availableCounts` 필드 추가. 커스텀/오답은 단어 수 고정이라 selector 자체가 없음 |
| ProgressUpdateService 메서드 | `updateProgressWithSrs` 1개만 유지. 커스텀/오답은 호출 안 함 |
| 오답노트 졸업 정책 | 없음. 정답 처리해도 wrong_answer_terms 유지 (수동 삭제만), totalIncorrectCount 만 += 1 |
| 오답노트 진입 방식 | by-period 또는 by-glossary 필터 필수 (양자택일, path 로 분리) |
| 커스텀노트 cards 정렬 | createdAt 순 |
| distractors 풀 부족 처리 | 한 단어만 못 채워도 전체 `TEST_E001` (전체 단어 약 3000개라 실 발생 거의 없음) |
| submit termId 검증 | 기본 단어장 submit 만 적용 (기존 `validateTermIdsBelongToGlossary` 유지). 커스텀/오답은 클라 책임 |
| `wrong_answer_terms.sourceGlossaryId` | non-null (오답노트 단어는 항상 sourceGlossaryId 가짐) |
| user preferences 도메인 분리 | `domain/user/preference/` 신설. `user_preferences` 테이블 1:1 매핑 (userId PK). defaultTestType + selectedGlossaryId + 향후 dailyGoal 통합 관리 |
| user_preferences row 생성 시점 | 가입 service 가 user 생성 트랜잭션 안에서 INSERT. 기존 유저는 V42 일괄. 모든 user 에 row 보장 |
| selectedGlossaryId 결정 로직 | `user_preferences.selected_glossary_id` 단일 source. 자동 fallback / lastLearnedAt 추적 폐기. 신규 유저는 심장내과(6) 자동 세팅, NOT NULL |
| selected-glossary mutation endpoint | `PUT /v2/users/me/preferences/selected-glossary` body `{ glossaryId }` non-null. dashboard 도메인이 아닌 preference 도메인 (데이터 소유 원칙) |
| preferences GET endpoint | `GET /v2/users/me/preferences` 신설. UserProfileResponse 에서 defaultTestType 분리 (breaking) |
| 프로필 이미지 업로드 | `POST /v2/users/me/profile-image` multipart. 백엔드 multipart 경유 방식 (presigned URL 미사용) |
| S3 라이브러리 | `io.awspring.cloud:spring-cloud-aws-starter-s3` 3.3.x (Spring Boot 3.4 호환). SDK v2 직접 사용 안 함 |
| S3 환경 분리 | dev / prod 별도 버킷 (`snurdy-backend-dev`, `snurdy-backend-prod`). local 은 dev 버킷 공유 + `local/` prefix 격리 |
| S3 버킷 구조 | 단일 버킷 + 도메인 폴더 prefix (`profile-images/{userId}/{uuid}.{ext}`). 향후 다른 도메인 이미지는 폴더만 추가 |
| S3 객체 권한 | bucket policy 로 prefix 단위 public-read (ACL 비활성, 2024+ 권장 패턴) |
| 프로필 이미지 허용 포맷 / 크기 | jpg, jpeg, png / 최대 5MB |
| 커스텀노트 카드 bookmarked | 4종 카드 일관 (Phase 2.10 구현 시 누락 정정 — 단어별 sourceGlossaryId pair 로 bookmark 조회) |
| dev Swagger 외부 노출 차단 | dev profile 전용 SecurityFilterChain (Order 0) 으로 swagger path 에 Basic Auth 적용. local / prod 영향 없음 |
| Spring graceful shutdown | `server.shutdown: graceful` + 30s timeout. blue-green 배포 시 이전 슬롯 stop 시점 in-flight 요청 보호 |
| prod blue-green 진단 | nginx + blue/green 컨테이너 (8080/8081) 구조 정상. deploy.sh 헬스체크 버그 (`24` vs `12`) 운영 단에서 fix 완료 |
| DB destructive 마이그레이션 정책 | 누적 시 blue 슬롯 다운타임 가능 (V31/V32 등 drop/rename). expand-contract 패턴은 향후 도입 (현재는 짧은 다운타임 수용) |
