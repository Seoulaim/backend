CREATE TABLE product_categories (
    id            uuid         PRIMARY KEY,
    name          varchar(50)  NOT NULL,
    display_order int          NOT NULL DEFAULT 0,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE TABLE products (
    id               uuid         PRIMARY KEY,
    category_id      uuid         NOT NULL REFERENCES product_categories (id),
    artist_group_id  uuid         NOT NULL, -- TODO(#10): artist 테이블 반영 후 FK 추가
    artist_id        uuid,                  -- TODO(#10): artist 테이블 반영 후 FK 추가
    name             varchar(200) NOT NULL,
    summary          varchar(500),
    detail_image_key varchar(512),
    price            bigint       NOT NULL,
    is_diary         boolean      NOT NULL DEFAULT false,
    sales_count_30d  int          NOT NULL DEFAULT 0,
    created_at       timestamptz  NOT NULL,
    updated_at       timestamptz  NOT NULL
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_artist_group_id ON products (artist_group_id);
CREATE INDEX idx_products_sales_count_30d ON products (sales_count_30d);

CREATE TABLE product_images (
    id            uuid         PRIMARY KEY,
    product_id    uuid         NOT NULL REFERENCES products (id),
    image_key     varchar(512) NOT NULL,
    display_order int          NOT NULL DEFAULT 0,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

CREATE TABLE promotions (
    id              uuid         PRIMARY KEY,
    product_id      uuid         REFERENCES products (id),
    title           varchar(200) NOT NULL,
    image_key       varchar(512),
    importance      varchar(1)   NOT NULL CHECK (importance IN ('A', 'B')),
    artist_group_id uuid,        -- TODO(#10): artist 테이블 반영 후 FK 추가
    artist_id       uuid,        -- TODO(#10): artist 테이블 반영 후 FK 추가
    starts_at       timestamptz,
    ends_at         timestamptz,
    display_order   int,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);

CREATE INDEX idx_promotions_importance ON promotions (importance);
