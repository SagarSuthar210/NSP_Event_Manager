-- schema.sql
-- Run with: mysql -u piet_app -p piet_events < schema.sql
-- Mirrors the fields of EventManagerApp.CollegeEvent.

CREATE TABLE IF NOT EXISTS events (
    id            INT PRIMARY KEY,
    name          VARCHAR(255)   NOT NULL,
    event_date    DATE           NOT NULL,
    slot          VARCHAR(20)    NOT NULL,   -- matches TimeSlot enum name(), e.g. SLOT_1
    venue         VARCHAR(30)    NOT NULL,   -- matches Venue enum name(), e.g. MAIN_AUDITORIUM
    category      VARCHAR(20)    NOT NULL,   -- matches Category enum name(), e.g. TECHNICAL
    organizer     VARCHAR(255)   NOT NULL DEFAULT 'Not specified',
    description   TEXT,
    participants  INT            NOT NULL DEFAULT 0,        -- expected/registered participant count
    form_url      VARCHAR(500)   DEFAULT NULL,               -- Google Form link for participant registration

    -- Enforces "one event per venue/date/slot" at the database level,
    -- as a backstop to the app's own isSlotTaken() check.
    UNIQUE KEY uniq_venue_date_slot (event_date, venue, slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;