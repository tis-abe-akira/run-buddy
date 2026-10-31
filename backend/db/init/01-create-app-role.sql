-- Runs once when a fresh PostgreSQL container initialises (docker-entrypoint-initdb.d).
--
-- The application connects as runbuddy_app. It does not own the tables, so row level
-- security applies to it; the owner (and any superuser) would bypass the policies.
-- Migrations run as the owner. The password is for local development and tests only.
create role runbuddy_app login password 'runbuddy_app';
