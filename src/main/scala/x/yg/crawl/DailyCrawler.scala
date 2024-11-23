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

object DailyCrawler extends ZIOAppDefault {
  val program = for {
    downloader <- ZIO.service[DataDownloader]
    data <- downloader.download("https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=1")
    // _ <- Console.printLine("downloaded => " + data)
    // _ <- ZIO.succeed(processingData(data))
    filtered <- processingData(data)
    _ <- Console.printLine("filtered => " + filtered)
  } yield ()

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

    // f.foreach{
    //   x => {
    //     val tds = x >> elementList("td")
    //     println("----------------")
    //     tds.length match {
    //       case a if a > 5 => 
    //         println(tds(0).text + "-->" + tds(1).text + "-->" + tds(3).text + 
    //           "-->" + tds(4).text + "-->" + tds(5).text)
            
    //       case _ => println("skip")
    //     }
    //   }
    // }

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
