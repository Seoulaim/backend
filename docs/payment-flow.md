# 결제 연동 가이드 (iOS)

결제는 토스페이먼츠 결제위젯/결제창 SDK 로 진행하고, **승인(confirm)은 반드시 서버를 거친다.** 금액은 서버가 계산한 값만 유효하다.

## 흐름

```
iOS                                API 서버                          토스
 │ 1. POST /api/v1/orders ─────────▶ 금액 계산, 주문 PENDING / 결제 READY
 │ ◀──── { orderId, orderNumber, finalAmount, tossClientKey }
 │ 2. 토스 결제창 SDK 호출 ──────────────────────────────────────────▶ 인증
 │ ◀──────────────── 성공 콜백 { paymentKey, orderId(=orderNumber), amount }
 │ 3. POST /api/v1/orders/{orderId}/payments/confirm ─▶ 승인 API 호출 ─▶ 승인
 │ ◀──── 200 { orderId, status: "PAID" }  또는  402 PAYMENT_FAILED
```

헤더: 모든 요청에 `Authorization: Bearer <access token>`

## 1. 주문 생성

`POST /api/v1/orders`

```json
{
  "items": [{ "productId": "0199a1b2-...", "quantity": 1 }],
  "recipientName": "Kim Minji",
  "addressLine1": "서울특별시 중구 세종대로 110",
  "addressLine2": "101호",
  "postalCode": "04524",
  "contactType": "KAKAO",
  "contactValue": "seoul_fan"
}
```

| 필드 | 규칙 |
|---|---|
| `items` | 1~50개. 같은 `productId` 를 두 줄로 보내면 `ORDER_INVALID_ITEM` |
| `quantity` | 1~99. 다이어리 상품은 1 만 가능 (`ORDER_INVALID_ITEM`) |
| `addressLine1` | 앞뒤 공백 제거 후 `서울특별시` / `서울` / `Seoul`(대소문자 무시) 중 하나로 시작해야 한다 (`ORDER_ADDRESS_NOT_SEOUL`) |
| `contactType` | `PHONE` / `KAKAO` / `INSTAGRAM` / `NONE` / 생략 |

응답 201

```json
{
  "orderId": "0199a1b2-...",
  "orderNumber": "ORD-20261029-AB12CD",
  "finalAmount": 28000,
  "tossClientKey": "test_ck_..."
}
```

`finalAmount` = 상품 합계 + 배송비(3,000원). 앱에서 다시 계산하지 말고 이 값을 그대로 쓴다.

## 2. 토스 결제창 호출

토스 iOS SDK 에 아래 값을 넘긴다.

| SDK 파라미터 | 값 |
|---|---|
| clientKey | 응답의 `tossClientKey` |
| orderId | 응답의 **`orderNumber`** (UUID `orderId` 가 아님) |
| amount | 응답의 `finalAmount` |
| orderName | 첫 번째 상품명 (여러 개면 `"<첫 상품명> 외 N건"` 처럼 앱에서 조합해도 된다) |

## 3. 결제 승인

성공 콜백으로 받은 `paymentKey`, `amount` 를 그대로 서버에 보낸다. 경로의 `{orderId}` 는 1단계 응답의 UUID `orderId` 다.

`POST /api/v1/orders/{orderId}/payments/confirm`

```json
{ "paymentKey": "tgen_20261029...", "amount": 28000 }
```

| 응답 | 의미 | 앱 처리 |
|---|---|---|
| 200 `{ orderId, status: "PAID" }` | 결제 완료 | 완료 화면 |
| 400 `PAYMENT_AMOUNT_MISMATCH` | 콜백 금액 ≠ 주문 금액. 승인하지 않았다 | 주문 재생성 |
| 402 `PAYMENT_FAILED` | 토스 승인 거절. `fields.code`, `fields.message` 에 토스 에러 | 메시지 노출 후 주문 재생성 |
| 404 `ORDER_NOT_FOUND` | 없는 주문 또는 타인 주문 | - |
| 502 `PAYMENT_GATEWAY_ERROR` | 토스 서버 오류/통신 실패. 승인 여부 불확실 | 같은 요청으로 재시도 |

- 승인 요청은 **멱등**하다. 이미 `PAID` 인 주문에 다시 보내면 200 을 그대로 돌려주므로, 네트워크 타임아웃 등으로 응답을 못 받았으면 같은 요청을 재시도하면 된다.
- 402 응답 예:

```json
{
  "code": "PAYMENT_FAILED",
  "message": "결제에 실패했습니다",
  "fields": { "code": "REJECT_CARD_COMPANY", "message": "카드사에서 거절했습니다" },
  "traceId": "..."
}
```

## 실패·취소

