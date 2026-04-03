-- Add department code to users and backfill existing rows.
ALTER TABLE users
    ADD COLUMN department_code VARCHAR(2) NULL AFTER user_pw;

UPDATE users
SET department_code = '01'
WHERE department_code IS NULL;

ALTER TABLE users
    MODIFY department_code VARCHAR(2) NOT NULL;

