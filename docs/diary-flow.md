# 다이어리 연동 가이드 (웹앱)

다이어리는 실물 제작이 아니라 **PDF 다운로드** 상품이다. PDF 는 웹앱이 편집 화면에서 직접 만들고, 서버는 렌더링하지 않는다. 배송·제작 기한은 없다.

헤더: 모든 요청에 `Authorization: Bearer <access token>`

## 흐름

```
웹앱                                         API 서버                     S3
 │ 1. GET /diary-templates, /diary-stickers ─▶ 활성 목록
 │ 2. 다이어리 상품 주문·결제 (docs/payment-flow.md) ─▶ 주문 PAID
 │ 3. GET /diaries/pending-orders ──────────▶ 제출 가능한 orderItemId
 │ 4. 편집 → 클라이언트에서 PDF 생성
 │ 5. POST /files/presigned-upload (DIARY, application/pdf) ─▶ { fileId, uploadUrl }
 │ 6. PUT uploadUrl ───────────────────────────────────────────────────────▶ 저장
 │ 7. POST /diaries { orderItemId, pageCount, content, pdfFileId } ─▶ 201 { diaryId }
 │ 8. GET /diaries/{diaryId}/download ──────▶ { downloadUrl, expiresAt }
 │ 9. downloadUrl 로 이동 ─────────────────────────────────────────────────▶ PDF 첨부 다운로드
```

경로는 모두 `/api/v1` 하위다.

## 1. 템플릿·스티커

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

- `id`: 템플릿 안에서 프레임을 구분하는 문자열 (편집 데이터 `content` 에서 참조용)
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

## 2. 다이어리 상품 주문·결제

`isDiary=true` 상품을 일반 상품과 같은 방법으로 주문·결제한다. 다이어리 상품은 수량 1 만 가능하다. 자세한 순서는 `docs/payment-flow.md`.

## 3. 제출 가능한 주문 확인

`GET /api/v1/diaries/pending-orders`

```json
[{ "orderItemId": "...", "orderId": "...", "orderNumber": "ORD-20261029-AB12CD", "productName": "Seoul Diary", "paidAt": "2026-10-29T03:01:00Z" }]
```

결제 완료(PAID) 주문의 다이어리 상품 중 아직 제출하지 않은 것만 내려온다. 주문 상품 1개당 다이어리 1개.

## 4~6. 편집, PDF 생성, 업로드

1. 편집 데이터(`content`)는 웹앱이 정한 JSON object 그대로 보낸다. 서버는 구조를 검증하지 않고 저장·반환만 한다. 템플릿 id, 스티커 위치·크기·회전, 텍스트, 사진 S3 key 등을 넣는다
2. 사진은 `domain=DIARY`, 이미지 contentType 으로 따로 업로드해 key 를 `content` 에 넣는다 (10MB 제한)
3. PDF 를 만든 뒤 presigned 업로드:

```json
POST /api/v1/files/presigned-upload
{ "domain": "DIARY", "contentType": "application/pdf", "sizeBytes": 3481123 }
```

- PDF 는 `DIARY` 도메인에서만 허용, 최대 20MB
- presigned 요청의 `sizeBytes` 는 실제 업로드할 Blob/File 의 byte 길이와 정확히 같아야 하며, 업로드 시 `Content-Type` 도 요청 값과 동일해야 한다. PDF 는 생성을 마친 뒤 `blob.size` 로 요청할 것. 다르면 S3 가 `403 SignatureDoesNotMatch` 를 반환한다
- 응답의 `uploadUrl` 로 PUT (10분 만료). 자세한 규칙은 `docs/file-upload.md`

```js
await fetch(uploadUrl, { method: "PUT", headers: { "Content-Type": "application/pdf" }, body: pdfBlob }); // presigned 요청 sizeBytes = pdfBlob.size
```

## 7. 제출

`POST /api/v1/diaries`

```json
{
  "orderItemId": "0199a1b2-...",
  "pageCount": 12,
  "content": { "pages": [{ "templateId": "...", "elements": [] }] },
  "pdfFileId": "0199a1b3-..."
}
```

응답 201 `{ "diaryId": "..." }`

| 오류 코드 | 상태 | 조건 |
|---|---|---|
| `DIARY_ORDER_NOT_ELIGIBLE` | 400 | 내 주문이 아니거나, 결제 완료가 아니거나, 다이어리 상품이 아님 |
| `DIARY_ALREADY_SUBMITTED` | 409 | 이미 제출한 주문 상품 |
| `DIARY_INVALID_PDF` | 400 | `pdfFileId` 가 없거나, `DIARY` 도메인이 아니거나, `application/pdf` 가 아니거나, 내가 올린 파일이 아님 |
| `DIARY_CONTENT_TOO_LARGE` | 400 | `content` 직렬화 크기 1MB 초과 |
| `COMMON_INVALID_REQUEST` | 400 | 필수값 누락, `pageCount` 가 1~30 밖, `content` 가 object 가 아님 |

제출 후 수정 API 는 없다.

## 8~9. 조회·다운로드

- `GET /api/v1/diaries` → `[{ diaryId, orderNumber, productName, pageCount, submittedAt }]` (최신순)
- `GET /api/v1/diaries/{diaryId}` → `{ diaryId, orderItemId, pageCount, content, submittedAt }`
- `GET /api/v1/diaries/{diaryId}/download` → `{ downloadUrl, expiresAt }`

`downloadUrl` 은 10분 유효한 S3 presigned GET URL 이고 `Content-Disposition: attachment; filename="seoulaim-diary-{orderNumber}.pdf"` 로 내려온다. 만료되면 다시 발급받는다. 남의 다이어리는 모두 404 `DIARY_NOT_FOUND`.
