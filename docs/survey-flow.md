# 플랜 만족도 조사 연동 가이드 (회원 앱·어드민)

회원 앱 TRAVEL 탭에서 초대 팝업을 띄우고, 예를 누르면 설문 화면으로 이동한다. 응답 1건마다 아메리카노 기프티콘 1장을 운영자가 계정 이메일로 직접 보내고 어드민에서 "발송 완료"로 표시한다. **언제 띄울지는 서버가 판단한다.** 앱은 방문·팝업 이벤트만 보낸다.

- 회원 API: `Authorization: Bearer <access token>`, 경로는 `/api/v1` 하위
- 어드민 API: `X-Admin-Key` 헤더, 경로는 `/api/v1/admin/survey` 하위
- 다국어 문구(초대 제목·본문, 질문 제목, 보기 문구)는 전부 `{ "en": "...", "ko": "...", "zh": "...", "ja": "..." }`. **en 필수**, 나머지는 선택. 앱이 표시 언어를 고르고 없으면 en 으로 대체한다
- enum 값은 응답에서 대문자(`RATING`, `PENDING` …)로 내려간다. 요청의 질문 `type`, 이벤트 `type` 은 대소문자를 가리지 않는다 (`rating`, `shown` 도 됨)

## 흐름

```
회원 앱                                               API 서버
 │ 1. (다른 화면 → TRAVEL 진입) POST /me/travel-visits ─▶ { surveyInvite: {title, body} | null }
 │ 2. 팝업을 띄우면 POST /me/plan-survey/events {type: SHOWN}
 │    X → CLOSED / 예 → ACCEPTED / 오늘 하루 보지 않기 → SNOOZED     ─▶ 204
 │ 3. (예) GET /me/plan-survey ──────────────────────▶ { invite, questions, email, answered }
 │ 4. POST /me/plan-survey { answers } ──────────────▶ 204

어드민
 │ GET  /admin/survey ─────────────────────────────▶ 설정 + 질문 + 질문별 응답 수
 │ PUT  /admin/survey ─────────────────────────────▶ 문구·질문 전체 저장
 │ PATCH /admin/survey/enabled ────────────────────▶ 팝업 켜기/끄기
 │ GET  /admin/survey/responses?from&to ───────────▶ 응답 목록 + 팝업 통계
 │ POST /admin/survey/responses/gifts-sent ────────▶ 기프티콘 발송 완료 표시
```

## 1. 초대 팝업 (회원)

### 언제 띄우나 — 서버 판단, 아래를 전부 만족할 때

1. 어드민에서 팝업이 켜져 있다 (`enabled`)
2. 회원에게 플랜이 1개 이상 있다
3. 플랜이 있는 상태에서 TRAVEL 방문이 **2회 이상** (플랜을 만든 뒤 두 번째 방문부터)
4. 아직 응답하지 않았다 (1인 1응답)
5. "오늘 하루 보지 않기" 상태가 아니다 (누른 날 **한국 시간 자정**까지)

닫기(X)나 예를 누른 뒤 제출하지 않고 나간 경우는 막지 않는다 → 다음 TRAVEL 방문 때 다시 뜬다.

### TRAVEL 방문 기록

`POST /api/v1/me/travel-visits` (바디 없음)

```json
{ "surveyInvite": { "title": { "en": "How was your plan?", "ko": "플랜은 어떠셨나요?" },
                    "body":  { "en": "Answer a short survey and get an Americano gift card." } } }
```

띄우지 않을 때는 `{ "surveyInvite": null }`.

- **다른 화면(HOME, DIARY, 메뉴 등)에서 TRAVEL 로 들어올 때만** 호출한다. 장소 상세(HT07)·지도(HT09)·설문 화면에서 돌아오는 것은 방문이 아니다
- 플랜이 없는 회원이 호출해도 방문 수는 늘지 않는다 (`surveyInvite: null`)

### 팝업 이벤트

`POST /api/v1/me/plan-survey/events` `{ "type": "SHOWN" }` → 204

