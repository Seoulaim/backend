-- 어드민 그룹·멤버 사진, 장소 사진 업로드용 도메인 추가 (#44)
ALTER TABLE files DROP CONSTRAINT files_domain_check;
ALTER TABLE files ADD CONSTRAINT files_domain_check
    CHECK (domain IN ('REVIEW', 'PLAN', 'PROFILE', 'PRODUCT', 'DIARY', 'INQUIRY',
    'DIARY_TEMPLATE', 'DIARY_STICKER', 'ARTIST', 'PLACE'));