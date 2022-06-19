create table reddit_data
(
    timestamp timestamptz      not null,
    text      text             not null,
    sentiment double precision not null,
    entities text[] null
);

select create_hypertable('reddit_data', 'timestamp')