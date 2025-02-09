package x.yg.crawl.data

import zio._
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{Escape, H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill.*
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
// import io.getquill.autoQuote
import sourcecode.Text.generate


// STOCK_END_PRICE_ANALYZE_RESULT
case class EndPriceResult(
    targetDt: String,
    itemCode: String,
    matchScore: Float = 0.0f,
    basePrice: Int = 0,
    nextDayHigh5m: Int = 0,
    afterDayHigh5d: Int = 0,
    updDt: Timestamp = new Timestamp(java.lang.System.currentTimeMillis()),
    memo: String = "NA"
)

trait EndPriceResultRepo {
  def getEndPriceReuslt(itemCode: String, targetDt: String): ZIO[Any, Throwable, Option[EndPriceResult]]
  def getNonCheck10mResult(): ZIO[Any, Throwable, List[EndPriceResult]]
  def upsertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long]
  def insertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long]
  def updateNextDayHigh5m(itemCode: String, targetDt: String, nextDayHigh5m: Int): ZIO[Any, Throwable, Long]
  def updateAfterDayHigh5d(itemCode: String, targetDt: String, nextDayHigh5d: Int): ZIO[Any, Throwable, Long]
}

class EndPriceResultRepoImpl (quill: Quill.Mysql[SnakeCase]) extends EndPriceResultRepo {
  import quill._
  private inline def qryEndPriceResultTable = quote(querySchema[EndPriceResult](entity = "STOCK_END_PRICE_ANALYZE_RESULT"))

  override def getNonCheck10mResult(): ZIO[Any, Throwable, List[EndPriceResult]] = 
    run(
      qryEndPriceResultTable
        .filter(_.nextDayHigh5m == 0)
        // .filter(_.targetDt >= lift("20250201"))
    )

  override def getEndPriceReuslt(itemCode: String, targetDt: String): ZIO[Any, Throwable, Option[EndPriceResult]] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .filter(_.targetDt == lift(targetDt))
    ).map(_.headOption)

  override def upsertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long] = {
    transaction(for {
      updateCount <- executeUpdate(endPriceResult)
      result <- updateCount match {
        case 0 => run(qryEndPriceResultTable.insertValue(lift(endPriceResult)))
        case _ => ZIO.succeed(updateCount)
      }
    } yield result)
  }

  def executeUpdate(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long] = {
    val baseQuery = new StringBuilder(
      s"UPDATE STOCK_END_PRICE_ANALYZE_RESULT SET upd_dt = '${endPriceResult.updDt}', memo = '${sanitize(endPriceResult.memo)}'"
    )

    if (endPriceResult.matchScore != 0.0f) {
      baseQuery.append(s", match_score = ${endPriceResult.matchScore}")
    }
    if (endPriceResult.basePrice != 0) {
      baseQuery.append(s", base_price = ${endPriceResult.basePrice}")
    }
    if (endPriceResult.nextDayHigh5m != 0) {
      baseQuery.append(s", next_day_high5m = ${endPriceResult.nextDayHigh5m}")
    }
    if (endPriceResult.afterDayHigh5d != 0) {
      baseQuery.append(s", after_day_high5d = ${endPriceResult.afterDayHigh5d}")
    }

    baseQuery.append(s" WHERE item_code = '${sanitize(endPriceResult.itemCode)}' AND target_dt = '${endPriceResult.targetDt}'")

    val sqlString = baseQuery.toString()
    println(s"Executing SQL: $sqlString")

    quill.run(quote({sql"""#$sqlString""".as[Action[Long]]}))
  }

  def sanitize(value: String): String = {
    value.replaceAll("'", "''")
  }

  override def insertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable.insertValue(lift(endPriceResult))
    )
  
  override def updateNextDayHigh5m(itemCode: String, targetDt: String, nextDayHigh5m: Int): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .filter(_.targetDt == lift(targetDt))
        .update(_.nextDayHigh5m -> lift(nextDayHigh5m), 
        _.updDt -> lift(new Timestamp(java.lang.System.currentTimeMillis())))
    )

  override def updateAfterDayHigh5d(itemCode: String, targetDt: String, nextDayHigh5d: Int): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .filter(_.targetDt == lift(targetDt))
        .update(_.afterDayHigh5d -> lift(nextDayHigh5d), 
        _.updDt -> lift(new Timestamp(java.lang.System.currentTimeMillis())))
    )
}

object EndPriceResultRepo {
  def live: ZLayer[Quill.Mysql[SnakeCase], Nothing, EndPriceResultRepo] = 
    ZLayer.fromFunction(EndPriceResultRepoImpl(_))
}