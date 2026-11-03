# 다이어리 연동 가이드 (웹앱) — v2 회원 앱 모델

다이어리는 회원이 앱에서 만들고 편집하는 디지털 다이어리다. **주문(order)을 거치지 않는다.** 첫 다이어리는 무료(앞 2 spread 까지), 그 외는 다이어리 단위로 결제한 뒤 편집한다. 서버는 편집 데이터를 해석·렌더링하지 않고 그대로 저장·반환한다.

헤더: 모든 요청에 `Authorization: Bearer <access token>`. 경로는 모두 `/api/v1` 하위다.

## 흐름

```
웹앱                                              API 서버
 │ 1. GET /diaries/new-terms ───────────────────▶ { free, price }
 │ 2. POST /diaries ────────────────────────────▶ 201 { diaryId }  (첫 다이어리만 free)
 │    (유료면) 결제: GET /diaries/{id}/checkout → POST /diaries/{id}/payments → 토스 → POST /payments/{orderId}/confirm
 │ 3. GET /diary-templates, /diary-stickers ────▶ 활성 목록
 │ 4. GET /diaries/{id}/editor ─────────────────▶ 편집 데이터
 │ 5. 사진: POST /files/presigned-upload (DIARY, image/jpeg) → PUT uploadUrl
 │ 6. PUT /diaries/{id}/spreads (자동 저장, 반복) ─▶ 204
 │    (결제된 다이어리면 첫 편집 시) POST /diaries/{id}/edited ─▶ 204
 │ 7. POST /diaries/{id}/finalize ──────────────▶ 204 (이후 편집 불가)
 │ 8. 완성 페이지 이미지를 만들어 presigned 업로드
 │ 9. POST /diaries/{id}/finished-pages { fileIds } ─▶ { pageKeys }
 │10. GET /diaries ─────────────────────────────▶ 목록 (thumbnailKey = pageKeys[0])
```

## 1. 다이어리 만들기·목록

### 생성 조건

`GET /api/v1/diaries/new-terms` → `{ "free": true, "price": 3000 }`

- `free`: 다음에 만들 다이어리가 무료인지. 무료 다이어리는 **계정당 1개**이며, 삭제해도 다시 무료로 만들 수 없다
- `price`: 유료 다이어리 가격(원). 서버 설정 `DIARY_PRICE`

### 생성

`POST /api/v1/diaries` (바디 없음) → 201 `{ "diaryId": "..." }`

- 이름은 `My Diary` 로 시작한다
- 유료 다이어리는 `paid=false` 로 만들어지며, **결제 전까지 목록에 나오지 않고 편집 데이터도 저장할 수 없다** (402). 생성 직후 결제 화면으로 이동할 것

### 목록

`GET /api/v1/diaries`

```json
[
  {
    "id": "019a4c10-...",
    "name": "My Diary",
    "free": true,
    "paid": false,
    "thumbnailKey": "diary/2026/11/019a4c55-....jpg",
    "lastEditedAt": "2026-11-03T03:10:00Z",
    "completedAt": "2026-11-03T03:20:00Z"
  }
]
```

- 삭제되지 않았고 `free` 이거나 `paid` 인 것만, 생성 최신순
- `thumbnailKey` 는 완성 페이지 첫 장. 완성 전이면 `null`

### 이름 변경·삭제

- `PATCH /api/v1/diaries/{id}/name` `{ "name": "Seoul Trip" }` → 204. 앞뒤 공백 제거 후 1~30자가 아니면 400 `DIARY_INVALID_NAME`
- `DELETE /api/v1/diaries/{id}` → 204. soft delete. 이후 해당 다이어리의 모든 API 는 404 `DIARY_NOT_FOUND`

## 2. 템플릿·스티커

- `GET /api/v1/diary-templates` → `[{ id, name, description, imageKey, overlayImageKey, aspect, frames }]`
- `GET /api/v1/diary-stickers` → `[{ id, name, imageKey }]`

활성 항목만 `displayOrder` 순으로 내려온다. `imageKey` 는 S3 key 이며 CDN 도메인 조합은 앱에서 한다.

### 템플릿 레이아웃 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `imageKey` | string | 배경(밑그림) 이미지 S3 key. 항상 있음 |
| `overlayImageKey` | string \| null | 사진 **위에** 덮는 이미지(테두리·장식, 투명 PNG 권장) S3 key. 없으면 null |
| `aspect` | number | 페이지 비율 = 세로 / 가로. 기본 `1.4142857` (A4 세로, 297/210) |
| `frames` | array | 사진이 들어갈 자리 목록. 없으면 `[]` |

