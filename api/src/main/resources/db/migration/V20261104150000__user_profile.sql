-- 마케팅 수신 동의 변경 시각. null = 가입 후 한 번도 바꾸지 않음 (가입 시 기본값 false)
ALTER TABLE users ADD COLUMN marketing_updated_at timestamptz;
