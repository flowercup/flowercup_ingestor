create table tweet_data
(
    timestamp timestamptz      not null,
    text      text             not null,
    coin      varchar(128)     not null,
    sentiment double precision not null
);

select create_hypertable('tweet_data', 'timestamp')