`frames` 원소: `{ id, x, y, width, height, rotation }`

- `id`: 템플릿 안에서 프레임을 구분하는 문자열 (편집 데이터 `spreads` 에서 참조용)
- `x`, `y`, `width`, `height`: 좌상단 좌표와 크기. 단위·기준은 웹앱 편집기와 어드민이 약속한 값 그대로 저장·반환하며 서버는 검증하지 않는다 (페이지 가로·세로 대비 0~1 비율 사용 권장)
- `rotation`: 회전 각도(도, degree)

렌더링 순서: 배경(`imageKey`) → 프레임 안 사진 → 오버레이(`overlayImageKey`) → 스티커·텍스트.

```json
{
  "id": "019a4b2c-...",
  "name": "Polaroid",
  "description": "폴라로이드 2컷",
  "imageKey": "diary_template/2026/11/019a4b2c-....png",
  "overlayImageKey": "diary_template/2026/11/019a4b2d-....png",
  "aspect": 1.4142857,
  "frames": [
    { "id": "photo-1", "x": 0.08, "y": 0.1, "width": 0.84, "height": 0.38, "rotation": 0 },
    { "id": "photo-2", "x": 0.12, "y": 0.55, "width": 0.76, "height": 0.34, "rotation": -4 }
  ]
}
```

템플릿·스티커 이미지는 어드민이 `POST /api/v1/admin/files/presigned-upload` (`DIARY_TEMPLATE` / `DIARY_STICKER`) 로 올려 등록한다 (`docs/file-upload.md` 의 어드민 업로드).

### 어드민 등록·수정 (`X-Admin-Key`)

- `POST /api/v1/admin/diary-templates` `{ name, description?, imageFileId, displayOrder, isActive?, overlayFileId?, aspect?, frames? }` → 201
- `PUT /api/v1/admin/diary-templates/{id}` 같은 바디 → 200
- `POST /api/v1/admin/diary-stickers` `{ name?, imageFileId, displayOrder, isActive? }` → 201
- `PUT /api/v1/admin/diary-stickers/{id}` 같은 바디 → 200

| 필드 | 생성(POST) 에서 생략 | 수정(PUT) 에서 생략 |
|---|---|---|
| `imageFileId` | 400 `COMMON_INVALID_REQUEST` (`fields.imageFileId`) | 기존 `imageKey` 유지 |
| `overlayFileId` | 오버레이 없음 | 기존 값 유지 |
| `aspect` | `1.4142857` | 기존 값 유지 |
| `frames` | `[]` | 기존 값 유지 (보내면 전체 교체) |
| `isActive` | `true` | 기존 값 유지 |

- `name`(템플릿 필수), `displayOrder`(필수), `description` 은 PUT 에서도 매번 보낸 값으로 덮어쓴다
- `*FileId` 를 보내면 해당 파일을 LINKED 로 바꾸고 s3 key 를 저장한다. 없는 fileId 면 404 `FILE_NOT_FOUND`
- 없는 템플릿·스티커를 수정하면 404 `DIARY_TEMPLATE_NOT_FOUND` / `DIARY_STICKER_NOT_FOUND`

## 3. 편집

### 편집 화면 진입

`GET /api/v1/diaries/{id}/editor`

```json
{
  "diary": { "id": "019a4c10-...", "name": "My Diary", "finalized": false, "free": true, "paid": false },
  "pageKeys": null,
  "spreads": [ { "templateId": "019a4b2c-...", "left": { }, "right": { } } ],
  "placePhotos": { "019a3f00-...": [ { "id": "019a4c20-...", "key": "diary/2026/11/019a4c20-....jpg" } ] },
  "usedTemplates": []
}
```

- `spreads`, `placePhotos` 는 마지막으로 저장한 값 그대로다
- `usedTemplates`: `spreads` 안의 `templateId` 중 **어드민이 비활성화한 템플릿**. 활성 목록 API 에는 안 나오므로 기존 편집 내용을 그리려면 이 값을 함께 쓴다. 형태는 `GET /diary-templates` 원소와 같다. 서버는 `spreads` 의 깊이와 무관하게 `"templateId"` 필드(UUID 문자열)를 모두 찾는다
- `pageKeys`: 완성 페이지 등록 후에만 값이 있다

