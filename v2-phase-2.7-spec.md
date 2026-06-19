# Phase 2.7 — 공유 단어장 명세 (코드 + URL 듀얼 경로)

본 명세는 회사 PC 에서 설계만 작성. 구현은 집에서 진행 (오답노트 2.5 완료 후).
충돌 회피를 위해 본 단계에서는 코드/마이그레이션 변경 없음.

기획안 최종 (사용자 확정):
- 공유 매개체: **shareCode + shareUrl 둘 다 지원**
  - **카톡 등 메신저에서 URL 클릭 -> 앱 자동 열림 -> 자동 import** (Universal Link)
  - **앱 내 코드 입력 dialog 에서 8자 코드 직접 입력 -> import** (수동)
- 코드 포맷: **영문 + 숫자 8자** (모호 문자 제외)
- URL 형식: `{prefix}/{code}` (Universal Link, prefix 는 application.yml 설정)
- 공유 링크 만료: 공유한 사용자가 단어장 soft delete 시 자연 만료
- 공유받은 단어장 = 본인 단어장으로 복제 (이후 독립)
- 공유 비활성화: 필요

---

## 1. 디자인 화면 흐름 요약

### 경로 A — URL (카톡 등 외부 메신저 진입)
```
[발신자] 단어장 상세 -> 공유 아이콘 -> "공유하기" -> iOS share sheet -> 카톡으로 URL 전송
                                                                              |
[수신자] 카톡에서 URL 클릭 -> iOS Universal Link 동작 -> 앱 자동 실행
              -> 앱이 URL 에서 {code} 추출
              -> 자동으로 POST /v2/custom-notes/from-share/{code} 호출
              -> 단어장 자동 추가 완료 (로딩 화면 짧게 노출)
```

### 경로 B — 코드 (앱 내 수동 입력)
```
[단어장 메인]
  - 우측 하단 + FAB
        |
        |- "단어장 불러오기" -> 코드 입력 dialog -> 8자 코드 직접 입력
                                  |
                                  -> POST /v2/custom-notes/from-share/{code}
                                  -> 로딩 -> 완료
                                  |
                                  +- 실패 시 "코드가 올바르지 않습니다!"
```

### 공유 dialog (발신자 측, 단어장 상세 우상단 공유 아이콘)
```
"단어장을 공유하세요" dialog
   8자 코드 표시 (예: "k7Mx9pQ2")
   URL 표시 (예: "https://snurdy.app/shared/k7Mx9pQ2")
   [코드 복사]      <- shareCode 만 클립보드에
   [링크 복사]      <- shareUrl 만 클립보드에
   [공유하기]       <- iOS share sheet 호출 (URL 전송, 카톡 미리보기 표시)
   [취소]
```

백엔드는 **두 형태 동시 응답** (4.1). iOS 가 상황에 맞게 선택 사용.

---

## 2. 도메인 위치

`domain/user/sharedGlossary/` 신규 패키지.

```
domain/user/
├── customNote/
├── wrongAnswerNote/      (Phase 2.5, 별도 wrong_terms/wrong_terms_notes 테이블)
└── sharedGlossary/       (신규)
    ├── controller/
    │   ├── SharedGlossaryApi
    │   └── SharedGlossaryController
    ├── dto/SharedGlossaryDto
    └── service/
        ├── SharedGlossaryService    (코드 발급/조회/복제/비활성화)
        └── ShareCodeGenerator       (nanoid 스타일 12자)
```

ErrorCode/SuccessCode prefix: `SHARED_GLOSSARY_*`

---

## 3. 공유 코드 + URL 정책

### 3.1 코드 발급 — lazy
- 최초 `POST /v2/custom-notes/{noteId}/share` 호출 시 발급
- 이미 발급되어 있으면 같은 코드 반환 (멱등)

