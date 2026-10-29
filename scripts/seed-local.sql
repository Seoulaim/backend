-- 로컬 테스트용 그룹·아티스트 데이터
-- 고정 id + ON CONFLICT → 여러 번 실행해도 중복 없음
-- 실행: docker exec -i seoulaim-postgres psql -U seoulaim -d seoulaim < scripts/seed-local.sql

INSERT INTO artist_groups (id, name_en, name_ko, name_ja, name_zh, image_key, debut_date, display_order, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000000001', 'ATEEZ', '에이티즈', 'エイティーズ', NULL, NULL, NULL, 1, now(), now()),
    ('01a00000-0000-7000-8000-000000000002', 'SEVENTEEN', '세븐틴', 'セブンティーン', NULL, NULL, NULL, 2, now(), now())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO artists (id, artist_group_id, name_en, name_ko, name_ja, name_zh, image_key, birth_date, display_order, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000000101', '01a00000-0000-7000-8000-000000000001', 'HONGJOONG', '홍중', NULL, NULL, NULL, NULL, 1, now(), now()),
    ('01a00000-0000-7000-8000-000000000102', '01a00000-0000-7000-8000-000000000001', 'SEONGHWA', '성화', NULL, NULL, NULL, NULL, 2, now(), now()),
    ('01a00000-0000-7000-8000-000000000103', '01a00000-0000-7000-8000-000000000001', 'SAN', '산', 'サン', NULL, NULL, NULL, 3, now(), now()),
    ('01a00000-0000-7000-8000-000000000201', '01a00000-0000-7000-8000-000000000002', 'S.COUPS', '에스쿱스', NULL, NULL, NULL, NULL, 1, now(), now()),
    ('01a00000-0000-7000-8000-000000000202', '01a00000-0000-7000-8000-000000000002', 'JEONGHAN', '정한', NULL, NULL, NULL, NULL, 2, now(), now()),
    ('01a00000-0000-7000-8000-000000000203', '01a00000-0000-7000-8000-000000000002', 'JOSHUA', '조슈아', NULL, NULL, NULL, NULL, 3, now(), now())
    ON CONFLICT (id) DO NOTHING;