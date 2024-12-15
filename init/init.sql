
create table STOCK_MIN_VOLUME (
	item_code varchar(12) not null,
    ts_code varchar(16) not null,
    fixed_price int,
    sell_amt int,
    buy_amt int,
    volume int,
    primary key (item_code, ts_code)
);

create table STOCK_MIN_VOLUME (
    ts_code varchar(10),
    fixed_price int,
    sell_amt int,
    buy_amt int,
    volume int
);

create table STOCK_INFO (
	ITEM_CODE varchar(12) not null primary key,
	ITEM_NAME varchar(64) not null,
	LAST_SYNCED timestamp,
	CRAWL_STATUS int
);