### 3.2 코드 포맷
- **8자, 영문 + 숫자 혼합**
- 모호 문자 제외 (`0OIl1`) -> 사용자가 손으로 타이핑 가능성 고려 (메신저로 받은 코드 수동 입력)
- 사용 가능 문자 풀: `23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz` (54자)
- 조합 수: 54^8 ≈ 7.2조 -> 충돌 가능성 사실상 0
- 발급 시 DB UNIQUE 충돌 검출 시 재발급 retry 최대 3회

### 3.3 URL 생성 — Universal Link 필수
카톡 등 메신저에서 URL 클릭 -> 앱 자동 실행이 동작하려면 **Universal Link (HTTPS)** 필수. Custom Scheme (`snurdy://...`) 은 카톡 in-app browser 가 차단함.

- 형식: `{prefix}{code}` (예: `https://snurdy.app/shared/k7Mx9pQ2`)
- prefix 는 application.yml 의 환경별 설정:
  ```yaml
  sharing:
    glossary-url-prefix: ${SHARING_GLOSSARY_URL_PREFIX:https://snurdy.app/shared/}
  ```
- `@ConfigurationProperties("sharing")` 으로 바인딩 (`SharingProperties` 클래스)
- `SharedGlossaryService` 가 `prefix + code` 로 조립해서 응답

### 3.4 iOS 측 사전 작업 (백엔드 무관, 참고)
- `apple-app-site-association` 파일을 `https://snurdy.app/.well-known/` 에 호스팅
- iOS 앱 entitlement 에 `applinks:snurdy.app` 등록
- iOS 가 URL 의 마지막 path segment 를 `{code}` 로 추출 -> `POST /v2/custom-notes/from-share/{code}` 호출

### 3.5 신규 컬럼 (불필요)
`shareCode` 컬럼은 V27 에서 이미 nullable UNIQUE 로 존재. 추가 마이그레이션 없음.

---

## 4. API endpoint

### 4.1 공유 코드 발급/조회
**`POST /v2/custom-notes/{noteId}/share`**

응답:
```json
{
  "noteId": 12,
  "title": "외과 단어장",
  "ownerNickname": "지윤",
  "termCount": 42,
  "shareCode": "k7Mx9pQ2",
  "shareUrl": "https://snurdy.app/shared/k7Mx9pQ2"
}
```

- 권한: 본인 CUSTOM glossary 만
- DEFAULT/WRONG_ANSWER glossary 거부 (`SHARED_GLOSSARY_E002`)
- 이미 발급되어 있으면 같은 코드 반환 (재호출 안전)
- `shareCode` 와 `shareUrl` 동시 응답 — iOS 가 dialog 에 둘 다 노출, "코드 복사" / "링크 복사" / "공유하기" 각 버튼이 다르게 사용
- `title`, `ownerNickname`, `termCount` 동봉 — iOS 공유 dialog 헤더에 "어떤 단어장을 공유하는지" 표시 (예: "지윤 님의 '외과 단어장' (42개 단어) 을 공유합니다")

### 4.2 코드로 단어장 조회 (read-only 미리보기)
**`GET /v2/shared-glossaries/{code}`**

응답:
```json
{
  "shareCode": "k7Mx9pQ2",
  "title": "외과 단어장",
  "ownerNickname": "지윤",
  "termCount": 42,
  "createdAt": "2026-05-20T10:15:30Z",
  "page": 0,
  "size": 20,
  "totalPages": 3,
  "hasNext": true,
  "terms": [
    { "id": 586, "spelling": "Acute Kidney Injury", "meaning": "급성신손상", "abbreviation": "AKI" }
  ]
}
```

- 권한: 로그인 필요, 누구나 조회 가능
- **단어 목록 페이지네이션** (8.3 결정)
- 만료/비활성화/원본 soft-deleted 상태면 `SHARED_GLOSSARY_E001 SHARE_CODE_NOT_FOUND`
- 디자인 흐름상 이 endpoint 는 **import 전 preview 용**으로 사용될 수도, 또는 import 가 곧바로 진행될 수도 있음 — 13번 참조

