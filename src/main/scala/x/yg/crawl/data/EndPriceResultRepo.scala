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
    nextDayHigh5m: Int = 0,
    afterDayHigh5d: Int = 0,
    updDt: Timestamp = new Timestamp(java.lang.System.currentTimeMillis()),
    memo: String = "NA"
)

trait EndPriceResultRepo {
  def getEndPriceReuslt(itemCode: String, targetDt: String): ZIO[Any, Throwable, Option[EndPriceResult]]
  def upsertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long]
  def insertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long]
  def updateNextDayHigh5m(itemCode: String, nextDayHigh5m: Int): ZIO[Any, Throwable, Long]
  def updateAfterDayHigh5d(itemCode: String, nextDayHigh5d: Int): ZIO[Any, Throwable, Long]
}

class EndPriceResultRepoImpl (quill: Quill.Mysql[SnakeCase]) extends EndPriceResultRepo {
  import quill._

  override def getEndPriceReuslt(itemCode: String, targetDt: String): ZIO[Any, Throwable, Option[EndPriceResult]] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .filter(_.targetDt == lift(targetDt))
    ).map(_.headOption)

  private inline def qryEndPriceResultTable = quote(querySchema[EndPriceResult](entity = "STOCK_END_PRICE_ANALYZE_RESULT"))

  override def upsertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long] = 
    transaction(for {
      updateCount <- run(
        qryEndPriceResultTable
          .filter(_.itemCode == lift(endPriceResult.itemCode))
          .filter(_.targetDt == lift(endPriceResult.targetDt))
          .update(
            _.matchScore -> lift(endPriceResult.matchScore),
            _.nextDayHigh5m -> lift(endPriceResult.nextDayHigh5m),
            _.afterDayHigh5d -> lift(endPriceResult.afterDayHigh5d),
            _.updDt -> lift(new Timestamp(java.lang.System.currentTimeMillis())),
            _.memo -> lift(endPriceResult.memo)
          )
      )
      result <- updateCount match {
        case 0 => run(
          qryEndPriceResultTable.insertValue(lift(endPriceResult))
        )
        case _ => ZIO.succeed(updateCount)
      }
    } yield result)

  override def insertEndPriceResult(endPriceResult: EndPriceResult): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable.insertValue(lift(endPriceResult))
    )
  
  override def updateNextDayHigh5m(itemCode: String, nextDayHigh5m: Int): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .update(_.nextDayHigh5m -> lift(nextDayHigh5m))
    )

  override def updateAfterDayHigh5d(itemCode: String, nextDayHigh5d: Int): ZIO[Any, Throwable, Long] = 
    run(
      qryEndPriceResultTable
        .filter(_.itemCode == lift(itemCode))
        .update(_.afterDayHigh5d -> lift(nextDayHigh5d))
    )
}

object EndPriceResultRepo {
  def live: ZLayer[Quill.Mysql[SnakeCase], Nothing, EndPriceResultRepo] = 
    ZLayer.fromFunction(EndPriceResultRepoImpl(_))
}