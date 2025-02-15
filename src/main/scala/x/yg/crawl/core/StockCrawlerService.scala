package x.yg.crawl.core

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.checkerframework.checker.units.qual.s
import scalaz.Tags.Min
import x.yg.crawl.DataDownloader
import x.yg.crawl.data.StockRepo
import x.yg.crawl.utils.DataUtil
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver

import java.util.concurrent.TimeUnit
import javax.xml.crypto.Data
import org.scalafmt.config.Docstrings.BlankFirstLine.yes
import x.yg.crawl.data.CrawlStatusRepo

trait StockCrawlerService {
  def crawlDayData(stockCode: String, targetDt: String): ZIO[Scope, Throwable, Unit]
  def crawlAllDayData(targetDt: String): ZIO[Scope, Throwable, Int]
}

object StockCrawlerService {
  val live: ZLayer[CrawlStatusRepo & MinStockCrawler & StockRepo & ZClient[Any, Scope, Body, Throwable, Response] & DataDownloader, Nothing, StockCrawlerService] =
    ZLayer.fromFunction { (
      minStockCrawler: MinStockCrawler, 
      stockRepo: StockRepo, 
      client: ZClient[Any, Scope, Body, Throwable, Response], 
      dataDownloader: DataDownloader,
      crawlStatusRepo: CrawlStatusRepo
      ) =>
      new StockCrawlerService {
        
        override def crawlDayData(stockCode: String, targetDt: String): ZIO[Scope, Throwable, Unit] = {
          (ZIO.collectAllDiscard(
            (1 to 40).map { pageNo =>
              (for {
                _ <- Console.printLine(s"Start crawlDayData $stockCode $targetDt -> $pageNo")
                cd <- minStockCrawler.crawl(stockCode, targetDt, pageNo)
                _ <- stockRepo.insertStockMinVolumeSerialBulk(cd)
                _ <- ZIO.sleep(1.second) *> ZIO.log(s"Done crawlDayData $stockCode $targetDt $pageNo")
              } yield ()).ignore
            }
          ) *> ZIO.log(s"crawl daily data of $stockCode done .."))
          .provideSomeLayer[Scope](
            ZLayer.succeed(client) ++ ZLayer.succeed(dataDownloader)
          )
        }

        override def crawlAllDayData(targetDt: String): ZIO[Scope, Throwable, Int] = for {
          // crawlStatus <- ZIO.service[CrawlStatusRepo]
          targetSeeds <- crawlStatusRepo.getTargetToCrawl("ACTV")
          _ <- ZIO.foreach(targetSeeds)(stockCode =>
            Console.printLine(s"targetStockCode : $stockCode") *> crawlDayData(stockCode, targetDt))
        } yield targetSeeds.size

      }
    }
}

// object DailyAllCrawler0 extends ZIOAppDefault {

// 	def crawlDayData(stockCode: String, targetDt: String = DataUtil.stockTimestamp()): 
// 		ZIO[ZClient[Any, Scope, Body, Throwable, Response] & DataDownloader & (MinStockCrawler & StockRepo), Nothing, Unit] = {
// 		ZIO.collectAllDiscard(
// 			(1 to 40).map { pageNo => 
// 				(for {
// 					_ <- Console.printLine(s"Start crawlDayData ${stockCode} ${targetDt} -> ${pageNo}")
// 					crawler <- ZIO.service[MinStockCrawler]
// 					cd <- crawler.crawl(stockCode, targetDt, pageNo)
// 					stockRepo <- ZIO.service[StockRepo]
// 					res <- stockRepo.insertStockMinVolumeSerialBulk(cd)
// 					_ <- ZIO.sleep(Duration(1, TimeUnit.SECONDS)) *> ZIO.log(s"Done crawlDayData ${stockCode} ${targetDt} ${pageNo}")
// 				} yield ()).ignore
// 			}
// 		) *> ZIO.log(s"crawl daily data of $stockCode done ..")
// 	}

// 	val app = (offset: Int) => for {
// 		crawlStatus <- ZIO.service[CrawlStatusRepo]
// 		targetSeeds <- crawlStatus.getTargetToCrawl("ACTV")
// 		_ <- ZIO.foreach(targetSeeds)(stockCode => 
// 			Console.printLine(s"targetStockCode : $stockCode") 
// 			*> crawlDayData(stockCode, DataUtil.stockTimestamp(offset)))
// 	} yield ()

// 	override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
// 		// crawlDayData("000720", DataUtil.stockTimestamp(0))
// 		app(-7)
// 		.provide(
// 			Client.customized,
// 			NettyClientDriver.live,
// 			ZLayer.succeed(NettyConfig.default),
// 			DnsResolver.default,
// 			DataDownloader.live,
// 			MinStockCrawler.live,
// 			CrawlStatusRepo.live,
// 			StockRepo.live,
// 			Quill.Mysql.fromNamingStrategy(SnakeCase),
// 			Quill.DataSource.fromPrefix("StockMysqlAppConfig")
// 		)

// }
