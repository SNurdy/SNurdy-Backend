## Phase 2.11 — 학습 Home 보완 (default card count + 자주 틀린 단어)

학습하기 Home 화면 (`학습하기(Home).png`) 의 두 가지 신규 데이터 백엔드 지원.

1. **학습 목표 (default card count)** — 사용자별 한 세션 학습/테스트 카드 수 default (10/20/30/40/50)
2. **자주 틀린 단어** — 사용자가 자주 틀린 단어 카드 1개 (3회 이상 오답)

이전 spec 의 "daily goal" 해석은 잘못된 것 (오늘 누적 목표가 아니라 한 세션 카드 수). 본 spec 으로 정정.

---

## 1. 배경

### 1.1 학습 Home 화면 분석

`학습하기(Home).png` 의 핵심 영역:
- **연속 학습 21일째** + 주간 캘린더 — 기존 `/v1/user/streak` 또는 `/v2/users/me/dashboard` 의 weekStatistics 활용
- **심장내과 ▼ (드롭다운)** — selected glossary, 기존 `selectedGlossary` 활용
- **학습 목표 10개 ✎ (편집)** — 신규. 한 세션 학습 카드 수 default
- **다마고치 영역** (Lv.5, 86/100) — 기존 selectedGlossary level/progressRate 활용
- **학습하기 버튼** — `POST /v2/test/glossaries/{id}/cards` 진입
- **자주 틀린 단어 카드** (Acute sinusitis 예시) — 신규

### 1.2 "학습 목표 10개" 의 정확한 의미

| 잘못된 해석 | 실제 의미 |
|---|---|
| 오늘 누적 학습 목표 (daily goal) | **한 세션 학습 카드 수 default** |
| `todayGoalCompleted` (도달 시점 true) | 그런 개념 자체 없음 |

화면 3 (학습 목표 변경 시트) 의 카피 "오늘의 목표치를 달성해보세요!" 는 단순 UX 메시지. "한 세션 = 하루 한 번 학습" 가정의 표현일 뿐 누적 목표 아님.

사용자가 매 학습 진입 전에 `학습 목표 10개 ✎` 탭 → 시트에서 10/20/30/40/50 중 선택 → 저장. 다음 학습 시작 시 그 값이 default 로 표시.

---

## 2. default card count

### 2.1 데이터 모델

`user_preferences` 테이블에 컬럼 추가 (V44).

```sql
ALTER TABLE user_preferences
    ADD COLUMN default_card_count INT NOT NULL DEFAULT 10;
```

- 타입: `INT NOT NULL DEFAULT 10`
- 허용 값: 10, 20, 30, 40, 50
- 기존 row 일괄 default 10 적용

### 2.2 마이그레이션 (V44)

idempotent 패턴 (컬럼 존재 시 skip).

