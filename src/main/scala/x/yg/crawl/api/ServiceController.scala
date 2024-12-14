package x.yg.crawl.api

import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import x.yg.crawl.data.StockRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockItemInfo

case class ServiceController private(stockRepo: StockRepo, scheduleRepo: ScheduleRepo) {
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

  )
}

object ServiceController {
  val live = ZLayer.derive[ServiceController]
}