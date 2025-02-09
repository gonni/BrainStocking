package x.yg.analyzer

import zio._
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.analyser.DayVaildator
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.data.StockRepo

object DailyCheckMain extends ZIOAppDefault {


  val appAll10mPrice = for {
    endPriceResultRepo <- ZIO.service[EndPriceResultRepo] 
    dailyValidator <- ZIO.service[DayVaildator]
    targets <- endPriceResultRepo.getNonCheck10mResult()
    _ <- ZIO.foreach(targets)(epr => 
      for {
        _ <- Console.printLine(s"target : ${epr.itemCode} ${epr.targetDt}")
        _ <- dailyValidator.vaildateNextDay10minFromToday(epr.itemCode, epr.targetDt, "20250203")
      } yield ())
  } yield ()

  val appSample10mPrice = for {
    endPriceResultRepo <- ZIO.service[EndPriceResultRepo] 
    dailyValidator <- ZIO.service[DayVaildator]
    // targets <- endPriceResultRepo.getNonCheck10mResult()
    _ <- dailyValidator.vaildateNextDay10minFromToday("101000", "20250131", "20250203")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = appAll10mPrice
    .provide(
      DayVaildator.live,
      EndPriceResultRepo.live,
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
    .debug

  
}
