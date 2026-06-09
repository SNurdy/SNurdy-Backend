## Phase 2.11 — Daily Goal (사용자별 일일 학습 목표 단어 수)

사용자가 매일 학습할 목표 단어 수 (10/20/30/40/50) 를 설정하고, 그 목표 도달 여부를 dashboard 와 test submit 응답의 `todayGoalCompleted` 로 반영.

Phase 2.10 시점에는 `todayGoalCompleted` 가 "오늘 첫 활동 여부" 라는 임시 의미로 동작 중. 이를 "오늘 누적 학습 단어 수가 dailyGoal 이상" 으로 정정.

---

## 1. 배경

- 학습하기 Home 화면에 "오늘 목표 X 단어" UX 노출 예정
- Phase 2.8 spec 의 `SubmitGlossaryTestResponse.todayGoalCompleted` 가 의미 미정 상태로 배포됨 (현재는 dailyProgress 신규 생성 여부)
- 사용자가 자기 학습량에 맞춰 목표를 조절 가능해야 함

---

## 2. 데이터 모델

### 2.1 컬럼 추가 — `user_preferences.daily_goal`

기존 `user_preferences` 테이블에 컬럼 추가 (V44).

```sql
ALTER TABLE user_preferences
    ADD COLUMN daily_goal INT NOT NULL DEFAULT 10;
```

- 타입: `INT NOT NULL DEFAULT 10`
- 허용 값: 10, 20, 30, 40, 50 (5단계, 클라 UI 와 일치)
- 기존 row 일괄 default 10 적용
- 향후 다른 단위 (시간, 정답 수 등) 추가 가능성 있으면 컬럼 분리 검토 — 현재는 "단어 수" 단일 의미

### 2.2 마이그레이션 (V44)
idempotent 패턴 (컬럼 존재 시 skip).

---

## 3. Endpoint

### 3.1 PUT /v2/users/me/preferences/daily-goal — 일일 목표 변경

**요청 body**
```json
{ "dailyGoal": 30 }
```
- `dailyGoal: Int` non-null (`@field:NotNull`)
- 허용 값: 10, 20, 30, 40, 50 (서버 검증)
- 다른 값 → `USER_E044 DAILY_GOAL_INVALID`

**응답**
```json
{ "dailyGoal": 30 }
```

**SuccessCode**
- `USER_S033 UPDATE_DAILY_GOAL` 신규

### 3.2 GET /v2/users/me/preferences (기존 확장)

응답에 `dailyGoal` 필드 추가.

**변경된 응답**
```json
{
  "defaultTestType": "MEANING_TO_TERM",
  "selectedGlossaryId": 6,
  "dailyGoal": 30
}
```

- 클라가 학습 Home 진입 시 GET preferences 로 받아서 목표 표시
- preference 관련 모든 값이 한 endpoint 에서 조회

---

## 4. todayGoalCompleted 로직 정정

### 4.1 현재 동작 (Phase 2.10 시점)
- `SubmitGlossaryTestResponse.todayGoalCompleted` 가 "오늘 dailyProgress row 가 처음 생성되는 순간" 으로 동작
- 즉 오늘 첫 학습 완료 시 true, 이후엔 항상 false (의미 모호)

### 4.2 변경 후
- "오늘 누적 학습 단어 수가 dailyGoal 이상이고, **이번 submit 으로 도달**한 경우" 만 true
- 그 외 모두 false (이미 도달 후 추가 학습 / 아직 미달 모두 false)

### 4.3 로직 의사 코드
```kotlin
val beforeCount = dailyProgress.learnedCount  // submit 전 누적
val delta = events.count { it.isNew }         // 이번 submit 의 신규 학습 단어 수
val afterCount = beforeCount + delta
val dailyGoal = userPreferenceService.getPreference(userId).dailyGoal

val todayGoalCompleted = beforeCount < dailyGoal && afterCount >= dailyGoal
```

- 기준은 "신규 학습 단어 수" (`isNew` = 처음 학습한 단어). 복습은 제외할지 결정 필요 (아래 8 참조)
- 도달 시점에만 1회 true 응답 → 클라가 축하 애니메이션 등 처리

### 4.4 적용 대상
- `SubmitGlossaryTestResponse.todayGoalCompleted` (기본 단어장 submit)
- 커스텀/오답 노트 submit 은 진도 트래킹 X 이므로 `SubmitNoteTestResponse` 에 필드 없음 (기존 유지)

---

## 5. 대시보드 응답에 노출 여부

`GET /v2/users/me/dashboard` 응답에 `dailyGoal` / `todayLearnedCount` 추가 검토.

옵션 A — 추가 안 함 (preferences GET 으로 별도 조회)
- dashboard 응답 변경 없음 (breaking 회피)
- 클라가 preferences + dashboard 두 endpoint 호출

옵션 B — dashboard 응답에 통합
- `{ todayGoalCompleted: Boolean, todayLearnedCount: Int, dailyGoal: Int }` 필드 추가
- 클라 단일 호출로 화면 표시 가능
- 단 dashboard 의 weekStatistics.days 와 정보 중복 (`days[today].learnedCount` 가 같은 값)

