## Phase 2.12 — 학습 Home 대시보드 신설 + dashboard 도메인 재구조

학습하기 탭과 마이페이지 탭의 데이터 구조 차이를 명확히 분리. 학습 Home 전용 BFF endpoint 신설 + 기존 `domain/user/dashboard` 패키지를 `domain/dashboard/me` 로 이전 + rename.

이 spec 은 Phase 2.2 / 2.9 / 2.11 의 일부 결정사항을 정정/통합한다 (기존 spec 본문은 유지하되 정정 의도를 본 문서에서 명시).

---

## 1. 배경

### 1.1 두 탭 화면 분리 (기존 가정 정정)

| 탭 | 화면 png | 사용 endpoint |
|---|---|---|
| **마이페이지** | `마이페이지.png` | `GET /v2/dashboards/me` (기존, 변경 없음) + `GET /v2/users/me` (사용자 카드) |
| **학습하기** | `학습하기(Home).png` | **`GET /v2/dashboards/study` (신규)** |

기존 Phase 2.9 폐기 사유 "마이페이지와 통합" 은 잘못된 가정. 두 화면이 비슷한 데이터 (streak + 다마고치 + 주간 통계) 를 다루지만 표시 / UX 가 달라 endpoint 분리가 맞음.

### 1.2 학습 Home 화면이 추가로 필요한 데이터

`학습하기(Home).png` 분석:
- **연속 학습 21일째** → `currentStreak` (기존 dashboard 응답에 없음, `/v1/user/streak` 별도 호출 중)
- **주간 캘린더 ✓ 체크 표시** → 그 날 submit 1회 이상 여부 (마이페이지 막대 그래프 와 다른 표시 방식)
- **학습 목표 10개 ✎** → 사용자별 한 세션 학습 카드 수 default (신규)
- **자주 틀린 단어 카드** → 3회+ 오답 단어 중 랜덤 1개 (신규)

이 4가지가 학습 Home 전용 데이터 — 기존 마이페이지 dashboard 응답에 안 들어감.

---

## 2. 도메인 패키지 재구조

### 2.1 기존
```
domain/user/dashboard/
├── controller/UserDashboardApi
├── controller/UserDashboardController
├── service/UserDashboardService
└── dto/UserDashboardDto
```

### 2.2 변경 후
```
domain/dashboard/
├── me/                                         ← 이전 + rename
│   ├── controller/MyDashboardApi
│   ├── controller/MyDashboardController
│   ├── service/MyDashboardService
│   └── dto/MyDashboardDto
└── study/                                      ← 신규
    ├── controller/StudyDashboardApi
    ├── controller/StudyDashboardController
    ├── service/StudyDashboardService
    └── dto/StudyDashboardDto
```

### 2.3 이유

- 두 화면 모두 dashboard 추상화 → 같은 도메인 분류
- 기존 `domain/user/dashboard` 가 user 데이터 직접 소유 없이 다른 도메인 합성하는 BFF → user 하위 위치가 부정확했음
- 신설 StudyDashboard 도 같은 BFF 패턴
- 둘을 `domain/dashboard/` 아래 모아서 BFF dashboard 도메인 통합
- 클래스 / 폴더 prefix 는 UI 명칭 따라 `My` / `Study` (me / study URL 과 일관)

### 2.4 클래스명 / URL 영향

- **클래스명 그대로** (`MyDashboardService` 등) — 패키지 import 만 변경
- **URL `/v2/dashboards/me` 그대로 유지** — iOS breaking 없음
- 신규 URL `/v2/dashboards/study`

---

## 3. 마이페이지 — `GET /v2/dashboards/me` (변경 없음)

기존 응답 구조 유지. 본 spec 에 명시하는 이유는 학습 Home 응답과 비교를 위해.

### 요청
```
GET /v2/dashboards/me?startDate=2026-02-22
Authorization: Bearer <token>
```

