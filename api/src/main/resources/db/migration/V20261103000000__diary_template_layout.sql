-- 템플릿 레이아웃: 오버레이 이미지, 페이지 비율(세로/가로, 기본 A4 297/210), 사진 프레임 목록 (#40)
ALTER TABLE diary_templates
    ADD COLUMN overlay_image_key varchar(512),
    ADD COLUMN aspect            double precision NOT NULL DEFAULT 1.4142857,
    ADD COLUMN frames            jsonb            NOT NULL DEFAULT '[]'::jsonb;
