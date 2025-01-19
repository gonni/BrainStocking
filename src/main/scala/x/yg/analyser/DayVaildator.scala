package x.yg.analyser

import zio._
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.crawl.utils.DataUtil

trait DayVaildator {
  def validateNextDayHighIn5min(itemCode: String, targetYYYYMMDD: String): ZIO[Any, Throwable, Unit]
  def validateAfterDayHighIn5days(day: String): ZIO[Any, Throwable, Unit]
}

class DayVaildatorImpl(stockRepo: StockRepo, endPriceResultRepo: EndPriceResultRepo) extends DayVaildator {
  override def validateNextDayHighIn5min(itemCode: String, targetYYYYMMDD: String): ZIO[Any, Throwable, Unit] = 
    for {
      endPriceResult <- endPriceResultRepo.getEndPriceReuslt(itemCode, DataUtil.getByYYYYMMDD(targetYYYYMMDD, -1))
      _ <- endPriceResult match {
        case Some(epr) => 
          for {
            stockData <- stockRepo.selectStockDataByItemCode(itemCode, targetYYYYMMDD, "09:10").map(_.map(_.fixedPrice))
            // get D-1 end price, and compare with D(09:10) max price 
          } yield ()
          
        case None => ZIO.succeed(())
      }
      // _ <- endPriceResult match {
      //   case Some(endPriceResult) => 
      //     for {
      //       stockData <- stockRepo.getStockData(itemCode, targetYYYYMMDD)
      //       _ <- stockData match {
      //         case Some(stockData) => 
      //           if (stockData.highPrice > 0) {
      //             endPriceResultRepo.updateNextDayHigh5m(itemCode, stockData.highPrice)
      //           } else {
      //             ZIO.succeed(())
      //           }
      //         case None => ZIO.succeed(())
      //       }
      //     } yield ()
          
      //   case None => ZIO.succeed(())
      // }
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
