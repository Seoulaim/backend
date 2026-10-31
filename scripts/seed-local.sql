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

-- 키워드 (HC05 칩 순서)
INSERT INTO keywords (id, code, display_order, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000002001', 'CAFE', 0, now(), now()),
    ('01a00000-0000-7000-8000-000000002002', 'RESTAURANT', 1, now(), now()),
    ('01a00000-0000-7000-8000-000000002003', 'PHOTO_SPOT', 2, now(), now()),
    ('01a00000-0000-7000-8000-000000002004', 'ALBUM_SHOOTING_SITE', 3, now(), now()),
    ('01a00000-0000-7000-8000-000000002005', 'CONCERT_HALL', 4, now(), now()),
    ('01a00000-0000-7000-8000-000000002006', 'POPUP_STORE', 5, now(), now()),
    ('01a00000-0000-7000-8000-000000002007', 'BIRTHDAY_CAFE', 6, now(), now()),
    ('01a00000-0000-7000-8000-000000002008', 'GOODS_SHOP', 7, now(), now()),
    ('01a00000-0000-7000-8000-000000002009', 'DRAMA_FILMING_SITE', 8, now(), now()),
    ('01a00000-0000-7000-8000-000000002010', 'MV_FILMING_SITE', 9, now(), now()),
    ('01a00000-0000-7000-8000-000000002011', 'PARK', 10, now(), now()),
    ('01a00000-0000-7000-8000-000000002012', 'SHOPPING', 11, now(), now()),
    ('01a00000-0000-7000-8000-000000002013', 'EXHIBITION', 12, now(), now()),
    ('01a00000-0000-7000-8000-000000002014', 'MUSEUM', 13, now(), now()),
    ('01a00000-0000-7000-8000-000000002015', 'NIGHT_VIEW', 14, now(), now()),
    ('01a00000-0000-7000-8000-000000002016', 'HEALING', 15, now(), now()),
    ('01a00000-0000-7000-8000-000000002017', 'ETC', 16, now(), now())
    ON CONFLICT (id) DO NOTHING;

-- 장소 (경도, 위도 순서)
INSERT INTO places (id, name, category, address, location, district, opens_at, closes_at, avg_stay_minutes, phone, homepage_url, video_key, visit_count, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000003001', '어니언 성수', 'VISIT_SPOT', '서울 성동구 아차산로9길 8',
     ST_GeogFromText('SRID=4326;POINT(127.0581 37.5447)'), '성동구', '10:00', '22:00', 60, NULL, NULL, NULL, 0, now(), now()),
    ('01a00000-0000-7000-8000-000000003002', '테스트 생일카페', 'BIRTHDAY_CAFE', '서울 마포구 테스트로 1',
     ST_GeogFromText('SRID=4326;POINT(126.9236 37.5563)'), '마포구', '10:00', '21:00', 60, NULL, NULL, NULL, 0, now(), now())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO place_images (id, place_id, image_key, display_order, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000003101', '01a00000-0000-7000-8000-000000003001', 'place/sample-1.jpg', 0, now(), now()),
    ('01a00000-0000-7000-8000-000000003102', '01a00000-0000-7000-8000-000000003001', 'place/sample-2.jpg', 1, now(), now())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO place_events (id, place_id, title, starts_on, ends_on, opens_at, closes_at, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000003201', '01a00000-0000-7000-8000-000000003002', '테스트 생일 이벤트',
     '2026-10-20', '2026-11-10', '11:00', '20:00', now(), now())
    ON CONFLICT (id) DO NOTHING;

-- 장소·아티스트 연결 (그룹 단위 1건, 멤버 단위 1건)
INSERT INTO artist_places (id, place_id, artist_group_id, artist_id, description, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000003301', '01a00000-0000-7000-8000-000000003001', '01a00000-0000-7000-8000-000000000001', NULL, '테스트용 그룹 연결', now(), now()),
    ('01a00000-0000-7000-8000-000000003302', '01a00000-0000-7000-8000-000000003002', '01a00000-0000-7000-8000-000000000001', '01a00000-0000-7000-8000-000000000103', '테스트용 멤버 연결', now(), now())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO place_keywords (id, place_id, keyword_id, created_at, updated_at)
VALUES
    ('01a00000-0000-7000-8000-000000003401', '01a00000-0000-7000-8000-000000003001', '01a00000-0000-7000-8000-000000002001', now(), now()),
    ('01a00000-0000-7000-8000-000000003402', '01a00000-0000-7000-8000-000000003002', '01a00000-0000-7000-8000-000000002007', now(), now())
    ON CONFLICT (id) DO NOTHING;