### 4.3 본인 단어장으로 복제 (두 경로 공통 종착점)
**`POST /v2/custom-notes/from-share/{code}`**

이 endpoint 는 두 가지 진입 경로에서 동일하게 호출됨:
- **경로 A (URL)**: 카톡에서 URL 클릭 -> iOS Universal Link 동작 -> 앱이 URL 에서 code 추출 -> 이 endpoint 호출
- **경로 B (코드)**: 앱 내 코드 입력 dialog -> 사용자가 8자 코드 직접 입력 -> 이 endpoint 호출

백엔드 입장에서는 `{code}` path variable 만 받으므로 두 경로의 처리 로직 동일.

응답: 신규 생성된 CustomNote 응답 (`CustomNoteResponse`)

- 권한: 로그인 필요. 본인 단어장 코드로 복제 시도도 허용
- 새 CUSTOM glossary 생성:
  - `ownerId` = 복제하는 사용자
  - `type` = CUSTOM
  - `shareCode` = null (복제본은 별도 공유 코드 필요 시 재발급)
- `glossary_terms` 매핑 복사 (단어 데이터 자체는 사전 공통 자원이므로 매핑만)
- title 기본값: 원본 title 그대로. UNIQUE `(owner_id, title)` 충돌 시 ` (사본)` 또는 ` (2)` suffix 자동 부여
- 진행도/메모/북마크 복사 안 함 (새 단어장이므로 처음부터)
- **복제 후 원본과 완전 독립**
- 본인 CUSTOM 갯수 20개 도달 시 `SHARED_GLOSSARY_E003 IMPORT_LIMIT_EXCEEDED` (8.2)
- 코드 만료/존재 안 함 시 `SHARED_GLOSSARY_E001` -> iOS 가 통일된 "링크가 올바르지 않습니다!" 표시

### 4.4 공유 비활성화 (기획안 필수)
**`DELETE /v2/custom-notes/{noteId}/share`**

- `shareCode = null` 처리
- 권한: 본인 CUSTOM 만
- 비활성화 후 재발급 시 새 코드 발급 (코드 회전 효과)
- 비활성화된 코드로 `GET /v2/shared-glossaries/{code}` 조회 시 `SHARE_CODE_NOT_FOUND`

---

## 5. 단어장 삭제 시 자연 만료 처리

CustomNote 는 **soft delete** (`deletedAt` 컬럼) 확정 (8.5).

구현:
- `GlossaryRepository.findByShareCode` 쿼리에 `deletedAt IS NULL AND shareCode = :code` 조건
- 삭제된 단어장의 shareCode 로 조회 시 `SHARED_GLOSSARY_E001`
- 이미 복제된 단어장은 영향 없음 (4.3 의 독립성)

---

## 6. 에러 처리 정책

기존 디자인이 단일 에러 메시지로 표현. 코드만 사용해도 동일 정책 유지.

백엔드는 errorCode 를 구분해서 응답하되, iOS 가 이 코드들을 통합 매핑:

| ErrorCode | 발생 케이스 | iOS UX |
|---|---|---|
| `SHARED_GLOSSARY_E001` | 코드 형식 잘못 (8자 아님), 존재 안 함, 비활성화됨, 원본 단어장 삭제됨 | "코드가 올바르지 않습니다!" |
| `SHARED_GLOSSARY_E003` | 본인 CUSTOM 20개 한도 도달 | 별도 메시지 (예: "단어장이 가득 찼습니다") |

iOS 가 422/400 vs 404 로 나눠서 보여줄 필요는 없지만, 백엔드는 의미 보존을 위해 코드 구분.

**입력 검증 (controller 레벨)**:
- path `{code}` 가 8자 영숫자 형식이 아니면 400 + `SHARED_GLOSSARY_E001` (DB 조회 회피)
- `@PathVariable @Pattern(regexp = "^[A-Za-z0-9]{8}$")`

