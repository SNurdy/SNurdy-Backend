# 커스텀 단어장 북마크 뷰 spec

세부 명세는 swagger 참고. 본 문서는 배경 + UX 흐름 + 핵심 결정 사항 정리.

## 1. 배경

사전 단어 카드에서 "내 단어장에 추가" 버튼 탭 시 노출되는 북마크 뷰 (카드 학습 북마크 뷰.png) 흐름 지원. 사용자는 단어 1개를 본인 소유 커스텀 단어장 중 N개에 한 번에 담을 수 있어야 한다.

기존 endpoint 한계:
- `POST /v2/custom-notes/{customNoteId}/terms` 는 단어 N개를 단어장 1개에 담는 구조 (단어 N : 단어장 1).
- 북마크 뷰는 반대 케이스 (단어 1 : 단어장 N) 라 신규 endpoint 필요.
- 또한 진입 시점에 각 단어장이 그 단어를 이미 보유 중인지 (체크/disabled 표시) 알아야 하므로 조회용 endpoint 도 필요.

## 2. UX 흐름

1. 사용자가 단어 카드에서 북마크 액션 탭
2. 북마크 뷰 진입 → 본인 단어장 list + 각 항목의 "이 단어 보유 여부" 표시
   - 이미 보유한 단어장 → 회색 "이미 저장했어요" disabled
   - 미보유 단어장 → 체크 가능
3. 체크한 단어장들 + "추가하기" 탭 → 단어 동시 추가

화면 의도상 보유 단어장은 토글 불가 (즉 등록과 삭제 동시 발생 X). 추가만 처리.

## 3. 신규 endpoint

### 3.1 진입 조회

```
GET /v2/custom-notes/terms/{glossaryId}/{termId}
```

- 응답: 본인 커스텀 단어장 전체 + 각 단어장의 `hasTerm` (이 단어 보유 여부) + `termCount` (250 도달 disabled 처리용)
- 정렬: 생성 역순 (기존 list endpoint 와 일관)
- 사전 단어 존재 검증 X (성능). 가짜 termId 면 모든 hasTerm=false
- SuccessCode: `CUSTOM_NOTE_S017 LIST_NOTES_FOR_TERM`

### 3.2 다중 단어장 동시 추가

```
POST /v2/custom-notes/terms
```

- 요청: `glossaryId`, `termId`, `customNoteIds` (1~20)
- 응답: `addedCustomNoteIds` (skip 된 노트는 응답에서 제외)
- SuccessCode: `CUSTOM_NOTE_S016 ADD_TERM_TO_NOTES`

#### 검증 정책 (CLAUDE.md 4.2 단일/다중 일관)

| 케이스 | 처리 |
|---|---|
| customNoteIds 일부 존재 X | 전체 거부 `CUSTOM_NOTE_E001` |
| 일부 본인 소유 X | 전체 거부 `CUSTOM_NOTE_E002` |
| glossaryId 사전 X | `DICTIONARY_E001` |
| (glossaryId, termId) 매칭 X | `DICTIONARY_E002` |
| 추가 대상 노트 중 termCount + 1 > 250 | 전체 거부 `CUSTOM_NOTE_E004` |
| 일부 노트에 이미 단어 존재 (race) | silently skip |

## 3.3 기존 단어 추가 API 와의 차이

해당 API 는 기존 구현되어 있는 커스텀 단어장 내 단어 추가 API 와 플로우가 다르다. 둘은 보완 관계로 공존한다.

| 항목 | 기존 `POST /v2/custom-notes/{customNoteId}/terms` | 신규 `POST /v2/custom-notes/terms` |
|---|---|---|
| 카디널리티 | 단어장 1 : 단어 N | 단어 1 : 단어장 N |
| 진입 화면 | 단어장 상세 화면 (단어장 안에서 단어 다중 선택) | 단어 카드 화면 (북마크 뷰) |
| path | `/{customNoteId}/terms` (특정 단어장 sub-resource) | `/terms` (단어 시점 액션) |
| 요청 body 핵심 | `items: [{ glossaryId, termId }, ...]` (최대 100개) | `glossaryId, termId, customNoteIds: [...]` (최대 20개) |
| 응답 핵심 | `added`, `skipped`, `totalTermCount` (단어 수 카운트) | `addedCustomNoteIds` (추가된 단어장 id 목록) |
| skip 정책 | 이미 단어장에 있는 단어 silently skip + `skipped` 카운트 노출 | 이미 단어 보유한 단어장 silently skip + 응답에서 제외 |
| 사용 UX | 단어장 편집 모드 같은 흐름에서 다수 단어를 한 단어장에 batch 추가 | 단어 1개를 여러 단어장에 한 번에 담기 |

---

## 4. 응답 디자인 결정

`addedCustomNoteIds` 만 노출하고 `skippedNoteIds` 는 응답에서 제외. UX 상 보유 단어장은 disabled 라 skip 자체가 race 케이스만 발생 (사실상 항상 빈 배열). 가이드 4.1 "의미 없는 필드 노출 금지" 일관.

## 5. 제약

- `customNoteIds` 최대 20개 (사용자당 단어장 최대 개수와 일치)
- 단어장당 최대 단어 수 250 (기존 제약 유지)
- 두 endpoint 모두 인증 필수

## 6. URL 도메인 결정

신규 도메인 패키지 (`domain/term/`) 신설 안 함. 기존 `customNote` 도메인 안에서 처리하되 URL 만 `/v2/custom-notes/terms/...` 로 단어 시점 액션임을 명시.

## 7. iOS 전달 사항

DTO 이름/필드 prefix 정책 변경도 함께 적용됨. 별 문서 `ios-handoff-customNote-wrongAnswerNote-rename.md` 참고.
