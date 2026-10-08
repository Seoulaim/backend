CREATE TABLE files (
    id           uuid         PRIMARY KEY,
    uploader_id  uuid,                    -- TODO(#TBD): users 테이블 반영 후 FK 추가
    domain       varchar(30)  NOT NULL
        CHECK (domain IN ('REVIEW', 'PLAN', 'PROFILE', 'PRODUCT', 'DIARY', 'INQUIRY')),
    s3_key       varchar(512) NOT NULL UNIQUE,
    content_type varchar(100),
    size_bytes   bigint,
    status       varchar(20)  NOT NULL CHECK (status IN ('PENDING', 'LINKED')),
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL
);

CREATE INDEX idx_files_status ON files (status);
CREATE INDEX idx_files_uploader_id ON files (uploader_id);
