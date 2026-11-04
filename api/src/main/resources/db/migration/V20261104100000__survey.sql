-- 플랜 만족도 조사 (#46)
-- 다국어 문구는 jsonb { "en": "...", "ko": "...", "zh": "...", "ja": "..." } (en 필수)

-- 설정은 행 1개만. 앱 기동 시 없으면 생성한다 (SurveySettingsInitializer)
CREATE TABLE survey_settings (
    id           uuid        PRIMARY KEY,
    enabled      boolean     NOT NULL DEFAULT false,
    invite_title jsonb       NOT NULL DEFAULT '{}'::jsonb,
    invite_body  jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL
);

-- 상수 식 unique index → 두 번째 행 insert 는 실패한다 (여러 인스턴스 동시 기동 방어)
CREATE UNIQUE INDEX uk_survey_settings_singleton ON survey_settings ((true));

CREATE TABLE survey_questions (
    id            uuid        PRIMARY KEY,
    type          varchar(20) NOT NULL CHECK (type IN ('RATING', 'SINGLE', 'MULTIPLE', 'TEXT')),
    title         jsonb       NOT NULL,
    required      boolean     NOT NULL DEFAULT false,
    display_order int         NOT NULL,
    archived      boolean     NOT NULL DEFAULT false, -- 응답이 있는 질문은 삭제 대신 숨김
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);

CREATE TABLE survey_options (
    id            uuid        PRIMARY KEY,
    question_id   uuid        NOT NULL REFERENCES survey_questions (id),
    label         jsonb       NOT NULL,
    display_order int         NOT NULL,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);

CREATE INDEX idx_survey_options_question_id ON survey_options (question_id);

-- 회원별 초대 팝업 상태. 카운트는 upsert 로 원자적으로 증가시킨다
CREATE TABLE survey_popup_states (
    user_id        uuid        PRIMARY KEY REFERENCES users (id),
    travel_visits  int         NOT NULL DEFAULT 0, -- 플랜이 있는 동안의 TRAVEL 방문 수
    shown_count    int         NOT NULL DEFAULT 0,
    closed_count   int         NOT NULL DEFAULT 0,
    accepted_count int         NOT NULL DEFAULT 0,
    snoozed_count  int         NOT NULL DEFAULT 0,
    snoozed_until  timestamptz, -- '오늘 하루 보지 않기' → 다음 KST 자정
    created_at     timestamptz NOT NULL,
    updated_at     timestamptz NOT NULL
);

-- 1인 1응답
CREATE TABLE survey_responses (
    id            uuid         PRIMARY KEY,
    user_id       uuid         NOT NULL UNIQUE REFERENCES users (id),
    email         varchar(255) NOT NULL, -- 기프티콘 받을 계정 이메일 (응답 시점)
    plan_snapshot jsonb        NOT NULL, -- { planId, startDate, endDate, dayCount, placeCount }
    answers       jsonb        NOT NULL, -- { [questionId]: number | optionId | optionId[] | text }
    submitted_at  timestamptz  NOT NULL,
    gift_status   varchar(20)  NOT NULL CHECK (gift_status IN ('PENDING', 'SENT')),
    gift_sent_at  timestamptz,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_survey_responses_submitted_at ON survey_responses (submitted_at);
