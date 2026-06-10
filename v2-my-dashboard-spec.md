# 마이페이지 대시보드 spec

세부 명세는 swagger 참고. 본 문서는 배경 + 화면 매핑 + 핵심 결정 사항.

## 1. 배경

탭바의 "마이페이지" 진입 화면 (마이페이지.png) 에 필요한 데이터를 통합 제공. 마이페이지는 메인 + 단어장 변경 바텀시트 + 과 지표 상세 3개 화면으로 구성되며 각각 별 endpoint.

학습 Home 과 별도 BFF 도메인 (`domain/dashboard/me/`) 으로 분리 — 두 화면이 본질적으로 다른 정보 (마이페이지 = 사용자 활동 요약 / 학습 Home = 학습 시작 화면).

## 2. URL

| Endpoint | 화면 | SuccessCode |
|---|---|---|
| `GET /v2/dashboards/me?startDate=...` | 마이페이지 메인 | `USER_S020 GET_DASHBOARD` |
| `GET /v2/dashboards/me/glossaries` | 단어장 변경 바텀시트 | `USER_S021 LIST_GLOSSARIES` |
| `GET /v2/dashboards/me/glossaries/statistics?period=WEEK\|ALL` | 과 지표 상세 | `USER_S022 LIST_GLOSSARY_STATISTICS` |

## 3. 응답 필드 - 화면 매핑

### 3.1 메인 (`MyDashboardResponse`)

| 필드 | 화면 매핑 |
|---|---|
| `streak.longestStreak` | "3주" (클라 = 21일 / 7 변환) |
| `streak.totalLearningTerms` | "340개 학습한 총 단어" |
| `selectedGlossary.*` | 다마고치 영역 (title, learnedCount, totalTermCount, level) |
| `weekStatistics.startDate` / `endDate` | "< 2월 22일 ~ 2월 28일 >" 헤더 (좌우 화살표 슬라이딩) |
| `weekStatistics.days[].date / dayOfWeek` | 일/월/화/수/목/금/토 라벨 |
| `weekStatistics.days[].newLearnedCount` | 막대 그래프 높이 (신규 학습 단어 수) |

### 3.2 단어장 변경 바텀시트 (`DashboardGlossaryListItem`)

| 필드 | 화면 매핑 |
|---|---|
| `glossaryId` | 변경 시 PUT 호출 식별자 |
| `category` | "전체 / 외과 / 내과 / 기타" 탭 그룹화 키 |
| `title` | "상세 과명" |
| `learnedCount / totalTermCount` | "000 / 000" 진척률 막대 + 숫자 |

(학습 Home 의 단어장 변경 바텀시트도 이 endpoint 재사용)

### 3.3 과 지표 상세 (`DashboardGlossaryStatisticItem`)

| 필드 | 화면 매핑 |
|---|---|
| `glossaryId` | 카드 식별자 |
| `title` | "정형외과" |
| `learnedCount / totalTermCount` | 게이지 "85 / 100" + "총 학습 단어" 라벨 |
| `level` | 다마고치 이미지 |
| `periodWrongAnswerCount` | "232개 오답 노트" |

## 4. 핵심 결정 사항

### 4.1 주간 슬라이딩 (마이페이지만)
- `startDate` query 로 7일 윈도우 이동
- 자연주 (학습 Home 의 자연주 고정) 와 다름

### 4.2 막대 그래프 metric
- `newLearnedCount` 만 노출 (디자인팀 확정 - 신규 학습 단어 수)
- 게이지 (`learnedCount`) 와는 의미 다름 (그날 신규 vs 누적 완료)

### 4.3 과 지표 period 의미
- `WEEK`: 이번 주 월요일 00:00 KST 이후 가장 최근 오답 발생한 활성 오답 단어 수
- `ALL`: 현재 활성 상태인 모든 오답 단어 수 (시점 무관)
- "활성" = `wrong_answer_terms.deleted_at IS NULL`
- period 와 무관하게 같은 단어장 list 노출 (학습 시작한 모든 단어장)

### 4.4 과 지표 응답 - period 영향 필드
- `periodWrongAnswerCount` 만 period 영향
- `learnedCount / totalTermCount / level` 은 누적 (period 무관)

## 5. 미사용 필드 정리 결과

- `MyDashboardResponse.selectedGlossary.category` / `progressRate` / `lastLearnedAt` / `availableCounts` 제거
- `DashboardGlossaryListItem.progressRate` / `level` / `lastLearnedAt` 제거 (lastLearnedAt 은 서비스 내부 정렬 키로만 사용)
- `DashboardGlossaryStatisticItem.category` / `progressRate` / `periodLearnedTermCount` 제거 (마지막은 디자인 mock 실수로 learnedCount 와 중복)
