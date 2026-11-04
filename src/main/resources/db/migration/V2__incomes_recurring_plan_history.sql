-- Extra income, monthly recurring expenses, and what a new plan keeps from earlier plans.

ALTER TABLE budget_settings ADD COLUMN history_start_month VARCHAR(7);
ALTER TABLE budget_settings ADD COLUMN saved_before_cents BIGINT NOT NULL DEFAULT 0;

-- Extra money received. target: where it went ("living", "savings" or "engagement").
CREATE TABLE incomes (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount_cents BIGINT       NOT NULL,
    target       VARCHAR(20)  NOT NULL,
    note         VARCHAR(200) NOT NULL,
    received_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX incomes_user_updated ON incomes (user_id, updated_at);

-- An expense the app adds every month on day_of_month. last_added_month stops it being added twice,
-- also after a reinstall restores it.
CREATE TABLE recurring_expenses (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount_cents     BIGINT       NOT NULL,
    category         VARCHAR(20)  NOT NULL,
    note             VARCHAR(200) NOT NULL,
    day_of_month     INT          NOT NULL,
    start_month      VARCHAR(7)   NOT NULL,
    last_added_month VARCHAR(7),
    active           BOOLEAN      NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX recurring_user_updated ON recurring_expenses (user_id, updated_at);
