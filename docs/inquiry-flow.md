# 문의·환불 연동 가이드 (회원 앱·어드민)

Help Center(MR03)에서 회원이 남긴 문의를 어드민이 이메일로 답한다. **환불은 유형이 `refund` 인 문의로만** 들어오고, 어드민 "환불 관리"에서 승인하면 서버가 토스 결제를 취소한다.

- 회원 API: `Authorization: Bearer <access token>`, 경로는 `/api/v1` 하위
- 어드민 API: `X-Admin-Key` 헤더, 경로는 `/api/v1/admin` 하위
- 응답의 `category`, `status`, `refund.status` 는 **소문자** (`refund`, `received`, `requested` …). 요청의 `category` 는 대소문자를 가리지 않는다
- 메일은 회원 계정 이메일로만 보낸다 (푸시 없음)

## 흐름

```
회원 앱                                                  API 서버
 │ (사진이 있으면) POST /files/presigned-upload {domain: INQUIRY} → S3 PUT
 │ (환불이면) GET /me/payments → refundable 인 결제를 고르게 한다
 │ POST /inquiries ─────────────────────────────────────▶ 201 { id }

어드민 — 문의 관리 (환불 제외)
 │ GET  /admin/inquiries ───────────────────────────────▶ { inquiries, refundCount }
 │ GET  /admin/inquiries/{id}
 │ POST /admin/inquiries/{id}/answers { body } ─────────▶ 메일 발송 → answered
 │ PUT  /admin/inquiries/{id}/memo { memo }

어드민 — 환불 관리
 │ GET  /admin/refund-requests ─────────────────────────▶ 처리 대기 먼저
 │ GET  /admin/refund-requests/{id}
 │ POST /admin/refund-requests/{id}/approve { exception? } ─▶ 토스 취소 → 다이어리 처리 → 메일
 │ POST /admin/refund-requests/{id}/reject { reason } ──▶ 거절 메일
```

## 1. 문의 작성 (회원)

`POST /api/v1/inquiries`

```json
{
  "category": "refund",
  "title": "Refund for my second diary",
  "content": "I made a second diary by mistake.",
  "photoFileIds": ["019a5a10-..."],
  "language": "en",
  "paymentId": "019a4c30-..."
}
```

→ `201 { "id": "019a5a20-..." }`

| 필드 | 규칙 |
|---|---|
| `category` | `refund` `payment` `diary` `account` `report` `other` |
| `title` | 앞뒤 공백 제거 후 1~50자 |
| `content` | 앞뒤 공백 제거 후 1~500자 |
| `photoFileIds` | 최대 5개. 본인이 `domain: INQUIRY` 로 올린 이미지만. 생략 가능 |
| `language` | 작성한 앱 언어 `en` `ko` `zh` `ja`. 어드민이 이 언어로 답한다 |
| `paymentId` | `refund` 일 때 필수 (`GET /me/payments` 의 `id`). 다른 유형에서는 무시 |

검증 실패는 전부 `400 INQUIRY_INVALID` (`fields` 에 어느 필드인지).

### 환불 문의

- 본인의 `PAID` 결제만 고를 수 있다 (환불된 결제·남의 결제 → `400 INQUIRY_INVALID`)
- 같은 결제에 처리 대기 중이거나 이미 환불된 요청이 있으면 `409 INQUIRY_REFUND_ALREADY_REQUESTED`. **거절된 뒤에는 다시 신청할 수 있다**
- 서버가 신청 시점에 환불 정책 안인지 판정해 저장한다 (`refundWithinPolicy`): 결제 후 7일 이내 **그리고** 다이어리 미완성 **그리고** 결제 후 편집 이력 없음
- 정책 밖이어도(7일 경과·편집·완성) **접수는 받는다**. 어드민이 예외 사유를 골라야만 승인할 수 있다
- 앱은 `GET /me/payments` 에서 `refundable: true` 인 결제만 고르게 하면 된다. 신청하면 그 결제는 `refundRequested: true`, `refundable: false` 가 된다

## 2. 문의 관리 (어드민, 환불 제외)

### 목록·상세

`GET /api/v1/admin/inquiries` — 환불 문의 제외, 최신순

```json
{
  "inquiries": [
    {
      "id": "019a5a20-...",
      "author": { "id": "019a...", "nickname": "Sora", "email": "sora@example.com" },
      "language": "en",
      "category": "payment",
      "title": "Charged twice?",
      "content": "I see two charges on my card for the same diary.",
      "photoKeys": ["inquiry/2026/11/....jpg"],
      "createdAt": "2026-11-05T01:00:00Z",
      "status": "answered",
      "answers": [
        { "id": "019a...", "body": "Hi Sora, ...", "sentAt": "2026-11-05T02:00:00Z", "adminName": "운영자" }
      ],
      "memo": "토스에서 중복 결제 확인함"
    }
  ],
  "refundCount": 2
}
```