---

## 7. ErrorCode / SuccessCode (신규)

### SuccessCode (`SharedGlossarySuccess`)
| Code | Name | Message |
|---|---|---|
| `SHARED_GLOSSARY_S001` | `ISSUE_SHARE_CODE` | 공유 코드 발급 성공 |
| `SHARED_GLOSSARY_S002` | `GET_SHARED_GLOSSARY` | 공유 단어장 조회 성공 |
| `SHARED_GLOSSARY_S003` | `IMPORT_SHARED_GLOSSARY` | 공유 단어장 불러오기 성공 |
| `SHARED_GLOSSARY_S004` | `REVOKE_SHARE_CODE` | 공유 비활성화 성공 |

### ErrorCode (`SharedGlossaryError`)
| Code | Name | Message |
|---|---|---|
| `SHARED_GLOSSARY_E001` | `SHARE_CODE_NOT_FOUND` | 유효하지 않거나 만료된 공유 코드입니다 |
| `SHARED_GLOSSARY_E002` | `SHARE_NOT_ALLOWED` | 본인 소유 CUSTOM 단어장만 공유할 수 있습니다 |
| `SHARED_GLOSSARY_E003` | `IMPORT_LIMIT_EXCEEDED` | 단어장 갯수 한도를 초과했습니다 (20개) |
| `SHARED_GLOSSARY_E004` | `SHARE_CODE_GENERATION_FAILED` | 공유 코드 발급에 실패했습니다 |

---

## 8. 사용자 결정 사항 (확정)

| # | 항목 | 결정 |
|---|---|---|
| 8.1 | ownerNickname 노출 | 노출 |
| 8.2 | 복제 시 본인 갯수 한도 | CUSTOM 본인당 20개 |
| 8.3 | 단어 목록 페이지네이션 | 적용 |
| 8.4 | shareCode + shareUrl | **둘 다 응답** (카톡 클릭 자동 import 경로 + 앱 내 코드 수동 입력 경로 동시 지원) |
| 8.5 | CustomNote 삭제 방식 | soft delete |
| 8.6 | 코드 포맷 | 영문+숫자 8자 (모호 문자 제외) |

기획안에서 결정된 항목 (재확인용):
- 만료: 단어장 soft-delete 시 자연 만료
- 복제: 필수
- 비활성화: 필수
- 단어 데이터 복사: 매핑만 (단어는 사전 공통 자원)

### 8.2 본인 갯수 한도 적용 위치
- `POST /v2/custom-notes/from-share/{code}` 진입 시 본인 CUSTOM 갯수 조회 (deletedAt IS NULL)
- 20개 도달 상태면 `SHARED_GLOSSARY_E003 IMPORT_LIMIT_EXCEEDED`
- 기존 `POST /v2/custom-notes` 생성 endpoint 에도 같은 한도 적용 — 본 작업 전 코드 확인 필요. 없으면 같이 추가 (별도 도메인 ErrorCode 사용)

---

## 9. 충돌 예측 (오답노트 2.5 와)

오답노트 2.5 는 `wrong_terms`, `wrong_terms_notes` **별도 테이블 신설**로 구현 (집 작업 확정). 본 작업과 충돌 가능성 사실상 0.

| 항목 | 충돌 여부 |
|---|---|
| `LearningCommandService` | 무관 |
| `LearningSubmitResponse` | 무관 |
| `glossary_terms` 테이블 | 무관 (2.5 가 별도 테이블 사용) |
| `wrong_terms` / `wrong_terms_notes` | 무관 (본 작업은 CUSTOM glossary 만 다룸) |
| `GlossaryEntity` | shareCode 필드만 사용 |
| `ErrorCode.kt` | 새 enum 추가만, 기존 항목 미수정 |
| V## 마이그레이션 | 추가 없음 (shareCode 컬럼 V27 에 이미 존재) |

