package x.yg.analyser

import zio._
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.crawl.utils.DataUtil

trait DayVaildator {
  def validateNextDayHighIn5min(itemCode: String, targetYYYYMMDD: String): ZIO[Any, Throwable, Unit]
  def validateAfterDayHighIn5days(day: String): ZIO[Any, Throwable, Unit]
}

// "000720", "20250123"  --> baseDay : 20250122
class DayVaildatorImpl(stockRepo: StockRepo, endPriceResultRepo: EndPriceResultRepo) extends DayVaildator {
  override def validateNextDayHighIn5min(
    itemCode: String, 
    targetYYYYMMDD: String = DataUtil.getYYYYMMDD()): ZIO[Any, Throwable, Unit] = 
    for {
      endPriceResult <- endPriceResultRepo.getEndPriceReuslt(itemCode, DataUtil.getByYYYYMMDD(targetYYYYMMDD, -1))
      _ <- endPriceResult match {
        case Some(epr) => 
          for {
            yDayBasePrice <- stockRepo.selectOneStockDataByItemCode(itemCode, DataUtil.getByStockYYYYMMDD(targetYYYYMMDD, -1) + "_15:10")
              .map(_.map(_.fixedPrice))
            _ <- yDayBasePrice match {
              case Some(endPrice) => for {
                curIn10min <- stockRepo.selectStockDataByItemCode(itemCode, targetYYYYMMDD, "09:10").map(_.map(_.fixedPrice))
                _ <- Console.printLine(s"---- yDayPrice : ${yDayBasePrice}")
                _ <- Console.printLine(s"---- stockData : ${curIn10min.max}")
                res <- endPriceResultRepo.updateNextDayHigh5m(itemCode,  DataUtil.getByYYYYMMDD(targetYYYYMMDD, -1), curIn10min.max.toInt)
              } yield res
              case None => ZIO.succeed(0)
            }
          } yield ()
        case None => Console.printLine("No Data") *> ZIO.succeed((-1))
      }
    } yield ()


  override def validateAfterDayHighIn5days(day: String): ZIO[Any, Throwable, Unit] = 
    ???
  
}

object DayVaildator {
  val live : ZLayer[StockRepo & EndPriceResultRepo, Nothing, DayVaildator] = 
    ZLayer {
      for {
        stockRepo <- ZIO.service[StockRepo]
        endPriceResultRepo <- ZIO.service[EndPriceResultRepo]
      } yield new DayVaildatorImpl(stockRepo, endPriceResultRepo)
      
    }
}
