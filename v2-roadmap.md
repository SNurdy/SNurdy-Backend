# v2 API 로드맵

v2 기능 구현 단계별 진행 상황과 의존 관계를 추적한다.

---

## Phase 1 — 도메인 모델 변경 (완료)

| 단계 | 작업 | 상태 |
|---|---|---|
| 1.1 | UserEntity 확장 (profileImageUrl, level, exp, marketingConsent, marketingConsentAt) + V26 | done |
| 1.2 | GlossaryEntity 확장 (type, ownerId, shareCode) + V27 | done |
| 1.3 | GlossaryEntity CHECK 제약 (type-ownerId 정합성) + V29 | done |
| 1.4 | UserTermMemoEntity + UserTermMemoId 신규 (termMemo 패키지) + V28 | done |

---

## Phase 2 — v2 API 구현

### 진행 순서 원칙

- 의존성 없는 단순 도메인부터 시작
- 같은 도메인 변경은 한 단계 안에서 묶음
- 큰 도메인 (커스텀 단어장) 은 단계 안에서 다시 세분화
- 다른 도메인이 모두 끝난 뒤에 통합형 (홈 대시보드, 테스트 분리) 진행

### 단계 요약

| 단계 | 도메인 | 핵심 API | 의존 단계 | 상태 |
|---|---|---|---|---|
| 2.1 | 마이페이지 (User) | 프로필 조회/수정, 마케팅 동의 | Phase 1.1 | todo |
| 2.2 | 학습 리포트 대시보드 | 통합 조회 (streak + 과별 + 일자별) | Phase 1.1 | todo |
| 2.3 | 레벨 (다마고치) | 레벨 조회, 경험치 적립 | Phase 1.1 | todo |
| 2.4 | 커스텀 단어장 + 단어 관리 | CRUD + 단어 추가/삭제/조회 | Phase 1.2 | todo |
| 2.5 | 오답노트 | 목록/상세/해제/복구 | Phase 1.2, 2.4 | todo |
| 2.6 | 단어 카드 메모 | 작성/수정/조회 | Phase 1.4 | todo |
| 2.7 | 공유 단어장 | 공유 코드 생성, 코드 조회 | Phase 1.2, 2.4 | todo |
| 2.8 | 테스트 분리 | 커스텀/오답노트별 submit | 2.4, 2.5 | todo |
| 2.9 | 홈화면 대시보드 | 학습 노트별 현황, 자주 틀린 단어 | 2.4, 2.5 | todo |

---

## 단계별 상세

### 2.1 마이페이지 (User)

**목표** — 사용자가 자기 프로필을 조회/수정하고 마케팅 동의 상태를 토글한다.

**신규 endpoint**
- `GET /v2/users/me` — 본인 프로필 조회 (닉네임, 이메일, 이미지, 레벨/경험치, 마케팅 동의 등)
- `PUT /v2/users/me` — 프로필 수정 (닉네임, 이미지 등)
- `PUT /v2/users/me/marketing-consent` — 마케팅 동의 변경

**작업 항목**
- UserMeV2Api / UserMeV2Controller / UserMeV2Service (또는 기존 UserMeController 확장)
- UserProfileResponse, UserProfileUpdateRequest, MarketingConsentRequest DTO

**결정 필요**
- 프로필 이미지 업로드 방식 — multipart vs presigned URL vs URL 직접 입력
- 기존 v1 의 UserMeController (streak 조회) 와 같은 패키지 안에 둘지

---

### 2.2 학습 리포트 대시보드

**목표** — 마이페이지 안의 "학습 리포트" 화면용 통합 응답. streak + 과별 통계 + 일자별 통계를 한 요청에 묶어 반환한다.

**신규 endpoint**
- `GET /v2/users/me/dashboard?from=YYYY-MM-DD&to=YYYY-MM-DD`

**응답 구조 (제안)**
- streak: currentStreak, longestStreak, totalLearningTerms
- glossaryStats: glossary 별 (id, title, termCount, learnedCount, progressRate)
- dailyStats: 날짜별 (date, learnedCount, reviewedCount)

**작업 항목**
- 신규 DashboardController/Service (BFF 패턴, 기존 query service 재활용)
- 신규 DashboardResponse DTO

**결정 필요**
- 기간 범위 default (예: 최근 30일)
- 기간 범위 최대 (예: 1년)
- 빈 결과의 응답 모양

---

### 2.3 레벨 (다마고치)

**목표** — 사용자의 현재 레벨 상태를 보여주고, 학습 완료 시 경험치 적립.

**신규 endpoint**
- `GET /v2/users/me/level` — 현재 level, exp, 다음 레벨까지 남은 exp
- `POST /v2/users/me/level/exp` — 경험치 명시적 적립 (선택)

