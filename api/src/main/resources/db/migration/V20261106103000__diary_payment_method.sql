-- 토스 결제 승인 응답의 결제수단 (카드, 간편결제 등). 어드민 결제 관리 표시용 (#57). 이전 결제는 null
ALTER TABLE diary_payments ADD COLUMN method varchar(30);

-- 어드민 결제 관리 목록 최신순·기간 필터
CREATE INDEX idx_diary_payments_created_at ON diary_payments (created_at);