- `refundCount`: 환불 관리에서 처리 대기 중인 요청 수 (사이드바 배지용)
- 탈퇴한 회원은 `author.email` 이 `null`
- `GET /admin/inquiries/{id}` 는 같은 항목 1건. 환불 문의 id 면 `404 INQUIRY_NOT_FOUND`

### 답변

`POST /api/v1/admin/inquiries/{id}/answers { "body": "Hi Sora, ..." }` → 갱신된 문의

- 작성자 이메일로 제목 `[Seoulaim] Re: {문의 제목}`, 본문 = `body` 그대로 발송한 뒤 기록하고 `status: answered`
- 언어별 템플릿은 없다. 어드민이 문의 `language` 로 본문을 쓴다
- 메일 발송이 실패하면 아무것도 저장하지 않는다 (`503 AUTH_MAIL_SEND_FAILED`). 탈퇴로 이메일이 없으면 `400 INQUIRY_INVALID`
- 여러 번 답할 수 있다 (`answers` 에 쌓인다)

### 메모

`PUT /api/v1/admin/inquiries/{id}/memo { "memo": "..." }` → 갱신된 문의. 회원에게 보내지 않는다. 빈 문자열이면 지운다 (최대 2000자)

## 3. 환불 관리 (어드민)

### 목록·상세

`GET /api/v1/admin/refund-requests` — **처리 대기(requested) 오래된 순**(기한 임박) 먼저, 그다음 결정된 것 최신순

```json
[
  {
    "id": "019a5a30-...",
    "author": {
      "id": "019a...",
      "nickname": "Jin",
      "email": "jin@example.com",
      "joinedAt": "2026-10-20T08:00:00Z",
      "language": "en",
      "diaryCount": 2,
      "paidPaymentCount": 1,
      "refundedCount": 0,
      "rejectedRefundCount": 0,
      "pendingInquiryCount": 1,
      "withdrawn": false
    },
    "language": "en",
    "title": "Refund for my second diary",
    "content": "I made a second diary by mistake.",
    "photoKeys": [],
    "createdAt": "2026-11-05T03:00:00Z",
    "refund": {
      "status": "requested",
      "decidedAt": null,
      "adminName": null,
      "exception": null,
      "rejectReason": null
    },
    "payment": {
      "id": "019a4c30-...",
      "orderId": "DIA-20261104-ABCDE12345",
      "amount": 3000,
      "method": "CARD",
      "currency": "KRW",
      "provider": "toss",
      "paidAt": "2026-11-04T03:00:00Z",
      "refundConsentAt": "2026-11-04T02:59:30Z",
      "editedAt": null,
      "diary": {
        "id": "019a4c10-...",
        "name": "My Diary",
        "first": false,
        "pages": 4,
        "completedAt": null,
        "createdAt": "2026-11-04T02:50:00Z",
        "saveCount": 3,
        "lastSavedAt": "2026-11-04T02:58:00Z",
        "photoCount": 5,
        "pageKeys": [],
        "thumbnailKey": null,
        "downloads": [],
        "deleted": false
      }
    },
    "refundWithinPolicy": true,
    "deadline": "2026-11-10",
    "policy": {
      "within7Days": { "ok": true, "paidAt": "2026-11-04T03:00:00Z", "deadlineAt": "2026-11-11T03:00:00Z" },
      "notEdited": { "ok": true, "editedAt": null },
      "notFinalized": { "ok": true, "finalizedAt": null },
      "notDownloaded": { "ok": true, "downloadCount": 0, "lastDownloadedAt": null },
      "allOk": true
    }
  }
]
```

