# terms 데이터 정규화 명세

## 1. 개요

`terms` 테이블(사전 카탈로그)에 표기 규칙이 통일되지 않은 데이터가 누적되어 있다. 대소문자, 괄호, 한글 띄어쓰기, 약어 표기가 제각각이고, 같은 개념이 표기만 다르게 여러 번 입력된 중복 행이 존재한다. 이 문서는 문제를 카테고리로 정리하고 정규화 규칙과 안전한 적용 절차를 정의한다.

- 작성 기준 데이터: prod export 스냅샷 (활성 term 948행, soft-delete 0)
- 현재 단계: 규칙 확정 및 문서화. 실제 데이터 반영은 규칙 확정 후 진행

## 2. 전제 조건

### 2.1 데이터 모델
`terms` 컬럼: `spelling`(VARCHAR 100), `meaning`(VARCHAR 255), `abbreviation`(VARCHAR 50, nullable), `explanation`(TEXT).
- `spelling`에 unique 제약 없음. 같은 spelling 다중 행이 DB상 허용됨
- collation `utf8mb4_unicode_ci` (대소문자 및 악센트 무구분)

### 2.2 동작 특성
- 검색: fulltext `MATCH ... AGAINST`. collation이 대소문자를 무시하므로 검색은 이미 대소문자 무관
- 퀴즈 정답 판정: 클라이언트가 수행하고 서버는 결과(`correct`)만 신뢰. 대소문자 무시 채점은 클라이언트 책임
- 형태소 검색: `morpheme_variants`에 등록된 변형으로 조회

### 2.3 참조 관계 (중요)
`terms`를 참조하는 FK는 대부분 `ON DELETE CASCADE`다. term 행을 삭제하면 아래 사용자 데이터가 함께 삭제된다.
- `term_progresses`(학습 진도), `bookmarks`(북마크), `wrong_answer_terms`(오답), `custom_note_terms`(커스텀노트), `glossary_terms`(단어장 매핑), `term_morphemes`(형태소 매핑)

---

## 3. 정규화 규칙

### 3.1 대소문자 (카테고리 A)

앱 내 의학용어 노출 기준.

| 유형 | 규칙 | 예 |
|---|---|---|
| 일반 질환/해부 용어 | 전체 소문자 | `appendicitis`, `tachycardia` |
| 인명/지명 포함 | 고유명사 첫 글자만 대문자 | `Alzheimer's disease`, `Foley catheter` |
| 약어 | 전체 대문자 | `MRI`, `COPD` |
| 약어 풀텀 | 고유명사 아니면 전체 소문자 | `magnetic resonance imaging` |

실측: `spelling` 전체소문자 115행, 혼합(Title Case 등 소문자화 후보) 833행.

정책 이슈 두 가지.
1. 고유명사는 프로그램으로 자동 판별할 수 없다. 833행을 일괄 소문자화하면 `Alzheimer's`, `Foley` 등이 깨진다. 반드시 고유명사 예외 목록을 사람이 검수해 확정해야 한다.
2. 약어 전체 대문자 규칙에는 예외가 있다. 소문자가 의미를 담는 표준 약어는 대문자화하면 틀린 표기가 된다. 아래 예외 목록 참조.

약어 대문자 규칙 예외 (소문자 유지):
`SpO2`, `SaO2`, `PaO2`, `FiO2`, `aPTT`, `Hb`, `Hct`, `Dx`, `Bx`, `Fx`, `AComA` 등. 부록 A의 개별 처리안 참조.

### 3.2 괄호 분리 (카테고리 B, C)

괄호는 무조건 분리한다.

- B. `spelling`(영어) 안 괄호: 서로 다른 두 term으로 분리
  - `Compound Fracture (Open Fracture)` -> `Compound Fracture`, `Open Fracture`
  - 실측 7행 (id 309, 362, 484, 517, 717, 720, 789)
- C. `meaning`(의미) 안 괄호(동의어): 같은 spelling에 의미별 별도 행으로 분리 (쉼표 병기 아님)
  - `Thoracentesis 흉막천자 (흉강천자)` -> `Thoracentesis 흉막천자`, `Thoracentesis 흉강천자`
  - 괄호 앞 공백 유무는 무관하게 동일 처리 (`늑막삼출(흉수)`, `흉막천자 (흉강천자)` 모두)
  - 실측 31행

분리로 새로 생기는 행은 새 termId를 받는다. 원본 행은 id를 유지해 기존 사용자 데이터를 보존한다. 신규 행은 `glossary_terms` 매핑과 필요 시 `morpheme_variants` 변형을 함께 넣어야 노출 및 형태소 검색이 된다.

