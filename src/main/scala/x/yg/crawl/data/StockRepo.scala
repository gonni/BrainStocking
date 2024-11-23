package x.yg.crawl.data

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{Escape, H2ZioJdbcContext}
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.*
import zio.Task
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}
import zio.schema._
import zio.schema.DeriveSchema._
import scala.meta.internal.javacp.BaseType.S
import java.sql.SQLException

case class StockMinVolumeTable(tsCode: String, fixedPrice: Double, sellAmt: Int, buyAmt: Int, volume: Int)
object StockMinVolumeTable:
  given Schema[StockMinVolumeTable] = DeriveSchema.gen[StockMinVolumeTable]

trait StockRepo {
  def insertStockMinVolume(stockMinVolume: StockMinVolumeTable): ZIO[Any, Throwable, Long]
  def selectStockMinVolume(tsCode: String): Task[List[StockMinVolumeTable]]  
}

class StockRepoImpl(quill: Quill.Mysql[SnakeCase]) extends StockRepo {

  import quill._
  private inline def qryStockMinVolumeTable = quote(querySchema[StockMinVolumeTable](entity = "STOCK_MIN_VOLUME"))

  def insertStockMinVolume(stockMinVolume: StockMinVolumeTable): ZIO[Any, Throwable, Long] =
    run(
      qryStockMinVolumeTable.insertValue(lift(stockMinVolume))
    )

  def selectStockMinVolume(tsCode: String): Task[List[StockMinVolumeTable]] =
    run(
      qryStockMinVolumeTable.filter(_.tsCode == lift(tsCode))
    )
  
}

object StockRepo {
  val live: ZLayer[Quill.Mysql[SnakeCase], Nothing, StockRepo] = 
    ZLayer.fromFunction (new StockRepoImpl(_))
}