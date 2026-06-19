# DDD-Lite 마이그레이션 명세

본 명세는 SNurdy 백엔드를 Layered Architecture 에서 DDD-Lite (Hexagonal 미포함) 구조로 점진 전환하기 위한 설계 문서.

작성 시점: 회사 PC 에서 설계만 진행. 실제 마이그레이션은 별도 일정.

---

## 1. 채택 결정

### 채택 — B-2 (DDD-Lite)
- 도메인 모델 (`model/`) 과 JPA 엔티티 (`entity/`) 분리
- Repository 는 Spring Data JPA 그대로 (entity 받음)
- service 가 entity ↔ model 매핑 책임 (또는 extension function)
- **Hexagonal 미도입** (Repository Adapter/Port 분리 없음)

### 제외 — Hexagonal
- 효용: DB 교체 자유, 멀티 어댑터, 테스트 격리 극대화
- SNurdy 현실: 단일 MySQL + 단일 REST API → 효용 낮음
- 비용: Repository Adapter 추가, 모든 호출에 매핑 한 단계 더
- 결론: 코드량만 늘고 실익 적어 미도입

---

## 2. 디렉터리 구조

```
domain/{도메인}/
├── model/                   순수 도메인 (JPA/Spring 무관)
│   ├── User.kt                 aggregate root - 비즈니스 행위/invariant 보유
│   ├── Nickname.kt             value object - 길이 제약 등
│   ├── Email.kt                value object - 형식 검증 등
│   └── policy/                 도메인 정책 (수치/규칙)
│       └── UserPolicy.kt
├── entity/                  JPA 영속성 (현재와 동일)
│   └── UserEntity.kt
├── repository/              Spring Data JPA (entity 다룸, 현재와 동일)
│   └── UserRepository.kt
├── service/                 application service (트랜잭션, 조율)
│   └── UserService.kt
├── controller/
│   ├── UserApi.kt
│   └── UserController.kt
└── dto/                     외부 표현 (Request/Response)
    └── UserDto.kt
```

### 핵심 차이 (현재 대비)
- **model/ 신설** — 비즈니스 규칙을 가진 순수 객체
- **entity/ 는 ORM 매핑만** — DB row 표현 책임
- **service 가 매핑 layer** — entity ↔ model 양방향
- **dto 매핑은 model 기준** — `User.toResponse()`, `UserCreateRequest.toModel()` (기존 entity 기반 매핑에서 변경)

---

## 3. 매핑 방향 정책

### 3.1 데이터 흐름

```
HTTP Request (DTO)
    ↓ UserCreateRequest.toModel()
domain model (User)
    ↓ User.toEntity()
JPA entity (UserEntity)
    ↓ repository.save()
DB
    ↑ repository.findById()
JPA entity (UserEntity)
    ↑ UserEntity.toModel()
domain model (User)
    ↑ User.toResponse()
HTTP Response (DTO)
```

### 3.2 매핑 위치
- **dto ↔ model**: dto 파일의 extension function (`fun User.toResponse()`, `fun UserCreateRequest.toModel()`)
- **model ↔ entity**: entity 파일의 extension function (`fun UserEntity.toModel()`, `fun User.toEntity()`)
- service 는 매핑 호출만, 직접 매핑 로직 없음

### 3.3 의존 방향
```
dto    ──▶ model ◀── entity
controller ──▶ service ──▶ repository
                ↓
              model
```

- model 은 어디도 의존 안 함 (가장 안쪽)
- entity 는 model 의 영속성 매핑이라 model 을 import
- dto 는 model 을 import (응답 만들 때)
- service 는 model + entity + repository 모두 import (조율 책임)
- controller 는 service + dto 만 import

---

## 4. Value Object 적용 범위 — 선별적

### 적용 대상
비즈니스 검증/제약이 있는 속성만 VO 로:

| 후보 | 도입 여부 | 이유 |
|---|---|---|
| Nickname | O | 최대 20자, 공백 trim |
| Email | O | 형식 검증 (가능 시) |
| ShareCode | O | 8자 영숫자 패턴 |
| GlossaryTitle | O | 최대 14자 |
| Spelling | X | 단순 String, 검증 없음 |
| Meaning | X | 단순 String, 검증 없음 |
| UserId | △ | UUID wrapper. 타입 안전성 가치 vs 코드량 비교 |
| TermId | △ | Long wrapper. 위와 동일 |

