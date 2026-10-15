-- 로컬 확인용 goods 시드 데이터 (Flyway 마이그레이션 아님)
-- 실행: psql -h localhost -U <user> -d <database> -f scripts/seed-goods.sql
-- id 는 gen_random_uuid()(v4) 를 쓰므로 id 정렬이 시간순이 아니다. 시드 전용.
-- artist_group_id / artist_id 는 artist 도메인이 없어 임의의 고정 UUID 를 쓴다.
--   그룹 A: aaaaaaaa-0000-0000-0000-000000000000 (멤버 aaaaaaaa-0000-0000-0000-000000000001)
--   그룹 B: bbbbbbbb-0000-0000-0000-000000000000 (멤버 bbbbbbbb-0000-0000-0000-000000000001)

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM product_categories) THEN
        RAISE EXCEPTION 'product_categories 에 이미 데이터가 있습니다. 빈 DB 에서만 실행하세요.';
    END IF;
END $$;

INSERT INTO product_categories (id, name, display_order, created_at, updated_at)
VALUES (gen_random_uuid(), 'Diary', 0, now(), now()),
       (gen_random_uuid(), 'Doll', 1, now(), now()),
       (gen_random_uuid(), 'Photocard', 2, now(), now());

-- created_at 을 서로 다르게 두어 LATEST 정렬을, sales_count_30d 동점(80)으로 id tie-break 를 확인할 수 있다
INSERT INTO products (id, category_id, artist_group_id, artist_id, name, summary, detail_image_key,
                      price, is_diary, sales_count_30d, created_at, updated_at)
SELECT gen_random_uuid(), c.id, v.artist_group_id, v.artist_id, v.name, v.summary, v.detail_image_key,
       v.price, v.is_diary, v.sales_count_30d, now() - v.age, now() - v.age
FROM (VALUES ('Diary', 'aaaaaaaa-0000-0000-0000-000000000000'::uuid, NULL::uuid,
              'Seoul Trip Diary - Group A', '그룹 A 서울 여행 다이어리', 'goods/detail/diary-a.jpg',
              25000::bigint, true, 120, interval '6 days'),
             ('Diary', 'bbbbbbbb-0000-0000-0000-000000000000'::uuid, NULL::uuid,
              'Seoul Trip Diary - Group B', '그룹 B 서울 여행 다이어리', 'goods/detail/diary-b.jpg',
              25000::bigint, true, 45, interval '5 days'),
             ('Doll', 'aaaaaaaa-0000-0000-0000-000000000000'::uuid,
              'aaaaaaaa-0000-0000-0000-000000000001'::uuid,
              'Member A1 Plush Doll', '멤버 A1 인형 20cm', 'goods/detail/doll-a1.jpg',
              32000::bigint, false, 80, interval '4 days'),
             ('Doll', 'bbbbbbbb-0000-0000-0000-000000000000'::uuid,
              'bbbbbbbb-0000-0000-0000-000000000001'::uuid,
              'Member B1 Plush Doll', '멤버 B1 인형 20cm', 'goods/detail/doll-b1.jpg',
              32000::bigint, false, 80, interval '3 days'),
             ('Photocard', 'aaaaaaaa-0000-0000-0000-000000000000'::uuid, NULL::uuid,
              'Group A Photocard Set', '그룹 A 포토카드 8종', 'goods/detail/pc-a.jpg',
              12000::bigint, false, 200, interval '2 days'),
             ('Photocard', 'bbbbbbbb-0000-0000-0000-000000000000'::uuid,
              'bbbbbbbb-0000-0000-0000-000000000001'::uuid,
              'Member B1 Photocard', '멤버 B1 포토카드', NULL,
              5000::bigint, false, 0, interval '1 day')) AS v(category_name, artist_group_id, artist_id, name,
                                                                summary, detail_image_key, price, is_diary,
                                                                sales_count_30d, age)
         JOIN product_categories c ON c.name = v.category_name;

-- 상품마다 이미지 2장 (display_order 0 이 썸네일)
INSERT INTO product_images (id, product_id, image_key, display_order, created_at, updated_at)
SELECT gen_random_uuid(), p.id, 'goods/products/' || p.id || '/' || n || '.jpg', n, now(), now()
FROM products p
         CROSS JOIN generate_series(0, 1) AS n;

-- 프로모션: Main(A) 1개, Sub(B) 2개
INSERT INTO promotions (id, product_id, title, image_key, importance, artist_group_id, artist_id,
                        starts_at, ends_at, display_order, created_at, updated_at)
SELECT gen_random_uuid(), p.id, 'Group A Photocard Launch', 'goods/promotions/main-a.jpg', 'A',
       p.artist_group_id, NULL, NULL, NULL, 0, now(), now()
FROM products p
WHERE p.name = 'Group A Photocard Set';

INSERT INTO promotions (id, product_id, title, image_key, importance, artist_group_id, artist_id,
                        starts_at, ends_at, display_order, created_at, updated_at)
SELECT gen_random_uuid(), p.id, 'Member B1 Doll Week', 'goods/promotions/sub-b1.jpg', 'B',
       p.artist_group_id, p.artist_id, now() - interval '1 day', now() + interval '30 days', 0, now(), now()
FROM products p
WHERE p.name = 'Member B1 Plush Doll';

INSERT INTO promotions (id, product_id, title, image_key, importance, artist_group_id, artist_id,
                        starts_at, ends_at, display_order, created_at, updated_at)
VALUES (gen_random_uuid(), NULL, 'Seoulaim Diary Event', 'goods/promotions/sub-diary.jpg', 'B',
        NULL, NULL, NULL, now() + interval '60 days', 1, now(), now());

COMMIT;
