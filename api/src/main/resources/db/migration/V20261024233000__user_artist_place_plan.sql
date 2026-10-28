CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================================
-- user
-- ============================================================

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

-- ============================================================
-- artist
-- ============================================================

CREATE TABLE artist_groups (
                               id            uuid         PRIMARY KEY,
                               name_en       varchar(100) NOT NULL,
                               name_ko       varchar(100) NOT NULL,
                               name_ja       varchar(100),
                               name_zh       varchar(100),
                               image_key     varchar(512),
                               debut_date    date,
                               display_order int          NOT NULL DEFAULT 0,
                               created_at    timestamptz  NOT NULL,
                               updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_artist_groups_display_order ON artist_groups (display_order);

CREATE TABLE artists (
                         id              uuid         PRIMARY KEY,
                         artist_group_id uuid         NOT NULL REFERENCES artist_groups (id),
                         name_en         varchar(100) NOT NULL,
                         name_ko         varchar(100) NOT NULL,
                         name_ja         varchar(100),
                         name_zh         varchar(100),
                         image_key       varchar(512),
                         birth_date      date,
                         display_order   int          NOT NULL DEFAULT 0,
                         created_at      timestamptz  NOT NULL,
                         updated_at      timestamptz  NOT NULL
);

CREATE INDEX idx_artists_group_order ON artists (artist_group_id, display_order);

-- 이름 부분 일치 검색 (LIKE '%...%')
CREATE INDEX idx_artists_name_en_trgm ON artists USING GIN (name_en gin_trgm_ops);
CREATE INDEX idx_artists_name_ko_trgm ON artists USING GIN (name_ko gin_trgm_ops);
CREATE INDEX idx_artist_groups_name_en_trgm ON artist_groups USING GIN (name_en gin_trgm_ops);
CREATE INDEX idx_artist_groups_name_ko_trgm ON artist_groups USING GIN (name_ko gin_trgm_ops);

CREATE TABLE user_biases (
                             id         uuid        PRIMARY KEY,
                             user_id    uuid        NOT NULL REFERENCES users (id),
                             artist_id  uuid        NOT NULL REFERENCES artists (id),
                             created_at timestamptz NOT NULL,
                             updated_at timestamptz NOT NULL,
                             CONSTRAINT uk_user_biases_user_artist UNIQUE (user_id, artist_id)
);

-- ============================================================
-- place
-- ============================================================

CREATE TABLE keywords (
                          id            uuid        PRIMARY KEY,
                          code          varchar(50) NOT NULL,
                          display_order int         NOT NULL DEFAULT 0,
                          created_at    timestamptz NOT NULL,
                          updated_at    timestamptz NOT NULL,
                          CONSTRAINT uk_keywords_code UNIQUE (code)
);

CREATE TABLE places (
                        id               uuid                   PRIMARY KEY,
                        name             varchar(200)           NOT NULL,
                        category         varchar(30)            NOT NULL
                            CHECK (category IN ('BIRTHDAY_CAFE', 'BILLBOARD', 'VISIT_SPOT')),
                        address          varchar(500),
                        location         geography(Point, 4326) NOT NULL,
                        district         varchar(50),
                        opens_at         time,
                        closes_at        time,
                        avg_stay_minutes int                    NOT NULL DEFAULT 60,
                        phone            varchar(50),
                        homepage_url     varchar(512),
                        video_key        varchar(512),
                        visit_count      int                    NOT NULL DEFAULT 0,
                        created_at       timestamptz            NOT NULL,
                        updated_at       timestamptz            NOT NULL
);

CREATE INDEX idx_places_location ON places USING GIST (location);
CREATE INDEX idx_places_district ON places (district);
CREATE INDEX idx_places_category ON places (category);

CREATE TABLE place_events (
                              id         uuid         PRIMARY KEY,
                              place_id   uuid         NOT NULL REFERENCES places (id),
                              title      varchar(200),
                              starts_on  date         NOT NULL,
                              ends_on    date         NOT NULL,
                              opens_at   time,
                              closes_at  time,
                              created_at timestamptz  NOT NULL,
                              updated_at timestamptz  NOT NULL,
                              CHECK (starts_on <= ends_on)
);

CREATE INDEX idx_place_events_place_id ON place_events (place_id);
CREATE INDEX idx_place_events_period ON place_events (starts_on, ends_on);

CREATE TABLE place_images (
                              id            uuid         PRIMARY KEY,
                              place_id      uuid         NOT NULL REFERENCES places (id),
                              image_key     varchar(512) NOT NULL,
                              display_order int          NOT NULL DEFAULT 0,
                              created_at    timestamptz  NOT NULL,
                              updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_place_images_place_order ON place_images (place_id, display_order);

CREATE TABLE place_keywords (
                                id         uuid        PRIMARY KEY,
                                place_id   uuid        NOT NULL REFERENCES places (id),
                                keyword_id uuid        NOT NULL REFERENCES keywords (id),
                                created_at timestamptz NOT NULL,
                                updated_at timestamptz NOT NULL,
                                CONSTRAINT uk_place_keywords_place_keyword UNIQUE (place_id, keyword_id)
);

CREATE INDEX idx_place_keywords_keyword_id ON place_keywords (keyword_id);

CREATE TABLE artist_places (
                               id              uuid         PRIMARY KEY,
                               place_id        uuid         NOT NULL REFERENCES places (id),
                               artist_group_id uuid         NOT NULL REFERENCES artist_groups (id),
                               artist_id       uuid         REFERENCES artists (id),  -- NULL 이면 그룹 단위 연결
                               description     varchar(500),
                               created_at      timestamptz  NOT NULL,
                               updated_at      timestamptz  NOT NULL
);

CREATE INDEX idx_artist_places_place_id ON artist_places (place_id);
CREATE INDEX idx_artist_places_group_id ON artist_places (artist_group_id);
CREATE INDEX idx_artist_places_artist_id ON artist_places (artist_id);

-- artist_id 가 NULL 이면 일반 UNIQUE 로 중복 방지 불가 → 그룹 단위 / 아티스트 단위로 분리
CREATE UNIQUE INDEX uk_artist_places_place_group
    ON artist_places (place_id, artist_group_id) WHERE artist_id IS NULL;
CREATE UNIQUE INDEX uk_artist_places_place_artist
    ON artist_places (place_id, artist_id) WHERE artist_id IS NOT NULL;

CREATE TABLE place_favorites (
                                 id         uuid        PRIMARY KEY,
                                 user_id    uuid        NOT NULL REFERENCES users (id),
                                 place_id   uuid        NOT NULL REFERENCES places (id),
                                 created_at timestamptz NOT NULL,
                                 updated_at timestamptz NOT NULL,
                                 CONSTRAINT uk_place_favorites_user_place UNIQUE (user_id, place_id)
);

CREATE INDEX idx_place_favorites_user_created ON place_favorites (user_id, created_at);

-- ============================================================
-- plan
-- ============================================================

CREATE TABLE travel_plans (
                              id         uuid        PRIMARY KEY,
                              user_id    uuid        NOT NULL REFERENCES users (id),
                              start_date date        NOT NULL,
                              end_date   date        NOT NULL,
                              created_at timestamptz NOT NULL,
                              updated_at timestamptz NOT NULL,
                              CHECK (start_date <= end_date)
);

CREATE INDEX idx_travel_plans_user_created ON travel_plans (user_id, created_at);

CREATE TABLE plan_days (
                           id         uuid        PRIMARY KEY,
                           plan_id    uuid        NOT NULL REFERENCES travel_plans (id),
                           day_number int         NOT NULL CHECK (day_number >= 1),
                           visit_date date        NOT NULL,
                           created_at timestamptz NOT NULL,
                           updated_at timestamptz NOT NULL,
                           CONSTRAINT uk_plan_days_plan_number UNIQUE (plan_id, day_number),
                           CONSTRAINT uk_plan_days_plan_date UNIQUE (plan_id, visit_date)
);

CREATE TABLE plan_items (
                            id           uuid        PRIMARY KEY,
                            plan_day_id  uuid        NOT NULL REFERENCES plan_days (id),
                            place_id     uuid        NOT NULL REFERENCES places (id),
                            sequence     int         NOT NULL,
                            arrival_time time,
                            created_at   timestamptz NOT NULL,
                            updated_at   timestamptz NOT NULL
);

-- 순서 변경 중 일시적 중복 발생 → UNIQUE 대신 일반 인덱스
CREATE INDEX idx_plan_items_day_sequence ON plan_items (plan_day_id, sequence);
CREATE INDEX idx_plan_items_place_id ON plan_items (place_id);