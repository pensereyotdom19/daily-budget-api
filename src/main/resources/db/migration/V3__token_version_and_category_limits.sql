-- "Sign out everywhere": every token carries the user's token_version from when it was made.
-- Raising this number makes all older tokens stop working at once.
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- Optional spending limit per category, as JSON text: {"coffee":1500,"food":3000} (cents).
-- NULL means no limits.
ALTER TABLE budget_settings ADD COLUMN category_limits TEXT;
