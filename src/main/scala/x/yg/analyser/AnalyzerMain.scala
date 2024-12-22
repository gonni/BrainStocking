package x.yg.analyser

import zio._
import zio.stream._
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.StockMinVolumeTable
import org.checkerframework.checker.units.qual.m
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import scala.math._

object AnalyzerMain extends ZIOAppDefault {

  def makeDataStream(stockCode: String, targetDt: String): ZStream[StockRepo, Throwable, StockMinVolumeTable] = 
    ZStream.fromZIO(getDailyStockData(stockCode, targetDt)).flatMap(ZStream.fromIterable)

  def checkUpwardTrend(dataStream: ZStream[StockRepo, Throwable, StockMinVolumeTable]) = {
   dataStream
      .map(record => (record.tsCode, record.fixedPrice))  // tsCode와 fixedPrice만 추출
      .runFold((true, Option.empty[Double])) { case ((isTrending, prevPriceOpt), (_, currentPrice)) =>
        prevPriceOpt match {
          case Some(prevPrice) if currentPrice < prevPrice => (false, Some(currentPrice))
          case _                                           => (isTrending, Some(currentPrice))
        }
      }
      .map(_._1) // 결과의 첫 번째 값 (우상향 여부)을 반환
  }

  def calculateSlopeAndValidate(
      stream: ZStream[StockRepo, Throwable, StockMinVolumeTable],
      allowedError: Double = 0.2
  ): ZIO[StockRepo, Throwable, Boolean] = {
    for {
      // 데이터를 리스트로 수집하여 시점 계산
      dataList <- stream.runCollect.map(_.toList)
      result <- ZIO.attempt {
        val sortedData = dataList.sortBy(_.tsCode) // 시간 기준으로 정렬
        if (sortedData.size < 2) false // 데이터가 부족한 경우 바로 실패

        val start = sortedData.head
        val end = sortedData.last

        val startX = 0 // 시작 시점의 X값 (0으로 설정)
        val endX = sortedData.size - 1 // 끝 시점의 X값
        val slope = (end.fixedPrice - start.fixedPrice) / (endX - startX) // 기울기 계산

        // 모든 데이터 포인트를 순회하며 유효성 검사
        sortedData.zipWithIndex.forall { case (data, index) =>
          val expectedY = start.fixedPrice + slope * index
          val error = abs(data.fixedPrice - expectedY) / expectedY
          error <= allowedError
        }
      }
    } yield result
  }

  val parogram = for {
    isTrending <- checkUpwardTrend(makeDataStream("005880", "20241220"))
    _ <- ZIO.succeed {
      isTrending match {
        case true => println("Upward Trend")
        case false => println("Downward Trend")
      }
    }
  } yield ()

  val program2 = for {
    isTrending <- calculateSlopeAndValidate(makeDataStream("005880", "20241220"))
    _ <- ZIO.succeed {
      isTrending match {
        case true => println("Upward Trend")
        case false => println("Downward Trend")
      }
    }
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    program2
    .provide(
      StockRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
}