```sql
SET @add_col = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_preferences'
              AND column_name = 'default_card_count'
        ),
        'SELECT 1',
        'ALTER TABLE user_preferences ADD COLUMN default_card_count INT NOT NULL DEFAULT 10'
    )
);
PREPARE stmt FROM @add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 2.3 Endpoint

#### `PUT /v2/users/me/preferences/default-card-count`

**요청 body**
```json
{ "count": 30 }
```
- `count: Int` non-null (`@field:NotNull`)
- 허용 값: 10, 20, 30, 40, 50 (service 단 검증)
- 다른 값 → `USER_E044 DEFAULT_CARD_COUNT_INVALID`

**응답**
```json
{ "defaultCardCount": 30 }
```

**SuccessCode**
- `USER_S033 UPDATE_DEFAULT_CARD_COUNT` 신규

#### `GET /v2/users/me/preferences` (기존 확장)

응답에 `defaultCardCount` 필드 추가.

**변경된 응답**
```json
{
  "defaultTestType": "MEANING_TO_TERM",
  "selectedGlossaryId": 6,
  "defaultCardCount": 30
}
```

iOS 가 학습 Home 진입 시 GET preferences 로 받아서 selector 초기값 표시.

### 2.4 default 적용 동작

기존 `POST /v2/test/glossaries/{id}/cards` 의 `GlossaryCardsRequest.count` 는 클라가 명시 (기존 패턴 유지). 백엔드는 default 자동 적용 안 함.

- 클라가 selector 초기값으로 `defaultCardCount` 사용 (GET preferences 응답에서)
- 사용자가 다른 값 선택 시 그 값으로 cards 호출
- 사용자가 매번 같은 값 쓰면 selector 가 늘 그 값으로 보임 → UX 일관

즉 서버 동작은 변경 없음. 단순 사용자별 default 값 저장/조회.

---

## 3. 자주 틀린 단어 (랜덤 출제)

### 3.1 조건

`wrong_answer_terms` 중:
- `user_id = ?`
- `deleted_at IS NULL` (활성)
- `total_incorrect_count >= 3` (3회 이상 오답)
- `ORDER BY RAND() LIMIT 1` — **랜덤 출제** (학습 효과)

3회 미만 단어는 "자주 틀린" 으로 인정 안 함. 단어 없으면 응답에 null (클라가 카드 숨김).

**왜 랜덤?** 매번 같은 (가장 많이 틀린) 단어 노출 시 학습 효과 떨어짐. 3회+ 풀에서 랜덤이면 사용자가 매 학습 Home 진입마다 다른 단어와 마주침 → 반복 노출로 자연 학습.

### 3.2 Endpoint

#### `GET /v2/users/me/frequently-incorrect-term`

**응답 — 단어 있을 때**
```json
{
  "term": {
    "termId": 245,
    "spelling": "Acute sinusitis",
    "meaning": "급성부비동염",
    "abbreviation": null,
    "totalIncorrectCount": 5
  }
}
```

**응답 — 단어 없을 때** (3회 이상 오답 단어 없음)
```json
{ "term": null }
```

**SuccessCode**
- `USER_S034 GET_FREQUENTLY_INCORRECT_TERM` 신규

**ErrorCode**
- `USER_E010` (인증)

### 3.3 쿼리 성능 참고

MySQL `ORDER BY RAND()` 는 전체 row 정렬이라 큰 테이블에 비효율적. 단 사용자별 `wrong_answer_terms` row 수는 보통 수십~수백 개라 실효 부담 미미. 향후 row 수가 크게 늘면 OFFSET + COUNT 기반 랜덤 또는 별도 인덱스 전략 검토.

### 3.3 음성 재생

화면의 🔊 아이콘 = 단어 발음 재생. iOS 측에서 TTS (예: AVSpeechSynthesizer) 로 처리. 백엔드 영향 없음.

### 3.4 학습 Home 화면 다른 데이터와의 관계

dashboard endpoint 와 별개로 호출:
- iOS 가 학습 Home 진입 시:
  - `GET /v2/users/me/dashboard` (streak + selectedGlossary + weekStatistics)
  - `GET /v2/users/me/preferences` (defaultCardCount + defaultTestType + selectedGlossaryId)
  - `GET /v2/users/me/frequently-incorrect-term` (자주 틀린 단어, 랜덤)
- 또는 iOS 가 일부 캐싱 후 stale-while-revalidate (바텀시트 패턴 재사용). 단 자주 틀린 단어는 매번 랜덤이라 캐싱 X (또는 짧은 TTL)

dashboard 응답에 통합하지 않는 이유:
- 자주 틀린 단어는 dashboard 도메인 아닌 wrongAnswerNote 도메인 데이터
- 통합 시 dashboard 응답 비대화 + 도메인 책임 모호
- 별도 endpoint 가 가벼움 + 학습 Home 외 다른 화면에서도 재사용 가능

---

## 4. 영향 받는 기존 코드

### UserPreferenceEntity
- `defaultCardCount: Int` 컬럼 추가 (기본값 10)
- `updateDefaultCardCount(count: Int)` 메서드 추가

### UserPreferenceService
- 신규 메서드 `updateDefaultCardCount(userId, count)`
- `getPreferenceResponse` 응답 DTO 에 defaultCardCount 포함

### UserPreferenceApi / Controller
- 신규 endpoint `PUT /preferences/default-card-count`

### 신규 도메인 (자주 틀린 단어)

위치 결정 옵션:
- A. `user/wrongAnswerNote/` 안에 신규 endpoint 추가 (도메인 일관)
- B. `user/dashboard/` 안에 추가 (학습 Home BFF 관점)

추천: **A** — `wrong_answer_terms` 데이터 직접 사용이라 도메인 일관 우선.

`WrongAnswerNoteService` 에 메서드 추가:
- `getFrequentlyIncorrectTerm(userId): FrequentlyIncorrectTermResponse`
- 새 endpoint controller 추가

---

## 5. SuccessCode / ErrorCode

### 신규
| 코드 | 이름 | 메시지 |
|---|---|---|
| `USER_S033` | UPDATE_DEFAULT_CARD_COUNT | 한 세션 학습 카드 수를 변경했습니다 |
| `USER_S034` | GET_FREQUENTLY_INCORRECT_TERM | 자주 틀린 단어 조회에 성공했습니다 |
| `USER_E044` | DEFAULT_CARD_COUNT_INVALID | 학습 카드 수는 10/20/30/40/50 중 하나여야 합니다 |

---

## 6. 구현 순서

1. spec 작성 (본 문서) ✓
2. V44 마이그레이션 — `user_preferences.default_card_count` 컬럼 추가
3. UserPreferenceEntity / DTO 확장 (`defaultCardCount` 필드)
4. UserPreferenceService — `updateDefaultCardCount` + `getPreferenceResponse` 확장
5. UserPreferenceApi 에 `PUT /preferences/default-card-count` endpoint 추가
6. `WrongAnswerNoteService` 에 `getFrequentlyIncorrectTerm` 신규
7. 신규 endpoint controller (`GET /v2/users/me/frequently-incorrect-term`)
8. Repository 쿼리 (`wrong_answer_terms` 3회 이상 + `ORDER BY RAND() LIMIT 1`)
9. DTO (`FrequentlyIncorrectTermResponse`)
10. SuccessCode / ErrorCode enum 추가
11. v2-roadmap.md 결정 누적 갱신
12. iOS 전달 메시지

---

## 7. 결정 누적

| 항목 | 결정 |
|---|---|
| "학습 목표 N개" 의미 | 한 세션 학습/테스트 카드 수 default (오늘 누적 목표 아님) |
| 저장 위치 | `user_preferences.default_card_count` 컬럼 (V44) |
| 허용 값 | 10, 20, 30, 40, 50 (5단계) |
| 기본값 | 10 (DB DEFAULT + 기존 row 일괄) |
| 타입 | Int (enum 안 만듦, service 단 in-list 검증) |
| 적용 동작 | 백엔드는 default 저장/조회만. 클라가 cards 호출 시 명시 (기존 GlossaryCardsRequest.count 패턴 유지) |
| GET preferences 응답 확장 | `defaultCardCount` 추가 |
| 자주 틀린 단어 조건 | `total_incorrect_count >= 3`, `ORDER BY RAND() LIMIT 1` (랜덤 출제 — 학습 효과) |
| 자주 틀린 단어 없을 때 | `{ "term": null }` 응답 (404 X) |
| 자주 틀린 단어 도메인 위치 | `user/wrongAnswerNote/` (데이터 도메인 일관) |
| dashboard 응답 변경 | 없음 — 별도 endpoint 로 분리 |
| `todayGoalCompleted` 처리 | **그대로 유지** (Phase 2.8 의 "오늘 첫 테스트 완료" 동작 그대로). 사용자가 default 카드 수만큼 한 세션 학습 = "오늘의 목표 달성" 으로 자연스럽게 해석. 의미 변경 / 응답 변경 없음 |

---

## 8. 미확정 / 후속 검토

1. 학습 Home 화면 데이터 통합 endpoint 신설 검토
   - 현재 3개 호출 (dashboard + preferences + frequently-incorrect-term)
   - 통합 BFF endpoint (`GET /v2/users/me/learning-home`) 한 번에 받기
   - 단 도메인 책임 분산 vs 호출 통합 trade-off
   - 후속 spec 으로 분리

2. 다른 카드 수 단계 추가 (5, 100 등)
   - 디자인 변경 시 enum 확장

3. 자주 틀린 단어 응답에 발음 / 어근 정보 포함 검토
   - 현재는 termId + spelling + meaning + abbreviation + totalIncorrectCount 만
   - 화면에서 단어 탭 시 상세로 이동 흐름이면 termId 만으로 충분
   - 카드에서 바로 발음 듣기 / 어근 보기 UX 면 morphemes 등 추가 필요

---

## 9. 후속 작업 (Phase 2 범위 외)

- 자주 틀린 단어 카드에서 단어 상세 / 학습 진입 흐름 — UX 결정 후
- 자주 틀린 단어 리스트 (3회 이상 전체) — 별도 endpoint 가능
- 학습 Home 의 "연속 학습 7일치" 캘린더 — 기존 weekStatistics 활용 (변경 없음)
