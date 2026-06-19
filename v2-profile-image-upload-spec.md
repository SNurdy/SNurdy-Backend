## 프로필 이미지 업로드 (S3, 환경별 분리)

마이페이지 프로필 수정 화면의 사진 변경 기능 구현. S3 무료 티어 활용. 백엔드 multipart 경유 방식. dev / prod 환경별 버킷 분리. 단일 버킷 + 도메인 폴더 prefix 구조로 향후 다른 이미지 도메인 확장 대비.

기존 `users.profile_image_url` 컬럼은 그대로 사용. 신규 endpoint 가 S3 업로드 후 컬럼 갱신까지 한 번에 처리.

---

## 1. 배경

- 현재 `PATCH /v2/users/me` 는 `profileImageUrl` 을 받아서 그대로 저장하는 구조 (외부 URL 직접 입력만 가능)
- iOS 디자인 (`마이페이지_프로필_프로필 수정.png` 1-7) 에 카메라 아이콘 — 사진 라이브러리 선택 후 업로드 흐름 필요
- presigned URL 방식은 호출 3번이라 복잡. 프로필 사진은 빈도 낮고 작은 파일이라 백엔드 multipart 가 단순
- 단일 버킷 + 도메인 폴더 prefix 가 업계 표준 (도메인별 버킷 분리는 보안 격리 / lifecycle 차이 필요 시에만)
- Spring Cloud AWS (`io.awspring.cloud`) 가 Spring Boot 환경 표준 라이브러리

---

## 2. 사전 작업 (코드 변경 전 필수)

본 작업은 코드 외 인프라 / 외부 계정 설정이 선행되어야 함.

### 2.1 AWS 콘솔 작업
- [ ] AWS 계정 준비 (이미 있으면 skip)
- [ ] S3 버킷 2개 생성
  - `snurdy-backend-dev` (개발/local 공용)
  - `snurdy-backend-prod` (운영 전용)
  - 리전: `ap-northeast-2` (서울)
  - 객체 소유권: ACL 비활성화
  - 퍼블릭 액세스 차단: ACL 관련 2개만 활성, bucket policy 관련 2개 비활성
  - 버킷 정책: prefix 단위 public-read 부여 (아래 5.1 참조)
  - 기본 암호화: SSE-S3 + 버킷 키 활성
  - 버전 관리: 비활성
  - 태그: Project=snurdy, Environment=dev|prod
  - CORS:
    ```json
    [{
      "AllowedOrigins": ["https://snurdy.org", "https://api-test.snurdy.org", "http://localhost:*"],
      "AllowedMethods": ["GET", "PUT"],
      "AllowedHeaders": ["*"]
    }]
    ```
- [ ] IAM 정책 + 사용자 2개 생성
  - `snurdy-s3-dev-user`: `snurdy-backend-dev` 버킷 `s3:PutObject`, `s3:DeleteObject`, `s3:GetObject` 권한
  - `snurdy-s3-prod-user`: `snurdy-backend-prod` 버킷 동일 권한
  - 운영 EC2 는 IAM instance role 권장 (access key 없이 권한 부여)
- [ ] Access Key ID / Secret 발급 (local / dev 용)

### 2.2 gradle 의존성
build.gradle.kts 에 추가:
```kotlin
// Spring Cloud AWS - S3
implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.3.1"))
implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
```

BOM 으로 sub-module 버전 통일. Spring Boot 3.4.x ↔ Spring Cloud AWS 3.3.x 호환 (3.4.x 는 Spring Boot 3.5.x 필요).

### 2.3 환경 변수 / application.yml

application.yml 공통 섹션에 추가:
```yaml
spring:
  cloud:
    aws:
      region:
        static: ${AWS_REGION:ap-northeast-2}
      credentials:
        access-key: ${AWS_ACCESS_KEY_ID:}
        secret-key: ${AWS_SECRET_ACCESS_KEY:}
      s3:
        enabled: true

storage:
  s3:
    bucket: ${AWS_S3_BUCKET}
    object-prefix: ${AWS_S3_OBJECT_PREFIX:}
```