### 사진 업로드

```json
POST /api/v1/files/presigned-upload
{ "domain": "DIARY", "contentType": "image/jpeg", "sizeBytes": 734001 }
```

응답의 `uploadUrl` 로 PUT 한 뒤, `fileId` 와 `s3Key` 를 `placePhotos` 에 `{ id: fileId, key: s3Key }` 로 넣는다. 규칙은 `docs/file-upload.md`.

### 저장

`PUT /api/v1/diaries/{id}/spreads` → 204

```json
{
  "spreads": [ { "templateId": "019a4b2c-...", "left": { "elements": [] }, "right": { "elements": [] } } ],
  "placePhotos": { "019a3f00-...": [ { "id": "019a4c20-...", "key": "diary/2026/11/019a4c20-....jpg" } ], "other": [] }
}
```

- `spreads`: JSON 배열. 원소 구조는 웹앱이 정한다 (서버는 길이만 본다)
- `placePhotos`: `{ [placeId | "other"]: [{ id, key }] }`. 저장할 때 `id`(fileId) 를 모두 LINKED 로 바꾼다 (48시간 PENDING 정리 대상에서 빠진다)
- 매번 **전체**를 보낸다. 두 필드 모두 필수
- 저장할 때마다 `lastEditedAt` 이 갱신된다

| 오류 코드 | 상태 | 조건 |
|---|---|---|
| `DIARY_FINALIZED` | 409 | 이미 완성(finalize)한 다이어리 |
| `DIARY_PAYMENT_REQUIRED` | 402 | 결제 전인데, 무료 다이어리가 아니거나 `spreads` 가 무료 범위(2개)를 넘음 → 결제 화면으로 |
| `DIARY_CONTENT_TOO_LARGE` | 400 | `spreads` + `placePhotos` 직렬화 크기 2MB 초과 |
| `COMMON_INVALID_REQUEST` | 400 | 필드 누락, `spreads` 가 배열 아님, `placePhotos` 형태 오류, 없는 fileId / 내가 올리지 않은 fileId / `key` 가 파일의 s3Key 와 다름 (`fields.placePhotos`) |
| `DIARY_NOT_FOUND` | 404 | 없음·남의 것·삭제됨 |

### 결제 후 편집 기록

결제된 다이어리를 **결제 후 처음 편집할 때** `POST /api/v1/diaries/{id}/edited` → 204 를 호출한다. 최신 PAID 결제에 첫 편집 시각을 남기며(이미 있으면 유지), 환불 정책(`withinPolicy`) 판단에 쓴다. 결제가 없으면 아무 일도 하지 않으므로 무료 다이어리에서 호출해도 된다.

## 4. 완성

### finalize

`POST /api/v1/diaries/{id}/finalize` — 바디·오류는 저장과 같다. 저장 후 `FINALIZED` 로 바꾸고 `completedAt` 을 기록한다 → 204. 이후 저장·finalize 는 409 `DIARY_FINALIZED`.

### 완성 페이지 등록

웹앱이 페이지 이미지를 만들어 사진과 같은 방식(`DIARY`, `image/jpeg`)으로 업로드한 뒤 등록한다.

`POST /api/v1/diaries/{id}/finished-pages`

```json
{ "fileIds": ["019a4c55-...", "019a4c56-..."] }
```

응답 200

```json
{ "pageKeys": ["diary/2026/11/019a4c55-....jpg", "diary/2026/11/019a4c56-....jpg"] }
```

- 배열 순서가 페이지 순서. 1~200개
- 등록하면 `spreads` 는 `[]`, `placePhotos` 는 `{}` 로 비워진다 (편집 원본 사진 S3 삭제는 추후)
- **멱등**: 이미 등록된 다이어리는 요청 바디와 무관하게 기존 `pageKeys` 를 그대로 돌려준다
- 409 `DIARY_NOT_FINISHED`: finalize 전. 400 `COMMON_INVALID_REQUEST`: 없거나 내가 올리지 않은 fileId (`fields.fileIds`)

## 5. 결제

유료 다이어리 결제와 무료 다이어리의 추가 spread 결제는 `docs/payment-flow.md` 의 "다이어리 결제" 를 따른다. 결제가 끝나면 다이어리는 `paid=true` 가 되어 spread 수 제한 없이 저장할 수 있다.
