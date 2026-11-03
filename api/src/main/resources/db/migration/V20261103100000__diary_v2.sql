-- 다이어리 v2: 주문 기반 제출 모델 → 회원 앱 편집·결제 모델 (#42)
-- 기존 diaries(V20261029120000, order_items 참조)는 실데이터가 없어 DROP 후 재생성한다
-- diary_templates / diary_stickers 는 그대로 둔다
DROP TABLE IF EXISTS diaries;

CREATE TABLE diaries (
    id             uuid        PRIMARY KEY,
    user_id        uuid        NOT NULL REFERENCES users (id),
    name           varchar(30) NOT NULL DEFAULT 'My Diary',
    is_free        boolean     NOT NULL,
    paid           boolean     NOT NULL DEFAULT false,
    status         varchar(20) NOT NULL CHECK (status IN ('DRAFT', 'FINALIZED')),
    spreads        jsonb       NOT NULL DEFAULT '[]'::jsonb,
    place_photos   jsonb       NOT NULL DEFAULT '{}'::jsonb,
    page_keys      jsonb, -- 완성 페이지 이미지 S3 key 배열. null 이면 아직 업로드 전
    completed_at   timestamptz,
    last_edited_at timestamptz,
    deleted_at     timestamptz, -- soft delete
    created_at     timestamptz NOT NULL,
    updated_at     timestamptz NOT NULL
);

CREATE INDEX idx_diaries_user_id ON diaries (user_id);
-- 무료 다이어리는 사용자당 1개 (soft delete 포함). 동시 생성 요청 방어
CREATE UNIQUE INDEX uk_diaries_user_id_free ON diaries (user_id) WHERE is_free;

CREATE TABLE diary_payments (
    id                uuid         PRIMARY KEY,
    diary_id          uuid         NOT NULL REFERENCES diaries (id),
    user_id           uuid         NOT NULL,
    order_number      varchar(64)  NOT NULL UNIQUE,
    amount            bigint       NOT NULL,
    status            varchar(20)  NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    refund_consent_at timestamptz  NOT NULL,
    toss_payment_key  varchar(200),
    paid_at           timestamptz,
    edited_at         timestamptz, -- 결제 후 첫 편집 시각. 환불 정책 판단용
    failure_code      varchar(100),
    failure_message   varchar(500),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL
);

CREATE INDEX idx_diary_payments_diary_id ON diary_payments (diary_id);
CREATE INDEX idx_diary_payments_user_id ON diary_payments (user_id);
