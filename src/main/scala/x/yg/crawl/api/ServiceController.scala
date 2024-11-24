package x.yg.crawl.api

import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import x.yg.crawl.data.StockRepo

case class ServiceController private(stockRepo: StockRepo) {
  def routes: Routes[Any, Response] = Routes(
    Method.GET / "stock" / "daily" / "all" -> handler {
      Response.text("Hello World")
    },

    
  )
}

object ServiceController {
  val live = ZLayer.derive[ServiceController]
}