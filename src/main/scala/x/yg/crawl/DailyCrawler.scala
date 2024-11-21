package x.yg.crawl

import zio._
import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.Header.UserAgent

object DailyCrawler extends ZIOAppDefault {
	val program = for {
		downloader <- ZIO.service[DataDownloader]
		data <- downloader.download("https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=1")
		_ <- Console.printLine("downloaded => " + data)
	} yield ()

	override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
		program.provide(
			DataDownloader.live,
			Client.customized,
			NettyClientDriver.live,
			DnsResolver.default,
			ZLayer.succeed(NettyConfig.default),
		)





//   def main(v: Array[String]) = {
// //     val browser = new JsoupBrowser()
// // //  val doc = browser.get("https://finance.naver.com/item/frgn.naver?code=247540")
// //     val doc = browser.get("https://finance.naver.com/item/main.naver?code=247540")
// //     println(doc.toHtml)
// //     println("---------------")
// //     val headerText = doc >> elementList(".sub_section") >> elementList(".right")
// // //     println(headerText)
//     sampleDailyMin()
//   }


//   def sampleDailyMin() = {
//     val targetUrl = "https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=2"
//     val browser = new JsoupBrowser()
//     val dom = browser.get(targetUrl)
		
//     println(dom.toHtml)

//     val fdom = dom >> "table" >> "tr"
//     println(fdom)

//   }
}
