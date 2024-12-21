package x.yg.analyser

import zio._
import zio.stream._
import x.yg.crawl.data.StockRepo

trait EndPriceAnalyzer {
  def analyze(stockCode: String, targetDt: String = "20210801"): ZIO[StockRepo, Throwable, Any]
} 

class EndPriceAnalyzerImpl extends EndPriceAnalyzer {
  override def analyze(stockCode: String, targetDt: String) = for {
    repo <- ZIO.service[StockRepo]
    targetData <- repo.selectStockDataByItemCode(stockCode, targetDt)
  } yield ()
}

def getDailyStockData(stockCode: String, targetDt: String) = for {
    repo <- ZIO.service[StockRepo]
    targetData <- repo.selectStockDataByItemCode(stockCode, targetDt)
  } yield targetData

def makeDataStream(stockCode: String, targetDt: String) = 
  ZStream.fromZIO(getDailyStockData(stockCode, targetDt)).flatMap(ZStream.fromIterable)

// val upwardTrendPipeline = 
//   ZPipeline.

object EndPriceAnalyzer {
  val live = ZLayer.succeed(new EndPriceAnalyzerImpl)
}
