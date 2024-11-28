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

object Scheduler extends ZIOAppDefault {

  val program = for {
    _ <- ZIO.log("Hello World")
    stockRepo <- ZIO.service[StockRepo]
    // _ <- stockRepo.selectStockItemsAll().map(_.foreach(println))
    // _ <- ZIO.succeed(println("Hello World")) repeat (Schedule.recurs(5) && Schedule.fixed(1.seconds))
    crawler <- ZIO.service[MinStockCrawler]
    cd <- crawler.crawl("205470")
    // _ <- ZIO.never
  } yield cd.foreach(println)
  
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    program.provide(
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
