-- 홈 콘텐츠: 팬캘린더·Event coming up(calendar entry), 생일카페, 배너 (#51)
-- 다국어 문구는 jsonb { "en": "...", "ko": "...", "zh": "...", "ja": "..." } (en 필수)
-- 날짜는 한국 날짜(date). 노출 판단은 서버가 KST 오늘 날짜로 한다

-- 팬캘린더(in_calendar) 와 Event coming up(image_key 있음) 을 한 목록으로 관리한다
CREATE TABLE calendar_entries (
    id            uuid          PRIMARY KEY,
    title         jsonb         NOT NULL,
    start_date    date          NOT NULL,
    end_date      date,
    in_calendar   boolean       NOT NULL,
    color         varchar(10)   CHECK (color IN ('PINK', 'PURPLE', 'BLUE', 'ORANGE', 'GRAY', 'GREEN')),
    image_key     varchar(512),
    venue         jsonb,
    target_url    varchar(1000),
    all_groups    boolean       NOT NULL DEFAULT false, -- 어드민 groupIds ['all']
    display_order int           NOT NULL,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    CONSTRAINT ck_calendar_entries_period CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_calendar_entries_start_date ON calendar_entries (start_date);
CREATE INDEX idx_calendar_entries_end_date ON calendar_entries (end_date);

-- 그룹이 삭제되면 매핑도 지운다 (all_groups 가 아니고 매핑이 모두 사라진 항목은 회원에게 안 보임)
CREATE TABLE calendar_entry_groups (
    entry_id        uuid NOT NULL REFERENCES calendar_entries (id) ON DELETE CASCADE,
    artist_group_id uuid NOT NULL REFERENCES artist_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (entry_id, artist_group_id)
);

CREATE INDEX idx_calendar_entry_groups_artist_group_id ON calendar_entry_groups (artist_group_id);

-- 생일카페. 멤버가 삭제되면 함께 지운다
CREATE TABLE birthday_cafes (
    id            uuid          PRIMARY KEY,
    artist_id     uuid          NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    venue         jsonb         NOT NULL,
    start_date    date          NOT NULL,
    end_date      date          NOT NULL,
    image_key     varchar(512),
    link_url      varchar(1000),
    display_order int           NOT NULL,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    CONSTRAINT ck_birthday_cafes_period CHECK (end_date >= start_date)
);

CREATE INDEX idx_birthday_cafes_artist_id ON birthday_cafes (artist_id);
CREATE INDEX idx_birthday_cafes_end_date ON birthday_cafes (end_date);

-- 배너 (HOME = 홈 메인 배너 HH01, DIARY = 다이어리 배너 DD01)
-- 기본 배너는 위치별 1개, 항상 노출(enabled true 고정), 기간 없음, 삭제 불가
CREATE TABLE banners (
    id            uuid          PRIMARY KEY,
    placement     varchar(10)   NOT NULL CHECK (placement IN ('HOME', 'DIARY')),
    admin_title   varchar(100)  NOT NULL,
    image_key     varchar(512),
    title         jsonb         NOT NULL DEFAULT '{}'::jsonb,
    description   jsonb         NOT NULL DEFAULT '{}'::jsonb,
    cta_label     jsonb         NOT NULL DEFAULT '{}'::jsonb, -- DIARY 만 사용
    target_type   varchar(10)   NOT NULL CHECK (target_type IN ('DIARY', 'URL')),
    target_url    varchar(1000),
    start_date    date,
    end_date      date,
    enabled       boolean       NOT NULL DEFAULT true,
    is_default    boolean       NOT NULL DEFAULT false,
    display_order int           NOT NULL,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    CONSTRAINT ck_banners_period CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_banners_default CHECK (
        NOT is_default OR (enabled AND start_date IS NULL AND end_date IS NULL))
);

CREATE UNIQUE INDEX uk_banners_default_placement ON banners (placement) WHERE is_default;
CREATE INDEX idx_banners_placement_order ON banners (placement, display_order);

-- 위치별 기본 배너 (id 는 UUID v7 형식 고정값)
INSERT INTO banners (id, placement, admin_title, image_key, title, target_type,
                     enabled, is_default, display_order, created_at, updated_at)
VALUES ('01a1a938-4680-7000-8000-000000000001', 'HOME', 'Default', NULL,
        '{"en": "Seoulaim"}'::jsonb, 'DIARY', true, true, 0, now(), now()),
       ('01a1a938-4680-7000-8000-000000000002', 'DIARY', 'Default', NULL,
        '{"en": "Seoulaim"}'::jsonb, 'DIARY', true, true, 0, now(), now());

-- 어드민 콘텐츠 이미지 업로드용 도메인 추가
ALTER TABLE files DROP CONSTRAINT files_domain_check;
ALTER TABLE files ADD CONSTRAINT files_domain_check
    CHECK (domain IN ('REVIEW', 'PLAN', 'PROFILE', 'PRODUCT', 'DIARY', 'INQUIRY',
    'DIARY_TEMPLATE', 'DIARY_STICKER', 'ARTIST', 'PLACE', 'CONTENT'));
