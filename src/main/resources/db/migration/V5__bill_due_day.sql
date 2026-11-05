-- A bill can have a due day; the app then marks it paid by itself on that day of each due month.
-- last_auto_month is the last month the app handled that way, so unticking it by hand sticks.
ALTER TABLE bills ADD COLUMN due_day INT;
ALTER TABLE bills ADD COLUMN last_auto_month VARCHAR(7);