- 결제창에서 사용자가 취소했거나 실패 콜백을 받은 경우, 또는 confirm 이 402/400 이면 **기존 주문은 버리고 1단계부터 주문을 새로 만든다.** `FAILED` 주문은 다시 승인할 수 없다.
- 결제창 단계에서 이탈한 `PENDING` 주문은 서버에 남지만 결제되지 않은 상태로 둔다.

## 주문 조회

- `GET /api/v1/orders` — 내 주문 최신순 최대 50개 `[{ orderId, orderNumber, status, finalAmount, orderedAt, itemCount, firstProductName }]` (`itemCount` 는 상품 종류 수)
- `GET /api/v1/orders/{orderId}` — 상세 (상품 스냅샷, 배송지 `shipping`, `paymentStatus`)

---

# 다이어리 결제 (웹앱)

다이어리는 주문(`/orders`)을 만들지 않고 **다이어리 단위로 결제**한다 (`docs/diary-flow.md`). 토스 승인 방식과 실패 처리 원칙은 위 상품 결제와 같다.

## 흐름

```
웹앱                                         API 서버                              토스
 │ 1. GET /api/v1/diaries/{id}/checkout ──────▶ 결제 화면 정보
 │ 2. POST /api/v1/diaries/{id}/payments ─────▶ diary_payments PENDING 생성 (기존 PENDING 은 FAILED)
 │ ◀──── { orderId, orderNumber, method, currency, amount, clientKey, variantKey, customerKey, ... }
 │ 3. 토스 SDK 호출 (CARD: 결제창 / PAYPAL: 결제위젯 v2) ─────────────────────────────────────────────────────▶ 인증
 │ ◀──────────────────── 성공 콜백 { paymentKey, orderId(=orderNumber), amount }
 │ 4. POST /api/v1/payments/{orderId}/confirm ─▶ 승인 API 호출 ──────────────────▶ 승인
 │ ◀──── 200 { diaryId }  또는  402 PAYMENT_FAILED
```

## 1. 결제 화면

`GET /api/v1/diaries/{diaryId}/checkout`

```json
{
  "item": {
    "diaryId": "019a4c10-...",
    "name": "My Diary",
    "thumbnailKey": null,
    "price": 3000,
    "prices": { "KRW": 3000, "USD": 2.99 }
  },
  "coupons": [],
  "paymentMethods": [
    { "id": "CARD", "name": "Card", "currency": "KRW", "amount": 3000 },
    { "id": "PAYPAL", "name": "PayPal", "currency": "USD", "amount": 2.99 }
  ]
}
```

- 쿠폰은 미구현이라 항상 `[]`
- `item.price` 는 원화 가격 (하위 호환). 통화별 가격은 `item.prices`
- 외화 MID 키가 설정되지 않은 서버에서는 `PAYPAL` 항목과 `prices.USD` 가 빠진다 (7절)
- #59 전에는 결제수단 id 가 `TOSS` 였다. 이제 `CARD` | `PAYPAL`
- 이미 결제된 다이어리면 409 `PAYMENT_ALREADY_PAID`

## 2. 결제 생성

`POST /api/v1/diaries/{diaryId}/payments`

```json
{ "couponId": null, "refundConsent": true, "method": "CARD" }
```

`method` 는 `CARD` | `PAYPAL` (대소문자 무시). 생략하면 `CARD`.

응답 201 (CARD)

```json
{
  "orderId": "019a4c30-...",
  "orderNumber": "DIA-20261103-K3M9Q2X7PA",
  "finalAmount": 3000,
  "orderName": "Seoulaim Diary",
  "tossClientKey": "test_ck_...",
  "method": "CARD",
  "currency": "KRW",
  "amount": 3000,
  "clientKey": "test_ck_...",
  "variantKey": null,
  "customerKey": "019a1b2c-3d4e-7f00-8a9b-0c1d2e3f4a5b"
}
```

응답 201 (PAYPAL)

```json
{
  "orderId": "019a4c31-...",
  "orderNumber": "DIA-20261107-P7Q2W9E4RT",
  "finalAmount": 2.99,
  "orderName": "Seoulaim Diary",
  "tossClientKey": "test_ck_...",
  "method": "PAYPAL",
  "currency": "USD",
  "amount": 2.99,
  "clientKey": "test_gck_...",
  "variantKey": "PAYPAL",
  "customerKey": "019a1b2c-3d4e-7f00-8a9b-0c1d2e3f4a5b"
}
```

