package x.yg.analyser

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.data.StockMinVolumeTable
import x.yg.crawl.data.StockRepo
import zio.*
import zio.stream.*

trait EndPriceAnalyzer {
  def analyze(stockCode: String, targetDt: String = "20210801"): ZIO[StockRepo, Throwable, Boolean]
} 

class EndPriceAnalyzerImpl extends EndPriceAnalyzer {

  def analyzeUpstream(data: List[StockMinVolumeTable],  allowedError: Double = 0.25, innerErrorPer: Double = 0.80) = {
        
    val streamedData = ZStream.fromIterable(data).filter(r => {
        val tokens = r.tsCode.split("_")
        tokens.length == 2 && tokens(1) <= "15:30"
      })
    // streamedData.runCollect.map(_.toList)
    (for {
      firstElem <- streamedData.runHead
      lastElem <- streamedData.runLast
      // delta <- ZIO.attempt(lastElem.get.fixedPrice - firstElem.get.fixedPrice)
      _ <- Console.printLine(s"First : ${firstElem}, end : ${lastElem}")
      delta <- ZIO.attempt(
        lastElem.flatMap(l => firstElem.map(f => l.fixedPrice - f.fixedPrice))
        .getOrElse(0.0)
        )
      _ <- Console.printLine(s"Delta : ${delta}")
      _ <- ZIO.when(delta <= 0.0)(ZIO.fail(new Exception("---> Stock price is not trending upward <--")))
      slope <- ZIO.attempt(delta / (data.length - 1))
      _ <- Console.printLine(s"Slope : ${slope}")

      result <- streamedData.zipWithIndex.runFold(0) { case (inCount, (element, idx)) =>
        val expectedY = firstElem.map(a => a.fixedPrice + slope * idx) 
        val error = expectedY.map(exp => Math.abs(element.fixedPrice - exp) / delta)
        error match {
          case Some(e) if e <= allowedError => 
            println("in Error => " + e + " <=≈ " + allowedError)
            inCount + 1
          case x => 
            println("out Error => " + x)
            inCount
        }
      }
      _ <- Console.printLine(s"Result : ${result} / ${data.length}")
      finResult <- ZIO.succeed(result > data.length * innerErrorPer)
    } yield finResult).catchAll(e => Console.printError(e.getLocalizedMessage()) *> ZIO.succeed(false))
    
  }

  override def analyze(stockCode: String, targetDt: String) = for {
    repo <- ZIO.service[StockRepo]
    targetData <- repo.selectStockDataByItemCode(stockCode, targetDt)
    result <- analyzeUpstream(targetData)
  } yield result
  
}

object EndPriceAnalyzer {
  val live = ZLayer.succeed(new EndPriceAnalyzerImpl)
}

object RunnerMain extends ZIOAppDefault {

  val app = for {
    analyzer <- ZIO.service[EndPriceAnalyzer]
    result <- analyzer.analyze("000720", "20250110")
  } yield result

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    app.provide(
      EndPriceAnalyzer.live,
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    ).debug

}