-- The original activity file of a run, zipped as Garmin's download returns it (ADR-0012, #13).
-- Laps and tracks are read from it on demand, so there are no tables for them. Same rules as
-- every table: user_id filled by the database, the own_rows policy, access for runbuddy_app.
create table run_fit_file (
    user_id     text        not null default app_current_user_id(),
    activity_id text        not null,
    fit_zip     bytea       not null,
    fetched_at  timestamptz not null default now(),
    primary key (user_id, activity_id),
    foreign key (user_id, activity_id) references run_result (user_id, activity_id) on delete cascade
);

alter table run_fit_file enable row level security;
alter table run_fit_file force row level security;
create policy own_rows on run_fit_file
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

grant select, insert, update, delete on run_fit_file to runbuddy_app;
