# 홈 콘텐츠 연동 가이드 (회원 앱·어드민)

회원 앱 홈(HH01)의 메인 배너·Event coming up·팬캘린더·생일카페와 다이어리 탭(DD01) 배너를 내려준다. **무엇을 보여줄지(기간·최애 필터)는 서버가 판단한다.** 앱은 받은 순서대로 그리기만 한다.

- 회원 API: 경로는 `/api/v1` 하위, **전부 공개 GET**. `Authorization: Bearer <access token>` 이 있으면 최애 그룹으로 거르고, 없으면 전부 내려준다
- 어드민 API: `X-Admin-Key` 헤더, 경로는 `/api/v1/admin` 하위
- 다국어 문구(`title`, `venue`, `description`, `ctaLabel`)는 `{ "en": "...", "ko": "...", "zh": "...", "ja": "..." }`. **title·venue 의 en 필수**, 나머지 언어는 선택. 빈 문자열은 저장하지 않는다
- 날짜는 `YYYY-MM-DD`(한국 날짜). "오늘"은 서버가 **KST** 로 계산하고, 종료일 당일(23:59)까지 노출한다
- 이미지는 S3 key(`imageKey`)로 내려간다. 업로드는 어드민 presigned(`domain: "CONTENT"`)

## 흐름

```
어드민                                                 API 서버
 │ POST /admin/files/presigned-upload {domain: CONTENT} ─▶ { fileId, uploadUrl }  → S3 PUT
 │ POST/PUT /admin/calendar-entries  { …, imageFileId } ─▶ 팬캘린더·Event coming up
 │ POST/PUT /admin/birthday-cafes    { …, imageFileId } ─▶ 생일카페
 │ POST/PUT /admin/banners           { …, imageFileId } ─▶ 홈·다이어리 배너
 │ PATCH /admin/banners/{id}/enabled, PUT /admin/banners/reorder

회원 앱 (토큰 선택)
 │ GET /home/promos ─────────────────▶ 홈 메인 배너
 │ GET /home/events ─────────────────▶ Event coming up (최대 10)
 │ GET /home/birthday-cafes ─────────▶ 생일카페 (최대 10)
 │ GET /home/fan-schedules?from&to ──▶ 팬캘린더
 │ GET /diary-banners ───────────────▶ 다이어리 배너
```

## 1. 토큰과 최애 필터

| 요청 | 동작 |
|---|---|
| Authorization 헤더 없음 | 비로그인 → 필터 없이 전부 |
| 유효한 토큰 | 그 회원의 **최애 그룹**(최애 멤버들이 속한 그룹)으로 거름. 최애 0명이면 전부 |
| 토큰이 있는데 만료·위조 | **401** (`AUTH_EXPIRED_TOKEN` / `AUTH_INVALID_TOKEN`). 앱은 토큰을 갱신해 다시 요청하고, 갱신이 안 되면 헤더 없이 다시 요청 |

배너(`/home/promos`, `/diary-banners`)는 최애와 상관없지만, 토큰을 보내도 된다(검사하지 않음).

### 거르는 규칙

| 목록 | 후보 | 최애 그룹과 맞는 것 | 맞는 게 없을 때 |
|---|---|---|---|
| Event coming up | 사진 있고 종료일 ≥ 오늘 | 대상 그룹이 최애 그룹이거나 `all` | 후보 **전체** |
| 생일카페 | 종료일 ≥ 오늘 | 생일 멤버의 그룹이 최애 그룹 | 후보 **전체** |
| 팬캘린더 | `inCalendar` 이고 시작일이 from~to 안 | 대상 그룹이 최애 그룹이거나 `all` | **대체 없음** (`all` 만, 그것도 없으면 빈 목록) |

- "맞는 게 없을 때"의 기준은 **최애 그룹 전용 항목이 없는 것**이다. `all` 항목만 맞는 경우도 없음으로 보고 전체를 보여준다 (예: ILLIT 팬 → ILLIT 이벤트가 없으면 전체 카드)
- Event coming up·생일카페는 거른 뒤 앞에서 10개, 어드민 순서. 팬캘린더는 시작일순

## 2. 회원 API

### 홈 메인 배너 `GET /home/promos`

기본 배너(항상) + 켜져 있고 오늘이 기간 안인 배너, 어드민 순서.

