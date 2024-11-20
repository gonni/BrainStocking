package x.yg.crawl

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

object DailyCrawler {
  def main(v: Array[String]) = {
//     val browser = new JsoupBrowser()
// //  val doc = browser.get("https://finance.naver.com/item/frgn.naver?code=247540")
//     val doc = browser.get("https://finance.naver.com/item/main.naver?code=247540")
//     println(doc.toHtml)
//     println("---------------")
//     val headerText = doc >> elementList(".sub_section") >> elementList(".right")
// //     println(headerText)
    sampleDailyMin()
  }


  def sampleDailyMin() = {
    val targetUrl = "https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=2"
    val browser = new JsoupBrowser()
    val dom = browser.get(targetUrl)
    
    println(dom.toHtml)

    val fdom = dom >> "table" >> "tr"
    println(fdom)

  }
}
