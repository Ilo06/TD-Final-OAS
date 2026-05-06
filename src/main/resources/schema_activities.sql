-- ─────────────────────────────────────────────────────────────────────────────
-- Schema additions for Bonus 1: Activities (E & F)
-- Add these statements to your schema_additions.sql (or run separately)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TYPE activity_type_enum AS ENUM ('MEETING', 'TRAINING', 'OTHER');
CREATE TYPE attendance_status_enum AS ENUM ('MISSING', 'ATTENDED', 'UNDEFINED');
CREATE TYPE day_of_week_enum AS ENUM ('MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU');

-- Main activity table
CREATE TABLE IF NOT EXISTS collectivity_activity (
    id                        VARCHAR(50) PRIMARY KEY,
    label                     VARCHAR(255),
    activity_type             activity_type_enum,
    -- Monthly recurrence rule (NULL when executive_date is set instead)
    recurrence_week_ordinal   INTEGER CHECK (recurrence_week_ordinal BETWEEN 1 AND 5),
    recurrence_day_of_week    day_of_week_enum,
    -- One-off date (NULL when recurrence rule is set instead)
    executive_date            DATE,
    -- Constraint: exactly one of (recurrence rule) or (executive_date) must be set
    CONSTRAINT chk_recurrence_xor_date CHECK (
        (recurrence_week_ordinal IS NOT NULL AND recurrence_day_of_week IS NOT NULL AND executive_date IS NULL)
        OR
        (recurrence_week_ordinal IS NULL AND recurrence_day_of_week IS NULL AND executive_date IS NOT NULL)
    ),
    collectivity_id           VARCHAR(50) NOT NULL,
    FOREIGN KEY (collectivity_id) REFERENCES collectivity(id)
);

-- Occupations targeted by an activity (many-to-many between activity and occupation)
CREATE TABLE IF NOT EXISTS collectivity_activity_occupation (
    activity_id VARCHAR(50)          NOT NULL,
    occupation  occupation_enum      NOT NULL,
    PRIMARY KEY (activity_id, occupation),
    FOREIGN KEY (activity_id) REFERENCES collectivity_activity(id)
);

-- Attendance records (for Bonus 1 - F: attendance endpoints)
CREATE TABLE IF NOT EXISTS activity_member_attendance (
    id                VARCHAR(50) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    activity_id       VARCHAR(50)           NOT NULL,
    member_id         VARCHAR(50)           NOT NULL,
    attendance_status attendance_status_enum NOT NULL DEFAULT 'UNDEFINED',
    UNIQUE (activity_id, member_id),
    FOREIGN KEY (activity_id) REFERENCES collectivity_activity(id),
    FOREIGN KEY (member_id)   REFERENCES member(id)
);
