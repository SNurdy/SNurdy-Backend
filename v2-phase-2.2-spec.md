# Phase 2.2 — 마이페이지 대시보드 재설계

기획팀 디자인 분석 (마이페이지.png, 마이페이지-1.png, 마이페이지_지표.png, 과별 지표 상세보기_이번 주.png, 과별 지표 상세보기_전체.png) 결과를 바탕으로 단일 dashboard endpoint 를 화면별 3개 endpoint 로 분리한다.

---

## 1. 화면 구조 요약

### 화면 A — 마이페이지 메인 (마이페이지.png)
- 학습한 총 단어 (totalLearningTerms)
- 최대 연속 학습 (longestStreak, 일수)
- 선택된 과의 다마고치 (단일 glossary level + 게이지)
- 학습 정보 막대 그래프 (7일치, 좌우 화살표 +7/-7, 캘린더에서 임의 날짜 선택 가능)

### 화면 B — 과 변경 바텀시트 (마이페이지-1.png)
- 진입 메인 화면 ④-1 드롭다운 클릭
- 카테고리 탭 전체/외과/내과/기타 (클라이언트 메모리 필터)
- 27개 정적 glossary 목록 (학습 시작 안 한 과 포함)
- 각 항목 학습 게이지 (learnedCount/totalTermCount)

### 화면 C — 과 지표 리스트 (마이페이지_지표.png + 과별 지표 상세보기_*.png)
- 진입 메인 화면 ④-2 자세히 보기 클릭
- 탭바 이번 주 / 전체
- 학습 시작한 과만 카드로 표시 (학습 안 한 과 제외)
- 각 카드 진척률 게이지, 다마고치 레벨, 오답 노트 개수, 총 학습 단어 개수

---

## 2. API 명세

### 2.1 GET /v2/users/me/dashboard — 메인 대시보드

**요청 파라미터**
- startDate LocalDate optional
  - 없으면 today-6 (오늘 포함 직전 7일, 오늘이 끝)
  - 있으면 startDate ~ startDate+6 (startDate 가 시작)
  - 캘린더 임의 날짜 선택 또는 좌우 화살표 (+7/-7) 시 클라이언트가 startDate 계산해 전달

**응답**
```
{
  "streak": {
    "longestStreak": 21,
    "totalLearningTerms": 340
  },
  "selectedGlossary": {
    "glossaryId": 12,
    "title": "심장내과",
    "category": "내과",
    "learnedCount": 85,
    "totalTermCount": 100,
    "progressRate": 85.0,
    "level": 5,
    "lastLearnedAt": "2026-06-04T10:23:00Z"
  },
  "weekStatistics": {
    "startDate": "2026-05-29",
    "endDate": "2026-06-04",
    "days": [
      { "date": "2026-05-29", "dayOfWeek": "FRIDAY", "learnedCount": 10, "completedCount": 8 }
    ]
  },
  "generatedAt": "2026-06-04T12:00:00Z"
}
```

**필드 결정 사항**
- streak 응답에서 currentStreak 제거 (디자인 마이페이지에 노출 안 됨, 별도 GET /v1/user/streak 에서 제공)
- selectedGlossary 결정 로직 (정정 — 이전 lastLearnedAt 자동 선택 폐기)
  - `user_preferences.selected_glossary_id` 단일 source. 자동 fallback / 최근 학습 추적 없음
  - 신규 유저는 가입 service 가 심장내과 (glossaryId=6) 로 INSERT 해두므로 항상 값 존재 → 응답 non-null 보장
  - 사용자가 학습을 진행해도 selectedGlossary 자동 변경 없음
  - 변경하려면 PUT /v2/users/me/preferences/selected-glossary 호출 필요
- 사용자가 드롭다운으로 다른 과 선택 시 클라이언트가 위 PUT endpoint 호출 후 dashboard 재조회
- weekStatistics.days 는 항상 7개. 학습 데이터 없는 날도 learnedCount=0, completedCount=0 으로 채움

**SuccessCode**
- USER_S020 GET_DASHBOARD (기존 유지)

**ErrorCode**
- USER_E010 ACCOUNT_NOT_FOUND (인증 토큰은 유효하지만 DB 사용자 없음)

---

### 2.2 GET /v2/users/me/dashboard/glossaries — 과 변경 바텀시트

**요청 파라미터**
- 없음 (27개 정적, 카테고리 필터는 클라이언트가 응답 받은 뒤 탭별로 처리)

**응답**
```
{
  "glossaries": [
    {
      "glossaryId": 1,
      "title": "신경외과",
      "category": "외과",
      "learnedCount": 0,
      "totalTermCount": 50,
      "progressRate": 0.0,
      "level": 1,
      "lastLearnedAt": null
    }
  ]
}
```

**필드 결정 사항**
- 모든 glossary 카탈로그 반환 (학습 시작 안 한 과 포함)
- 카테고리 라벨은 각 항목의 category 필드에 포함, 클라이언트가 탭별 필터
- 페이지네이션 없음 (27개 고정)
- 정렬 lastLearnedAt 내림차순 (학습 안 한 과는 null 이므로 마지막, glossaryId 오름차순으로 보조 정렬)

