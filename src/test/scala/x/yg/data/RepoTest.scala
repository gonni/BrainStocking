package x.yg.data

import zio._
import x.yg.analyser.DayVaildator
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.EndPriceResultRepo
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.data.EndPriceResult

object EndPriceResultRepoTest extends ZIOAppDefault {
  val app = for {
    _ <- Console.printLine("Start ZIO Application ..")
    svc <- ZIO.service[EndPriceResultRepo]
    _ <- svc.upsertEndPriceResult(EndPriceResult(targetDt = "20250116", itemCode = "000720", basePrice = 100, afterDayHigh5d = 200))
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
