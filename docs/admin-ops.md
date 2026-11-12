# 어드민 운영 가이드 — 결제·회원 관리 (#57)·대시보드 (#67)

라이브 결제 전환 전에 운영자가 결제를 토스 콘솔과 대조하고, 회원 이력을 보고, 필요하면 강퇴할 수 있게 하는 API.

- 전부 `X-Admin-Key` 헤더, 경로는 `/api/v1/admin` 하위. 키가 없거나 다르면 `403 AUTH_ACCESS_DENIED`
- 응답의 상태 값(`status`, `withdrawnBy`, 문의 `category`·`status`·`refundStatus`)은 **소문자**. 요청의 `status` 파라미터는 대소문자를 가리지 않는다
- 시각은 UTC ISO-8601 (`2026-11-04T03:00:00Z`), 금액은 원 단위 정수
- 페이지: `page` 0부터, `size` 1~200 (기본 50). 범위 밖이면 `400 COMMON_INVALID_REQUEST`
- 환불 심사(정책 판정·제작 이력·계정 이력)는 `docs/inquiry-flow.md` 3절

## 1. 결제 관리

다이어리 결제(`diary_payments`) 전체. 필드명은 어드민 mock(`src/api/payments.js`)을 따른다 (`orderId`, `member`, `provider`).

### 목록

`GET /api/v1/admin/payments?status=&q=&from=&to=&page=0&size=50`

| 파라미터 | 의미 |
|---|---|
| `status` | `pending` / `paid` / `failed` / `refunded`. 생략하면 전체 |
| `q` | 주문번호(`orderId`)·회원 이메일·닉네임 부분 일치 (대소문자 무시, `%` `_` 는 문자 그대로) |
| `from`, `to` | 결제 **생성일**(KST) `YYYY-MM-DD`, 양끝 포함. `from > to` 면 400 |

정렬: 생성 시각 최신순.

```json
{
  "items": [
    {
      "id": "019a4c30-...",
      "orderId": "DIA-20261104-ABCDE12345",
      "member": { "id": "019a...", "nickname": "Jin", "email": "jin@example.com" },
      "diary": { "id": "019a4c10-...", "name": "My Diary", "first": false, "pages": 4, "completedAt": null, "deleted": false },
      "amount": 3000,
      "currency": "KRW",
      "provider": "toss",
      "status": "paid",
      "createdAt": "2026-11-04T02:59:30Z",
      "paidAt": "2026-11-04T03:00:00Z",
      "refundedAt": null,
      "refundConsentAt": "2026-11-04T02:59:30Z",
      "editedAt": null,
      "refundRequested": true,
      "refundRequestId": "019a5a30-...",
      "method": "CARD",
      "methodDetail": "카드",
      "sameDiaryPayments": 0
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "summary": {
    "paidCount": 13,
    "paidAmount": 36000,
    "refundedCount": 1,
    "refundedAmount": 3000,
    "paidAmounts": { "KRW": 36000, "USD": 2.99 },
    "refundedAmounts": { "KRW": 3000 }
  }
}
```

