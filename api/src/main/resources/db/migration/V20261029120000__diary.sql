CREATE TABLE diary_templates (
    id            uuid         PRIMARY KEY,
    name          varchar(100) NOT NULL,
    description   varchar(500),
    image_key     varchar(512) NOT NULL,
    display_order int          NOT NULL DEFAULT 0,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE TABLE diary_stickers (
    id            uuid         PRIMARY KEY,
    name          varchar(100),
    image_key     varchar(512) NOT NULL,
    display_order int          NOT NULL DEFAULT 0,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE TABLE diaries (
    id            uuid         PRIMARY KEY,
    user_id       uuid         NOT NULL, -- TODO(#TBD): users 테이블 반영 후 FK 추가
    order_item_id uuid         NOT NULL UNIQUE REFERENCES order_items (id),
    page_count    int          NOT NULL CHECK (page_count BETWEEN 1 AND 30),
    content       jsonb        NOT NULL,
    pdf_file_id   uuid         NOT NULL REFERENCES files (id),
    pdf_s3_key    varchar(512) NOT NULL,
    submitted_at  timestamptz  NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_diaries_user_id ON diaries (user_id);
