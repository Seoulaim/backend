-- 소셜 로그인 계정 연결. 제공자별 사용자 고유 id(sub)로 회원 식별
CREATE TABLE user_social_accounts (
                                      id               uuid         PRIMARY KEY,
                                      user_id          uuid         NOT NULL REFERENCES users (id),
                                      provider         varchar(20)  NOT NULL CHECK (provider IN ('GOOGLE')),
                                      provider_user_id varchar(255) NOT NULL,
                                      created_at       timestamptz  NOT NULL,
                                      updated_at       timestamptz  NOT NULL,
                                      CONSTRAINT uk_user_social_accounts_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_user_social_accounts_user_id ON user_social_accounts (user_id);