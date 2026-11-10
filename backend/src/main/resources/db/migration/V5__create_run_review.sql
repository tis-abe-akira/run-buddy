-- An AI's review of one run (#15, #35): a headline, the run itself and the comparison with similar
-- recent runs. One per run; reviewing again replaces it, and it goes with its run. Same rules as
-- every table: user_id filled by the database, the own_rows policy, access for runbuddy_app.
create table run_review (
    user_id     text        not null default app_current_user_id(),
    activity_id text        not null,
    headline    text        not null,
    run_itself  text        not null,
    comparison  text        not null,
    reviewer    text        not null,
    model       text        not null,
    reviewed_at timestamptz not null,
    primary key (user_id, activity_id),
    foreign key (user_id, activity_id) references run_result (user_id, activity_id) on delete cascade
);

alter table run_review enable row level security;
alter table run_review force row level security;
create policy own_rows on run_review
    using (user_id = (select app_current_user_id()))
    with check (user_id = (select app_current_user_id()));

grant select, insert, update, delete on run_review to runbuddy_app;