| type | 언제 | 서버 |
|---|---|---|
| `SHOWN` | 팝업이 화면에 떴을 때 | 노출 수 +1 |
| `ACCEPTED` | 예 | 수락 수 +1 |
| `CLOSED` | X | 닫기 수 +1 |
| `SNOOZED` | 오늘 하루 보지 않기 | 다음 KST 자정까지 초대 중지, 스누즈 수 +1 |

모르는 type 은 400 `COMMON_INVALID_REQUEST`.

## 2. 설문 화면 (회원)

### 조회

`GET /api/v1/me/plan-survey`

```json
{
  "invite": { "title": { "en": "How was your plan?" }, "body": { "en": "..." } },
  "questions": [
    { "id": "0193…a1", "type": "RATING", "title": { "en": "How happy are you with your plan?" }, "required": true, "options": [] },
    { "id": "0193…a2", "type": "SINGLE", "title": { "en": "What did you like most?" }, "required": false,
      "options": [ { "id": "0193…b1", "label": { "en": "The places" } }, { "id": "0193…b2", "label": { "en": "The route" } } ] },
    { "id": "0193…a3", "type": "MULTIPLE", "title": { "en": "What could be better?" }, "required": false, "options": [ … ] },
    { "id": "0193…a4", "type": "TEXT", "title": { "en": "Anything else?" }, "required": false, "options": [] }
  ],
  "email": "fan@example.com",
  "answered": false
}
```

- 숨긴(archived) 질문은 빠지고 순서대로 온다. `email` 은 기프티콘을 받을 계정 이메일
- 플랜이 없으면 404 `PLAN_NOT_FOUND`

### 제출

`POST /api/v1/me/plan-survey` → 204

```json
{ "answers": {
    "0193…a1": 5,
    "0193…a2": "0193…b2",
    "0193…a3": ["0193…c1", "0193…c3"],
    "0193…a4": "Loved the cafe!"
} }
```

| type | 값 |
|---|---|
| `RATING` | 1~5 정수 |
| `SINGLE` | 그 질문의 보기 id |
| `MULTIPLE` | 그 질문의 보기 id 배열 (중복 불가) |
| `TEXT` | 문자열 1000자 이하 (앞뒤 공백 제거) |

- 답하지 않은 질문은 빼고 보낸다. `null`, 빈 문자열, 빈 배열도 "답하지 않음"으로 보고 저장하지 않는다
- 필수 질문 누락, 형식 불일치, 없는·숨긴 질문 id → 400 `SURVEY_INVALID` (`fields` 에 `{ 질문id: 이유 }`)
- 이미 응답 → 409 `SURVEY_ALREADY_ANSWERED`. 플랜 없음 → 404 `PLAN_NOT_FOUND`
- 서버는 응답과 함께 **그 시점의 최신 플랜 요약**(`planId, startDate, endDate, dayCount, placeCount`)과 **계정 이메일**을 저장한다

## 3. 어드민

### 설정 조회

`GET /api/v1/admin/survey`

```json
{
  "enabled": true,
  "invite": { "title": { "en": "How was your plan?" }, "body": { "en": "..." } },
  "questions": [
    { "id": "0193…a1", "type": "RATING", "title": { "en": "..." }, "required": true, "archived": false, "options": [] }
  ],
  "updatedAt": "2026-11-04T03:00:00Z",
  "answerCounts": { "0193…a1": 24 }
}
```

`questions` 는 숨긴 질문 포함, 순서대로. `answerCounts` 는 질문마다 (응답 없으면 0).

### 저장 (전체 교체)

`PUT /api/v1/admin/survey` → 위와 같은 형태

```json
{
  "invite": { "title": { "en": "How was your plan?", "ko": "플랜은 어떠셨나요?" }, "body": { "en": "..." } },
  "questions": [
    { "id": "0193…a1", "type": "RATING", "title": { "en": "..." }, "required": true, "archived": false, "options": [] },
    { "type": "SINGLE", "title": { "en": "New question" }, "required": false, "archived": false,
      "options": [ { "label": { "en": "Yes" } }, { "label": { "en": "No" } } ] }
  ]
}
```

