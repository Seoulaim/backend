-- 다이어리 저장·다운로드 이력 (#57). 환불 심사(편집·완성·다운로드 여부) 판단용

-- 편집 데이터 저장(PUT spreads·finalize) 횟수와 마지막 저장 시각, 마지막 저장 시점의 placePhotos 항목 수
ALTER TABLE diaries ADD COLUMN save_count    int NOT NULL DEFAULT 0;
ALTER TABLE diaries ADD COLUMN last_saved_at timestamptz;
ALTER TABLE diaries ADD COLUMN photo_count   int NOT NULL DEFAULT 0;

-- 완성 다이어리 다운로드(클라이언트가 PDF·JPG 생성 후 보고) 이력
CREATE TABLE diary_downloads (
    id         uuid        PRIMARY KEY,
    diary_id   uuid        NOT NULL REFERENCES diaries (id),
    user_id    uuid        NOT NULL REFERENCES users (id),
    format     varchar(10) NOT NULL CHECK (format IN ('PDF', 'JPG')),
    page_count int         NOT NULL CHECK (page_count >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_diary_downloads_diary_id ON diary_downloads (diary_id);
