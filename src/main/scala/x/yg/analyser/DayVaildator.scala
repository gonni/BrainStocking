package x.yg.analyser

import zio._
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.crawl.utils.DataUtil

trait DayVaildator {
  // baseday: today
  def vaildateNextDay10minFromToday(itemCode: String, endDetectedDay: String, todayYYYYMMDD: String = DataUtil.getYYYYMMDD())
    : ZIO[Any, Throwable, Option[Int]]
  def validateNextDayHighIn10min(itemCode: String, targetYYYYMMDD: String): ZIO[Any, Throwable, Unit]
  def validateAfterDayHighIn5days(day: String): ZIO[Any, Throwable, Unit]
}

// "000720", "20250123"  --> baseDay : 20250122
class DayVaildatorImpl(stockRepo: StockRepo, endPriceResultRepo: EndPriceResultRepo) extends DayVaildator {


  override def vaildateNextDay10minFromToday(itemCode: String, endDetectedDay: String, todayYYYYMMDD: String = DataUtil.getYYYYMMDD())
    : ZIO[Any, Throwable, Option[Int]] = for {
    curIn10min <- stockRepo.selectStockDataByItemCode(itemCode, todayYYYYMMDD, "09:10").map(_.map(_.fixedPrice))
    res <- ZIO.when(!curIn10min.isEmpty && curIn10min.length > 4) {
      Console.printLine(s"Valid data exists : ${itemCode} ${todayYYYYMMDD}") *>
      endPriceResultRepo.updateNextDayHigh5m(itemCode,  endDetectedDay, curIn10min.max.toInt) *>
      ZIO.succeed(1)
    }//.orElse(ZIO.succeed(0))
  } yield res

  override def validateNextDayHighIn10min(
    itemCode: String, 
    targetYYYYMMDD: String = DataUtil.getYYYYMMDD()): ZIO[Any, Throwable, Unit] = for {
      // this way can be ignored
      baseEndPrice <- stockRepo.selectOneStockDataByItemCode(itemCode, DataUtil.getByStockYYYYMMDD(targetYYYYMMDD, -1) + "_15:10")
        .map(_.map(_.fixedPrice))
      _ <- baseEndPrice match {
        case Some(endPrice) => for {
          curIn10min <- stockRepo.selectStockDataByItemCode(itemCode, targetYYYYMMDD, "09:10").map(_.map(_.fixedPrice))
          res <- ZIO.when(!curIn10min.isEmpty && curIn10min.length > 8){
            Console.printLine(s"Valid data exists : ${itemCode} ${targetYYYYMMDD}") *>
            endPriceResultRepo.updateNextDayHigh5m(itemCode,  DataUtil.getByYYYYMMDD(targetYYYYMMDD, -1), curIn10min.max.toInt)
            *> ZIO.succeed(1)
          }.orElse(ZIO.succeed(0))
        } yield res
        case None => ZIO.succeed(0)
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
