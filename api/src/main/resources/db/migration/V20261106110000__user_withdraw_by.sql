-- 탈퇴 주체·사유 (#57). 어드민 강퇴(ADMIN)와 회원 본인 탈퇴(SELF) 구분
ALTER TABLE users ADD COLUMN withdraw_reason varchar(200);
ALTER TABLE users ADD COLUMN withdrawn_by    varchar(10) CHECK (withdrawn_by IN ('ADMIN', 'SELF'));

-- 지금까지의 탈퇴는 전부 회원 본인 탈퇴(DELETE /users/me)
UPDATE users SET withdrawn_by = 'SELF' WHERE status = 'WITHDRAWN' AND withdrawn_by IS NULL;

-- 어드민 회원 목록 가입 최신순
CREATE INDEX idx_users_created_at ON users (created_at);
