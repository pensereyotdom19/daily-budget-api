-- Subscriptions like Netflix or iCloud: a price that renews every month or every year.
-- cycle: "monthly" or "yearly". next_renewal: the next time it is charged.
-- anchor_day: the day of the month it renews on (1..31), so a renewal on the 31st stays on the 31st
-- (or the month's last day) instead of drifting after a short month.
-- start_month: the first month it was paid, like "2026-11".
CREATE TABLE subscriptions (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(60) NOT NULL,
    amount_cents BIGINT      NOT NULL,
    cycle        VARCHAR(10) NOT NULL,
    next_renewal TIMESTAMP WITH TIME ZONE NOT NULL,
    anchor_day   INT         NOT NULL,
    start_month  VARCHAR(7)  NOT NULL,
    active       BOOLEAN     NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX subscriptions_user_updated ON subscriptions (user_id, updated_at);
