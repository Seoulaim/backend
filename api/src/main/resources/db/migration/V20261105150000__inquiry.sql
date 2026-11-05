-- 문의·환불 (#55)
-- 환불은 category = 'REFUND' 인 문의로만 들어오고, 회원이 고른 다이어리 결제(payment_id)가 붙는다
CREATE TABLE inquiries (
    id                      uuid        PRIMARY KEY,
    user_id                 uuid        NOT NULL REFERENCES users (id),
    category                varchar(20) NOT NULL
        CHECK (category IN ('REFUND', 'PAYMENT', 'DIARY', 'ACCOUNT', 'REPORT', 'OTHER')),
    title                   varchar(50) NOT NULL,
    content                 text        NOT NULL CHECK (char_length(content) <= 500),
    photo_keys              jsonb       NOT NULL DEFAULT '[]'::jsonb,
    language                varchar(5)  NOT NULL CHECK (language IN ('en', 'ko', 'zh', 'ja')),
    status                  varchar(20) NOT NULL CHECK (status IN ('RECEIVED', 'ANSWERED')),
    memo                    text, -- 어드민 전용 메모. 회원에게 보내지 않는다
    -- 아래는 REFUND 만
    payment_id              uuid        REFERENCES diary_payments (id),
    refund_within_policy    boolean, -- 신청 시점 판정 (7일 이내 && 미완성 && 결제 후 편집 없음)
    refund_status           varchar(20) CHECK (refund_status IN ('REQUESTED', 'REFUNDED', 'REJECTED')),
    refund_decided_at       timestamptz,
    refund_exception_reason varchar(50), -- 정책 밖 승인 사유 (환불 정책 제5조 등)
    refund_exception_memo   text,
    refund_reject_reason    text,
    created_at              timestamptz NOT NULL,
    updated_at              timestamptz NOT NULL,
    -- REFUND 문의만 결제·환불 판정·환불 상태를 가진다
    CONSTRAINT ck_inquiries_refund_fields CHECK (
        (category = 'REFUND') = (payment_id IS NOT NULL
            AND refund_within_policy IS NOT NULL
            AND refund_status IS NOT NULL))
);

CREATE INDEX idx_inquiries_user_id ON inquiries (user_id);
CREATE INDEX idx_inquiries_category_status ON inquiries (category, status);
CREATE INDEX idx_inquiries_payment_id ON inquiries (payment_id);
-- 같은 결제에 처리 중이거나 환불된 요청은 하나만 (거절된 뒤에는 다시 신청 가능). 동시 신청 방어
CREATE UNIQUE INDEX uk_inquiries_payment_id_active_refund ON inquiries (payment_id)
    WHERE refund_status IN ('REQUESTED', 'REFUNDED');

CREATE TABLE inquiry_answers (
    id         uuid        PRIMARY KEY,
    inquiry_id uuid        NOT NULL REFERENCES inquiries (id),
    body       text        NOT NULL,
    sent_at    timestamptz NOT NULL, -- 회원에게 메일을 보낸 시각
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_inquiry_answers_inquiry_id ON inquiry_answers (inquiry_id);

-- 환불(토스 결제 취소) 결과
ALTER TABLE diary_payments ADD COLUMN IF NOT EXISTS refunded_at timestamptz;
ALTER TABLE diary_payments ADD COLUMN IF NOT EXISTS toss_cancel_key varchar(200);