```json
[
  { "id": "01a1a938-4680-7000-8000-000000000001",
    "title": { "en": "Seoulaim" }, "description": {},
    "imageKey": null, "targetType": "DIARY", "targetUrl": null },
  { "id": "019a…", "title": { "en": "ANITEEZ IN DEJAVU" },
    "description": { "en": "ATEEZ pop-up store in Seoul" },
    "imageKey": "content/2026/11/….jpg", "targetType": "URL",
    "targetUrl": "https://example.com/aniteez-in-dejavu" }
]
```

- `targetType`: `DIARY`(앱의 다이어리 화면으로 이동) | `URL`(`targetUrl` 열기)
- 마이그레이션이 만든 기본 배너는 이미지가 없다(`imageKey: null`). 어드민에서 이미지를 올려 두거나, 앱이 null 일 때 기본 그림을 쓴다

### 다이어리 배너 `GET /diary-banners`

홈 배너와 같은 규칙, `ctaLabel` 추가.

```json
[{ "id": "…", "title": { "en": "Make every trip\nmore special" },
   "description": { "en": "Record your travels…" }, "ctaLabel": { "en": "Discover now" },
   "imageKey": "content/2026/11/….png", "targetType": "DIARY", "targetUrl": null }]
```

### Event coming up `GET /home/events`

```json
[{ "id": "…", "title": { "en": "ATEEZ 2026 WORLD TOUR", "ko": "에이티즈 2026 월드투어" },
   "startDate": "2026-11-07", "endDate": "2026-11-09",
   "venue": { "en": "KSPO DOME" }, "imageKey": "content/2026/11/….jpg",
   "targetType": "URL", "targetUrl": "https://example.com/ateez-tour" }]
```

### 생일카페 `GET /home/birthday-cafes`

```json
[{ "id": "…", "startDate": "2026-11-06", "endDate": "2026-11-08",
   "venue": { "en": "Cafe Lettering Seoul", "ko": "카페 레터링 서울" },
   "imageKey": "content/2026/11/….jpg", "linkUrl": "https://example.com/birthday-cafe-1" }]
```

### 팬캘린더 `GET /home/fan-schedules?from=2026-11-01&to=2026-11-30`

`from`·`to` 필수(포함). `to < from` 이거나 기간이 366일 이상이면 400 `COMMON_INVALID_REQUEST`.

```json
[{ "id": "…", "date": "2026-11-07", "title": { "en": "ATEEZ 2026 WORLD TOUR" }, "color": "purple" }]
```

`color`: `pink | purple | blue | orange | gray | green` (소문자). `date` = 항목의 시작일.

## 3. 어드민 API

### 캘린더 항목 (팬캘린더 + Event coming up) `/admin/calendar-entries`

| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/calendar-entries` | 전체, 어드민 순서 |
| GET | `/calendar-entries/{id}` | 상세. 없으면 404 `CONTENT_NOT_FOUND` |
| POST | `/calendar-entries` | 201. 맨 뒤 순서로 추가 |
| PUT | `/calendar-entries/{id}` | 200 |
| DELETE | `/calendar-entries/{id}` | 204 |

요청

```json
{ "groupIds": ["019a…ateez"], "title": { "en": "ATEEZ 2026 WORLD TOUR" },
  "startDate": "2026-11-07", "inCalendar": true, "color": "purple",
  "imageFileId": "019a…", "endDate": "2026-11-09",
  "venue": { "en": "KSPO DOME" }, "targetUrl": "https://example.com/ateez-tour" }
```

응답

```json
{ "id": "…", "groupIds": ["019a…ateez"], "title": { "en": "ATEEZ 2026 WORLD TOUR" },
  "startDate": "2026-11-07", "inCalendar": true, "color": "purple",
  "imageKey": "content/2026/11/….jpg", "endDate": "2026-11-09",
  "venue": { "en": "KSPO DOME" }, "targetUrl": "https://example.com/ateez-tour",
  "displayOrder": 0, "createdAt": "2026-11-05T01:00:00Z" }
```

- `groupIds`: 그룹 id 1개 이상, 또는 `["all"]`(모든 회원). `"all"` 이 섞여 있으면 나머지는 무시하고 전체 대상. 없는 그룹·UUID 가 아닌 값 → 400 `CONTENT_INVALID_GROUP`(`fields.groupIds`)
- `inCalendar: true` → `color` 필수. `inCalendar: false`(Event coming up 탭에서 등록) → 사진 필수
- 사진이 있으면 `endDate` 필수 (Event coming up 노출 기간). `endDate < startDate` 는 400
- 그룹이 삭제되면 그 그룹 매핑도 지워진다. 대상 그룹이 모두 사라진 항목은 회원에게 보이지 않는다

### 생일카페 `/admin/birthday-cafes`

캘린더 항목과 같은 CRUD(GET 목록·상세, POST 201, PUT, DELETE 204).

```json
{ "artistId": "019a…san", "venue": { "en": "Cafe Lettering Seoul" },
  "startDate": "2026-11-06", "endDate": "2026-11-08",
  "imageFileId": "019a…", "linkUrl": "https://example.com/birthday-cafe-1" }
