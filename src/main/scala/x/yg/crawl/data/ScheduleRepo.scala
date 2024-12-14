package x.yg.crawl.data

import zio._
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{Escape, H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill.*
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

case class StockItemInfo(
  itemCode: String,
  itemName: String,
  lastSynced: Timestamp = new Timestamp(java.lang.System.currentTimeMillis())
)
trait ScheduleRepo {
  def syncStockItemInfo(stockItemInfo: StockItemInfo): ZIO[Any, Throwable, Long]
  def getStockItemInfo(itemCode: String) : ZIO[Any, Throwable, StockItemInfo]
}

class ScheduleRepoImlp(quill: Quill.Mysql[SnakeCase]) extends ScheduleRepo {
  import quill.*
  private inline def qryStockItemInfo = quote(querySchema[StockItemInfo](entity = "STOCK_INFO"))

  override def syncStockItemInfo(stockItemInfo: StockItemInfo): ZIO[Any, Throwable, Long] = 
    run(
      qryStockItemInfo.insertValue(lift(stockItemInfo))
    )

  override def getStockItemInfo(itemCode: String): ZIO[Any, Throwable, StockItemInfo] = 
    run(
      qryStockItemInfo.filter(_.itemCode == lift(itemCode))
    ).map(_.head)
 
}

object ScheduleRepo {
  def live: ZLayer[Quill.Mysql[SnakeCase], Nothing, ScheduleRepo] = 
    ZLayer.fromFunction(ScheduleRepoImlp(_)
  )
}