- 목록 순서 = 노출 순서. `id` 없는 질문·보기는 새로 만든다. 질문·보기 id 는 바뀌지 않는다
- 요청에서 빠진 기존 질문: 응답이 없으면 삭제, **있으면 409 `SURVEY_QUESTION_HAS_ANSWERS`** → `archived: true` 로 숨길 것
- 응답이 있는 질문의 `type` 변경, 응답에 쓰인 보기 삭제도 409 `SURVEY_QUESTION_HAS_ANSWERS`
- 400 `SURVEY_INVALID`: 초대 제목·본문, 질문 제목, 보기 문구의 en 이 비어 있음 / SINGLE·MULTIPLE 보기 2개 미만 / 모르는 type / 다른 질문의 보기 id / id 중복
- 없는 질문 id → 404 `SURVEY_QUESTION_NOT_FOUND`
- RATING·TEXT 의 `options` 는 무시한다 (저장 안 함)

### 팝업 켜기/끄기

`PATCH /api/v1/admin/survey/enabled` `{ "enabled": false }` → `{ "enabled": false }`

끄면 회원 앱에 바로 초대가 뜨지 않는다. 응답·결과는 그대로 남는다.

### 결과

`GET /api/v1/admin/survey/responses?from=2026-11-01&to=2026-11-30` (KST 날짜, 둘 다 선택·포함)

```json
{
  "questions": [ … 숨긴 질문 포함 … ],
  "responses": [
    { "id": "0193…r1", "userId": "0193…u1", "email": "fan@example.com", "submittedAt": "2026-11-04T03:00:00Z",
      "answers": { "0193…a1": 5, "0193…a2": "0193…b2" },
      "plan": { "planId": "0193…p1", "startDate": "2026-12-01", "endDate": "2026-12-05", "dayCount": 2, "placeCount": 9 },
      "gift": { "status": "PENDING", "sentAt": null } }
  ],
  "popup": { "planCreators": 120, "responded": 24, "shown": 83, "closed": 33, "accepted": 30, "snoozed": 14 }
}
```

- `responses` 는 기간 내 최신순. `popup` 은 기간과 무관한 전체 누적
- `planCreators`: 플랜이 있는 회원 수. `snoozed`: "오늘 하루 보지 않기" 누적 횟수
- 날짜 형식 오류, `from` > `to` → 400 `COMMON_INVALID_REQUEST`

### 기프티콘 발송 완료 표시

`POST /api/v1/admin/survey/responses/gifts-sent` `{ "responseIds": ["0193…r1", "0193…r2"] }`
→ 이번에 `SENT` 로 바뀐 응답 배열 (위 `responses` 항목 형태). 이미 `SENT` 이거나 없는 id 는 건너뛴다.

## 에러 코드

| 코드 | HTTP | 언제 |
|---|---|---|
| `SURVEY_INVALID` | 400 | 응답 검증 실패, 설문 저장 검증 실패 |
| `SURVEY_ALREADY_ANSWERED` | 409 | 이미 응답함 |
| `SURVEY_QUESTION_HAS_ANSWERS` | 409 | 응답이 있는 질문 삭제·형식 변경, 응답에 쓰인 보기 삭제 |
| `SURVEY_QUESTION_NOT_FOUND` | 404 | 설문 저장 시 없는 질문 id |
| `PLAN_NOT_FOUND` | 404 | 플랜 없이 설문 조회·제출 |

## 서버 메모

- `survey_settings` 는 행 1개만 둔다. 앱 기동 시 없으면 꺼진 상태로 만든다 (`uk_survey_settings_singleton`)
- 팝업 카운트는 `survey_popup_states` 에 upsert 로 원자적으로 누적한다
- 응답은 개인정보처리방침에 따라 1년 보관 (삭제 배치는 아직 없음)
