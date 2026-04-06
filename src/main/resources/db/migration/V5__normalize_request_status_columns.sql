-- Normalize status columns to VARCHAR so new enum values like CANCELLED are accepted.
-- This prevents MariaDB ENUM truncation errors when application enums evolve.

ALTER TABLE leave_requests
    MODIFY COLUMN app_status VARCHAR(50) NOT NULL;

ALTER TABLE overtime_requests
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

