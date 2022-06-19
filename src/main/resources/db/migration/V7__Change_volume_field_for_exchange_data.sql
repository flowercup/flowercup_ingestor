alter table exchange_data drop column volume;
alter table exchange_data add column volume double precision not null default 0;