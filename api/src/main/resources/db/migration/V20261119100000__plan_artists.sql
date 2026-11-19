-- 일정 생성 때 고른 아티스트(bias). 다른 기기에서도 같은 bias 태그 표시용
CREATE TABLE plan_artists (
                              id            uuid        PRIMARY KEY,
                              plan_id       uuid        NOT NULL REFERENCES travel_plans (id),
                              artist_id     uuid        NOT NULL REFERENCES artists (id),
                              display_order int         NOT NULL,
                              created_at    timestamptz NOT NULL,
                              updated_at    timestamptz NOT NULL,
                              CONSTRAINT uk_plan_artists_plan_artist UNIQUE (plan_id, artist_id)
);

CREATE INDEX idx_plan_artists_artist_id ON plan_artists (artist_id);