| param | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate` | LocalDate | 선택 | 막대 그래프 7일 범위 시작일. 미지정 시 오늘 포함 직전 7일 |

### 응답
```json
{
  "streak": {
    "longestStreak": 21,
    "totalLearningTerms": 340
  },
  "selectedGlossary": {
    "glossaryId": 6,
    "category": "내과",
    "title": "심장내과",
    "learnedCount": 85,
    "totalTermCount": 100,
    "progressRate": 85.0,
    "level": 1,
    "lastLearnedAt": "2026-02-27T08:15:00Z",
    "availableCounts": { "MEANING_TO_TERM": 100, ... }
  },
  "weekStatistics": {
    "startDate": "2026-02-22",
    "endDate": "2026-02-28",
    "days": [
      { "date": "2026-02-22", "dayOfWeek": "SUNDAY", "learnedCount": 12, "completedCount": 18 },
      ...
    ]
  },
  "generatedAt": "2026-02-27T09:00:00Z"
}
```

SuccessCode 기존 `USER_S020 GET_DASHBOARD` 유지.

---

## 4. 학습 Home — `GET /v2/dashboards/study` (신규)

### 4.1 요청
```
GET /v2/dashboards/study
Authorization: Bearer <token>
```

**Query**: 없음 (이번 주 자연주 고정 — 월요일 시작 ~ 일요일)

### 4.2 응답
```json
{
  "currentStreak": 21,
  "selectedGlossary": {
    "glossaryId": 6,
    "category": "내과",
    "title": "심장내과",
    "learnedCount": 86,
    "totalTermCount": 100,
    "progressRate": 86.0,
    "level": 5,
    "lastLearnedAt": "2026-06-08T14:23:00Z",
    "availableCounts": {
      "TERM_TO_MEANING": 100,
      "MEANING_TO_TERM": 100,
      "LISTEN_AND_WRITE": 100,
      "WRITE_ABBREVIATION": 42,
      "MC_PICK_TERM": 100,
      "MC_PICK_MEANING": 100,
      "MC_PICK_ABBR": 42
    }
  },
  "defaultCardCount": 10,
  "weekStatistics": {
    "startDate": "2026-06-08",
    "endDate": "2026-06-14",
    "days": [
      { "date": "2026-06-08", "dayOfWeek": "MONDAY",    "studied": true  },
      { "date": "2026-06-09", "dayOfWeek": "TUESDAY",   "studied": false },
      { "date": "2026-06-10", "dayOfWeek": "WEDNESDAY", "studied": false },
      { "date": "2026-06-11", "dayOfWeek": "THURSDAY",  "studied": false },
      { "date": "2026-06-12", "dayOfWeek": "FRIDAY",    "studied": false },
      { "date": "2026-06-13", "dayOfWeek": "SATURDAY",  "studied": false },
      { "date": "2026-06-14", "dayOfWeek": "SUNDAY",    "studied": false }
    ]
  },
  "frequentlyIncorrectTerm": {
    "termId": 245,
    "spelling": "Acute sinusitis",
    "meaning": "급성부비동염",
    "abbreviation": null,
    "totalIncorrectCount": 5
  },
  "generatedAt": "2026-06-08T09:00:00Z"
}
```

### 4.3 필드 의미

| 필드 | 의미 / 화면 매핑 |
|---|---|
| `currentStreak` | "연속 학습 21일째" — lazy compute (UserStreakService 의 기존 로직 재사용) |
| `selectedGlossary` | "심장내과 ▼" 드롭다운 + Lv.5 + 86/100 게이지 |
| `selectedGlossary.availableCounts` | testType 별 가용 단어 수 (학습 시작 셀렉터 max 결정) |
| `defaultCardCount` | "학습 목표 10개 ✎" — `user_preferences.default_card_count` |
| `weekStatistics.days[].studied` | "12 ✓ 13 ○ 14 ○ ..." 주간 캘린더 체크 표시. **그 날 submit 1회 이상** = `learnedCount + completedCount > 0` |
| `frequentlyIncorrectTerm` | "자주 틀린 단어" 카드. 3회+ 오답 풀에서 랜덤 1개. 없으면 null |

### 4.4 SuccessCode
- `USER_S023 GET_STUDY_DASHBOARD` 신규

### 4.5 ErrorCode
- `USER_E010 ACCOUNT_NOT_FOUND` (인증)

---

## 5. defaultCardCount

### 5.1 데이터 모델
`user_preferences` 테이블에 컬럼 추가 (V44).

```sql
ALTER TABLE user_preferences
    ADD COLUMN default_card_count INT NOT NULL DEFAULT 10;
