-- Add leave request reason for approval review.
ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS request_reason VARCHAR(500) NULL;

