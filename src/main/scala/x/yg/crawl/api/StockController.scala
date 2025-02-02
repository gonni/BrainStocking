package x.yg.crawl.api
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockItemInfo
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.core.StockCrawlerService

case class StockController private(
    stockRepo: StockRepo, 
    scheduleRepo: ScheduleRepo, 
    crawlStatusRepo: CrawlStatusRepo,
    stockCrawlerService: StockCrawlerService
) {
  def routes: Routes[Any, Response] = Routes(
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
            )//.as(Response.text("Success"))
          }
      }
    }, 
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
    Method.GET / "stock" / "getClosingPriceTarget" / "all" -> handler {
      Response.text("Hello World 15")
    },
    Method.GET / "stock" / "checkNextday10m" / "all" -> handler {
      Response.text("Hello World 15")
    },
    Method.GET / "stock" / "checkNextday5d" / "all" -> handler {
      Response.text("Hello World 15")
    },
  )
}

object StockController {
  val live: ZLayer[StockRepo & ScheduleRepo & CrawlStatusRepo & StockCrawlerService, Nothing, StockController] =
    ZLayer.fromFunction(StockController.apply)
}
// object StockController {
//   val live = ZLayer.derive[StockController]
// }