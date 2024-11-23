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

object DailyCrawler extends ZIOAppDefault {
  val program = for {
    downloader <- ZIO.service[DataDownloader]
    data <- downloader.download("https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=1")
    // _ <- Console.printLine("downloaded => " + data)
    // _ <- ZIO.succeed(processingData(data))
    filtered <- processingData(data)
    _ <- Console.printLine("filtered => " + filtered)
  } yield ()

  def processingData(data: String): Task[String] = {
    val browser = new JsoupBrowser()
    val dom = browser.parseString(data)

    val f = dom >> "table" >> elementList("tr")
    f.foreach{
      x => {
        val tds = x >> elementList("td")
        println("----------------")
        tds.length match {
          case a if a > 3 => println(tds(0).text + "-->" + tds(1).text) 
          case _ => println("skip")
        }
      }
    }



    ZIO.succeed("1")
    // ZIO.attempt(dom >> "table" >> "tr" >> elementList("td"))
    //   .map(_.map(_.text))
    //   .map(_.mkString(","))
  }

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    program.provide(
      DataDownloader.live,
      Client.customized,
      NettyClientDriver.live,
      DnsResolver.default,
      ZLayer.succeed(NettyConfig.default),
    )

  def sampleDailyMin() = {
  	val targetUrl = "https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=2"
  	val browser = new JsoupBrowser()
  	val dom = browser.get(targetUrl)
    
  	println(dom.toHtml)

  	val fdom = dom >> "table" >> "tr"
  }
}
