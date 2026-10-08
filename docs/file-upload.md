# 파일 업로드 가이드

이미지는 서버를 거치지 않고 클라이언트가 S3 에 직접 올린다. 서버는 presigned PUT URL 만 발급하고, 업로드된 파일은 `files` 테이블의 `fileId` 로 관리한다.

## 흐름

```
iOS                          API 서버                         S3
 │ 1. POST /api/v1/files/presigned-upload ─▶ files 행 PENDING 생성
 │ ◀──────────── { fileId, s3Key, uploadUrl, expiresAt }
 │ 2. PUT uploadUrl (이미지 바이트) ─────────────────────────────▶ 저장
 │ 3. 도메인 API 호출 (예: 리뷰 작성) 에 fileId 전달
 │                              └─ FileService.markLinked(fileIds) → LINKED
```

- PENDING 상태로 **48시간** 이 지난 파일은 매일 04:00(UTC) 정리 배치가 S3 객체와 DB 행을 함께 삭제한다.
- 따라서 업로드 후 도메인 엔티티에 연결(3단계)하지 않은 파일은 사라진다.

## 1. presigned URL 발급

`POST /api/v1/files/presigned-upload`

헤더: `X-User-Id: <UUID>` (auth 연동 전 임시. JWT 도입 시 `Authorization` 으로 교체되며 prod 에서는 401)

```json
{ "domain": "REVIEW", "contentType": "image/jpeg", "sizeBytes": 482113 }
```

| 필드 | 규칙 |
|---|---|
| `domain` | `REVIEW` / `PLAN` / `PROFILE` / `PRODUCT` / `DIARY` / `INQUIRY` |
| `contentType` | `image/jpeg`, `image/png`, `image/webp`, `image/heic` |
| `sizeBytes` | 실제 업로드할 바이트 수. 최대 10MB (10,485,760) |

응답 200

```json
{
  "fileId": "0199a1b2-...",
  "s3Key": "review/2026/10/0199a1b2-....jpg",
  "uploadUrl": "https://<bucket>.s3.ap-northeast-2.amazonaws.com/review/...?X-Amz-...",
  "expiresAt": "2026-10-08T03:10:00Z"
}
```

에러

| code | status | 원인 |
|---|---|---|
| `FILE_INVALID_DOMAIN` | 400 | domain 값 오류 |
| `FILE_INVALID_CONTENT_TYPE` | 400 | 허용되지 않은 형식 |
| `FILE_TOO_LARGE` | 400 | 10MB 초과 |
| `AUTH_INVALID_TOKEN` | 401 | X-User-Id 없음/형식 오류 |

## 2. S3 업로드 (iOS)

- URL 은 **10분** 뒤 만료된다. 만료되면 1단계부터 다시 요청한다.
- `Content-Type` 과 `Content-Length` 가 서명에 포함된다. **발급 요청 때 보낸 값과 정확히 같아야** 하며, 다르면 S3 가 `403 SignatureDoesNotMatch` 를 반환한다.
- HEIC 를 JPEG 로 변환하는 등 바이트가 바뀌면 변환 후의 형식·크기로 발급받는다.

```swift
var request = URLRequest(url: uploadUrl)
request.httpMethod = "PUT"
request.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
let (_, response) = try await URLSession.shared.upload(for: request, from: imageData)
// 200 이면 성공
```

curl 로 확인할 때:

```bash
curl -X PUT -H "Content-Type: image/jpeg" --data-binary @photo.jpg "<uploadUrl>"
```

## 3. 도메인에서 파일 연결 (백엔드)

다른 도메인은 `files` repository/entity 를 직접 참조하지 않고 `FileService` 를 통한다.

```java
// 예: ReviewService
@Transactional
public ReviewResponse create(UUID userId, ReviewCreateRequest request) {
  fileService.markLinked(request.imageFileIds()); // 없는 fileId 가 있으면 FILE_NOT_FOUND
  ...
}
```

- 도메인 엔티티에는 `fileId` 가 아니라 `s3Key` 를 `*_image_key` 컬럼에 저장한다 (전역 규칙). 필요하면 `fileService.getById(fileId).s3Key()` 로 조회한다.
- `markLinked` 는 이미 LINKED 인 파일을 그대로 둔다 (재시도 안전).
- 같은 트랜잭션 안에서 호출해야 도메인 저장이 실패했을 때 LINKED 도 함께 롤백된다.

## 디버깅

`GET /api/v1/files/{fileId}` → `{ fileId, s3Key, status, contentType }`

## 환경 설정

| 프로파일 | 버킷 | 자격증명 |
|---|---|---|
| local | `seoulaim-dev-591553897454` (ap-northeast-2) | AWS 기본 자격증명 체인 (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 환경변수) |
| dev/prod | `STORAGE_BUCKET` 환경변수 | 기본 체인 (IAM role 권장) |

- 버킷 퍼블릭 액세스는 차단돼 있다. 조회도 추후 CDN/presigned GET 으로 처리한다.
- 버킷 CORS 는 PUT/GET, AllowedOrigins `*` (임시. 추후 좁힘).
- `app.storage.access-key`/`secret-key` 를 지정하면 기본 체인 대신 그 값을 쓴다 (보통 비워 둔다).
