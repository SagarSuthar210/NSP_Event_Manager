-- migration_add_participants.sql
-- Run this ONCE against your existing piet_events database to add the
-- new columns without losing any events you've already saved.
-- Run with: mysql -u piet_app -p piet_events < migration_add_participants.sql

ALTER TABLE events
    ADD COLUMN participants INT NOT NULL DEFAULT 0 AFTER description,
    ADD COLUMN form_url VARCHAR(500) DEFAULT NULL AFTER participants;