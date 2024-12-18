package x.yg.crawl.core

import zio._
import zio.http._
import x.yg.crawl.data.StockRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.checkerframework.checker.units.qual.s
import scalaz.Tags.Min
import javax.xml.crypto.Data
import x.yg.crawl.DataDownloader
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import java.util.concurrent.TimeUnit

object CrawlScheduler extends ZIOAppDefault {

  def producer(queue: Queue[String]): ZIO[Any, Nothing, Unit] = 
    (for {
      _ <- ZIO.sleep(Duration(10, TimeUnit.SECONDS))
      // _ <- queue.offer("068270")
    } yield ()).repeat(Schedule.spaced(1.second)).unit


  val unitProc = (stockCode: String) => for {
    _ <- ZIO.log("Hello World")
    stockRepo <- ZIO.service[StockRepo]
    crawler <- ZIO.service[MinStockCrawler]
    // cd <- crawler.crawl("205470")
    cd <- crawler.crawl(stockCode)
    stockRepo <- ZIO.service[StockRepo]
    // res <- stockRepo.insertStockMinVolumeBulk(cd)
    res <- stockRepo.insertStockMinVolumeSerialBulk(cd)
  } yield cd.foreach(println)
  
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    unitProc("205470").provide(
      Client.customized,
      NettyClientDriver.live,
      ZLayer.succeed(NettyConfig.default),
      DnsResolver.default,
      DataDownloader.live,
      MinStockCrawler.live,
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
}
