-- 장소 다국어(이름·주제·주소), 네이버 지도 링크, 자료화면 링크 (#44)

-- 이름: EN 필수, 나머지 언어는 비면 EN 으로 표시. 기존 이름은 KO·EN 에 복사
ALTER TABLE places RENAME COLUMN name TO name_ko;
ALTER TABLE places ALTER COLUMN name_ko DROP NOT NULL;
ALTER TABLE places ADD COLUMN name_en varchar(200);
UPDATE places SET name_en = name_ko;
ALTER TABLE places ALTER COLUMN name_en SET NOT NULL;
ALTER TABLE places ADD COLUMN name_ja varchar(200);
ALTER TABLE places ADD COLUMN name_zh varchar(200);

-- 주제: 일정 목록에서 장소 이름 아래 한 줄 소개
ALTER TABLE places ADD COLUMN summary_en varchar(200);
ALTER TABLE places ADD COLUMN summary_ko varchar(200);
ALTER TABLE places ADD COLUMN summary_ja varchar(200);
ALTER TABLE places ADD COLUMN summary_zh varchar(200);

-- 주소: KO 는 좌표 찾기 기준
ALTER TABLE places RENAME COLUMN address TO address_ko;
ALTER TABLE places ADD COLUMN address_en varchar(500);
ALTER TABLE places ADD COLUMN address_ja varchar(500);
ALTER TABLE places ADD COLUMN address_zh varchar(500);

ALTER TABLE places ADD COLUMN naver_map_url varchar(512);

-- 팬덤 플레이스 근거 영상 링크 (최대 5개)
ALTER TABLE places ADD COLUMN reference_links jsonb NOT NULL DEFAULT '[]'::jsonb;