**캐싱 정책 (iOS 책임)**
- iOS 가 stale-while-revalidate 패턴 적용 (캐시 즉시 표시 + 백그라운드 갱신)
- 백엔드는 Cache-Control / ETag 헤더 미적용 (iOS 자체 캐시 정책과 충돌 회피)
- 학습 submit 후 진척률 변경 시 iOS 가 캐시 명시적 무효화

**SuccessCode**
- USER_S021 LIST_DASHBOARD_GLOSSARIES

**ErrorCode**
- USER_E010 ACCOUNT_NOT_FOUND

---

### 2.3 GET /v2/users/me/dashboard/glossary-statistics — 과 지표 리스트

**요청 파라미터**
- period DashboardGlossaryPeriod optional, default WEEK
  - WEEK 이번 주 자연주 (월요일 00:00 Asia/Seoul ~ 오늘 23:59:59)
  - ALL 전체 누적

**응답**
```
{
  "period": "WEEK",
  "glossaries": [
    {
      "glossaryId": 12,
      "category": "외과",
      "title": "정형외과",
      "learnedCount": 85,
      "totalTermCount": 100,
      "progressRate": 85.0,
      "level": 5,
      "periodLearnedTermCount": 49,
      "periodWrongAnswerCount": 232
    }
  ]
}
```

**필드 결정 사항**
- 학습 시작한 과만 반환 (GlossaryProgressEntity 가 있는 과). 학습 안 한 과는 제외
- 정렬 lastLearnedAt 내림차순 (가장 최근 학습한 과가 위), 동률 시 glossaryId 오름차순
- learnedCount, totalTermCount, progressRate, level 은 항상 누적 (period 영향 없음)
- periodLearnedTermCount 정의 user + glossary 한정으로 lastReviewedAt 가 기간 내인 distinct termId 수
- periodWrongAnswerCount 정의 user + glossary 한정으로 WrongAnswerTermEntity 의 incorrectAt 가 기간 내이고 deletedAt IS NULL 인 단어 종 수 (한 단어 entity 가 단어 1종이라 자연 distinct)
- ALL period 일 때 두 카운트 모두 기간 조건 없음 (전체 누적)

**SuccessCode**
- USER_S022 LIST_DASHBOARD_GLOSSARY_STATISTICS

**ErrorCode**
- USER_E010 ACCOUNT_NOT_FOUND

**N+1 회피**
- TermProgressRepository.countLearnedSinceByGlossaries 와 WrongAnswerTermRepository.countActiveByGlossaries 가 GROUP BY 로 한 번에 batch 조회

---

## 3. 코드 변경 사항 (구현 완료)

### 변경
- UserDashboardService.getDashboard 시그니처 from/to LocalDate? -> startDate LocalDate? 로 교체
- dailyStatistics -> weekStatistics 로 변경, 항상 7일 보장 (없는 날 0 채움)
- streak 응답에서 currentStreak 제거 (응답 DTO 필드 삭제, 도메인 streak 엔티티는 그대로)
- glossaryStatistics 리스트 제거, selectedGlossary 단일 필드로 교체
- 단일 detail endpoint /dashboard/glossaries/{glossaryId} 폐기, 리스트 /dashboard/glossary-statistics 신설

### 신규
- UserDashboardController endpoint 2.2 / 2.3 추가
- DashboardGlossaryListResponse / DashboardGlossaryStatisticsResponse DTO
- LIST_DASHBOARD_GLOSSARIES / LIST_DASHBOARD_GLOSSARY_STATISTICS SuccessCode
- Repository 신규 쿼리 2개
  - TermProgressRepository.countLearnedSinceByGlossaries (batch)
  - WrongAnswerTermRepository.countActiveByGlossaries (batch)

### 미사용 (코드 안정성 유지)
- USER_E020 DASHBOARD_GLOSSARY_NOT_FOUND ErrorCode 는 단일 detail 폐기로 더 이상 throw 되지 않음. 이전 응답 호환 위해 enum value 는 유지

---

## 4. 자연주 정의 합의

- 자연주 시작 요일 월요일 (DayOfWeek.MONDAY)
- WEEK period 정의 이번 주 월요일 00:00 ~ 오늘 23:59:59 Asia/Seoul (오답노트 도메인과 일관)
- 메인 대시보드 막대 그래프는 자연주 아님 (예외, 사용자 임의 7일 슬라이딩)
- 자연월 정의 매달 1일 ~ 오늘 (오답노트 도메인 정책 그대로 유지)

---

## 5. 화면별 endpoint 매핑

| 화면 | 진입 트리거 | endpoint |
|---|---|---|
| 마이페이지 메인 (마이페이지.png) | 탭바 마이페이지 클릭 | 2.1 GET /dashboard |
| 과 변경 바텀시트 (마이페이지-1.png) | 메인 ④-1 드롭다운 클릭 | 2.2 GET /dashboard/glossaries |
| 과 지표 리스트 (마이페이지_지표.png) | 메인 ④-2 자세히 보기 클릭 | 2.3 GET /dashboard/glossary-statistics |
