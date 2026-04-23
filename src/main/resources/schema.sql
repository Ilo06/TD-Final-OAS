CREATE TYPE occupation_enum AS ENUM ('JUNIOR', 'SENIOR', 'SECRETARY', 'TREASURER', 'VICE_PRESIDENT', 'PRESIDENT');
CREATE TABLE IF NOT EXISTS collectivity (
    id                  SERIAL PRIMARY KEY,
    location            VARCHAR(255),
    federation_approval BOOLEAN DEFAULT FALSE,
    president_id        INTEGER,
    vice_president_id   INTEGER,
    treasurer_id        INTEGER,
    secretary_id        INTEGER
);

CREATE TABLE IF NOT EXISTS member (
    id            SERIAL PRIMARY KEY,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    birth_date    DATE,
    gender        VARCHAR(10),
    address       VARCHAR(255),
    profession    VARCHAR(100),
    phone_number  VARCHAR(30),
    email         VARCHAR(150),
    occupation    occupation_enum,
    collectivity_id INTEGER,
    registration_fee_paid  BOOLEAN DEFAULT FALSE,
    membership_dues_paid   BOOLEAN DEFAULT FALSE,
    collectivity_id_fk INTEGER,
    CONSTRAINT fk_collectivity
        FOREIGN KEY (collectivity_id_fk) REFERENCES collectivity(id)
);

ALTER TABLE collectivity
    ADD CONSTRAINT fk_president FOREIGN KEY (president_id) REFERENCES member(id),
    ADD CONSTRAINT fk_vice_president FOREIGN KEY (vice_president_id) REFERENCES member(id),
    ADD CONSTRAINT fk_treasurer FOREIGN KEY (treasurer_id) REFERENCES member(id),
    ADD CONSTRAINT fk_secretary FOREIGN KEY (secretary_id) REFERENCES member(id);

CREATE TABLE IF NOT EXISTS member_referee (
    member_id   INTEGER NOT NULL,
    referee_id  INTEGER NOT NULL,
    PRIMARY KEY (member_id, referee_id),
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (referee_id) REFERENCES member(id)
);

ALTER TABLE collectivity
    ADD COLUMN IF NOT EXISTS number INTEGER UNIQUE,
    ADD COLUMN IF NOT EXISTS name   VARCHAR(255) UNIQUE;