package x.yg.analyser

import zio._
import x.yg.analyser.AnalyzeScheduler
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.utils.DataUtil
import x.yg.crawl.data.StockRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

object AnalyzeScheduler extends ZIOAppDefault {

  def analyzeAll(targetDt: String = DataUtil.getYYYYMMDD(0)) = for {
    crawlStatus <- ZIO.service[CrawlStatusRepo]
    analyzer <- ZIO.service[EndPriceAnalyzer]
    targetItmCodes <- crawlStatus.getTargetToCrawl("ACTV")
    res <- ZIO.foreachPar(targetItmCodes)(itemCode => analyzer.analyze(itemCode, targetDt))
  } yield targetItmCodes.zip(res)
  
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    analyzeAll(DataUtil.getYYYYMMDD(-1))
    .provide(
      CrawlStatusRepo.live,
      EndPriceAnalyzer.live,
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
    .debug
}
