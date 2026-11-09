-- 다이어리 스티커 카테고리 (#61). 스티커 하나가 여러 카테고리에 속할 수 있다

CREATE TABLE diary_sticker_categories (
    id            uuid        PRIMARY KEY,
    name          varchar(50) NOT NULL,
    display_order int         NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);

-- 카테고리를 지우면 매핑만 사라지고 스티커는 남는다
CREATE TABLE diary_sticker_category_map (
    sticker_id  uuid NOT NULL REFERENCES diary_stickers (id) ON DELETE CASCADE,
    category_id uuid NOT NULL REFERENCES diary_sticker_categories (id) ON DELETE CASCADE,
    PRIMARY KEY (sticker_id, category_id)
);

CREATE INDEX idx_diary_sticker_category_map_category_id ON diary_sticker_category_map (category_id);
