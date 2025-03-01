package x.yg.analyzer

import zio._
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.utils.DataUtil
import x.yg.crawl.data.StockRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.data.EndPriceResultRepo
import org.scalafmt.internal.Policy.End
import x.yg.crawl.data.EndPriceResult
import x.yg.analyser.EndPriceAnalyzer

object EndPriceAnalyzerMain extends ZIOAppDefault {

  def analyzeToday(targetDt: String = DataUtil.getYYYYMMDD(0)): ZIO[CrawlStatusRepo & EndPriceResultRepo & EndPriceAnalyzer & StockRepo, Throwable, Int] = for {
    crawlStatus <- ZIO.service[CrawlStatusRepo]
    endPriceResult <- ZIO.service[EndPriceResultRepo]
    analyzer <- ZIO.service[EndPriceAnalyzer]
    targetItmCodes <- crawlStatus.getTargetToCrawl("ACTV")
    cntValid <- ZIO.foldLeft(targetItmCodes)(0)((acc, itemCode) => 
      for {
        res <- analyzer.analyze(itemCode, targetDt)
        r <- res match {
          case (true, scroe, basePrice) => 
            Console.printLine(s"insert data : ${itemCode} ${targetDt} ${scroe}") *>
            endPriceResult.upsertEndPriceResult(EndPriceResult(targetDt, itemCode, scroe, basePrice)) *>
            ZIO.succeed(acc + 1)
          case _ => 
            println(s"ignored data : ${itemCode} ${targetDt}")
            ZIO.succeed(acc)
        }
      } yield r
    )
  } yield cntValid // targetItmCodes.zip(res)
  
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    analyzeToday("20250131") //DataUtil.getYYYYMMDD(-5))
    .provide(
      CrawlStatusRepo.live,
      EndPriceResultRepo.live,
      EndPriceAnalyzer.live,
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
    .debug
}