- `spring.cloud.aws.*` — Spring Cloud AWS autoconfig (S3Client, S3Template 빈 자동 생성)
- `credentials.*` 비워두면 DefaultCredentialsProvider chain 동작 (EC2 instance role 등)
- `storage.s3.*` — 우리 도메인 설정 (`S3Properties` 로 binding)

application-local.yml 에 추가:
```yaml
storage:
  s3:
    object-prefix: "local/"
```

application-dev.yml / application-prod.yml — 별도 설정 불필요 (object-prefix 기본 빈 문자열).

환경별 환경 변수:

| 변수 | local | dev | prod |
|---|---|---|---|
| `AWS_REGION` | ap-northeast-2 | ap-northeast-2 | ap-northeast-2 |
| `AWS_S3_BUCKET` | snurdy-backend-dev | snurdy-backend-dev | snurdy-backend-prod |
| `AWS_ACCESS_KEY_ID` | dev key | dev key | (instance role) |
| `AWS_SECRET_ACCESS_KEY` | dev secret | dev secret | (instance role) |
| `AWS_S3_OBJECT_PREFIX` | (application-local.yml 에 `local/` 박힘) | (없음) | (없음) |

prod 는 EC2 instance role 부여 시 access key 환경 변수 비워두면 SDK 가 자동으로 instance metadata 에서 credential 조회.

local 은 dev 버킷 공유 + 객체 경로에 `local/` prefix 로 데이터 격리.

### 2.4 docker-compose-dev.yml 환경 변수 추가
`.env` 에 AWS 키 추가 → `docker-compose-dev.yml` 의 `app-dev.environment` 에 다음 추가:
```yaml
- AWS_REGION=ap-northeast-2
- AWS_S3_BUCKET=snurdy-backend-dev
- AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
- AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
```

### 2.5 prod 배포 환경
prod 는 별도 인프라 (현재 infra/ 에 prod docker-compose 없음). 배포 정의에 위와 동일 환경 변수 추가 필요 (단 `AWS_S3_BUCKET=snurdy-backend-prod`, credential 은 instance role 사용 시 생략).

---

## 3. Endpoint

### POST /v2/users/me/profile-image

multipart/form-data. 단일 file part.

**요청**
- Content-Type: `multipart/form-data`
- form field: `image` (단일 file)

**응답**
```json
{ "profileImageUrl": "https://snurdy-backend-prod.s3.ap-northeast-2.amazonaws.com/profile-images/{userId}/{uuid}.jpg" }
```

**SuccessCode**
- `PROFILE_IMAGE_S001 ProfileImageSuccess.UPLOAD` 신규

---

## 4. 파일 검증 정책

| 항목 | 정책 |
|---|---|
| 허용 MIME | image/jpeg, image/png |
| 허용 확장자 | jpg, jpeg, png |
| 최대 크기 | 5MB |
| 최소 크기 | 1KB (빈 파일 거부) |

위반 시:
- 파일 누락 → `PROFILE_IMAGE_E001 REQUIRED` (400)
- MIME / 확장자 불일치 → `PROFILE_IMAGE_E002 INVALID_FORMAT` (400)
- 크기 초과 → `PROFILE_IMAGE_E003 TOO_LARGE` (413)

이미지 리사이즈 / 압축은 클라이언트 책임. 백엔드는 원본 그대로 저장.

application.yml 의 `server.tomcat.max-http-form-post-size: 10MB` 가 이미 충분.

### 4.1 검증 위치
- service 진입 전 `MultipartFile` 검증 (contentType + 확장자 + size)
- 매직 바이트 (Apache Tika) 까지는 안 함 (확장 검토)

---

## 5. S3 객체 권한 정책

### 5.1 Bucket policy (public-read prefix 단위 부여)
ACL 비활성화 환경이라 bucket policy 로만 public 제어. `profile-images/` prefix 만 public-read 허용.

