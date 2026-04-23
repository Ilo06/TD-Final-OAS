CREATE TYPE occupation_enum AS ENUM ('JUNIOR', 'SENIOR', 'SECRETARY', 'TREASURER', 'VICE_PRESIDENT', 'PRESIDENT');

CREATE TABLE IF NOT EXISTS collectivity (
    id                  VARCHAR(50) PRIMARY KEY,
    number              INTEGER UNIQUE,
    name                VARCHAR(255) UNIQUE,
    location            VARCHAR(255),
    federation_approval BOOLEAN DEFAULT FALSE,
    president_id        VARCHAR(50),
    vice_president_id   VARCHAR(50),
    treasurer_id        VARCHAR(50),
    secretary_id        VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS member (
    id                     VARCHAR(50) PRIMARY KEY,
    first_name             VARCHAR(100),
    last_name              VARCHAR(100),
    birth_date             DATE,
    gender                 VARCHAR(10),
    address                VARCHAR(255),
    profession             VARCHAR(100),
    phone_number           VARCHAR(30),
    email                  VARCHAR(150),
    occupation             occupation_enum,
    collectivity_id        VARCHAR(50),
    registration_fee_paid  BOOLEAN DEFAULT FALSE,
    membership_dues_paid   BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_collectivity FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

ALTER TABLE collectivity
    ADD CONSTRAINT fk_president      FOREIGN KEY (president_id)      REFERENCES member(id),
    ADD CONSTRAINT fk_vice_president FOREIGN KEY (vice_president_id) REFERENCES member(id),
    ADD CONSTRAINT fk_treasurer      FOREIGN KEY (treasurer_id)      REFERENCES member(id),
    ADD CONSTRAINT fk_secretary      FOREIGN KEY (secretary_id)      REFERENCES member(id);

CREATE TABLE IF NOT EXISTS member_referee (
    member_id   VARCHAR(50) NOT NULL,
    referee_id  VARCHAR(50) NOT NULL,
    PRIMARY KEY (member_id, referee_id),
    FOREIGN KEY (member_id)  REFERENCES member(id),
    FOREIGN KEY (referee_id) REFERENCES member(id)
);