### 3.3 한글 띄어쓰기 (카테고리 D)

`meaning`의 한글 띄어쓰기가 불규칙하다.
- `급성 신부전`(띄움) vs `급성골수성백혈병`(붙임) vs `급성 신장손상`(띄움)

통일 규칙 결정 대기. 후보는 부록 B의 미결 결정 참조. 규칙이 확정되어야 dedup 시 near-duplicate를 병합할 수 있다 (3.4 참조).

### 3.4 약어 표기 (카테고리 E)

`abbreviation` 필드의 표기 정리. 실측: 소문자 포함 24행, 특수문자/공백 포함 19행, 첨자 4행.

공통 처리 원칙.
- 첨자 `₂` 등 비ASCII는 ASCII 숫자로 (`SpO₂` -> `SpO2`). 키보드 입력 및 fulltext 호환 (결정 대기)
- 마침표 제거 (`Imp.` -> `Imp`)
- 복수형 s 제거해 단수화 (`TIAs` -> `TIA`, `NSAIDs` -> `NSAID`)
- 한 필드에 약어가 여러 개(슬래시/콤마/괄호)면 대표 하나로 확정하고 나머지는 별도 처리
- 약어 + 일반어 복합(`GI tract`, `CV system`)은 앞 약어만 대문자, 뒷 일반어는 소문자 유지

개별 처리안은 부록 A 참조.

---

## 4. 중복 정리 (카테고리 F)

### 4.1 실측 현황
- 완전중복(`spelling`+`meaning`+`abbreviation` 동일) 65 그룹
- 같은 spelling 다중행 110 그룹
- 같은 개념이 대소문자와 한글 띄어쓰기만 다르게 2~4번 입력된 사례 다수
  - 예: `Deep Vein Thrombosis 심부정맥혈전증`(id 26, 677) vs `Deep vein thrombosis 심부정맥 혈전증`(id 234, 315). 총 4행이 casing과 띄어쓰기 변형으로 갈라져 완전중복 매칭에 안 걸림

### 4.2 삭제 대상에 물린 사용자 데이터 (실측)
같은 spelling 중복 중 삭제 대상(canonical 제외) 130행을 참조하는 데이터.

| 테이블 | 삭제대상 참조 행 | 영향 유저/노트 |
|---|---|---|
| term_progresses | 1,389 | 150명 |
| custom_note_terms | 270 | 84개 노트 |
| wrong_answer_terms | 138 | 62명 |
| bookmarks | 77 | 19명 |
| glossary_terms | 130 | 카탈로그 |
| term_morphemes | 149 | 카탈로그 |

완전중복만 대상으로 좁혀도(74행) 진도 815행 등이 물려 있다.

### 4.3 안전 절차 (삭제 아님, repoint + 병합)
FK가 `ON DELETE CASCADE`이므로 `DELETE FROM terms`를 직접 하면 위 사용자 데이터가 cascade 삭제된다. 반드시 다음 순서를 따른다.

1. 모든 참조 테이블의 `term_id`를 nonCanonical -> canonical로 갱신(repoint)
2. PK 충돌 병합. 한 유저가 canonical과 중복 term 양쪽에 데이터가 있으면 한 행으로 합친다 (예: 진도는 MASTERED 또는 높은 값 우선). 대상 PK는 `term_progresses(user_id, glossary_id, term_id)`, `bookmarks(user_id, glossary_id, term_id)`, `wrong_answer_terms(user_id, term_id)`, `custom_note_terms(note_id, glossary_id, term_id)`, `glossary_terms(glossary_id, term_id)`
3. 참조가 모두 옮겨진 뒤에만 nonCanonical term 행 삭제

### 4.4 과합병 금지
같은 spelling 다중행이 항상 중복은 아니다. 카테고리 C의 괄호 분리 결과는 의도적으로 같은 spelling에 다른 의미를 갖는다. 따라서 dedup 대상은 아래로 한정한다.
- 완전중복(spelling+meaning+abbreviation 동일)
- 대소문자/한글 띄어쓰기만 다른 near-duplicate. 단 A와 D 규칙을 먼저 적용하면 완전중복으로 수렴하므로, 그 뒤에 병합

진짜로 의미가 다른 동의어는 유지한다.

---

## 5. 작업 순서

순서가 강제된다. 정규화가 먼저다.

