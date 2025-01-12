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

object BulkCrawlScheduler extends ZIOAppDefault {


	def crawlDayData(stockCode: String, targetDt: String = DataUtil.stockTimestamp()) = {
		ZIO.collectAllDiscard(
			(1 to 40).map { pageNo => 
				(for {
					crawler <- ZIO.service[MinStockCrawler]
					cd <- crawler.crawl(stockCode, targetDt, pageNo)
					stockRepo <- ZIO.service[StockRepo]
					res <- stockRepo.insertStockMinVolumeSerialBulk(cd)
					_ <- ZIO.sleep(Duration(1, TimeUnit.SECONDS)) *> ZIO.log(s"Done crawlDayData ${stockCode} ${targetDt} ${pageNo}")
				} yield ()).ignore
			}
		) *> ZIO.log(s"crawl daily data of $stockCode done ..")
	}

	override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
		crawlDayData("097230", DataUtil.stockTimestamp(0)).provide(
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
