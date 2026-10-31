-- Every row belongs to one user. The application never writes user_id: the column is filled
-- from the transaction-local setting app.user_id (see RlsTransactionManager), and row level
-- security hides other users' rows.

-- The current user, or an error when no transaction has set one. Failing loudly matters: with a
-- plain current_setting() a query that forgot its transaction just sees zero rows, which hides
-- the bug (this happened in the ADR-0007 spike). nullif: once a SET LOCAL has ended, PostgreSQL
-- leaves the setting as '' rather than NULL on that pooled connection.
create function app_current_user_id() returns text
    language plpgsql stable
as $$
declare
    user_id text := nullif(current_setting('app.user_id', true), '');
begin
    if user_id is null then
        raise exception 'app.user_id is not set; database access must run in a transaction (RlsTransactionManager)';
    end if;
    return user_id;
end
$$;

create table monthly_plan (
    user_id       text   not null default app_current_user_id(),
    month         text   not null check (month ~ '^\d{4}-(0[1-9]|1[0-2])$'),
    target_meters bigint not null check (target_meters >= 0),
    primary key (user_id, month)
);

create table planned_workout (
    user_id         text    not null default app_current_user_id(),
    month           text    not null,
    position        integer not null,
    workout_date    date    not null,
    type            text    not null check (type in ('EASY', 'TEMPO', 'INTERVAL', 'LONG', 'RACE')),
    distance_meters bigint  not null check (distance_meters >= 0),
    note            text    not null,
    primary key (user_id, month, position),
    foreign key (user_id, month) references monthly_plan (user_id, month) on delete cascade
);

create table run_result (
    user_id            text      not null default app_current_user_id(),
    activity_id        text      not null,
    started_at         timestamp not null,
    distance_meters    bigint    not null check (distance_meters > 0),
    duration_millis    bigint    not null check (duration_millis > 0),
    average_heart_rate integer,
    primary key (user_id, activity_id)
);

create index run_result_user_started_at on run_result (user_id, started_at);

-- Row level security. FORCE makes the owner subject to it too (superusers still bypass it).
-- The function sits in a scalar subquery so it runs once per statement rather than once per row.
alter table monthly_plan enable row level security;
alter table monthly_plan force row level security;
create policy own_rows on monthly_plan
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

alter table planned_workout enable row level security;
alter table planned_workout force row level security;
create policy own_rows on planned_workout
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

alter table run_result enable row level security;
alter table run_result force row level security;
create policy own_rows on run_result
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

grant select, insert, update, delete on monthly_plan, planned_workout, run_result to runbuddy_app;