| 필드 | 의미 |
|---|---|
| `orderId` | 토스 orderId (= 주문번호 `DIA-yyyyMMdd-10자리`). 토스 콘솔 검색에 쓴다 |
| `member.email` | 탈퇴 회원은 `null` |
| `diary` | 다이어리를 못 찾으면 `null`. `deleted` 는 soft delete (환불·탈퇴) |
| `status` | 결제 상태 그대로. 환불 요청 중인지는 `refundRequested` 로 본다 (mock 의 `refund-requested` 상태 대신) |
| `editedAt` | 결제 후 첫 편집 시각 (환불 정책 판단용) |
| `refundRequested` / `refundRequestId` | 처리 대기 환불 요청 여부 / 가장 최근 환불 요청(문의) id (`GET /admin/refund-requests/{id}`) |
| `amount`, `currency` | 결제 통화 단위 금액 (KRW `3000`, USD `2.99`) |
| `method` | 회원이 고른 결제수단 `CARD` / `PAYPAL` (#59 이전 결제는 `CARD`) |
| `methodDetail` | 토스 승인 응답의 결제수단 (`카드`, `간편결제(토스페이)`, `해외간편결제(PAYPAL)` 등). #57 이전 결제·미승인 결제는 `null`. #57~#59 사이에는 이 값이 `method` 로 나갔다 |
| `sameDiaryPayments` | 이 결제가 `paid` 일 때 같은 다이어리의 다른 `paid` 결제 수. 1 이상이면 **중복 결제 의심** (환불 정책 제5조 1호) |
| `summary` | `q`·`from`·`to` 를 적용한 전체 합계. **`status` 필터는 무시**한다 (상태 탭을 바꿔도 같은 합계). `paidCount`·`paidAmount` 는 지금 `paid` 인 것만 (환불된 결제는 `refunded*`). 건수는 모든 통화, **`paidAmount`·`refundedAmount` 는 원화만** (통화가 섞이면 더할 수 없어 하위 호환용으로 원화 합계). 통화별 합계는 `paidAmounts`·`refundedAmounts` (결제가 없는 통화는 빠진다) |

### 상세

`GET /api/v1/admin/payments/{id}` → 목록 항목과 같은 필드(펼친 형태) +

```json
{
  "id": "019a4c30-...",
  "orderId": "DIA-20261104-ABCDE12345",
  "...": "목록 항목 필드",
  "paymentKey": "tgen_20261104...",
  "tossCancelKey": null,
  "failureCode": null,
  "failureMessage": null,
  "refundRequest": { "inquiryId": "019a5a30-...", "status": "requested" }
}
```

- `paymentKey`: 토스 paymentKey (승인 전 `null`). `tossCancelKey`: 환불 시 토스 취소 거래 키
- `failureCode`/`failureMessage`: 승인 실패·대체(`SUPERSEDED`)된 결제의 사유
- `refundRequest`: 가장 최근 환불 요청 `{ inquiryId, status: requested|refunded|rejected }`, 없으면 `null`
- 없는 결제 `404 PAYMENT_NOT_FOUND`

## 2. 회원 관리

### 목록

`GET /api/v1/admin/users?q=&status=&page=0&size=50`

| 파라미터 | 의미 |
|---|---|
| `q` | 이메일(마스킹 전 값)·닉네임 부분 일치 (대소문자 무시). 탈퇴 회원은 이메일이 지워져 닉네임으로만 찾는다 |
| `status` | `active` / `suspended` / `withdrawn`. 생략하면 전체 |

정렬: 가입 최신순.

```json
{
  "items": [
    {
      "id": "019a...",
      "nickname": "Mina",
      "emailMasked": "mi**@example.com",
      "language": "en",
      "joinedAt": "2026-10-20T08:00:00Z",
      "diaryCount": 2,
      "paidPaymentCount": 1,
      "status": "active"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1
}
```

- `emailMasked`: 어드민 `lib/privacy.js` `maskEmail` 과 같은 규칙. 로컬파트 1~2자 → 첫 1자 + `**` (`a**@`), 3자 이상 → 앞 2자 + 나머지 길이만큼 `*`(최소 2개, `ab***@`). 탈퇴 회원은 `null`
- `diaryCount`: 삭제되지 않았고 무료이거나 결제된 다이어리 수. `paidPaymentCount`: 지금 `paid` 인 결제 수 (환불 제외)
- `lastLoginAt` 은 없다 (users 에 로그인 시각 컬럼이 없음)

### 상세

`GET /api/v1/admin/users/{id}` — 이메일 전체를 보여준다. 탈퇴 회원도 조회된다. 없으면 `404 USER_NOT_FOUND`.

```json
{
  "id": "019a...",
  "nickname": "Mina",
  "email": "mina@example.com",
  "profileImageKey": null,
  "language": "en",
  "joinedAt": "2026-10-20T08:00:00Z",
  "status": "active",
  "withdrawnAt": null,
  "withdrawnBy": null,
  "withdrawReason": null,
  "marketingConsent": true,
  "marketingUpdatedAt": "2026-10-25T01:00:00Z",
  "pushEnabled": true,
  "biases": [
    {
      "id": "019a...", "nameEn": "SAN", "nameKo": "산", "nameJa": null, "nameZh": null, "imageKey": null,
      "artistGroup": { "id": "019a...", "nameEn": "ATEEZ", "nameKo": "에이티즈" }
    }
  ],
  "diaries": [
    {
      "id": "019a4c10-...", "name": "My Diary", "free": false, "paid": true, "finalized": true,
      "createdAt": "2026-11-04T02:50:00Z", "lastSavedAt": "2026-11-05T00:00:00Z", "saveCount": 7,
      "finalizedAt": "2026-11-05T00:00:00Z", "downloadCount": 1, "deleted": false
    }
  ],
  "payments": [
    {
      "id": "019a4c30-...", "orderNumber": "DIA-20261104-ABCDE12345", "diaryId": "019a4c10-...",
      "amount": 3000, "method": "CARD", "currency": "KRW", "status": "paid", "paidAt": "2026-11-04T03:00:00Z", "refundedAt": null
    }
  ],
  "inquiries": [
    {
      "id": "019a5a30-...", "category": "refund", "title": "Refund for my second diary",
      "status": "received", "createdAt": "2026-11-05T03:00:00Z", "refundStatus": "requested"
    }
  ],
  "stats": { "refundedCount": 0, "rejectedRefundCount": 0, "pendingInquiryCount": 1 }
}
```

| 필드 | 의미 |
|---|---|
| `withdrawnBy` | `admin`(강퇴) / `self`(본인 탈퇴) / 탈퇴 전 `null`. #57 이전 탈퇴는 `self` |
| `withdrawReason` | 강퇴 사유. 본인 탈퇴는 `null` |
| `marketingConsent` / `marketingUpdatedAt` | 마케팅 수신 동의 / 마지막 변경 시각 (가입 후 안 바꿨으면 `null`). users 에 있는 동의 관련 값은 이것과 `pushEnabled` 뿐 (약관·개인정보·연령 동의 컬럼 없음) |
| `biases` | 현재 최애 멤버(그룹 포함). 탈퇴하면 삭제되어 `[]` |
| `diaries` | 삭제·미결제 다이어리 포함 최신순 |
| `payments` | `paid`·`refunded` 결제 최신순 (대기·실패는 결제 관리에서) |
| `inquiries` | 환불 포함 모든 문의 최신순. `refundStatus` 는 환불 문의만 |

### 강퇴

`POST /api/v1/admin/users/{id}/withdraw`

```json
{ "reason": "환불 악용 반복 (이용약관 제5조 3항)" }
```

→ 204

- 회원 본인 탈퇴(`DELETE /users/me`)와 **같은 처리**: 최애·장소 즐겨찾기·일정 삭제, 다이어리 soft delete, 결제 기록 유지, 설문 응답 이메일 익명화, 이메일·비밀번호 삭제(같은 이메일로 재가입 가능), **refresh token 전부 폐기** → 다음 토큰 재발급부터 `401 AUTH_INVALID_TOKEN`, 로그인은 `401 AUTH_INVALID_CREDENTIALS`
- 이미 발급된 access token 은 만료 시까지 남는다 (프로필·설정 API 는 탈퇴 회원에게 `404 USER_NOT_FOUND`)
- `users.withdrawn_by = ADMIN`, `withdraw_reason` 저장
- `reason` 1~200자(앞뒤 공백 제거) 아니면 `400 COMMON_INVALID_REQUEST`. 없는 회원 `404 USER_NOT_FOUND`, 이미 탈퇴 `409 USER_ALREADY_WITHDRAWN`
- 강퇴 안내 메일은 보내지 않는다 (아래 "아직 없는 것")

## 3. 장소 관리

- `PATCH /admin/places/{id}/visible { visible }` → 어드민 장소 상세. 노출 여부만 바꾸고 다른 필드는 검증하지 않는다 (전체 `PUT` 은 `summary.en`·`address.en` 필수라 노출 토글에 쓰지 않는다). 없는 장소 `404 PLACE_NOT_FOUND`

## 4. 대시보드 (#67)

`GET /admin/dashboard?range=7d|30d|90d` (기본 `7d`, 대소문자 무시. 그 밖의 값 `400 COMMON_INVALID_REQUEST`, `fields.range`)

- 날짜 경계는 전부 **KST(Asia/Seoul) 자정**. `range=Nd` = 오늘 포함 N일 `[오늘-(N-1) 00:00 KST, 내일 00:00 KST)`. 일별 배열은 빈 날도 0 으로 채우고 오래된 순
- 앞의 6개 필드(`signups`, `payments`, `daily`, `pendingInquiries`, `pendingRefunds`, `survey`)는 **어드민 mock(`api/dashboard.js`) 그대로** — 화면 코드를 바꾸지 않아도 된다. 나머지는 기간 지표 추가분
- 금액은 통화 단위 숫자 (KRW 원 정수, USD 소수 2자리). 통화별 맵은 결제가 없어도 `{ "KRW": 0, "USD": 0.00 }` 두 키를 항상 내려준다
- 다른 통화끼리 더하지 않는다. mock 호환 필드 `payments.*.revenue` 는 **원화만** (결제 관리 `summary.paidAmount` 와 같은 규칙), 통화별은 `revenues`

### 응답 예시

```json
{
  "signups": { "today": 4, "yesterday": 6 },
  "payments": {
    "thisMonth": { "paymentCount": 21, "revenue": 45000, "revenues": { "KRW": 45000, "USD": 17.94 } },
    "lastMonth": { "paymentCount": 30, "revenue": 72000, "revenues": { "KRW": 72000, "USD": 17.94 } }
  },
  "daily": [
    { "date": "2026-10-30", "signups": 3, "payments": 1 },
    "… 14개, 마지막이 오늘 …",
    { "date": "2026-11-12", "signups": 4, "payments": 2 }
  ],
  "pendingInquiries": [
    { "id": "019a…", "category": "diary", "title": "Can't save my diary", "nickname": "Mina", "createdAt": "2026-11-12T01:20:00Z" }
  ],
  "pendingRefunds": [
    { "id": "019a…", "title": "Refund for my second diary", "nickname": "Jin", "amount": 2.99, "currency": "USD",
      "createdAt": "2026-11-10T09:00:00Z", "deadline": "2026-11-13" }
  ],
  "survey": { "averageRating": 4.333333333333333, "responsesThisWeek": 5, "responsesInRange": 5 },
  "range": "7d",
  "from": "2026-11-06",
  "to": "2026-11-12",
  "members": {
    "total": 1284, "newInRange": 31, "withdrawnInRange": 2,
    "daily": [{ "date": "2026-11-06", "signups": 5 }, "…", { "date": "2026-11-12", "signups": 4 }]
  },
  "diaries": { "total": 1502, "createdInRange": 40, "finalized": 611, "finalizedInRange": 18, "paid": 233 },
  "sales": {
    "paidCount": 12, "paidAmounts": { "KRW": 27000, "USD": 8.97 },
    "refundedCount": 1, "refundedAmounts": { "KRW": 0, "USD": 2.99 },
    "methodCounts": { "CARD": 9, "PAYPAL": 3 },
    "daily": [
      { "date": "2026-11-12", "paidCount": 2, "paidAmounts": { "KRW": 3000, "USD": 2.99 },
        "refundedCount": 0, "refundedAmounts": { "KRW": 0, "USD": 0.00 } }
    ]
  },
  "pending": { "refundRequests": 1, "refundsDueSoon": 1, "refundsOverdue": 0, "inquiries": 1 },
  "recent": {
    "payments": ["결제 관리 목록 items 와 같은 항목 × 최대 5"],
    "inquiries": ["GET /admin/inquiries 의 inquiries 항목 × 최대 5"],
    "refundRequests": ["GET /admin/refund-requests 항목 × 최대 5"]
  }
}
```

### 필드 기준

| 필드 | 기준 |
|---|---|
| `signups.today` / `yesterday` | KST 오늘·어제 가입 수 (이후 탈퇴한 회원 포함) |
| `payments.thisMonth` / `lastMonth` | KST 달력 월. `paymentCount` = 그 달에 **승인**된 결제 수(이후 환불된 것 포함, 모든 통화) |
| `daily` | 최근 14일. `payments` = 그날 승인된 결제 수 |
| `pendingInquiries` | 답변 대기(RECEIVED) 문의 **전체**, 환불 문의 제외, 최신순 (화면이 `length` 로 건수를 센다) |
| `pendingRefunds` | 처리 대기(REQUESTED) 환불 **전체**, 최신순. mock 에 없는 `currency`·`deadline`(신청일 + 영업일 3일) 추가 |
| `survey.averageRating` | 숨기지 않은 첫 별점 질문의 **전체 기간** 평균 (어드민 `headlineRating` 과 같음, 답하지 않은 응답 제외). 질문·응답이 없으면 `null`. 반올림 안 함 |
| `survey.responsesThisWeek` | 최근 7×24시간 (mock 과 같은 기준, KST 날짜 아님) |
| `survey.responsesInRange` | 기간 내 응답 수 |
| `members.total` | 탈퇴하지 않은 회원 (정지 포함) |
| `members.newInRange` / `withdrawnInRange` | 기간 내 가입 / 탈퇴(본인·어드민 강퇴 모두, `withdrawn_at` 기준) |
| `diaries.total` | 회원 앱 목록에 보이는 다이어리 = 삭제 안 됐고 무료이거나 결제됨 |
| `diaries.createdInRange` | 기간 내 생성 + 무료이거나 결제됨 (이후 삭제된 것 포함, 결제 안 한 빈 다이어리 제외) |
| `diaries.finalized` / `finalizedInRange` | 보이는 것 중 완성 / 기간 내 완성(`completed_at`, 이후 삭제 포함) |
| `diaries.paid` | 보이는 것 중 결제된 다이어리 (환불되면 빠진다) |
| `sales.paid*`, `methodCounts` | 기간 내 **승인 시각(`paid_at`)** 기준. 나중에 환불된 결제도 포함 |
| `sales.refunded*` | 기간 내 **환불 시각(`refunded_at`)** 기준. 같은 결제가 승인·환불 양쪽에 잡힐 수 있다 (순매출 = paid − refunded) |
| `pending.refundsDueSoon` / `refundsOverdue` | 처리 대기 환불 중 기한이 오늘·내일 / 이미 지남 (KST 날짜) |
| `pending.inquiries` | `pendingInquiries` 건수 |
| `recent.*` | 최근 생성 5건씩 (상태 무관). 결제는 PENDING·FAILED 포함, 문의는 환불 제외, 환불 요청은 신청 최신순 |

## 에러 코드

| code | HTTP | 언제 |
|---|---|---|
| `COMMON_INVALID_REQUEST` | 400 | 잘못된 `status`·날짜 형식·`from > to`·page/size 범위, 강퇴 사유 검증 실패, 대시보드 `range` 값 |
| `COMMON_NOT_FOUND` | 404 | 등록되지 않은 경로 (전 API 공통) |
| `COMMON_METHOD_NOT_ALLOWED` | 405 | 경로는 있지만 지원하지 않는 메서드 (`Allow` 헤더에 지원 메서드) |
| `PAYMENT_NOT_FOUND` | 404 | 없는 결제 |
| `USER_NOT_FOUND` | 404 | 없는 회원 |
| `USER_ALREADY_WITHDRAWN` | 409 | 이미 탈퇴한 회원 강퇴 |
| `AUTH_ACCESS_DENIED` | 403 | `X-Admin-Key` 없음·불일치 |

## 아직 없는 것

- 회원 상세 열람 로그(개인정보처리방침 제11조 4호, mock 의 `views`), 강퇴 기록 목록(mock `getWithdrawals`), 강퇴 전 확인(`getWithdrawalChecks`), 강퇴 안내 메일
- `lastLoginAt` (로그인 시각 미저장), 약관·개인정보·연령 동의 시각 (users 에 컬럼 없음)
- 회원 정지(SUSPENDED) 처리 API
