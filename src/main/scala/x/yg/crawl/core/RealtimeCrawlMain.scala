package x.yg.crawl.core
import zio._
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.DataDownloader
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase

object RealtimeCrawlMain extends ZIOAppDefault {
	
	val app = for {
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
	
	override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
		app.provide(
			// Client.customized,
			// NettyClientDriver.live,
			// ZLayer.succeed(NettyConfig.default),
			// DnsResolver.default,
			DataDownloader.live,
			MinStockCrawler.live,
			CrawlStatusRepo.live,
			StockRepo.live,
			Quill.Mysql.fromNamingStrategy(SnakeCase),
			Quill.DataSource.fromPrefix("StockMysqlAppConfig")
		).exitCode	
}