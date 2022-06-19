create or replace function exchange_data_increase(coin text, exchange text, fromTime timestamptz, toTime timestamptz)
    returns table
            (
                first_value bigint,
                last_value  bigint,
                change_type text,
                change      double precision
            )
as
$body$
select first(value, timestamp) as first_value,
       last(value, timestamp)  as last_value,
       case
           when first(value, timestamp) < last(value, timestamp) THEN
               'INCREASE'
           when first(value, timestamp) > last(value, timestamp) THEN
               'DECREASE'
           end
                               as change_type,
       case
           when first(value, timestamp) > last(value, timestamp) THEN
                       (first(value, timestamp)::decimal - last(value, timestamp)::decimal) /
                       (first(value, timestamp)::decimal) * 100
           when first(value, timestamp) < last(value, timestamp) THEN
                       (last(value, timestamp)::decimal - first(value, timestamp)::decimal) /
                       (first(value, timestamp)::decimal) * 100
           end
                               as change
from exchange_data
where exchange_data.coin = $1
  and exchange_data.exchange = $2
  and exchange_data.timestamp between $3 AND $4
$body$
    language sql;
