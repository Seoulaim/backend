-- 키워드 언어별 이름 (어드민 키워드 탭)
-- 영어 필수, 나머지 언어는 비면 영어로 표시
ALTER TABLE keywords ADD COLUMN name_en varchar(100);
ALTER TABLE keywords ADD COLUMN name_ko varchar(100);
ALTER TABLE keywords ADD COLUMN name_ja varchar(100);
ALTER TABLE keywords ADD COLUMN name_zh varchar(100);

-- 기존 키워드 이름 채우기 (알 수 없는 코드는 코드 기반 영어 이름)
UPDATE keywords
SET name_en = CASE code
                  WHEN 'CAFE' THEN 'Cafe'
                  WHEN 'RESTAURANT' THEN 'Restaurant'
                  WHEN 'PHOTO_SPOT' THEN 'Photo Spot'
                  WHEN 'ALBUM_SHOOTING_SITE' THEN 'Album Shooting Site'
                  WHEN 'CONCERT_HALL' THEN 'Concert Hall'
                  WHEN 'POPUP_STORE' THEN 'Pop-up Store'
                  WHEN 'BIRTHDAY_CAFE' THEN 'Birthday Cafe'
                  WHEN 'GOODS_SHOP' THEN 'Goods Shop'
                  WHEN 'DRAMA_FILMING_SITE' THEN 'Drama Filming Site'
                  WHEN 'MV_FILMING_SITE' THEN 'MV Filming Site'
                  WHEN 'PARK' THEN 'Park'
                  WHEN 'SHOPPING' THEN 'Shopping'
                  WHEN 'EXHIBITION' THEN 'Exhibition'
                  WHEN 'MUSEUM' THEN 'Museum'
                  WHEN 'NIGHT_VIEW' THEN 'Night View'
                  WHEN 'HEALING' THEN 'Healing'
                  WHEN 'ETC' THEN 'Etc'
                  ELSE initcap(replace(lower(code), '_', ' '))
    END,
    name_ko = CASE code
                  WHEN 'CAFE' THEN '카페'
                  WHEN 'RESTAURANT' THEN '맛집'
                  WHEN 'PHOTO_SPOT' THEN '포토스팟'
                  WHEN 'ALBUM_SHOOTING_SITE' THEN '앨범 촬영지'
                  WHEN 'CONCERT_HALL' THEN '공연장'
                  WHEN 'POPUP_STORE' THEN '팝업스토어'
                  WHEN 'BIRTHDAY_CAFE' THEN '생일카페'
                  WHEN 'GOODS_SHOP' THEN '굿즈샵'
                  WHEN 'DRAMA_FILMING_SITE' THEN '드라마 촬영지'
                  WHEN 'MV_FILMING_SITE' THEN '뮤비 촬영지'
                  WHEN 'PARK' THEN '공원'
                  WHEN 'SHOPPING' THEN '쇼핑'
                  WHEN 'EXHIBITION' THEN '전시'
                  WHEN 'MUSEUM' THEN '박물관'
                  WHEN 'NIGHT_VIEW' THEN '야경'
                  WHEN 'HEALING' THEN '힐링'
                  WHEN 'ETC' THEN '기타'
        END;

ALTER TABLE keywords ALTER COLUMN name_en SET NOT NULL;