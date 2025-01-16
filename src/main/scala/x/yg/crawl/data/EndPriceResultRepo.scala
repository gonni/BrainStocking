package x.yg.crawl.data

import zio._
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{Escape, H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill.*
import java.sql.Timestamp
import java.util.concurrent.TimeUnit


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

trait EndPriceResultRepo

class EndPriceResultRepoImpl {
  
}
