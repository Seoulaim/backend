-- 어드민 장소 관리 (#44)
-- 카테고리 5종 변경, 노출 여부, 요일별 영업시간

-- 카테고리: 기존 3종 → 5종. 기존 데이터는 FANDOM_PLACE 로 이관
ALTER TABLE places DROP CONSTRAINT places_category_check;
UPDATE places SET category = 'FANDOM_PLACE';
ALTER TABLE places ADD CONSTRAINT places_category_check
    CHECK (category IN ('FANDOM_PLACE', 'GOODS_SHOP', 'ALBUM_SHOP', 'PHOTO_BOOTH', 'ATTRACTION'));

-- 노출 여부. false 면 일정 추천·공개 상세에서 제외
ALTER TABLE places ADD COLUMN visible boolean NOT NULL DEFAULT true;

-- 요일별 영업시간. 장소에 행이 없으면 "영업시간 없음 (언제든 방문 가능)"
CREATE TABLE place_opening_hours (
                                     id              uuid        PRIMARY KEY,
                                     place_id        uuid        NOT NULL REFERENCES places (id),
                                     day_of_week     varchar(3)  NOT NULL
                                         CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
                                     closed          boolean     NOT NULL DEFAULT false,
                                     opens_at        time,
                                     closes_at       time,
                                     break_starts_at time,
                                     break_ends_at   time,
                                     created_at      timestamptz NOT NULL,
                                     updated_at      timestamptz NOT NULL,
                                     CONSTRAINT uk_place_opening_hours_place_day UNIQUE (place_id, day_of_week)
);

-- 기존 단일 영업시간 → 7개 요일로 복사
INSERT INTO place_opening_hours (id, place_id, day_of_week, closed, opens_at, closes_at, created_at, updated_at)
SELECT gen_random_uuid(), p.id, d.day_of_week, false, p.opens_at, p.closes_at, now(), now()
FROM places p
         CROSS JOIN (VALUES ('MON'), ('TUE'), ('WED'), ('THU'), ('FRI'), ('SAT'), ('SUN')) AS d (day_of_week)
WHERE p.opens_at IS NOT NULL AND p.closes_at IS NOT NULL;

ALTER TABLE places DROP COLUMN opens_at, DROP COLUMN closes_at;