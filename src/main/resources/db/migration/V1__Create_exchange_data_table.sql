create table exchange_data
(
    timestamp timestamptz not null,
    value     bigint      not null,
    coin varchar(128) not null,
    exchange  varchar(64) not null
);

select create_hypertable('exchange_data', 'timestamp')