**경험치 적립 트리거 결정 필요**
- 옵션 A — 학습 제출 (submitLearning) 안에서 자동 적립
- 옵션 B — 클라이언트가 별도 API 호출
- 옵션 A 가 안전 (서버 신뢰)

**작업 항목**
- LevelService — 경험치 합산, 레벨 승급 로직
- LevelController, LevelResponse DTO
- 레벨 공식 결정 (예: 다음 레벨까지 100 * level exp 필요)
- 기존 LearningCommandService 에 경험치 적립 훅 추가 (옵션 A 면)

**결정 필요**
- 레벨 공식 (선형 vs 지수)
- 최대 레벨 (캡)
- 경험치 적립 단위 (정답 1건당 N exp 등)

---

### 2.4 커스텀 단어장 + 단어 관리

**목표** — 사용자가 자기 단어장을 만들고 단어를 추가/삭제한다. v2 의 가장 큰 도메인 변화.

**신규 endpoint**
- `POST /v2/glossaries` — 생성 (title, category)
- `GET /v2/glossaries?type=CUSTOM` — 본인 단어장 목록
- `GET /v2/glossaries/{id}` — 상세
- `PUT /v2/glossaries/{id}` — 수정 (title)
- `DELETE /v2/glossaries/{id}` — 삭제
- `POST /v2/glossaries/{id}/terms` — 단어 추가 (termId 목록)
- `DELETE /v2/glossaries/{id}/terms/{termId}` — 단어 삭제
- `GET /v2/glossaries/{id}/terms?filter=&sort=` — 단어 목록 (mask/sort 파라미터는 클라이언트 처리, 백엔드는 기본 데이터 반환)

**작업 항목**
- GlossaryRepository 에 type/owner 필터 메서드 추가
- 권한 검증 (CUSTOM 은 본인만 변경 가능)
- 단어 추가 시 glossary_terms 매핑 추가
- 단어 삭제 시 매핑 + 관련 진행도 처리 정책

**결정 필요**
- 커스텀 단어장 갯수 제한 (사용자당 N개)
- 한 단어장 안 최대 단어 수
- title 길이 제약
- category 입력 받을지 (DEFAULT 의 카테고리와 다른 시스템)
- 단어 삭제 시 진행도/북마크/메모 처리 (CASCADE? 유지?)

---

### 2.5 오답노트

**목표** — 학습 도중 틀린 단어를 자동으로 모아 별도 단어장처럼 관리.

**신규 endpoint**
- `GET /v2/wrong-answers?period=ALL|WEEK|TODAY` — 목록 (필터)
- `GET /v2/wrong-answers/{termId}` — 상세
- `DELETE /v2/wrong-answers/{termId}` — 해제
- `POST /v2/wrong-answers/{termId}` — 복구

**자동 생성 로직**
- 학습 submit 시 correct=false 면 사용자의 WRONG_ANSWER glossary 에 추가
- WRONG_ANSWER glossary 가 없으면 자동 생성 (사용자당 1개)

**작업 항목**
- LearningCommandService 에 오답 단어 자동 추가 훅
- WrongAnswerService 신규
- WrongAnswerRepository 또는 GlossaryRepository 재사용 (type=WRONG_ANSWER 필터)

**결정 필요**
- WRONG_ANSWER glossary 의 title/category (자동 생성 시 어떻게 채울지)
- 오답 해제 후 다시 틀리면 자동 복구? 아니면 그대로 해제 상태 유지?
- 정답 N회 연속 시 자동 졸업 정책 (지금 안 함? 추후 결정?)

---

### 2.6 단어 카드 메모

**목표** — 단어 카드에 사용자별 개인 메모 작성/수정/조회.

**신규 endpoint**
- `GET /v2/terms/{termId}/memo` — 본인 메모 조회
- `POST /v2/terms/{termId}/memo` — 작성 (없을 때)
- `PUT /v2/terms/{termId}/memo` — 수정 (있을 때)
- `DELETE /v2/terms/{termId}/memo` — 삭제 (기획서 명시 없지만 일반적)

**작업 항목**
- UserTermMemoRepository 신규
- UserTermMemoService 신규 (CRUD)
- termMemo 패키지 안 controller/api/dto/service 추가

**결정 필요**
- 메모 길이 (Phase 1 에서 500자 결정 완료)
- POST/PUT 분리 vs upsert (PUT 하나만으로 통합)

---

### 2.7 공유 단어장

**목표** — 커스텀 단어장에 공유 코드를 부여하고, 공유받은 사용자가 코드로 단어장 조회.