| 필드 | 의미 |
|---|---|
| `refund.status` | `requested` / `refunded` / `rejected` |
| `refund.adminName` | 결정되면 `"운영자"` (운영자 계정이 따로 없음) |
| `refund.exception` | 정책 밖 승인 사유 `{ reason, memo }`, 없으면 `null` |
| `payment.orderId` | 토스 orderId (= 결제 orderNumber). 토스 대시보드 대조용 |
| `payment.diary.first` | 회원의 첫(무료) 다이어리 |
| `payment.diary.pages` | 편집 중이면 spreads 수, 완성 후에는 완성 페이지 수 |
| `refundWithinPolicy` | **신청 시점** 서버 판정. 이후 편집·경과일과 무관하게 이 값으로 승인 조건을 본다 |
| `deadline` | 처리 기한 = 신청일(KST) + 영업일 3일 (환불 정책 제6조). 주말만 건너뛰고 공휴일은 반영하지 않는다 |
| `policy` | **지금 시점** 정책 항목별 판정 (아래 "정책 판정"). 결제를 못 찾으면 `null` |
| `payment.diary.saveCount` / `lastSavedAt` | 편집 데이터 저장(`PUT spreads`·`finalize`) 횟수와 마지막 저장 시각 |
| `payment.diary.photoCount` | 마지막 저장 시점 `placePhotos` 전체 항목 수 (같은 사진이 여러 장소에 있으면 각각) |
| `payment.diary.pageKeys` / `thumbnailKey` | 완성 페이지 key (없으면 `[]`) / 첫 장 (없으면 `null`) |
| `payment.diary.downloads` | `[{ format: "PDF"|"JPG", pageCount, at }]` 최신순 |
| `payment.diary.deleted` | soft delete 됨 (환불·회원 삭제·탈퇴) |
| `author.language` | 회원의 **현재** 앱 언어. 상위 `language` 는 문의를 쓸 때 언어 |
| `author.diaryCount` | 삭제되지 않았고 무료이거나 결제된 다이어리 수 (회원 앱 목록에 보이는 수) |
| `author.paidPaymentCount` | 지금 PAID 인 결제 수 (환불된 결제 제외) |
| `author.refundedCount` / `rejectedRefundCount` | 승인 / 거절된 환불 요청 수 |
| `author.pendingInquiryCount` | 답변 전(received) 문의 수. 처리 대기 환불 요청 포함 |
| `author.withdrawn` | 탈퇴 회원 (이때 `email` 은 `null`) |

`GET /admin/refund-requests/{id}` 는 같은 항목 1건. 환불 문의가 아니면 `404 INQUIRY_NOT_FOUND`. 승인·거절 응답도 같은 형태다.

### 정책 판정

환불 정책(회원 앱 `refund.ko.md`) 항목을 서버가 **조회하는 지금 시점**으로 판정해 `policy` 로 내려준다. 어드민 화면의 "환불 정책 확인" 체크 항목을 이 값으로 그리면 된다.

| 항목 | ok 조건 | 근거 |
|---|---|---|
| `within7Days` | `now ≤ paidAt + 7일` (`deadlineAt`). 결제 전이면 false | 제3조 1·5항 |
| `notEdited` | 결제 후 편집 기록(`editedAt`)이 없음. 첫 다이어리의 1~2쪽 편집은 앱이 `/edited` 를 부르지 않으므로 기록되지 않는다 | 제3조 1·2항 |
| `notFinalized` | 다이어리가 완성(Done)되지 않음. `finalizedAt` = 완성 시각 | 제4조 |
| `notDownloaded` | 완성본 다운로드 기록(`POST /diaries/{id}/downloads`)이 없음 | 제4조 |
| `allOk` | 네 항목 모두 ok | |

- 7일은 KST 날짜가 아니라 결제 시각 + 7×24시간 (기존 `refundWithinPolicy`·`/me/payments` 의 `refundable` 과 같은 기준)
- 다운로드는 완성 후에만 기록되므로 `notFinalized` 가 ok 면 `notDownloaded` 도 대개 ok 다. 둘 다 내려주는 건 어드민이 "완성했지만 아직 안 내려받음"을 보게 하려는 것
- 다이어리를 찾을 수 없으면 `notFinalized`·`notDownloaded` 는 ok 로 본다

**`refundWithinPolicy` 와의 관계**: `refundWithinPolicy` 는 환불을 **신청한 시점**의 판정(7일 이내 && 미편집 && 미완성)이고 승인 조건(정책 밖이면 `exception` 필수)은 이 값으로 본다. `policy.allOk` 는 같은 기준을 **지금** 다시 본 것이라, 신청 후 편집·완성·다운로드했거나 7일이 지났으면 둘이 다를 수 있다 (예: 신청 시 `true` → 처리 전에 다운로드 → `allOk: false`). 둘이 다르면 어드민이 사정을 확인하도록 둘 다 내려준다. 이미 환불된 결제도 `policy` 는 항목대로 계산한다.

### 승인

`POST /api/v1/admin/refund-requests/{id}/approve` → 갱신된 항목

```json
{ "exception": { "reason": "duplicate", "memo": "토스에서 같은 다이어리 중복 결제 확인" } }
```