dev 버킷:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicReadProfileImages",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": [
      "arn:aws:s3:::snurdy-backend-dev/profile-images/*",
      "arn:aws:s3:::snurdy-backend-dev/local/profile-images/*"
    ]
  }]
}
```

prod 버킷 (`local/` prefix 없음):
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicReadProfileImages",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::snurdy-backend-prod/profile-images/*"
  }]
}
```

향후 다른 public 도메인 추가 시 Resource 배열에 prefix 추가.

---

## 6. S3 객체 경로 (단일 버킷 + 도메인 폴더)

```
{env-prefix/}{domain-folder}/{...}
```

| 도메인 | 경로 |
|---|---|
| 프로필 이미지 (현재) | `{env-prefix/}profile-images/{userId}/{uuid}.{ext}` |
| 단어 이미지 (향후 가능) | `{env-prefix/}term-images/{termId}/{uuid}.{ext}` |
| 커스텀노트 커버 (향후 가능) | `{env-prefix/}custom-note-cover/{noteId}/{uuid}.{ext}` |

| 환경 | env-prefix | 전체 경로 예시 |
|---|---|---|
| local | `local/` | `local/profile-images/{userId}/{uuid}.jpg` |
| dev | (없음) | `profile-images/{userId}/{uuid}.jpg` |
| prod | (없음, 별도 버킷) | `profile-images/{userId}/{uuid}.jpg` |

env-prefix 는 `storage.s3.object-prefix` 설정값. `S3Properties` 가 읽어서 경로 빌드 시 자동 prepend.

---

## 7. 구 이미지 처리

새 업로드 성공 시:
1. S3 upload (신규 객체)
2. `users.profile_image_url` UPDATE
3. 기존 S3 객체 delete (best effort)

3번 실패해도 트랜잭션 롤백 안 함. orphan 객체는 무료 티어 5GB 안에서 무시. 추후 정기 cleanup batch 도입 검토.

기존 URL 이 우리 버킷에 속하는지 host 로 판단 후 삭제. 외부 URL 이면 삭제 시도 안 함.

---

## 8. 영향 받는 기존 코드

### UserEntity
- 변경 없음 (`profileImageUrl` 컬럼 그대로)

### UserProfileService
- 신규 메서드 `uploadProfileImage(userId, file): ProfileImageUploadResponse`
- 기존 `updateProfile` 는 닉네임 + URL 직접 입력 둘 다 유지 (breaking 회피)

### UserProfileApi
- 신규 endpoint `POST /v2/users/me/profile-image` 추가
- 기존 PATCH 의 `profileImageUrl` 필드 유지

### 신규 패키지 / 클래스
- `global/storage/S3Properties` — `storage.s3.*` 환경 변수 binding (`@ConfigurationProperties`)
- `global/storage/S3StorageService` — `S3Template` wrapper. 도메인별 upload, delete API 제공

### gradle
- Spring Cloud AWS BOM + s3 starter 추가

---

## 9. SuccessCode / ErrorCode

도메인 prefix 분리 — UserSuccess / UserError 가 아닌 `ProfileImageSuccess` / `ProfileImageError` 별도 enum 으로 분리. 향후 다른 이미지 도메인 (term-images 등) 도 자체 prefix 사용 가능.

### 신규
| 코드 | enum | 메시지 |
|---|---|---|
| `PROFILE_IMAGE_S001` | ProfileImageSuccess.UPLOAD | 프로필 이미지를 변경했습니다 |
| `PROFILE_IMAGE_E001` | ProfileImageError.REQUIRED | 이미지 파일이 필요합니다 |
| `PROFILE_IMAGE_E002` | ProfileImageError.INVALID_FORMAT | 지원하지 않는 이미지 형식입니다 (jpg, png 만 허용) |
| `PROFILE_IMAGE_E003` | ProfileImageError.TOO_LARGE | 이미지 크기는 5MB 이하여야 합니다 |

