CREATE MATERIALIZED VIEW if not exists exchange_data_candlestick
    WITH (timescaledb.continuous) AS
select time_bucket('1 min', timestamp) AS bucket,
       coin,
       exchange,
       first(value, timestamp)         as open,
       max(value)                      as high,
       min(value)                      as low,
       last(value, timestamp)          as close
from exchange_data
group by bucket, coin, exchange;

SELECT add_continuous_aggregate_policy('exchange_data_candlestick',
                                       start_offset := null,
                                       end_offset := null,
                                       schedule_interval => INTERVAL '15 sec');