- 정책 안(`refundWithinPolicy: true`)이면 본문 없이(또는 `{}`) 보내면 된다
- 정책 밖인데 `exception`(사유 비어 있음 포함)이 없으면 `400 INQUIRY_POLICY_EXCEPTION_REQUIRED`
- `reason` 은 어드민 사유 id (`duplicate` `not-provided` `company-error` `misrepresented` `other`, 최대 50자). `other` 는 `memo` 필수 (없으면 `400 INQUIRY_INVALID`)
- 이미 결정됨 `409 INQUIRY_ALREADY_DECIDED`

서버 처리 순서 (한 트랜잭션):

1. 토스 `POST /v1/payments/{paymentKey}/cancel { cancelReason }` (전액 취소). 결제한 상점(MID)의 키로 보낸다 — CARD 는 원화 MID, PAYPAL 은 외화 MID
   - **PayPal 결제는 취소해도 토스 수수료가 환불되지 않는다** (회사 부담)
   - 외화 MID 키가 빠진 서버에서 PayPal 결제를 승인하면 토스 호출 없이 `502 PAYMENT_REFUND_FAILED` (`fields.code: "MERCHANT_UNAVAILABLE"`)
   - 토스가 거절하면 `502 PAYMENT_REFUND_FAILED`, `fields` 에 토스 `code`/`message`. **결제·다이어리·문의 모두 그대로**라 다시 시도할 수 있다
   - 토스 통신 오류·5xx 도 `502 PAYMENT_REFUND_FAILED` (`fields.code: "GATEWAY_ERROR"`)
   - 토스에서 이미 취소된 결제(`ALREADY_CANCELED_PAYMENT`, 예: 타임아웃 뒤 재시도)는 환불 완료로 처리한다
2. 결제 `REFUNDED`, `refunded_at`, 토스 취소 거래 키 저장
3. 다이어리 (환불 정책 제3조 3·4항)
   - 첫(무료) 다이어리: `paid=false` 만. 꾸민 내용은 남고, 앱은 무료 다이어리처럼 2쪽까지만 연다. 다시 결제하면 그대로 보인다
   - 두 번째 이후: soft delete (결제 내역에는 남고 이름·썸네일이 `null`). 사진 S3 삭제는 TODO
4. 문의 `refund.status: refunded`, `status: answered`
5. 커밋 후 "환불 완료" 메일 (문의 언어 en/ko/ja/zh, 제목 `[Seoulaim] Re: {문의 제목}`). 돈은 이미 돌려줬으므로 메일 실패는 로그만 남긴다. 금액은 결제 통화로 적는다 (`KRW 3,000`, `USD 2.99`)

### 거절

`POST /api/v1/admin/refund-requests/{id}/reject { "reason": "완성(Done)한 다이어리는 환불 정책 제4조에 따라 환불되지 않아요." }` → 갱신된 항목

- `reason` 1~1000자 (아니면 `400 INQUIRY_INVALID`). 그대로 회원에게 메일로 보낸다 (안내 문구는 문의 언어, 사유는 어드민이 쓴 그대로)
- `refund.status: rejected`, `status: answered`. 메일 발송이 실패하면 롤백된다 (`503 AUTH_MAIL_SEND_FAILED`)
- 거절된 결제는 회원이 다시 환불을 신청할 수 있다

## 에러 코드

| code | HTTP | 언제 |
|---|---|---|
| `INQUIRY_INVALID` | 400 | 문의·답변·메모·거절 사유·예외 사유 검증 실패 |
| `INQUIRY_NOT_FOUND` | 404 | 없는 문의 (문의 관리에서 환불 문의 id, 환불 관리에서 일반 문의 id 포함) |
| `INQUIRY_REFUND_ALREADY_REQUESTED` | 409 | 같은 결제에 처리 대기·환불된 요청이 있음 |
| `INQUIRY_ALREADY_DECIDED` | 409 | 이미 승인·거절된 환불 요청 |
| `INQUIRY_POLICY_EXCEPTION_REQUIRED` | 400 | 정책 밖 요청을 예외 사유 없이 승인 |
| `PAYMENT_REFUND_FAILED` | 502 | 토스 결제 취소 실패 |
| `AUTH_MAIL_SEND_FAILED` | 503 | 답변·거절 메일 발송 실패 (저장 안 됨) |

## 아직 없는 것

- 회원의 내 문의 목록·답변 조회 API (답변은 메일로만)
- `GET /admin/dashboard`
- 환불·삭제된 다이어리 사진의 S3 정리