결론: 2.5 머지 후 본 작업 진행 시 머지 충돌 사실상 없음.

---

## 10. 작업 순서 (commit 단위)

1. SharedGlossaryError/Success enum 신설
2. application.yml 에 `sharing.glossary-url-prefix` 추가 + `SharingProperties` 바인딩
3. `ShareCodeGenerator` (8자 영숫자, 모호 문자 제외 풀)
4. `GlossaryRepository` 에 `findByShareCodeAndDeletedAtIsNull` 메서드 추가
5. `SharedGlossaryService` — 발급(코드+URL 동시 응답)/조회/복제/비활성화
6. `SharedGlossaryApi`/`Controller` — 4개 endpoint (path `{code}` 에 `@Pattern` 검증)
7. CustomNote 생성 endpoint 에 20개 한도 적용 (없으면)
8. Swagger 명세 (path/query 만, Request body 없음)
9. 권한/한도 검증 통합 테스트 (선택)

각 commit 은 한 줄 한국어 subject (commit 규칙 준수).

---

## 12. CLAUDE.md 컨벤션 체크리스트

- [ ] 도메인명 풀네임 (sharedGlossary)
- [ ] DTO 접미어 Request/Response
- [ ] 메서드명 동작+대상 (`issueShareCode`, `getSharedGlossary`, `importSharedGlossary`, `revokeShareCode`)
- [ ] entity -> DTO 매핑은 DTO companion `from()`
- [ ] ErrorCode 가 SharedGlossaryError 도메인에 속함
- [ ] 응답에 의미 없는 필드 없음
- [ ] 코드 문자열 (SuccessCode/ErrorCode value) 변경 없음
- [ ] 새 endpoint 에 `@ApiSuccessCode` + `@ApiErrorCodes` 부착
- [ ] Swagger Request body 없음 (path/query 만) — `@Parameter(description, example)` 부착
- [ ] Swagger Response 에 `@Schema(example)` 부착 금지

---

## 13. 미결정 / iOS 협의 필요

1. **URL 형식 최종 결정** (8.4)
   - 옵션 1: Universal Link `https://snurdy.app/shared/{code}` (브라우저 fallback 가능)
   - 옵션 2: Custom Scheme `snurdy://shared/{code}` (도메인 설정 불필요)
   - iOS 와 결정 후 application.yml 값 확정

2. **iOS 가 "단어장 불러오기" 입력에서 URL -> 코드 추출 정책**
   - 디자인은 URL 통째로 받는 형태
   - 옵션 A: iOS 가 URL parse 해서 마지막 path segment 만 추출 후 `POST /v2/custom-notes/from-share/{code}` 호출
   - 옵션 B: 백엔드가 `POST /v2/custom-notes/from-share` body `{ "url": "..." }` 도 받아서 코드 추출
   - 추천 A (백엔드 단순화)

3. **import 흐름이 preview 후 import 인지, 즉시 import 인지**
   - 디자인은 "단어장 불러오기" -> 로딩 -> 결과 화면. 미리보기 단계가 명확하지 않음
   - 옵션 A: 즉시 import (4.3 만 호출, 4.2 안 씀)
   - 옵션 B: 4.2 로 preview 후 사용자 확인 -> 4.3 import
   - 추천 A (디자인 흐름과 일치). 4.2 는 그냥 보존 (다른 용도 가능)

4. **복제 시 단어 수 batch 처리**
   - 큰 단어장 (예: 500단어) 매핑 복사 시 응답 시간 -> 로딩 화면이 디자인에 있는 이유 추정
   - `INSERT ... SELECT` 한 번에 처리 권장 (트랜잭션 1회, 라운드트립 없음)

5. **공유 단어장 메타 조회 응답에 본인 import 여부 표시?**
   - 디자인엔 없음. 우선 만들지 않음
