package x.yg.crawl.api

import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockItemInfo
import x.yg.crawl.data.CrawlStatusRepo


case class ServiceController private(stockRepo: StockRepo, scheduleRepo: ScheduleRepo, crawlStatusRepo: CrawlStatusRepo) {
  def routes: Routes[Any, Response] = Routes(
    Method.GET / "stock" / "daily" / "all" -> handler {
      Response.text("Hello World")
    },
    Method.GET / "stock" / "conf" / "sample" -> handler {(req: Request) =>
      ZIO.log("detected fire") *> scheduleRepo.syncStockItemInfo(StockItemInfo("000000", "Sample")).mapBoth(
        e => Response.text(e.toString),
        _ => Response.text("Success")
      )
    },
    Method.GET / "stock" / string("stockCode") / string("targetDay") -> handler {
      (stockCode: String, targetDay: String, _: Request) =>
        ZIO.log("detected fire") *> 
        stockRepo.selectStockDataByItemCode(stockCode, targetDay).mapBoth (
          e => Response.text(e.toString),
          res => Response(body = Body.from(res))
        )
    },
    Method.GET / "stock" / "crawl" / "sync" / string("itemCode") -> handler {
      (itemCode: String, _: Request) =>
        ZIO.log("update crawl status fire ..") *> 
        crawlStatusRepo.syncCrawlStatus(itemCode, "ACTV").mapBoth (
          e => Response.text(e.toString),
          res => Response(body = Body.from(res))
        )
    },
    Method.GET / "stock" / "crawl" / "target" / int("min") -> handler{(min: Int, req: Request) =>
      ZIO.log("Get target itemCode for realtime crawl") *> 
      crawlStatusRepo.getExpiredItemCode(min).mapBoth (
        e => Response.text(e.toString),
        res => Response(body = Body.from(res))
      )
    }
  )
}

object ServiceController {
  val live = ZLayer.derive[ServiceController]
}