```

- 타입: `INT NOT NULL DEFAULT 10`
- 허용 값: 10, 20, 30, 40, 50

### 5.2 마이그레이션 (V44)
idempotent 패턴.

### 5.3 변경 endpoint (preferences 확장)

#### `PUT /v2/users/me/preferences/default-card-count` (신규)

**요청 body**
```json
{ "count": 30 }
```
- `count: Int` non-null
- 허용 값: 10, 20, 30, 40, 50
- 다른 값 → `USER_E044 DEFAULT_CARD_COUNT_INVALID`

**응답**
```json
{ "defaultCardCount": 30 }
```

**SuccessCode**: `USER_S033 UPDATE_DEFAULT_CARD_COUNT`

#### `GET /v2/users/me/preferences` (기존 응답 확장)

응답에 `defaultCardCount` 필드 추가.

```json
{
  "defaultTestType": "MEANING_TO_TERM",
  "selectedGlossaryId": 6,
  "defaultCardCount": 10
}
```

### 5.4 학습 카드 호출과의 관계

`POST /v2/test/glossaries/{id}/cards` 의 `count` 는 클라가 명시 (기존 패턴 유지). 백엔드는 default 자동 적용 안 함. 사용자 동작:
- 학습 Home 의 "학습 목표 10개" = `defaultCardCount` (현재값 표시)
- 사용자가 ✎ 탭 → 10/20/30/40/50 시트에서 선택 → PUT 호출 → `defaultCardCount` 갱신
- 학습하기 버튼 탭 → 클라가 `defaultCardCount` 값으로 `cards` 호출 (`count = defaultCardCount`)

---

## 6. frequentlyIncorrectTerm

### 6.1 조건

`wrong_answer_terms` 중:
- `user_id = ?`
- `deleted_at IS NULL` (활성)
- `total_incorrect_count >= 3` (3회 이상 오답)
- `ORDER BY RAND() LIMIT 1` — **랜덤** (학습 효과)

3회 미만은 자주 틀린 으로 인정 안 함. 단어 없으면 응답 필드 null.

### 6.2 위치

StudyDashboardService 가 `WrongAnswerNoteService` (또는 `WrongAnswerTermRepository`) 의 신규 메서드 호출.

```kotlin
// WrongAnswerNoteService 또는 별도 메서드
fun getRandomFrequentlyIncorrectTerm(userId: UUID, minCount: Int = 3): FrequentlyIncorrectTermPayload?
```

### 6.3 응답 필드

| 필드 | 설명 |
|---|---|
| `termId` | 단어 id |
| `spelling` | 영문 |
| `meaning` | 한글 의미 |
| `abbreviation` | 약어 (nullable) |
| `totalIncorrectCount` | 누적 오답 횟수 |

### 6.4 쿼리 성능 참고

MySQL `ORDER BY RAND()` 는 전체 row 정렬이라 큰 테이블에 비효율적. 사용자별 `wrong_answer_terms` 수십~수백 row 라 실효 부담 미미. 향후 row 수 크게 늘면 OFFSET + COUNT 기반 랜덤 검토.

---

## 7. weekStatistics 의 `studied` 필드

StudyDashboard 응답 전용. MyDashboard 응답에는 추가 안 함 (마이페이지 막대 그래프는 `completedCount` 로 충분).

`studied = (learnedCount + completedCount) > 0` — 그 날 submit 1회 이상 여부.

캘린더 `CalendarDailyItem` 에 이미 같은 패턴 (`studied = completedCount > 0`) 있음. StudyDashboard 의 days 도 동일 로직.

---

## 8. todayGoalCompleted 폐기 (제안)

### 8.1 현재 상태
- `SubmitGlossaryTestResponse.todayGoalCompleted` 가 "오늘 첫 활동 여부" 로 동작 (transition 패턴)
- Phase 2.8 시점 의미 미정으로 배포됨

### 8.2 폐기 이유
- 학습 Home 의 주간 캘린더 체크 표시는 `dashboards/study` 응답의 `weekStatistics.days[].studied` 로 표현 가능
- 별도 transition 이벤트 UX 없으면 의미 모호 + 정보 중복
- iOS 가 사용하지 않으면 제거 안전

### 8.3 폐기 절차
- iOS 사전 동의 받음 → `SubmitGlossaryTestResponse` 에서 `todayGoalCompleted` 필드 제거
- TestService 의 `todayWasActiveBefore` 계산 로직 제거 (DB query 1개 절약)

### 8.4 iOS 가 사용 중이면
- 폐기 보류 → 의미만 명확화 (현재 transition 의미 유지, 학습 Home 체크 표시는 weekStatistics.studied 별도 사용)

---

## 9. 응답 구조 비교 (요약)

| 필드 | MyDashboard (마이페이지) | StudyDashboard (학습 Home) |
|---|---|---|
| `streak.longestStreak` | ✅ | ❌ |
| `streak.totalLearningTerms` | ✅ | ❌ |
| `currentStreak` | ❌ | ✅ |
| `selectedGlossary.title/level/learnedCount` | ✅ | ✅ |
| `selectedGlossary.availableCounts` | ✅ (호환성) | ✅ (학습 셀렉터 max) |
| `weekStatistics.startDate/endDate` | ✅ (헤더) | ✅ |
| `weekStatistics.days[].learnedCount/completedCount` | ✅ (막대 높이) | ❌ |
| `weekStatistics.days[].studied` | ❌ | ✅ |
| `defaultCardCount` | ❌ | ✅ |
| `frequentlyIncorrectTerm` | ❌ | ✅ |

---

## 10. SuccessCode / ErrorCode

### 신규
| 코드 | 이름 | 메시지 |
|---|---|---|
| `USER_S023` | GET_STUDY_DASHBOARD | 학습 Home 대시보드 조회에 성공했습니다 |
| `USER_S033` | UPDATE_DEFAULT_CARD_COUNT | 한 세션 학습 카드 수를 변경했습니다 |
| `USER_E044` | DEFAULT_CARD_COUNT_INVALID | 학습 카드 수는 10/20/30/40/50 중 하나여야 합니다 |

### 폐기 (iOS 동의 시)
| 필드 | 위치 |
|---|---|
| `todayGoalCompleted` | `SubmitGlossaryTestResponse` |

---

## 11. 도메인 영향 정리

### 신설
- `domain/dashboard/study/` 패키지 (StudyDashboardController/Service/Dto)
- `user_preferences.default_card_count` 컬럼 (V44)
- `UserPreferenceService.updateDefaultCardCount` 메서드
- `WrongAnswerTermRepository.findRandomFrequentlyIncorrectTerm(userId, minCount)` (또는 service 메서드)

### 이동 + rename
- `domain/user/dashboard/` → `domain/dashboard/me/` (전체 패키지 이동 + import 정정)
- 클래스 rename: `UserDashboard*` → `MyDashboard*`
- URL 변경: `/v2/users/me/dashboard*` → `/v2/dashboards/me*`
- 응답 구조 변경 없음

### 응답 변경
- `GET /v2/users/me/preferences` 응답에 `defaultCardCount` 필드 추가
- `SubmitGlossaryTestResponse.todayGoalCompleted` 폐기 (iOS 동의 시)

### 변경 없음
- `GET /v2/dashboards/me` 응답 (MyDashboard)
- 다른 모든 endpoint

---

## 12. 구현 순서

1. spec 작성 (본 문서) ✓
2. **iOS 사전 동의** — 신규 endpoint 흐름 + todayGoalCompleted 폐기 확인
3. V44 마이그레이션 — `user_preferences.default_card_count` 컬럼
4. UserPreferenceEntity / DTO 확장 (defaultCardCount)
5. UserPreferenceService — `updateDefaultCardCount` + `getPreferenceResponse` 확장
6. UserPreferenceApi 에 `PUT /preferences/default-card-count` endpoint 추가
7. 패키지 이동 + rename: `domain/user/dashboard/` → `domain/dashboard/me/`, 클래스 `UserDashboard*` → `MyDashboard*`, URL `/v2/users/me/dashboard*` → `/v2/dashboards/me*` (전체 import 정정)
8. 신규 패키지 `domain/dashboard/study/` 생성
9. `StudyDashboardResponse` + 하위 DTO 작성
10. `StudyDashboardService` 작성 (BFF — UserStreakService + UserPreferenceService + WrongAnswerNoteService + GlossaryRepository + DailyProgressRepository 호출)
11. `WrongAnswerTermRepository.findRandomFrequentlyIncorrectTerm` 신규 쿼리
12. `StudyDashboardController` + Api
13. SuccessCode / ErrorCode enum 추가
14. (옵션) `SubmitGlossaryTestResponse.todayGoalCompleted` 폐기 + TestService 로직 정리
15. v2-roadmap.md 결정 누적 갱신
16. iOS 전달 메시지 최종

---

## 13. 결정 누적

| 항목 | 결정 |
|---|---|
| 두 탭 분리 인정 | 마이페이지 (`마이페이지.png`) ≠ 학습 Home (`학습하기(Home).png`). Phase 2.9 폐기 사유 정정 |
| 학습 Home endpoint | 신규 `GET /v2/dashboards/study` |
| 마이페이지 endpoint | 기존 `GET /v2/dashboards/me` 그대로 (URL / 응답 변경 없음) |
| 도메인 패키지 | `domain/dashboard/` 통합 — `me/` (이전 + rename) + `study/` (신규) |
| URL 컨벤션 | `/v2/dashboards/me` + `/v2/dashboards/study` (도메인 패키지와 URL prefix 일치) |
| StudyDashboard 책임 | BFF — UserStreak + Preference + WrongAnswer + Glossary + Progress 합성 |
| weekStatistics.days[].studied 위치 | StudyDashboard 응답에만 추가. MyDashboard 응답 변경 없음 |
| frequentlyIncorrectTerm 위치 | StudyDashboard 응답에 통합 (별도 endpoint 신설 안 함) |
| 자주 틀린 단어 조건 | `total_incorrect_count >= 3`, `ORDER BY RAND() LIMIT 1` (랜덤) |
| defaultCardCount 저장 | `user_preferences.default_card_count` (V44), 허용 값 10/20/30/40/50 |
| defaultCardCount 적용 | 백엔드는 default 자동 적용 안 함. 클라가 cards 호출 시 명시 |
| todayGoalCompleted | 폐기 (iOS 동의 시). 캘린더 체크는 weekStatistics.days[].studied 로 표현 |
| 패키지 이동 영향 | 클래스 rename + URL 변경. 응답 구조는 그대로. iOS 마이페이지 URL 3개 변경 필요 (breaking) |
| streak 분리 | 안 함 (longestStreak 거의 안 바뀌지만 분리 실익 미미. 마이페이지에서만 사용) |
| weekStatistics DTO 분리 | MyDashboard / StudyDashboard 각자 별도 DTO (다른 필드) |
| weekStatistics 범위 (학습 Home) | 이번 주 자연주 고정 (월요일 ~ 일요일) — 슬라이딩 안 함 |
| weekStatistics 범위 (마이페이지) | startDate 슬라이딩 (기존) |
| iOS 사전 동의 | 신규 endpoint 흐름 + todayGoalCompleted 폐기 — 구현 전 받음 |

---

## 14. iOS 영향 종합

### 변경 없음
- `GET /v2/dashboards/me` (마이페이지)
- `GET /v2/users/me` (프로필)
- `GET /v2/users/me/preferences` — 응답에 `defaultCardCount` 추가 (additive, breaking 아님)
- `PUT /v2/users/me/preferences/*` 기존 endpoint들
- `GET /v1/user/streak`

### 신규
- `GET /v2/dashboards/study` — 학습 Home 진입 시 호출
- `PUT /v2/users/me/preferences/default-card-count` — 학습 목표 변경 시 호출

### Breaking (사전 동의 시)
- `SubmitGlossaryTestResponse.todayGoalCompleted` 제거 — iOS 가 사용 안 하시면 응답에서 제거

---

## 15. 미확정 / 후속 검토

1. 학습 Home 화면 학습하기 버튼 → 클라가 `defaultCardCount` 값으로 cards 호출. 단 사용자가 selector 로 다른 값 선택 가능한 화면이 있는지? 현재는 ✎ 버튼으로 변경 → PUT 호출이 default 변경
2. defaultCardCount 외 다른 값 (예: 100, 5 등) 추가 단계 가능성
3. StudyDashboard 응답 캐싱 — iOS stale-while-revalidate 적용 검토 (바텀시트와 동일 패턴)
4. MyDashboard 도메인 이동 시 다른 사용처 영향 — `domain/user/dashboard/` 를 직접 import 하는 곳 있는지 grep 으로 확인 (가능성 낮지만 명시적 체크)