### 인프라 장애
- S3 upload / delete 실패 → `CommonError.SERVICE_UNAVAILABLE` (글로벌 처리)

---

## 10. 보안 / 권한

- 인증 필요 (`@CurrentUserId`)
- 본인 자원만 변경 가능 (path `/me` 강제, userId 는 토큰에서 추출)
- 파일 검증 백엔드 (클라 검증 신뢰 안 함)
- S3 객체 public-read 이지만 URL 의 UUID 가 추측 불가 (보안적으로 OK 수준)
- access key 는 환경 변수만, 코드 / 명세에 하드코딩 금지

---

## 11. 구현 순서

1. spec 작성 (본 문서) ✓
2. **AWS 콘솔 작업** (사용자) — 버킷 2개 + IAM 2개 + access key 발급
3. application.yml 에 `spring.cloud.aws` + `storage.s3` 섹션 추가
4. application-local.yml 에 `storage.s3.object-prefix: local/` 추가
5. gradle 의존성 추가 + sync
6. `S3Properties` + `S3StorageService` 신설
7. `UserProfileService.uploadProfileImage` 신규 메서드
8. `ProfileImageUploadResponse` DTO
9. `UserProfileApi` 에 `POST /profile-image` endpoint 추가
10. multipart 파일 검증 로직
11. SuccessCode / ErrorCode enum 추가
12. docker-compose-dev.yml 환경 변수 추가
13. prod 배포 정의에 환경 변수 추가 (사용자)
14. 테스트 (로컬에서 dev 버킷으로 실제 업로드 확인)
15. v2-roadmap.md 결정 누적 갱신
16. iOS 전달 메시지 작성

---

## 12. 결정 누적

| 항목 | 결정 |
|---|---|
| 업로드 방식 | 백엔드 multipart 경유 (presigned URL 미사용). 호출 1번 |
| 라이브러리 | Spring Cloud AWS (`io.awspring.cloud:spring-cloud-aws-starter-s3`). SDK v2 직접 사용 안 함 |
| 환경 분리 | dev / prod 별도 버킷. local 은 dev 버킷 공유 + `local/` prefix 객체 격리 |
| 버킷명 | snurdy-backend-dev, snurdy-backend-prod |
| 버킷 구조 | 단일 버킷 + 도메인 폴더 prefix (향후 다른 이미지 도메인 확장 시 폴더 추가만) |
| 리전 | ap-northeast-2 |
| 객체 권한 | bucket policy 로 prefix 단위 public-read (ACL 비활성, 2024+ 권장 패턴) |
| 객체 경로 | {env-prefix/}{domain-folder}/{...} |
| 허용 포맷 | jpg, jpeg, png |
| 최대 크기 | 5MB |
| 이미지 처리 | 원본 그대로 저장. 리사이즈/압축은 클라 책임 |
| 구 이미지 삭제 | 새 업로드 시 best effort. 같은 버킷 소속만 삭제, 외부 URL 은 skip |
| Credential | local/dev access key (.env), prod EC2 instance role |
| 코드 prefix | USER_* 유지 (별도 STORAGE_* 신설 안 함) |
| 기존 PATCH /v2/users/me 의 profileImageUrl | 유지 (breaking 회피). iOS 가 신규 endpoint 사용 권장 |
| MIME 검증 | contentType + 확장자 (매직 바이트 검증은 후속 검토) |

---

## 13. 후속 검토

- 이미지 리사이즈 / 썸네일 자동 생성 (서버 측)
- 매직 바이트 (Apache Tika) MIME 검증 강화
- presigned URL 전환 (트래픽 증가 시)
- CloudFront CDN 도입 (latency 개선)
- orphan S3 객체 정기 cleanup batch
- 다른 도메인 이미지 추가 시 `S3StorageService` 재사용 (단어 카드 이미지 등) — 폴더 prefix 만 다름
- webp 추가 (압축률 개선 필요 시)