```

- `artistId` = 생일인 멤버. 어드민 mock 이름인 `memberId` 로 보내도 받는다. 응답은 `artistId`
- 없는 멤버 → 400 `CONTENT_INVALID_GROUP`(`fields.artistId`). 멤버가 삭제되면 생일카페도 함께 삭제된다

### 배너 `/admin/banners`

| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | `/banners?placement=HOME\|DIARY` | 지정: 그 위치의 어드민 순서. 생략: 전체 최신순 |
| GET | `/banners/{id}` | 상세 |
| POST | `/banners` | 201. 켜진 상태로 그 위치 맨 뒤에 추가 |
| PUT | `/banners/{id}` | 200. `placement` 는 무시(변경 불가). 기본 배너는 기간이 null 로 고정 |
| PATCH | `/banners/{id}/enabled` `{ "enabled": false }` | 200 배너. 기본 배너면 409 `CONTENT_DEFAULT_BANNER` |
| DELETE | `/banners/{id}` | 204. 기본 배너면 409 `CONTENT_DEFAULT_BANNER` |
| PUT | `/banners/reorder` `{ "placement": "HOME", "ids": [...] }` | 200 새 순서의 배너 목록. `ids` 는 그 위치 배너 **전부**(기본 배너 포함), 빠지거나 중복이면 400 |

요청

```json
{ "placement": "HOME", "adminTitle": "ATEEZ 팝업 데자뷰", "imageFileId": "019a…",
  "title": { "en": "ANITEEZ IN DEJAVU" }, "description": { "en": "ATEEZ pop-up store in Seoul" },
  "ctaLabel": {}, "targetType": "URL", "targetUrl": "https://example.com/aniteez-in-dejavu",
  "startDate": "2026-11-01", "endDate": "2026-11-09" }
```

응답

```json
{ "id": "…", "placement": "HOME", "adminTitle": "ATEEZ 팝업 데자뷰",
  "imageKey": "content/2026/11/….jpg", "title": { "en": "ANITEEZ IN DEJAVU" },
  "description": { "en": "ATEEZ pop-up store in Seoul" }, "ctaLabel": {},
  "targetType": "URL", "targetUrl": "https://example.com/aniteez-in-dejavu",
  "startDate": "2026-11-01", "endDate": "2026-11-09",
  "enabled": true, "isDefault": false, "displayOrder": 3, "createdAt": "2026-11-05T01:00:00Z" }
```

- `targetType: "URL"` → `targetUrl` 필수
- `startDate`·`endDate` 는 각각 생략 가능(그쪽 제한 없음). 기간이 끝나도 `enabled` 는 바뀌지 않고 서버가 날짜로만 거른다
- 기본 배너: 위치별 1개(마이그레이션에서 생성), 항상 노출. 문구·이미지·이동 대상은 수정할 수 있다

### 이미지 규칙 (세 리소스 공통)

- `imageFileId`: 어드민 presigned 업로드(`domain: "CONTENT"`, jpeg/png/webp, 10MB)로 받은 fileId. 저장 시 LINKED 처리
- 수정에서 `imageFileId` 생략 → 기존 이미지 유지 (다이어리 템플릿과 같음)
- `removeImage: true` → 이미지 제거. `imageFileId` 와 같이 보내면 400
- 교체·제거된 이전 S3 파일은 지우지 않는다 (TODO)

## 4. 에러 코드

| 코드 | HTTP | 언제 |
|---|---|---|
| `CONTENT_NOT_FOUND` | 404 | 없는 캘린더 항목·생일카페·배너 |
| `CONTENT_DEFAULT_BANNER` | 409 | 기본 배너 끄기·삭제 |
| `CONTENT_INVALID_GROUP` | 400 | 없는 그룹(`fields.groupIds`)·멤버(`fields.artistId`) |
| `COMMON_INVALID_REQUEST` | 400 | en 누락, 기간 역전, 색 누락, 사진·종료일 조건, reorder 목록 불일치 등 (`fields` 에 사유) |
| `FILE_NOT_FOUND` | 404 | 없는 `imageFileId` |
