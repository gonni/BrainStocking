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

case class StockItem(itemCd: String, companyNm: String, stockCat: String, companyCat: String)
object StockItem:
  given Schema[StockItem] = DeriveSchema.gen[StockItem]

// ---
trait StockRepo {
  def selectStockItemsAll(): ZIO[Any, Throwable, List[StockItem]]
  def insertStockMinVolume(stockMinVolume: StockMinVolumeTable): ZIO[Any, Throwable, Long]
  def insertStockMinVolumeBulk(stockMinVolume: List[StockMinVolumeTable]): ZIO[Any, Throwable, List[Long]]
  def selectStockMinVolume(tsCode: String): Task[List[StockMinVolumeTable]]   
}

class StockRepoImpl(quill: Quill.Mysql[SnakeCase]) extends StockRepo {

  import quill._
  private inline def qryStockMinVolumeTable = quote(querySchema[StockMinVolumeTable](entity = "STOCK_MIN_VOLUME"))
  private inline def qryStockItemsTable = quote(querySchema[StockItem](entity = "STOCK_ITEMS"))

  override def selectStockItemsAll(): ZIO[Any, Throwable, List[StockItem]] = 
    run(
      qryStockItemsTable
    )

  override def insertStockMinVolume(stockMinVolume: StockMinVolumeTable): ZIO[Any, Throwable, Long] =
    run(
      qryStockMinVolumeTable.insertValue(lift(stockMinVolume))
    )

  override def insertStockMinVolumeBulk(stockMinVolume: List[StockMinVolumeTable]): ZIO[Any, Throwable, List[Long]] =
    run(
      liftQuery(stockMinVolume).foreach(e => qryStockMinVolumeTable.insertValue(e))
    )

  override def selectStockMinVolume(tsCode: String): Task[List[StockMinVolumeTable]] =
    run(
      qryStockMinVolumeTable.filter(_.tsCode == lift(tsCode))
    )
  
}

object StockRepo {
  val live: ZLayer[Quill.Mysql[SnakeCase], Nothing, StockRepo] = 
    ZLayer.fromFunction (new StockRepoImpl(_))
}