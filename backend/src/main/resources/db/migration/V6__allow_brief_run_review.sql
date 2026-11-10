-- A brief review (#36): the one-line headline written when a run is imported, before anyone asks for
-- the detailed review. It has no run_itself or comparison; a detailed review has both.
alter table run_review alter column run_itself drop not null;
alter table run_review alter column comparison drop not null;
alter table run_review add constraint run_review_brief_or_detailed
    check ((run_itself is null) = (comparison is null));