기본: **비즈니스 invariant 가 있을 때만 VO**. primitive (String/Long/Int) 그대로도 OK.

### VO 작성 패턴
```kotlin
@JvmInline
value class Nickname(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { "닉네임은 최대 ${MAX_LENGTH}자" }
        require(value.isNotBlank()) { "닉네임은 공백일 수 없습니다" }
    }
    companion object {
        const val MAX_LENGTH = 20
    }
}
```

- `@JvmInline value class` — 런타임 wrapping 비용 0
- `init` 블록에서 invariant 강제
- 정적 상수는 companion 안 (도메인 정책)

검증 실패 시 IllegalArgumentException 발생. service layer 에서 catch → ApiException 변환 또는 dto 단계에서 사전 검증.

---

## 5. Policy 패키지

도메인 수치/규칙을 명시적으로 분리.

```kotlin
// domain/user/customNote/model/policy/CustomNotePolicy.kt
object CustomNotePolicy {
    const val MAX_NOTES_PER_USER = 20
    const val MAX_TITLE_LENGTH = 14
    const val MAX_TERMS_PER_BATCH = 100
    const val MAX_TERMS_PER_NOTE = 250

    fun ensureNoteCountAvailable(currentCount: Int) {
        if (currentCount >= MAX_NOTES_PER_USER) {
            throw ApiException(CustomNoteError.LIMIT_EXCEEDED)
        }
    }
}
```

### Policy vs VO vs Model 책임 구분
- **VO**: 단일 속성의 invariant (Nickname 자체 규칙)
- **Model method**: aggregate 내부 상태 전이 (User.changeNickname(...))
- **Policy**: aggregate 외부 컨텍스트가 필요한 규칙 (예: "이 사용자가 단어장 20개 도달했는가" — 다른 데이터 조회 필요)

---

## 6. Repository 정책

### 6.1 Repository 는 그대로
- Spring Data JPA `JpaRepository<UserEntity, UUID>` 그대로 유지
- entity 받고 entity 반환 (model 모름)
- Hexagonal 미도입이라 Adapter 없음

### 6.2 Service 가 매핑 책임
```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) {
    @Transactional
    fun create(request: UserCreateRequest): UserResponse {
        val user: User = request.toModel()                  // dto → model
        val saved: UserEntity = userRepository.save(user.toEntity())  // model → entity, persist
        return saved.toModel().toResponse()                  // entity → model → dto
    }
}
```

매번 매핑 두 단계가 부담일 수 있음. 단 service 의 의도 (도메인 행위 + 영속성 + 응답) 가 명확.

### 6.3 조회 시 부분 model
- entity 의 모든 필드를 model 로 매핑하지 않아도 됨
- 응답에 필요한 정보만 model 에 노출
- 다만 일관성 위해 기본은 풀 매핑, 성능 이슈 시 Projection 활용 (현재 패턴 유지)

---

## 7. Application Service vs Domain Service

### Application Service (= 현재 service)
- 트랜잭션 경계
- 외부 의존 조율 (Repository, 다른 service, 외부 API)
- DTO ↔ model 매핑
- 위치: `service/`

### Domain Service
- 여러 aggregate 협력 로직 (한 aggregate 안에 안 들어가는 비즈니스 규칙)
- 예: "오답 발생 시 WrongAnswerNote 추가 + TermProgress 갱신" → 두 aggregate 가 함께 변하는 흐름
- 위치: `model/service/` 또는 `model/`
- 외부 의존 (Repository 등) 안 받음 — 순수 도메인 로직만

### 현실
SNurdy 도메인 대부분은 application service 로 충분. Domain Service 는 진짜 협력 필요할 때만 (현재 거의 없음).

---

## 8. CLAUDE.md 영향

마이그레이션 완료 시 CLAUDE.md 의 다음 항목 개정 필요:

### 2.1 도메인 패키지 원칙
```diff
- domain/{도메인}/ 안에 해당 도메인의 책임만 둔다
+ domain/{도메인}/ 구조:
+   - model/ (순수 도메인 + VO + policy)
+   - entity/ (JPA 매핑)
+   - repository/ (Spring Data JPA)
+   - service/ (application service)
+   - controller/, dto/
```

### 3.1 매핑 책임
```diff
- entity → DTO 매핑은 service 가 아닌 Kotlin extension function 으로
+ 매핑 단계:
+   - dto ↔ model: dto 파일의 extension
+   - model ↔ entity: entity 파일의 extension
+   - service 는 매핑 호출만 (직접 매핑 로직 없음)
```