**신규 endpoint**
- `POST /v2/glossaries/{id}/share` — 공유 코드 생성 (없으면 발급, 있으면 그대로 반환)
- `GET /v2/glossaries/by-code/{code}` — 코드로 단어장 조회 (조회만, 본인 단어장으로 import 는 별도)

**작업 항목**
- ShareCode 생성기 (12자 nanoid 스타일)
- GlossaryRepository.findByShareCode
- 권한 — POST 는 본인만, GET 은 누구나 (단 로그인 필요)

**결정 필요**
- 공유 코드 만료 정책 (없음? 30일? 사용자 결정?)
- 공유받은 단어장을 본인 단어장으로 복제 기능 필요? (기획서엔 없음)
- 공유 비활성화 API 필요? (코드 재생성)

---

### 2.8 테스트 (학습 완료 처리) 분리

**목표** — 단어장 종류별로 별도 테스트 submit API. 기존 v1 /learning/submit 은 기본 단어장 전용으로 유지.

**신규 endpoint**
- `POST /v2/tests/glossaries/{id}/submit` — 커스텀 단어장 테스트 (오답 → WRONG_ANSWER 자동 등록)
- `POST /v2/tests/wrong-answers/submit` — 오답노트 테스트 (정답 시 오답에서 자동 졸업?)

**작업 항목**
- 기존 LearningCommandService 의 submitLearning 흐름을 공유 (SrsCalculator, GlossaryProgress 등)
- 다만 단어장 종류별 후처리 분기
- 신규 TestController, TestService

**결정 필요**
- 오답노트 테스트 시 정답 처리 정책 (졸업? 그대로? 카운트만?)
- 기본 단어장 테스트도 v2 로 옮길지 (예: POST /v2/tests/default/submit) vs v1 유지

---

### 2.9 홈화면 대시보드

**목표** — 홈 진입 시 한 요청으로 학습/테스트 현황 + 자주 틀린 단어를 묶어 받는다.

**신규 endpoint**
- `GET /v2/home/dashboard`

**응답 구조 (제안)**
- noteSummary: 학습 노트별 현황 (기본 단어장 N개, 커스텀 N개, 오답노트 N개)
- frequentlyWrongTerms: 자주 틀린 단어 N개

**작업 항목**
- BFF 패턴 — 여러 service 호출 후 통합 응답
- HomeDashboardController, HomeDashboardService 신규

**결정 필요**
- "자주 틀린 단어" 정의 (오답 횟수? 비율? 기간?)
- 자주 틀린 단어 반환 갯수 (5개? 10개?)

---

## 진행 체크리스트

각 단계 시작 시 `todo` -> `in-progress`, 끝나면 `done` 으로 위 표 상태 갱신.

### Phase 2 진행 추천 순서

1. **2.1 마이페이지** — 단순. v2 패턴 정착 좋음
2. **2.3 레벨** — 단순. user 만 의존
3. **2.6 메모** — 단순. UserTermMemo 활용
4. **2.4 커스텀 단어장** — 큰 도메인. 가장 큰 작업
5. **2.7 공유** — 2.4 의존
6. **2.5 오답노트** — 2.4 의존
7. **2.8 테스트 분리** — 2.4, 2.5 의존
8. **2.2 학습 리포트 대시보드** — 기존 데이터 활용 가능, 언제든 가능
9. **2.9 홈 대시보드** — 모든 도메인 완성 후

---

## 공통 작업 (Phase 2 전반)

### v2 컨트롤러 패키지 구조 결정

옵션
- 기존 controller 안에 V1/V2 sub-package 분리
- v1 그대로 두고 v2 컨트롤러는 같은 패키지에 다른 클래스명
- 도메인 패키지 자체 분리 (예: domain/v2/glossary)

기존 일관성을 위해 같은 도메인 패키지 안에 V2 클래스로 추가 권장.

### v1 deprecation 계획

forced update 가능한 환경이라 했으니 v2 안정화 후 v1 endpoint 들은 한 차례 정리. 다만 일정은 v2 출시 후 결정.

---

## 결정 누적 (참고)

| 항목 | 결정 |
|---|---|
| v2 namespace | URL prefix `/v2` |
| Glossary 통합 모델 | type enum (DEFAULT/CUSTOM/WRONG_ANSWER) + ownerId + shareCode |
| 기존 Glossary 데이터 | type=DEFAULT 일괄 채움 |
| iOS 강제 업데이트 | 가능 |
| 메모 모델 | 별도 UserTermMemoEntity (옵션 B-1 복합키) |
| 메모 패키지 | domain/termmemo/ (모두 소문자, 패키지 컨벤션 준수) |
| 공유 코드 길이 | 12자 |
| 메모 본문 길이 | 500자 |
| 마케팅 동의 기본값 | false |
| 레벨/경험치 초기값 | level=1, exp=0 |
