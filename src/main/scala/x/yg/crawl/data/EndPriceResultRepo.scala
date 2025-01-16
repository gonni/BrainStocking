package x.yg.crawl.data

import java.sql.Timestamp


// STOCK_END_PRICE_ANALYZE_RESULT
case class EndPriceResult(
    targetDt: String,
    itemCode: String,
    matchScore: Float,
    nextDayHigh5m: Int,
    nextDayHigh5d: Int,
    afterDayHigh4d: Int,
    latestOutDt: Timestamp,
    memo: String
)

class EndPriceResultRepo {
  
}
