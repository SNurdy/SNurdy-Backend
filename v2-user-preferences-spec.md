## User Preferences 도메인 분리 + selectedGlossary 영구화

UserEntity 비대화 해소 + dashboard `selectedGlossary` 사용자 명시 선택 보존 누락 정정.

배경
- 현재 `GET /v2/users/me/dashboard` 응답의 `selectedGlossary` 는 서버가 자동으로 lastLearnedAt 최근 과를 골라서 내려줌
- 사용자가 바텀시트에서 과를 선택해도 저장할 곳이 없음 (컬럼 부재 + endpoint 부재)
- 동시에 UserEntity 가 16개 컬럼으로 비대해지고 있어 사용자 설정값을 별도 도메인으로 분리

---

## 1. 도메인 분리 — user_preferences

### 분리 기준
사용자가 의식적으로 선택해서 저장한 UX 환경값 = preference. 시스템 측정값(streak), 법적 동의(consent), 정체성 정보(name) 와 분리.

### 분리 대상
- `users.default_test_type` (Phase 2.8, V40) → `user_preferences.default_test_type` (V42 이관, V43 컬럼 drop)
- 신규 `user_preferences.selected_glossary_id`
- (Phase 2.11) `user_preferences.daily_goal`

### 잔존 대상 (UserEntity)
- 정체성/인증: id, socialId, socialType, email, name, role
- 프로필: nickname, profileImageUrl
- 학습 집계: currentStreak, longestStreak, totalLearningTerms
- 계정 상태: deletedAt, migrationDeclinedAt
- 법적 동의: marketingConsent, marketingConsentAt

### 도메인 패키지
```
domain/user/preference/
├── entity/UserPreferenceEntity
├── repository/UserPreferenceRepository
├── service/UserPreferenceService
├── controller/UserPreferenceApi
├── controller/UserPreferenceController
└── dto/
    ├── UserPreferenceResponse
    ├── UpdateDefaultTestTypeRequest    (UserProfile 에서 이전)
    ├── UpdateDefaultTestTypeResponse   (UserProfile 에서 이전)
    ├── UpdateSelectedGlossaryRequest
    └── UpdateSelectedGlossaryResponse
```

---

## 2. 테이블 설계

### V42 — user_preferences 신설 + 데이터 이관
```sql
CREATE TABLE user_preferences (
    user_id BINARY(16) PRIMARY KEY,
    default_test_type VARCHAR(32) NULL,
    selected_glossary_id BIGINT NOT NULL DEFAULT 6,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_preferences_glossary
        FOREIGN KEY (selected_glossary_id) REFERENCES glossaries(id) ON DELETE RESTRICT
);

INSERT INTO user_preferences (user_id, default_test_type, selected_glossary_id, created_at, updated_at)
SELECT id, default_test_type, 6, NOW(6), NOW(6)
FROM users;
```

### V43 — users.default_test_type drop
idempotent 패턴으로 컬럼 존재 시에만 drop.

### 1:1 매핑 패턴
- PK = userId (BINARY(16)). 별도 surrogate key 없음
- JPA: 단순 column-PK 매핑 (`@Id @Column(name="user_id") val userId: UUID`)
- `@OneToOne` / `@MapsId` 안 씀 (UserEntity 에 역방향 참조 안 만듦 — preference 도메인이 user 를 알 뿐, 반대 방향 의존 금지)

### row 생성 정책
- 사용자 가입 시 user_preferences row 자동 생성 (가입 service 트랜잭션 안에서 INSERT)
  - `default_test_type = null`, `selected_glossary_id = 6` (심장내과)
- V42 마이그레이션 시점에 기존 유저 전원 INSERT (selected_glossary_id = 6 일괄)
- 모든 user 에 대해 user_preferences row 가 항상 존재한다고 가정 가능 (조회 시 null 처리 불필요)

---

## 3. 신규 / 이전 endpoint

### 3.1 PUT /v2/users/me/preferences/test-type (이전)
- 위치: `UserProfileApi` → `UserPreferenceApi`
- URL / body / response 변경 없음 (breaking 없음)
- 내부 저장 위치만 `users.default_test_type` → `user_preferences.default_test_type`

### 3.2 PUT /v2/users/me/preferences/selected-glossary (신규)

요청
```json
{ "glossaryId": 6 }
```
- `glossaryId: Long` — non-null 필수 (`@field:NotNull`). 자동 선택 모드 없음, 항상 명시 지정
- null 전송 시 `COMMON_E001` (validation 400)

응답
```json
{ "selectedGlossaryId": 6 }
```
- 저장된 값을 그대로 echo

검증
- glossary 존재 확인 → 없으면 `DICTIONARY_E001 GLOSSARY_NOT_FOUND`
- glossaries 테이블은 V37 cleanup 으로 DEFAULT 카탈로그 전용이라 type 검증 불필요
  (custom note id 오용 방지는 클라 책임 — custom_notes 와 glossaries 가 별도 테이블이라 id 만으로 구분 불가)

SuccessCode
- `USER_S031 UPDATE_SELECTED_GLOSSARY`

ErrorCode
- `DICTIONARY_E001 GLOSSARY_NOT_FOUND`
- `USER_E010` (인증)

---

## 4. dashboard selectedGlossary 결정 로직

`UserDashboardService.buildSelectedGlossary` 단순화.

### 단일 source of truth
- `user_preferences.selected_glossary_id` 만 본다. fallback / lastLearnedAt 분기 없음
- 모든 user 에 row 가 보장되고 selected_glossary_id 가 NOT NULL 이라 항상 값이 존재
- 신규 유저는 가입 시점에 selected_glossary_id = 6 (심장내과) 으로 INSERT 되어 있음
- 사용자가 학습을 진행해도 selectedGlossary 자동 변경 없음. 사용자가 명시적으로 바꿔야 함

### invalidation
- FK `ON DELETE RESTRICT` 로 glossary 가 삭제될 수 없음 (참조 중이면 거부). 운영상 카탈로그 glossary 삭제는 발생하지 않으며, 발생 시 cleanup 정책은 별도 결정
- 응답 항상 non-null `DashboardSelectedGlossary`

### 응답 모양 변경
- `DashboardSelectedGlossary?` → `DashboardSelectedGlossary` (non-null) 로 타입 변경
- 기타 필드 추가 없음

---

## 5. 영향 받는 기존 코드

### UserEntity
- `defaultTestType` 컬럼 + 관련 매핑 삭제

### UserProfileApi / UserProfileService
- `updateDefaultTestType` 메서드 + DTO import 삭제 (UserPreference 쪽으로 완전 이전)
- `UpdateDefaultTestTypeRequest`, `UpdateDefaultTestTypeResponse` 파일 위치 이동

### UserDashboardService
- `buildSelectedGlossary` 에 `UserPreferenceRepository` 의존성 추가
- fallback 로직 3단계 적용

### TestService (또는 default test type 사용처)
- `user.defaultTestType` 참조 → `userPreferenceRepository.findByUserId(...).defaultTestType` 로 변경

---

## 6. SuccessCode / ErrorCode 정리

### 신규
| 코드 | 이름 | 메시지 |
|---|---|---|
| `USER_S031` | UPDATE_SELECTED_GLOSSARY | 선택된 단어장을 저장했습니다. |
| `(폐기) USER_E040` | SELECTED_GLOSSARY_NOT_ALLOWED | 선택할 수 없는 단어장입니다. |

### 재활용 (이전된 endpoint)
- `USER_S030 UPDATE_DEFAULT_TEST_TYPE` — 이전 후에도 동일 코드 사용

### 코드 prefix
- preference 전용 prefix 신설하지 않음. `USER_*` 안에서 관리 (도메인 패키지는 분리하되 코드 prefix 는 user 통합)

---

## 7. 구현 순서

1. spec 작성 (본 문서)
2. V42 마이그레이션 — user_preferences 테이블 생성 + 데이터 복사
3. UserPreferenceEntity / Repository / Service / DTO 신설
4. UserPreferenceApi / Controller 신설 — selected-glossary endpoint
5. UserProfileApi 의 updateDefaultTestType 을 UserPreferenceApi 로 이전 + 기존 위치 삭제
6. TestService 등 defaultTestType 사용처를 UserPreferenceService 경유로 변경
7. UserEntity.defaultTestType 컬럼 + 매핑 삭제
8. V43 마이그레이션 — users.default_test_type 컬럼 drop
9. UserDashboardService.buildSelectedGlossary fallback 3단계 적용
10. SuccessCode / ErrorCode enum 추가 (`USER_S031`, `(폐기) USER_E040`)
11. v2-roadmap.md 결정 누적 갱신
12. iOS 전달 메시지 작성

---

## 8. 결정 누적

| 항목 | 결정 |
|---|---|
| 분리 도메인 명 | user_preferences (Apple/Google 컨벤션, "사용자가 고른 환경값" 의미) |
| 마케팅 동의 분리 여부 | 분리 안 함 (법적 동의는 preference 아님) |
| streak 등 집계값 분리 여부 | 분리 안 함 (UserEntity 잔존, 쓰기 빈도 높음) |
| 1:1 매핑 패턴 | userId 를 PK 로 단순 column-PK. 역방향 OneToOne 안 만듦 |
| row 생성 시점 | 가입 service 안에서 즉시 INSERT. 기존 유저는 V42 일괄 INSERT |
| selected_glossary_id null 허용 | 불허. DB NOT NULL + DEFAULT 6, API non-null (자동 선택 모드 없음) |
| 신규 유저 default glossary | glossaryId = 6 (심장내과). DB DEFAULT + 가입 코드 명시 INSERT |
| 사용자 학습 시 selectedGlossary 자동 변경 | 안 함. 사용자 명시 선택만 반영 |
| 허용 glossary type | DEFAULT 만 (커스텀/오답 거부) |
| selected-glossary mutation 위치 | preference 도메인 (데이터 소유 도메인 원칙, dashboard 는 read-only BFF) |
| dashboard 응답 selectedGlossary | non-null 보장 (Optional → Required) |
| FK ON DELETE 정책 | user 삭제 시 CASCADE, glossary 는 RESTRICT (참조 중이면 삭제 거부) |
| 코드 prefix | USER_* 통합 (PREFERENCE_* 신설 안 함) |

---

## 9. 후속 검토

- Phase 2.11 daily_goal 컬럼은 user_preferences 에 추가 (본 spec 범위 밖)
- preference 가 향후 5개 이상으로 늘어나면 sub-grouping 검토 (현재 3개라 평평한 컬럼 구조 유지)
