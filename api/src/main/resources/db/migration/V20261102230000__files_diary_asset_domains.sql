-- 어드민 다이어리 에셋 업로드용 도메인 추가 (#40)
ALTER TABLE files DROP CONSTRAINT files_domain_check;
ALTER TABLE files ADD CONSTRAINT files_domain_check
    CHECK (domain IN ('REVIEW', 'PLAN', 'PROFILE', 'PRODUCT', 'DIARY', 'INQUIRY',
                      'DIARY_TEMPLATE', 'DIARY_STICKER'));
