-- PayPal(외화 MID) 결제 (#59). 토스는 MID 당 통화 하나라서 원화 카드(KRW MID)와 PayPal(FOREIGN MID, USD)을 나눠 운영한다

-- #57 의 method(토스 승인 응답의 결제수단 문자열)는 method_detail 로 옮기고, method 는 앱이 고른 결제수단(CARD|PAYPAL)으로 쓴다
ALTER TABLE diary_payments RENAME COLUMN method TO method_detail;
ALTER TABLE diary_payments ALTER COLUMN method_detail TYPE varchar(50);

-- 기존 행은 전부 원화 카드 결제
ALTER TABLE diary_payments
    ADD COLUMN method   varchar(10) NOT NULL DEFAULT 'CARD',
    ADD COLUMN merchant varchar(10) NOT NULL DEFAULT 'KRW',
    ADD COLUMN currency varchar(3)  NOT NULL DEFAULT 'KRW',
    ADD CONSTRAINT ck_diary_payments_method CHECK (method IN ('CARD', 'PAYPAL')),
    ADD CONSTRAINT ck_diary_payments_merchant CHECK (merchant IN ('KRW', 'FOREIGN')),
    ADD CONSTRAINT ck_diary_payments_currency CHECK (currency IN ('KRW', 'USD'));

-- USD 는 센트 단위 소수가 있어 통화 단위 금액을 numeric 으로 저장한다. 기존 원화 정수 값은 그대로 (3000 → 3000.00)
ALTER TABLE diary_payments ALTER COLUMN amount TYPE numeric(12, 2);
