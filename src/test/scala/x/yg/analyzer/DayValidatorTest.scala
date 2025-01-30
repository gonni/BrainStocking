package x.yg.analyzer

import zio.*
import x.yg.analyser.DayVaildator
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.EndPriceResultRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

object DayValidatorTest extends ZIOAppDefault {
  val app = for {
    _ <- Console.printLine("Start ZIO Application ..")
    svc <- ZIO.service[DayVaildator]
    _ <- svc.validateNextDayHighIn5min("000720", "20250123")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = //Console.printLine("Hello119")
    app.provide(
      DayVaildator.live,
      StockRepo.live,
      EndPriceResultRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
  
}
