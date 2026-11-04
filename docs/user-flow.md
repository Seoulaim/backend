# 회원 프로필·설정·탈퇴·비밀번호 재설정 연동 가이드 (회원 앱)

- 경로는 전부 `/api/v1` 하위
- `/users/me/**` 는 `Authorization: Bearer <access token>` 필수
- `/auth/password-reset/**` 는 로그인 없이 호출 (공개 경로)
- 에러 응답은 공통 형식 `{ code, message, fields, traceId }`. 앱은 `code` 로 분기한다

## 1. 프로필 수정

### 프로필 이미지 업로드 → 저장

```
1. POST /files/presigned-upload { domain: "PROFILE", contentType: "image/jpeg", sizeBytes }
   → { fileId, s3Key, uploadUrl, expiresAt }
2. PUT uploadUrl (S3 로 이미지 업로드, Content-Type 동일하게)
3. PATCH /users/me { profileImageFileId: fileId }
```

### `PATCH /users/me`

모든 필드 선택. 보내지 않거나 null 이면 그대로 둔다.

```json
// 요청
{ "nickname": "Sol", "profileImageFileId": "0193...", "removeProfileImage": false }

// 200 응답 (GET /users/me 와 같은 형태)
{
  "id": "0192f1c4-...",
  "email": "luna@example.com",
  "nickname": "Sol",
  "profileImageKey": "profile/2026/11/0193....jpg",
  "locale": "en"
}
```

| 상황 | 응답 |
|---|---|
| nickname 이 공백뿐이거나 앞뒤 공백 제거 후 20자 초과 | 400 `USER_INVALID_NICKNAME` |
| profileImageFileId 가 없는 파일·남이 올린 파일·PROFILE 이 아닌 파일 | 400 `COMMON_INVALID_REQUEST` (`fields.profileImageFileId`) |
| `removeProfileImage: true` 와 `profileImageFileId` 를 같이 보냄 | 400 `COMMON_INVALID_REQUEST` |

- `removeProfileImage: true` → `profileImageKey` 가 null 이 된다 (앱 기본 아바타 표시)
- 이미지는 key 로 내려간다. CDN/S3 도메인 조합은 앱에서

## 2. 메일 수신 설정

### `GET /users/me/notifications`

```json
{
  "email": "luna@example.com",
  "service": true,
  "marketing": false,
  "marketingUpdatedAt": null
}
```

- `service`: 결제·계정 관련 메일. **끌 수 없어 항상 true** (토글 비활성 표시)
- `marketingUpdatedAt`: 마케팅 수신 동의를 마지막으로 바꾼 시각(UTC). 가입 후 한 번도 안 바꿨으면 null

### `PATCH /users/me/notifications`

```json
// 요청 (marketing 생략 시 변경 없음)
{ "marketing": true }

// 200 응답 — GET 과 같은 형태
{ "email": "luna@example.com", "service": true, "marketing": true, "marketingUpdatedAt": "2026-11-04T06:00:00Z" }
```

값이 실제로 바뀔 때만 `marketingUpdatedAt` 이 갱신된다 (같은 값을 다시 보내면 그대로).

## 3. 앱 언어

### `PATCH /users/me/language` → 204

```json
{ "language": "ko" }
```

- `en | ko | zh | ja` 만 허용. 그 외는 400 `COMMON_INVALID_REQUEST` (`fields.language`)
- `GET /users/me` 의 `locale` 로 내려간다. 서버 발송 메일·푸시 언어 선택에 쓴다

## 4. 회원 탈퇴

### `DELETE /users/me` → 204

서버 처리:

| 데이터 | 처리 |
|---|---|
| 계정 | 이메일·비밀번호 삭제, 상태 WITHDRAWN. 같은 이메일로 재가입 가능 |
| refresh token | 모든 기기에서 폐기 |
| 최애(bias)·장소 즐겨찾기·여행 일정 | 삭제 |
| 플랜 만족도 조사 응답 | 응답은 남기고 이메일만 `"withdrawn"` 으로 익명화 (어드민 응답 통계 유지) |
| 다이어리 | 삭제 처리 (목록에서 사라짐) |
| 다이어리 결제 기록 | **남김** (환불·정산 대응) |

앱 처리:

1. 204 를 받으면 저장된 access/refresh token 을 지우고 로그인 화면으로 이동
2. 탈퇴한 계정의 이메일·비밀번호로 로그인하면 일반 로그인 실패와 같은 401 `AUTH_INVALID_CREDENTIALS`
3. 남아 있는 access token 은 만료(기본 1시간) 전까지 서명상 유효하지만, 프로필·설정 API 는 404 `USER_NOT_FOUND` 를 준다. 토큰은 반드시 지울 것

이미 탈퇴한 상태에서 다시 호출해도 204 (멱등).

## 5. 비밀번호 재설정 (로그인 전)

```
앱                                                  API 서버
 │ 1. POST /auth/password-reset/codes { email } ──▶ 200 { expiresInSeconds, resendAvailableInSeconds }
 │    (가입된 이메일이면 6자리 인증번호 메일 발송)
 │ 2. POST /auth/password-reset { email, code, newPassword } ──▶ 204
 │ 3. POST /auth/login { email, password: newPassword } ──▶ 200 토큰
```

### `POST /auth/password-reset/codes`

```json
// 요청
{ "email": "luna@example.com" }

// 200 응답
{ "expiresInSeconds": 300, "resendAvailableInSeconds": 60 }
```

- **가입되지 않은 이메일도 같은 200 응답** (메일만 발송되지 않음). 앱은 항상 "메일을 확인해주세요" 안내
- 같은 이메일로 60초 안에 다시 요청 → 429 `AUTH_OTP_RESEND_TOO_SOON` (가입 여부와 무관하게 적용)
- 메일 서버 장애 → 503 `AUTH_MAIL_SEND_FAILED`
- 회원가입용 인증번호(`/auth/email-verifications`)와 별개. 가입 인증번호로는 재설정할 수 없다

### `POST /auth/password-reset` → 204

```json
{ "email": "luna@example.com", "code": "123456", "newPassword": "newpass5678" }
```

| 상황 | 응답 |
|---|---|
| 새 비밀번호가 규칙 위반 (8~64자, 영문·숫자 각 1자 이상 — 회원가입과 동일) | 400 `AUTH_INVALID_PASSWORD` (인증번호는 소모되지 않음 → 고쳐서 다시 요청) |
| 인증번호 불일치 | 400 `AUTH_OTP_MISMATCH` |
| 인증번호 없음·만료(5분)·이미 사용 | 400 `AUTH_OTP_EXPIRED` → 1단계부터 다시 |
| 5회 틀림 | 429 `AUTH_OTP_ATTEMPTS_EXCEEDED` → 1단계부터 다시 |

성공하면 그 계정의 refresh token 이 **모든 기기에서** 폐기된다 → 다른 기기는 다시 로그인해야 한다.