| 필드 | 의미 |
|---|---|
| `amount`, `finalAmount` | 결제 통화 단위 금액 (KRW 3000, USD 2.99). 둘은 같은 값 |
| `clientKey` | 결제수단의 상점(MID) 클라이언트 키. CARD 는 원화 MID, PAYPAL 은 외화 MID 결제위젯 키(`*_gck_`) |
| `tossClientKey` | 하위 호환. 결제수단과 관계없이 **원화 MID** 키. 새 코드는 `clientKey` 를 쓴다 |
| `variantKey` | 결제위젯 UI variantKey. PAYPAL 만 (`TOSS_PAYPAL_VARIANT_KEY`, 기본 `PAYPAL`) |
| `customerKey` | 토스 customerKey (= 회원 id UUID 문자열, 36자) |

| 오류 코드 | 상태 | 조건 |
|---|---|---|
| `PAYMENT_CONSENT_REQUIRED` | 400 | `refundConsent` 가 `true` 가 아님 (동의 시각을 `refund_consent_at` 에 저장) |
| `PAYMENT_ALREADY_PAID` | 409 | 이미 결제된 다이어리 |
| `PAYMENT_INVALID_METHOD` | 400 | `method` 가 `CARD`·`PAYPAL` 이 아님 |
| `PAYMENT_METHOD_UNAVAILABLE` | 400 | 외화 MID 키가 없는 서버에서 `PAYPAL` |
| `DIARY_NOT_FOUND` | 404 | 없음·남의 것·삭제됨 |

- `couponId` 는 받기만 하고 무시한다
- 같은 다이어리에 결제를 다시 만들면 이전 `PENDING` 결제는 `FAILED`(`failure_code=SUPERSEDED`) 로 바뀐다. 이전 결제로는 승인할 수 없다

## 3. 토스 SDK 호출

CARD (원화 결제창):

| SDK 파라미터 | 값 |
|---|---|
| clientKey | `clientKey` (= `tossClientKey`) |
| customerKey | `customerKey` |
| orderId | **`orderNumber`** (UUID `orderId` 가 아님) |
| amount | `{ currency: "KRW", value: amount }` |
| orderName | `orderName` (`Seoulaim Diary`) |

PAYPAL 은 결제위젯 v2 로 띄운다 (7절).

## 4. 결제 승인

경로의 `{orderId}` 는 2단계 응답의 UUID `orderId` 다.

`POST /api/v1/payments/{orderId}/confirm`

```json
{ "paymentKey": "tgen_20261103...", "amount": 3000 }
```

- `amount` 는 토스 성공 리다이렉트의 `amount` 를 그대로 보낸다. 숫자(`2.99`)·숫자 문자열(`"2.99"`) 모두 받는다
- 서버는 결제 통화의 소수 자릿수까지 비교한다 (USD: `2.99` = `2.990`, `2.999`·`3` 은 불일치. KRW: `3000` = `3000.00`)
- 승인은 결제를 만들 때 정해진 상점(MID)의 시크릿 키로 보낸다. 토스 응답의 결제수단(`method`, `easyPay.provider`)은 `method_detail` 에 `해외간편결제(PAYPAL)` 처럼 저장한다

| 응답 | 의미 | 웹앱 처리 |
|---|---|---|
| 200 `{ diaryId }` | 결제 완료. 다이어리 `paid=true` | 편집 화면으로 |
| 400 `PAYMENT_AMOUNT_MISMATCH` | 콜백 금액 ≠ 결제 금액. 승인하지 않았다 | 2단계부터 다시 |
| 402 `PAYMENT_FAILED` | 토스 승인 거절(`fields.code`, `fields.message`) 또는 이미 실패·대체된 결제 | 메시지 노출 후 2단계부터 다시 |
| 404 `ORDER_NOT_FOUND` | 없는 결제 또는 남의 결제 | - |
| 502 `PAYMENT_GATEWAY_ERROR` | 토스 통신 실패. 승인 여부 불확실 | 같은 요청으로 재시도 |

승인 요청은 **멱등**하다. 이미 `PAID` 인 결제에 다시 보내면 토스 호출 없이 200 을 돌려준다.

## 5. 결제 내역

`GET /api/v1/me/payments` — `PAID`·`REFUNDED` 결제, `paidAt` 최신순

```json
[
  {
    "id": "019a4c30-...",
    "diaryId": "019a4c10-...",
    "name": "My Diary",
    "thumbnailKey": null,
    "amount": 3000,
    "method": "CARD",
    "currency": "KRW",
    "status": "PAID",
    "paidAt": "2026-11-03T03:05:00Z",
    "refundable": true,
    "withinPolicy": false,
    "refundRequested": false
  }
]
```

| 필드 | 의미 |
|---|---|
| `name`, `thumbnailKey` | 다이어리가 삭제됐으면 `null` |
| `amount`, `method`, `currency` | 결제 통화 단위 금액 (PayPal 이면 `2.99`, `"PAYPAL"`, `"USD"`) |
| `refundable` | `PAID` 이고 결제 후 7일 이내이며 다이어리가 아직 완성(FINALIZED) 전이고, 처리 대기 중인 환불 요청이 없음 |
| `withinPolicy` | `refundable` 이고 결제 후 편집 이력(`POST /diaries/{id}/edited`)이 없음 |
| `refundRequested` | 처리 대기(REQUESTED) 중인 환불 문의가 있음 |

