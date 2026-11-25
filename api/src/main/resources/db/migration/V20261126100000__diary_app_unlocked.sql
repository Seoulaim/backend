-- 앱(안드로이드·iOS) 다이어리 무료 (#78)
-- 앱에서 무료 범위를 넘겨 생성·저장·완성한 다이어리. true 면 웹에서도 결제 없이 편집·완성할 수 있다 (paid 와 별개, 결제 기록 아님)
ALTER TABLE diaries ADD COLUMN app_unlocked boolean NOT NULL DEFAULT false;