1. 규칙 확정 (부록 B 미결 결정 해소, 고유명사 예외 목록 확정)
2. 카테고리 A 대소문자 정규화 (검수된 예외 반영)
3. 카테고리 D 한글 띄어쓰기 정규화
4. 카테고리 E 약어 표기 정규화
5. 카테고리 B, C 괄호 분리 (신규 행 + glossary_terms/morpheme_variants 매핑)
6. 카테고리 F 중복 정리 (repoint + 병합 후 삭제). 2~4를 거치면 near-dup이 완전중복으로 수렴해 병합 가능

각 단계는 idempotent 마이그레이션 또는 검수된 데이터 반영으로 처리하고, 한 번 적용된 Flyway 파일은 수정하지 않는다.

---

## 부록 A. 약어 개별 처리안

`?`는 도메인 의미 확인이 필요한 항목(결정 대기).

| id | 현재 abbreviation | 처리안 | 비고 |
|---|---|---|---|
| 726 | `SpO₂/SaO₂` | `SpO2`, `SaO2` 두 term 분리 | 첨자 ASCII, 서로 다른 지표 |
| 353, 727 | `PaO₂` | `PaO2` | 두 행 존재, 의미 확인 후 dedup |
| 326 | `FiO₂` | `FiO2` | 소문자 i 유지 |
| 210 | `aPTT` | `aPTT` | 소문자 a 유지 |
| 289 | `AComA` | `AComA` | 혼합 대소문자 유지 |
| 686 | `Hb` | `Hb` | 대문자화 금지 |
| 684 | `Hct(HCT)` | `Hct` | 변형 HCT 제거 |
| 235 | `Dx` | `Dx` | Xx 계열, 대문자화 금지 |
| 659 | `Bx` | `Bx` | Xx 계열 |
| 386 | `Fx` | `Fx` | Xx 계열 |
| 105 | `TIAs` | `TIA` | 복수형 s 제거 |
| 349 | `NSAIDs` | `NSAID` | 복수형 s 제거 |
| 334 | `Imp.` | `Imp` | 마침표 제거 |
| 254 | `I & D` | `I&D` | 앰퍼샌드 공백 통일 |
| 846 | `D&C` | `D&C` | 앰퍼샌드 무공백 기준 |
| 368 | `R/O (r/o)` | `R/O` | 소문자 변형 제거 |
| 385 | `V-P shunt` | `VP shunt` 또는 `V-P shunt` | 하이픈 정책 결정 대기 |
| 544 | `GI tract` | `GI tract` | 약어+일반어, 유지 |
| 669 | `CV system` | `CV system` | 약어+일반어, 유지 |
| 293 | `A-Line` | `A-line` | 하이픈 뒤 소문자 |
| 692 | `Lab` | `Lab` 또는 `LAB` | 단어 축약, 결정 대기 |
| 83, 365 | `TB,Tbc` / `Tbc (TB)` | `TB` | 둘 다 Pulmonary Tuberculosis, 중복 통합 |
| 361, 781 | `PVC / VPC` / `PVC (VPC)` | `PVC` | 중복 통합, VPC 동의어 여부 확인 |
| 320, 678 | `EKG (ECG)` / `EKG(ECG)` | `ECG` 또는 `EKG` | 중복 통합, canonical 택1 |
| 836 | `C/S` | ? | 제왕절개 vs 배양감수성, 의미 확인 필요 |
| 666 | `CM (Cardio)` | ? | 의미 확인 필요 (Cardiology 표기?) |
| 316 | `DT (Delirium Tremens) / Delirium` | `DT` + Delirium 별도 검토 | 필드 정리 및 분리 여부 확인 |

## 부록 B. 미결 결정

1. 첨자 `2` ASCII 표기 확정 (`SpO2` vs `SpO₂`)
2. 한글 띄어쓰기 통일 규칙 (전부 붙임 / 상태 수식어만 띄움 / 국립국어원 기준 중 택1)
3. 약어 대문자 규칙 예외 목록 확정
4. `C/S`, `CM`, `DT` 등 도메인 의미 확인
5. 고유명사 예외 목록 검수 (833행 소문자화 대상 중 대문자 유지할 행)

## 부록 C. 데이터 반영 시 유의

- 행 분리/중복 정리는 새 termId 생성 또는 기존 참조 repoint를 수반한다. canonical id를 유지해 사용자 데이터 보존
- 신규 term은 `glossary_terms` 매핑과 `morpheme_variants` 변형을 함께 넣어야 단어장 노출 및 형태소 검색이 동작한다. admin 도메인 삭제로 자동 생성 로직이 없으므로 수동 반영 필요
- 검색과 클라이언트 채점은 대소문자를 무시하므로, 대소문자 정규화는 표기 일관성 목적이며 검색/채점 동작에는 영향을 주지 않는다
