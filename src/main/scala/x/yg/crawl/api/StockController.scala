package x.yg.crawl.api
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockItemInfo
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.core.StockCrawlerService
import x.yg.crawl.utils.DataUtil
import java.net.URLEncoder
import org.checkerframework.checker.units.qual.m
import x.yg.crawl.data.EndPriceResultRepo
import x.yg.analyser.DayVaildator
import x.yg.analyser.EndPriceAnalyzer
import x.yg.crawl.data.EndPriceResult
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

case class StockController private(
    stockRepo: StockRepo, 
    scheduleRepo: ScheduleRepo, 
    crawlStatusRepo: CrawlStatusRepo,
    stockCrawlerService: StockCrawlerService,
    endPriceResultRepo: EndPriceResultRepo,
    dayVaildator: DayVaildator,
    endPriceAnalyzer: EndPriceAnalyzer
) {
  def routes: Routes[StockRepo with ScheduleRepo with CrawlStatusRepo with StockCrawlerService with EndPriceResultRepo with DayVaildator with EndPriceAnalyzer, Response] = Routes(
    // temporary
    Method.GET / "stock" / "crawl" / string("itemCode") / string("targetDt") -> handler {
      (itemCode: String, targetDt: String, _: Request) =>
        if (Option(itemCode).forall(_.trim.isEmpty) || Option(targetDt).forall(_.trim.isEmpty)) {
          ZIO.succeed(Response.text("Invalid parameters: itemCode and targetDt cannot be empty").status(Status.BadRequest))
        } else {
          ZIO.scoped {
            ZIO.log(s"detected crawl $itemCode on $targetDt fired") *>
            stockCrawlerService.crawlDayData(itemCode, targetDt + "161049").mapBoth(
              e => Response.text(e.toString),
              _ => Response.text("Success")
            )
          }
      }
    }, 

    // temporary
    Method.GET / "stock" / "crawl" / "all" / string("targetDt") -> handler { (targetDt: String, _: Request) =>
      if (Option(targetDt).forall(_.trim.isEmpty)) {
        ZIO.succeed(Response.text("Invalid parameters: targetDt cannot be empty").status(Status.BadRequest))
      } else {
        ZIO.scoped {
          ZIO.log(s"detected crawl All $targetDt fire") *>
          stockCrawlerService.crawlAllDayData(targetDt + "161049").mapBoth(
            e => Response.text(e.toString),
            res => Response.text(s"Success: $res")
          )
        }
      }
    },
    
    // run on 15:10
    Method.GET / "stock" / "getClosingPriceTarget" / "all" / string("targetDt") -> handler { (targetDt: String, _: Request) =>
      (for{
        _ <- ZIO.log(s"detected getClosingPriceTarget $targetDt fired")
        targetItmCodes <- crawlStatusRepo.getTargetToCrawl("ACTV")
        cntValid <- ZIO.foldLeft(targetItmCodes)(0){(acc, itemCode) => 
          for {
            res <- endPriceAnalyzer.analyze(itemCode, targetDt)
            r <- res match {
              case (true, scroe, basePrice) => 
                Console.printLine(s"insert data : ${itemCode} ${targetDt} ${scroe}") *>
                endPriceResultRepo.upsertEndPriceResult(EndPriceResult(targetDt, itemCode, scroe, basePrice)) *>
                ZIO.succeed(acc + 1)
              case _ => 
                Console.printLine(s"ignored data : ${itemCode} ${targetDt}") *>
                ZIO.succeed(acc)
            }
          } yield r
        }
      } yield cntValid).mapBoth(
        e => Response.text(e.toString),
        res => Response.text(s"Successfully processed for ${res}") 
      )
    },

    // run on 09:10
    Method.GET / "stock" / "checkNextday10m" / "all"  -> handler {
      URL.decode("/stock/checkNextday10m/all/" + DataUtil.getYYYYMMDD()) match {
        case Left(e) => Response.text(e.getMessage())
        case Right(value) => Response.redirect(value)
      }
    },
    // run on 09:10 -- linked with None time
    Method.GET / "stock" / "checkNextday10m" / "all" / string("targetDt") -> handler {(targetDt: String, _: Request) =>
      (for {
        targets <- endPriceResultRepo.getNonCheck10mResult()
        res <- ZIO.foreach(targets)(epr => 
          for {
            _ <- Console.printLine(s"target : ${epr.itemCode} ${epr.targetDt}")
            res1 <- dayVaildator.vaildateNextDay10minFromToday(epr.itemCode, epr.targetDt, "20250203").mapBoth(
              e => Response.text(e.toString),
              _ => Response.text("Success")
            )
          } yield res1)
      } yield res).mapBoth(
        e => Response.text(e.toString),
        res => Response.text(s"Successfully processed for ${res.size}") 
      )
    },

    // run on 16:00
    Method.GET / "stock" / "checkNextday5d" / "all" -> handler {
      Response.text("Hello World 15")
    },
  )
}

object StockController {
  val live: ZLayer[StockRepo with ScheduleRepo with CrawlStatusRepo with StockCrawlerService with EndPriceResultRepo with DayVaildator with EndPriceAnalyzer, 
    Nothing, StockController] =
    ZLayer.fromFunction(StockController.apply)
}
