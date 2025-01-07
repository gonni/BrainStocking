package x.yg.crawl.data

import zio._
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.jdbczio.Quill
import io.getquill.*
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
  import io.getquill.extras.SqlTimestampOps


case class CrawlStatus(
    itemCode: String,
    statusCode: String,
    latestCrawlDt: Timestamp = new Timestamp(java.lang.System.currentTimeMillis()),
)

trait CrawlStatusRepo {
  def syncCrawlStatus(itemCode: String, crawlStatus: String): ZIO[Any, Throwable, Long]
  def getCrawlStatus(itemCode: String) : ZIO[Any, Throwable, CrawlStatus]
  def getTargetToCrawl(statusCode: String): ZIO[Any, Throwable, List[String]]
  def getExpiredItemCode(min: Int): ZIO[Any, Throwable, List[String]]
}

class CrawlStatusRepoImpl (quill: Quill.Mysql[SnakeCase]) extends CrawlStatusRepo {
  import quill._

  override def getExpiredItemCode(min: Int): ZIO[Any, Throwable, List[String]] = 
    run(
      qryCrawlStatusTable
        .filter(_.latestCrawlDt < lift(new Timestamp(java.lang.System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(min))))
    ).map(_.map(_.itemCode))

  private inline def qryCrawlStatusTable = quote(querySchema[CrawlStatus](entity = "STOCK_CRAWL_STATUS"))

  // upsert data
  override def syncCrawlStatus(itemCode: String, statusCode: String): ZIO[Any, Throwable, Long] = 
    transaction(for {
        updateCount <- run(
          qryCrawlStatusTable
            .filter(_.itemCode == lift(itemCode))
            .update(_.statusCode -> lift(statusCode), _.latestCrawlDt -> lift(new Timestamp(java.lang.System.currentTimeMillis())))
        )
        result <- updateCount match {
          case 0 => run(
            qryCrawlStatusTable.insertValue(lift(CrawlStatus(itemCode, statusCode)))
          )
          case _ => ZIO.succeed(updateCount)
        }
    } yield result)

  override def getTargetToCrawl(statusCode: String): ZIO[Any, Throwable, List[String]] = ???

  override def getCrawlStatus(itemCode: String): ZIO[Any, Throwable, CrawlStatus] = 
    run(
      qryCrawlStatusTable.filter(_.itemCode == lift(itemCode))
    ).map(_.head)
}

object CrawlStatusRepo {
  def live: ZLayer[Quill.Mysql[SnakeCase], Nothing, CrawlStatusRepo] = 
    ZLayer.fromFunction(CrawlStatusRepoImpl(_))
}