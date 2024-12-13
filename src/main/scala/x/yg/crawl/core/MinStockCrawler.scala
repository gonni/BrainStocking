package x.yg.crawl.core

import zio._
import x.yg.crawl.data.StockMinVolumeTable
import x.yg.crawl.DataDownloader
import zio.http.Header.UserAgent
import zio.http._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import x.yg.crawl.utils.DataUtil

trait MinStockCrawler {
  def crawl(stockCode: String, targetDt: String = DataUtil.stockTimestamp(), pageNo: Int = 1): 
    ZIO[Client & DataDownloader, Throwable, List[StockMinVolumeTable]]
}

class MinStockCrawlerImpl extends MinStockCrawler {
  override def crawl(stockCode: String, targetDt: String, pageNo: Int): 
    ZIO[Client & DataDownloader, Throwable, List[StockMinVolumeTable]] = for {
      _ <- ZIO.log(s"Crawling from ${stockCode} at ${targetDt} with page ${pageNo}")
      _ <- ZIO.log(s"crawl url -> https://finance.naver.com/item/sise_time.naver?code=${stockCode}&thistime=${targetDt}&page=${pageNo}")
      downloader <- ZIO.service[DataDownloader]
      data <- downloader.download(s"https://finance.naver.com/item/sise_time.naver?code=${stockCode}&thistime=${targetDt}&page=${pageNo}")
      res <- ZIO.attempt(extractFilteredData(data, targetDt))
  } yield res

  private def extractFilteredData(data: String, targetDay: String): List[StockMinVolumeTable] = {
    val browser = new JsoupBrowser()
    val dom = browser.parseString(data)

    val f = dom >> "table" >> elementList("tr")
    for {
      res <- f.flatMap{x=>
        val tds = x >> elementList("td")
        tds.length match {
          case a if a > 5 => 
            val tsCode = tds(0).text
            val fixedPrice = DataUtil convertNumTextToInt tds(1).text//.toDouble
            val sellAmt = DataUtil convertNumTextToInt tds(3).text//.toInt 
            val buyAmt = DataUtil convertNumTextToInt tds(4).text//.toInt
            val volume = DataUtil convertNumTextToInt tds(5).text//.toInt
            println(tsCode + "-->" + fixedPrice + "-->" + sellAmt + "-->" + buyAmt + "-->" + volume)

            List(StockMinVolumeTable(tsCode, fixedPrice, sellAmt, buyAmt, volume))
            // List(StockMinVolumeTable("1", 1.0, 1, 1, 1))
          case _ => List()
        }
      }
    } yield res
  }
}

object MinStockCrawler {
  val live: ZLayer[DataDownloader, Nothing, MinStockCrawler] = ZLayer.succeed(new MinStockCrawlerImpl)
}
