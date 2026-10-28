-- TODO(#10) 해소: goods 테이블의 artist 참조에 FK 추가
-- 기존 데이터에 artist_groups·artists 에 없는 id 가 있으면 실패 → 적용 전 정리 필요

ALTER TABLE products
    ADD CONSTRAINT fk_products_artist_group FOREIGN KEY (artist_group_id) REFERENCES artist_groups (id),
    ADD CONSTRAINT fk_products_artist FOREIGN KEY (artist_id) REFERENCES artists (id);

ALTER TABLE promotions
    ADD CONSTRAINT fk_promotions_artist_group FOREIGN KEY (artist_group_id) REFERENCES artist_groups (id),
    ADD CONSTRAINT fk_promotions_artist FOREIGN KEY (artist_id) REFERENCES artists (id);