### 신규 항목 추가
- VO 작성 패턴 (`@JvmInline value class`)
- Policy 패키지 위치/책임
- model 의존 방향 (어디도 의존 안 함)

---

## 9. 마이그레이션 순서 — 점진

### Pilot 도메인 선정
**`wrongAnswerNote`** 추천:
- 작고 깔끔 (entity 1개)
- 최근 작업해서 컨텍스트 살아있음
- 도메인 행위 (recordIncorrect, softDelete) 가 이미 entity 안에 있어 model 이전 자연스러움

Pilot 진행 후:
1. 패턴 확립 (매핑 위치, 네이밍, VO 선정 기준 등)
2. CLAUDE.md 개정
3. 도메인별 마이그레이션 (한 도메인 당 1개 PR/commit)

### 도메인별 우선순위 (작은 것부터)
1. wrongAnswerNote (pilot)
2. version, calendar, bookmark, termMemo (단순)
3. user/account, user/profile, user/streak (분리됐어도 작음)
4. customNote, sharedGlossary (Phase 2.7)
5. learning (TermProgress, GlossaryProgress, DailyProgress 묶음)
6. dictionary (glossary/term/morpheme — 가장 큼)
7. admin/audit, admin/auth, admin/dictionary, admin/workflow

### 마이그레이션 시 각 도메인당 작업
1. `model/` 폴더 생성, aggregate root + VO + policy 작성
2. entity 파일에 `toModel()`, `toEntity()` extension 추가
3. dto 파일의 매핑 대상을 entity → model 로 변경
4. service 의 모든 entity 조작을 model 거치도록 수정
5. 테스트 수정 (entity 직접 → model 활용)
6. 컴파일/테스트 확인 후 commit

---

## 10. 잠재 위험 / 트레이드오프

| 위험 | 대응 |
|---|---|
| 매번 entity ↔ model 매핑 비용 (성능 미미, 코드량 증가) | 핫패스 (HOT path) 는 Projection 활용 유지 |
| model 과 entity 필드 drift (한쪽만 변경) | 변경 시 양쪽 동기 강제 — PR 체크리스트 추가 |
| 작은 도메인엔 과한 추상화 | 단순 데이터 전달만 하는 도메인 (version 등) 은 굳이 model 만들지 않아도 됨 (선택적) |
| 학습 곡선 + Kotlin 익숙도 | pilot 후 패턴 확립, 일관 적용 |
| 두 패턴 공존 기간 | 마이그레이션 중간엔 도메인마다 구조 다름. PR 단위로 빠르게 정리 |

---

## 11. 검증 기준 (Definition of Done — 도메인별)

- [ ] `model/` 폴더에 aggregate root + 필요한 VO + policy 존재
- [ ] entity 는 JPA 매핑만 책임 (비즈니스 로직 없음)
- [ ] entity 파일에 `toModel()`, `toEntity()` extension 존재
- [ ] dto 파일의 모든 매핑이 model 기준 (entity 직접 매핑 없음)
- [ ] service 는 매핑 호출만, 직접 매핑 로직 없음
- [ ] policy 의 모든 const 가 entity 의 magic number 를 대체
- [ ] 도메인 행위가 model 메서드로 캡슐화 (anemic 회피)
- [ ] 테스트가 model 단위로 작성 가능 (JPA 무관)
- [ ] 컴파일 + 모든 테스트 통과
- [ ] 한 줄 한국어 commit

---

## 12. 미결정 / 추후 확정

1. **UserId, TermId 등 ID VO 도입 여부**: primitive (UUID/Long) 유지 vs 타입 안전성 위해 VO 화
2. **검증 실패 시 예외 매핑**: VO 의 IllegalArgumentException 을 ApiException 으로 변환할 글로벌 핸들러 추가 여부
3. **JPA Specification / Querydsl 도입 여부**: 복잡 조회 시 model 기준 쿼리 추상화 필요할지
4. **테스트 전략 변경**: model 단위 테스트 + entity 통합 테스트 분리 정도

---

## 13. 진행 트리거

본 명세 검토 후 사용자가 "pilot 시작" 신호 주면 wrongAnswerNote 부터 진행. 그 전까지는 dto ↔ entity 매핑 컨벤션 (Kotlin extension) 작업 계속.
