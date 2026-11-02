-- Races the runner trains for (issue #16). Same rules as every table: user_id filled by the
-- database, row level security with the own_rows policy, and access for runbuddy_app only.
create table race (
    user_id         text   not null default app_current_user_id(),
    id              uuid   not null,
    name            text   not null check (length(name) between 1 and 100),
    race_date       date   not null,
    distance_meters bigint not null check (distance_meters > 0),
    target_seconds  bigint check (target_seconds > 0),
    primary key (user_id, id)
);

create index race_user_date on race (user_id, race_date);

alter table race enable row level security;
alter table race force row level security;
create policy own_rows on race
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

grant select, insert, update, delete on race to runbuddy_app;
