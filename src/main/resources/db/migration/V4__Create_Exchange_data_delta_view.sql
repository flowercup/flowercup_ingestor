create or replace function exchange_data_delta(coin text, exchange text, fromTime timestamptz, toTime timestamptz)
    returns table
            (
                first_value bigint,
                last_value  bigint,
                delta       bigint
            )
as
$body$
select first(value, timestamp)                          as first_value,
       last(value, timestamp)                           as last_value,
       first(value, timestamp) - last(value, timestamp) as delta
from exchange_data
where exchange_data.coin = $1
  and exchange_data.exchange = $2
  and exchange_data.timestamp between $3 AND $4
$body$
    language sql;
