package x.yg.crawl


import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.api.ServiceController
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockRepo
import zio.*
import zio.ZIO
import zio.ZIOAppDefault
import zio.http.*
import zio.http.Middleware.basicAuth
import java.net.URI
import x.yg.crawl.api.StockController
import scala.meta.internal.javacp.BaseType.S
import x.yg.analyser.DayVaildator
import x.yg.crawl.core.StockCrawlerService
import scalaz.Tags.Min
import x.yg.crawl.core.MinStockCrawler
import javax.xml.crypto.Data
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.analyser.EndPriceAnalyzer
import x.yg.crawl.core.CrawlJobScheduler
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault { 
  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val apps = ZIO.serviceWith[ServiceController] {
    controller => 
      controller.routes
  }

  val crawlApps = ZIO.serviceWith[StockController] {
    controller => 
      controller.routes
  }

  val realtimeCrawl = for {
		queueRef <- Ref.make(List.empty[Queue[String]])
		stockRepo <- ZIO.service[StockRepo]
		crawler <- ZIO.service[MinStockCrawler]
		crawlStatus <- ZIO.service[CrawlStatusRepo]
		scheduler <- CrawlJobScheduler(
			queueRef, 
			stockRepo,
			crawler,
			crawlStatus
		).startProduce()
	} yield ()

  val program = for {
    _ <- realtimeCrawl.forkDaemon
    _ <- ZIO.log("realtime crawl started ...")
    app <- apps
    crawlApps <- crawlApps
    _ <- Server.serve((app ++ crawlApps) @@ Middleware.debug)
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    Runtime.removeDefaultLoggers
    program.provide(
      DayVaildator.live,
      EndPriceResultRepo.live,
      EndPriceAnalyzer.live,
      Server.defaultWithPort(8080),
      ServiceController.live,
      StockController.live,
      StockRepo.live,
      ScheduleRepo.live,
      CrawlStatusRepo.live,
      StockCrawlerService.live,
      MinStockCrawler.live,
      DataDownloader.live,
      Client.customized,
      NettyClientDriver.live,
      ZLayer.succeed(NettyConfig.default),
      DnsResolver.default,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
  }
}