**추천: 옵션 B** — UX 진입 시 즉시 표시 필요. 단 학습 Home 화면이 dashboard 와 별개면 옵션 A 도 OK. 디자인 확인 필요.

---

## 6. 영향 받는 기존 코드

### UserPreferenceEntity
- `dailyGoal` 컬럼 추가 (기본값 10)
- `updateDailyGoal(goal: Int)` 메서드 추가

### UserPreferenceService
- 신규 메서드 `updateDailyGoal(userId, goal)`
- `getPreference` 응답 DTO 에 dailyGoal 포함

### UserPreferenceApi
- 신규 endpoint `PUT /preferences/daily-goal`

### TestService (또는 ProgressUpdateService)
- submit 시 dailyGoal 조회 + 도달 계산
- `SubmitGlossaryTestResponse.todayGoalCompleted` 로직 변경

### Validation
- dailyGoal 5단계 enum 으로 처리할지 (`enum class DailyGoal { TEN, TWENTY, ... }`) vs Int 직접 받고 in-list 검증?
- enum 이 type-safe 단 클라/DB 와의 매핑 복잡. **Int 직접 받고 service 검증** 권장

---

## 7. SuccessCode / ErrorCode

### 신규
| 코드 | 이름 | 메시지 |
|---|---|---|
| `USER_S033` | UPDATE_DAILY_GOAL | 일일 학습 목표를 변경했습니다 |
| `USER_E044` | DAILY_GOAL_INVALID | 일일 학습 목표는 10/20/30/40/50 중 하나여야 합니다 |

---

## 8. 미확정 / 결정 필요

1. **신규 학습만 카운트 vs 복습 포함**
   - 학습 동기 관점에서는 신규만 카운트가 자연스러움 (목표 = 새 단어 학습)
   - 단 사용자가 복습만 한 날 "오늘 목표" 가 0% 로 표시되면 위화감
   - 추천: 신규만 (디자인 확인 필요)

2. **dailyProgress 컬럼 활용**
   - 현재 `daily_progresses.learned_count` 가 신규 학습 누적
   - 같은 컬럼 그대로 사용 가능
   - 복습 포함 정책으로 바뀌면 `completed_count` (= learned + reviewed) 사용

3. **dashboard 응답에 통합 여부** (위 5 참조)

4. **목표 초과 시 UX**
   - 30개 목표인데 50개 학습한 날 → 표시는 30/30 으로 캡? 또는 50/30 으로 초과 표시?
   - 백엔드는 raw 값 전달, UI 결정사항

---

## 9. 구현 순서

1. spec 작성 (본 문서)
2. V44 마이그레이션 — `user_preferences.daily_goal` 컬럼 추가
3. UserPreferenceEntity / DTO 확장 (dailyGoal 필드 추가)
4. UserPreferenceService.updateDailyGoal 신규 + `getPreferenceResponse` 응답에 포함
5. UserPreferenceApi 에 `PUT /preferences/daily-goal` endpoint 추가
6. TestService submit 분기 — todayGoalCompleted 로직 변경
7. (옵션 B 선택 시) UserDashboardService 응답에 dailyGoal / todayLearnedCount / todayGoalCompleted 추가
8. SuccessCode / ErrorCode enum 추가
9. v2-roadmap.md 결정 누적 갱신
10. iOS 전달 메시지

---

## 10. 결정 누적

| 항목 | 결정 |
|---|---|
| 저장 위치 | `user_preferences.daily_goal` 컬럼 (기존 도메인 확장, 새 테이블 안 만듦) |
| 허용 값 | 10, 20, 30, 40, 50 (5단계) |
| 기본값 | 10 (DB DEFAULT + 기존 row 일괄) |
| 타입 | Int (enum 안 만듦, service 단 in-list 검증) |
| todayGoalCompleted 정의 | 오늘 누적 학습 단어 수가 dailyGoal 이상 + 이번 submit 으로 도달한 경우만 true |
| 적용 대상 응답 | `SubmitGlossaryTestResponse` (기본 단어장만). 커스텀/오답 submit 응답에는 필드 없음 |
| 카운트 기준 | 신규 학습 단어 수 (`daily_progresses.learned_count`) — 복습 제외 권장, 디자인 확인 |
| dashboard 응답 통합 | 옵션 B 추천 (`dailyGoal`, `todayLearnedCount`, `todayGoalCompleted` 추가) — 디자인 확인 후 확정 |
| 코드 prefix | USER_* 유지 |
| 마이그레이션 번호 | V44 |

---

## 11. 후속 검토

- 목표 달성 streak (연속 며칠 목표 달성) — 별도 지표
- 주간 / 월간 평균 학습량 — 통계 확장
- 목표 미달 시 알림 / 리마인더 — Phase 2 범위 외 (푸시 인프라 결정 후)
- 목표값 사용자 임의 입력 허용 (5단계 외) — 현재 5단계 디자인 기준
