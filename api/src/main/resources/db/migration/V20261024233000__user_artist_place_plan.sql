CREATE TABLE users (
                       id                uuid         PRIMARY KEY,
                       email             varchar(255), -- 이메일 가입자의 로그인 아이디. 소셜 제공자에 따라 NULL 가능
                       password_hash     varchar(100), -- 소셜 전용 사용자는 NULL
                       nickname          varchar(50)  NOT NULL,
                       profile_image_key varchar(512),
                       locale            varchar(10)  NOT NULL DEFAULT 'en',
                       push_enabled      boolean      NOT NULL DEFAULT true,
                       marketing_enabled boolean      NOT NULL DEFAULT false,
                       status            varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
                       withdrawn_at      timestamptz,
                       created_at        timestamptz  NOT NULL,
                       updated_at        timestamptz  NOT NULL
);

-- 이메일 가입자 간 중복 방지 (대소문자 무시). 소셜 계정과의 중복 정책은 소셜 로그인 도입 시 결정
CREATE UNIQUE INDEX uk_users_email_password
    ON users (lower(email)) WHERE password_hash IS NOT NULL;