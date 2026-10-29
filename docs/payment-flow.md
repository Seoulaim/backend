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

헤더: 모든 요청에 `X-User-Id: <UUID>` (auth 연동 전 임시. JWT 도입 시 `Authorization` 으로 교체되며 prod 에서는 401)

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
