# 학습 Home 대시보드 spec

세부 명세는 swagger 참고. 본 문서는 배경 + 화면 매핑 + 핵심 결정 사항.

## 1. 배경

탭바의 "학습하기" 진입 시 단일 BFF endpoint 로 학습 시작 화면 (학습하기(Home).png) 의 모든 정보를 한 번에 전달. 클라가 streak, 선택 단어장 다마고치, 학습 카드 수 default, 주간 캘린더 체크, 자주 틀린 단어 카드를 각각 별 endpoint 로 호출하지 않도록 통합.

## 2. URL

```
GET /v2/dashboards/study
```

- Query 없음 (이번 주 자연주 고정)
- SuccessCode `USER_S023 GET_STUDY_DASHBOARD`

## 3. 응답 필드 - 화면 매핑

| 필드 | 화면 매핑 |
|---|---|
| `currentStreak` | "연속 학습 21 일째" |
| `selectedGlossary.glossaryId` | 학습하기 버튼 호출 시 식별자 |
| `selectedGlossary.title` | "심장내과 ▼" |
| `selectedGlossary.learnedCount / totalTermCount` | "86 / 100" 게이지 |
| `selectedGlossary.level` | "Lv.5" |
| `defaultCardCount` | "학습 목표 10개 ✎" |
| `weekStatistics.days[].date` | "12 13 14 15 16 17 18" |
| `weekStatistics.days[].studied` | ✓ 마크 |
| `frequentlyIncorrectTerm.*` | "자주 틀린 단어" 카드 |

## 4. 핵심 결정 사항

### 4.1 자주 틀린 단어 추출 조건
- `wrong_answer_terms` 중 `glossary_id = selectedGlossaryId` + `deleted_at IS NULL` + `total_incorrect_count >= 3` + `ORDER BY RAND() LIMIT 1`
- 선택 단어장 외 다른 단어장 오답은 제외 (학습 흐름 일관성)
- 단어 없으면 응답 필드 null

### 4.2 주간 학습 여부 판정
- `studied = (new_learned_count + all_learned_count) > 0` (1건이라도 활동 있으면 true)
- 캘린더 날짜 숫자만 표시 (요일 라벨 없음) → 응답에 `dayOfWeek` 없음

### 4.3 자연주 고정
- 이번 주 월요일 ~ 일요일 (Asia/Seoul)
- 사용자 슬라이딩 X (마이페이지와 다름)

## 5. 제약

- `defaultCardCount` 허용 값: 10, 20, 30, 40, 50 (`user_preferences.default_card_count`)
- 변경: `PUT /v2/users/me/preferences/default-card-count` (별 endpoint)

## 6. 미사용 필드 정리 결과

화면 매핑 점검 후 다음 필드 제거됨:
- `selectedGlossary.category` / `progressRate` / `lastLearnedAt` / `availableCounts`
- `weekStatistics.startDate` / `endDate`
- `weekStatistics.days[].dayOfWeek`
- `frequentlyIncorrectTerm.totalIncorrectCount`
