-- First version of the database. Flyway runs each V<n>__*.sql file once, in order,
-- and records it in the flyway_schema_history table. Never edit a file after it has run:
-- add V2__..., V3__... instead.
--
-- Money is stored as whole cents (BIGINT), exactly like the iPhone app, so there are no rounding errors.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- One row per user: the 6-month plan.
CREATE TABLE budget_settings (
    user_id              UUID       PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    start_month          VARCHAR(7) NOT NULL,          -- "2026-11"
    month_count          INT        NOT NULL,
    total_cents          BIGINT     NOT NULL,
    living_monthly_cents BIGINT     NOT NULL,
    engagement_cents     BIGINT     NOT NULL,
    first_opened_at      TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Ids are created by the phone (UUIDs), so the app can save offline and upload later
-- without the server handing out ids.
CREATE TABLE bills (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(60) NOT NULL,
    amount_cents BIGINT      NOT NULL,
    sort_order   INT         NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE              -- set instead of deleting, so other devices learn about it
);
CREATE INDEX bills_user_updated ON bills (user_id, updated_at);

-- Months a bill is due in, and months it was paid in ("2026-11").
CREATE TABLE bill_due_months (
    bill_id   UUID       NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    month_key VARCHAR(7) NOT NULL,
    PRIMARY KEY (bill_id, month_key)
);
CREATE TABLE bill_paid_months (
    bill_id   UUID       NOT NULL REFERENCES bills (id) ON DELETE CASCADE,
    month_key VARCHAR(7) NOT NULL,
    PRIMARY KEY (bill_id, month_key)
);

CREATE TABLE expenses (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount_cents BIGINT       NOT NULL,
    category     VARCHAR(20)  NOT NULL,                -- same names as the app: "food", "skinCare", "bills", ...
    note         VARCHAR(200) NOT NULL,
    spent_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    bill_id      UUID,                                 -- set when the expense is a bill payment
    bill_month   VARCHAR(7),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX expenses_user_spent ON expenses (user_id, spent_at);
CREATE INDEX expenses_user_updated ON expenses (user_id, updated_at);
