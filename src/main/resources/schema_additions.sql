-- ── AJOUTER à la fin de schema.sql ───────────────────────────────────────

CREATE TYPE frequency_enum AS ENUM ('WEEKLY','MONTHLY','ANNUALLY','PUNCTUALLY');
CREATE TYPE activity_status_enum AS ENUM ('ACTIVE','INACTIVE');
CREATE TYPE payment_mode_enum AS ENUM ('CASH','MOBILE_BANKING','BANK_TRANSFER');
CREATE TYPE mobile_banking_service_enum AS ENUM ('AIRTEL_MONEY','MVOLA','ORANGE_MONEY');
CREATE TYPE bank_enum AS ENUM ('BRED','MCB','BMOI','BOA','BGFI','AFG','ACCES_BAQUE','BAOBAB','SIPEM');

CREATE TABLE IF NOT EXISTS cash_account (
    id              SERIAL PRIMARY KEY,
    amount          NUMERIC DEFAULT 0,
    collectivity_id INTEGER UNIQUE NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

CREATE TABLE IF NOT EXISTS mobile_banking_account (
    id                     SERIAL PRIMARY KEY,
    holder_name            VARCHAR(150),
    mobile_banking_service mobile_banking_service_enum,
    mobile_number          VARCHAR(30),
    amount                 NUMERIC DEFAULT 0,
    collectivity_id        INTEGER NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

CREATE TABLE IF NOT EXISTS bank_account (
    id                  SERIAL PRIMARY KEY,
    holder_name         VARCHAR(150),
    bank_name           bank_enum,
    bank_code           INTEGER,
    bank_branch_code    INTEGER,
    bank_account_number BIGINT,
    bank_account_key    INTEGER,
    amount              NUMERIC DEFAULT 0,
    collectivity_id     INTEGER NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

CREATE TABLE IF NOT EXISTS membership_fee (
    id              SERIAL PRIMARY KEY,
    eligible_from   DATE,
    frequency       frequency_enum,
    amount          NUMERIC,
    label           VARCHAR(255),
    status          activity_status_enum DEFAULT 'ACTIVE',
    collectivity_id INTEGER NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

CREATE TABLE IF NOT EXISTS member_payment (
    id                    SERIAL PRIMARY KEY,
    amount                NUMERIC,
    payment_mode          payment_mode_enum,
    membership_fee_id     INTEGER,
    account_credited_type VARCHAR(20),
    account_credited_id   INTEGER,
    creation_date         DATE DEFAULT CURRENT_DATE,
    member_id             INTEGER NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (membership_fee_id) REFERENCES membership_fee(id)
);

CREATE TABLE IF NOT EXISTS collectivity_transaction (
    id                    SERIAL PRIMARY KEY,
    creation_date         DATE DEFAULT CURRENT_DATE,
    amount                NUMERIC,
    payment_mode          payment_mode_enum,
    account_credited_type VARCHAR(20),
    account_credited_id   INTEGER,
    member_debited_id     INTEGER,
    collectivity_id       INTEGER NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id),
    FOREIGN KEY (member_debited_id) REFERENCES member(id)
);
