
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

-- stock.stock_crawl_status definition

CREATE TABLE `stock_crawl_status` (
  `item_code` varchar(12) COLLATE utf8mb4_general_ci NOT NULL,
  `latest_crawl_dt` timestamp NULL DEFAULT NULL,
  `status_code` varchar(4) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Deprecated
create table STOCK_END_PRICE_ANALYZE_RESULT (
	TARGET_DT varchar(8) not null,
	ITEM_CODE varchar(12) not null,
	MATCH_SCORE int not null,
	NEXTDAY_HIGH_5M int,
	NEXTDAY_HIGH int,
	AFTERDAY_HIGH_5D int,
	LATEST_OUT_DT timestamp,
	MEMO varchar(256),
	primary key (TARGET_DT, ITEM_CODE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ;

--
create table STOCK_END_PRICE_ANALYZE_RESULT (
	TARGET_DT varchar(8) not null,
	ITEM_CODE varchar(12) not null,
	MATCH_SCORE float not null,
	NEXT_DAY_HIGH5M int,
	AFTER_DAY_HIGH5D int,
	UPD_DT timestamp,
	MEMO varchar(256),
	primary key (TARGET_DT, ITEM_CODE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ;