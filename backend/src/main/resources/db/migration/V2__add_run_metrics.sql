-- Measurements Garmin's activity list already returns (ADR-0009). All optional: manual entries
-- and watches without the sensor leave them empty. Row level security on run_result is unchanged.
alter table run_result
    add column max_heart_rate           integer check (max_heart_rate > 0),
    add column average_cadence          integer check (average_cadence > 0),
    add column max_cadence              integer check (max_cadence > 0),
    add column average_stride_length_cm integer check (average_stride_length_cm > 0),
    add column elevation_gain_meters    integer check (elevation_gain_meters >= 0);
