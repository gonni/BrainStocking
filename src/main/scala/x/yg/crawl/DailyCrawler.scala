package x.yg.crawl

import zio._
import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.Header.UserAgent
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import x.yg.crawl.data.StockRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.data.StockMinVolumeTable
import x.yg.crawl.utils.DataUtil

object DailyCrawler extends ZIOAppDefault {

  val program = for {
    downloader <- ZIO.service[DataDownloader]
    data <- downloader.download("https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241218161049&page=11")
    // _ <- Console.printLine("downloaded => " + data)
    // _ <- ZIO.succeed(processingData(data))
    filtered <- extractFilteredData("205470", data)
    // stockRepo <- ZIO.service[StockRepo]
    // res <- stockRepo.insertStockMinVolumeBulk(filtered)
    _ <- Console.printLine("filtered => " + filtered)
  } yield ()

  def extractFilteredData(stockCode: String = "NA", data: String): ZIO[Any, Throwable, List[StockMinVolumeTable]] = {
    val browser = new JsoupBrowser()
    val dom = browser.parseString(data)

    val f = dom >> "table" >> elementList("tr")
    val res =  for {
      res <- f.flatMap{x=>
        val tds = x >> elementList("td")
        tds.length match {
          case a if a > 5 => 
            //TODO need to catch exception
            
            val tsCode = tds(0).text
            val fixedPrice = DataUtil convertNumTextToInt tds(1).text//.toDouble
            val sellAmt = DataUtil convertNumTextToInt tds(3).text//.toInt 
            val buyAmt = DataUtil convertNumTextToInt tds(4).text//.toInt
            val volume = DataUtil convertNumTextToInt tds(5).text//.toInt
            println(tsCode + "-->" + fixedPrice + "-->" + sellAmt + "-->" + buyAmt + "-->" + volume)

            List(StockMinVolumeTable(stockCode, tsCode, fixedPrice, sellAmt, buyAmt, volume))
          case _ => List()
        }
      }
    } yield res

    ZIO.attempt(res)
  }

  def processingData(data: String): ZIO[StockRepo, Throwable, String] = {
    val browser = new JsoupBrowser()
    val dom = browser.parseString(data)

    val f = dom >> "table" >> elementList("tr")
    val res = f.map{x=>
      val tds = x >> elementList("td")
      tds.length match {
        case a if a > 5 => 
          tds(0).text + "-->" + tds(1).text + "-->" + tds(3).text + 
          "-->" + tds(4).text + "-->" + tds(5).text
          //TODO
        case _ => "skip"
      }
    }
    ZIO.succeed("1")
  }

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    program.provide(
      StockRepo.live,
      DataDownloader.live,
      Client.customized,
      NettyClientDriver.live,
      DnsResolver.default,
      ZLayer.succeed(NettyConfig.default),
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )

  def sampleDailyMin() = {
  	val targetUrl = "https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=2"
  	val browser = new JsoupBrowser()
  	val dom = browser.get(targetUrl)
    
  	println(dom.toHtml)

  	val fdom = dom >> "table" >> "tr"
  }
}