## 6. 환불

환불은 Help Center 의 환불 문의(`POST /inquiries`, `category: "refund"`, `paymentId`)로만 신청하고, 어드민이 승인하면 서버가 토스 결제를 취소한다. 흐름·API 는 [inquiry-flow.md](inquiry-flow.md) 참고.

취소는 결제한 상점(MID)으로 보내고 전액 취소만 한다. **PayPal 결제는 취소해도 토스 수수료가 돌려받아지지 않는다** (외화 MID 정산 조건).

## 7. PayPal (외화 MID) — #59

### 두 MID

토스페이먼츠는 MID(상점) 하나가 통화 하나만 받는다. 그래서 원화 카드와 PayPal(USD)을 **MID 두 개**로 운영한다.

| 결제수단 | 상점(`merchant`) | 통화 | 프론트 SDK | 서버 키 |
|---|---|---|---|---|
| `CARD` | `KRW` (기존 MID) | KRW | 결제창 | `TOSS_CLIENT_KEY` / `TOSS_SECRET_KEY` |
| `PAYPAL` | `FOREIGN` (외화 MID) | USD | 결제위젯 v2 | `TOSS_FOREIGN_CLIENT_KEY` / `TOSS_FOREIGN_SECRET_KEY` |

- 결제 행(`diary_payments`)에 `method`·`merchant`·`currency` 가 저장되고, 승인·취소는 그 행의 `merchant` 키로 보낸다. 승인·취소 API 경로(`/v1/payments/confirm`, `/v1/payments/{paymentKey}/cancel`)는 두 MID 가 같다
- 금액은 `numeric(12,2)` 통화 단위 (KRW `3000.00`, USD `2.99`). API 는 통화 소수 자릿수로 맞춰 내려준다 (KRW `3000`, USD `2.99`)

### 환경변수

| 이름 | 기본값 | 의미 |
|---|---|---|
| `TOSS_FOREIGN_CLIENT_KEY` | (빈 값) | 외화 MID 결제위젯 클라이언트 키 `*_gck_` |
| `TOSS_FOREIGN_SECRET_KEY` | (빈 값) | 외화 MID 결제위젯 시크릿 키 `*_gsk_` |
| `TOSS_PAYPAL_VARIANT_KEY` | `PAYPAL` | 결제위젯 PayPal UI 의 variantKey (상점관리자에서 만든 값) |
| `DIARY_PRICE_USD` | `2.99` | PayPal 결제 금액 (달러, 소수 2자리까지) |

외화 키 둘 중 하나라도 비어 있으면 PayPal 을 끈다: 체크아웃의 `paymentMethods`·`prices` 에서 빠지고, `method: "PAYPAL"` 결제 생성은 `400 PAYMENT_METHOD_UNAVAILABLE`. 이미 만들어진 PayPal 결제를 키 없이 승인하면 같은 400, 환불 승인은 `502 PAYMENT_REFUND_FAILED`(`fields.code: "MERCHANT_UNAVAILABLE"`).

### 결제위젯 v2 흐름 (웹앱)

```
1. POST /api/v1/diaries/{id}/payments { refundConsent: true, method: "PAYPAL" }
   → { orderId, orderNumber, amount: 2.99, currency: "USD", clientKey: "..._gck_...", variantKey: "PAYPAL", customerKey }
2. const tossPayments = TossPayments(clientKey)          // https://js.tosspayments.com/v2/standard
   const widgets = tossPayments.widgets({ customerKey })
   await widgets.setAmount({ currency: "USD", value: amount })
   await widgets.renderPaymentMethods({ selector: "#payment-method", variantKey })
   await widgets.requestPayment({
     orderId: orderNumber, orderName,
     successUrl: `${origin}/payment/success?orderId=${orderId}`,
     failUrl: `${origin}/payment/fail?orderId=${orderId}`,
   })
3. 성공 리다이렉트 ?paymentKey=...&orderId=<orderNumber>&amount=2.99
   → POST /api/v1/payments/{orderId(UUID)}/confirm { paymentKey, amount: "2.99" }
```

- 위젯 `orderId` 도 UUID 가 아니라 **`orderNumber`**
- 결제위젯 키(`*_gck_`)는 결제창 SDK(`payment()`)에 쓸 수 없고, 원화 키는 위젯에 쓸 수 없다. 수단별 `clientKey` 를 그대로 쓴다
- `variantKey` 는 토스 상점관리자의 결제 UI 설정과 같아야 한다

