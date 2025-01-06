package x.yg.crawl


import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import x.yg.crawl.api.ServiceController
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.data.ScheduleRepo
import x.yg.crawl.data.StockRepo
import zio.*
import zio.ZIO
import zio.ZIOAppDefault
import zio.http.*
import zio.http.Middleware.basicAuth
import java.net.URI

object Main extends ZIOAppDefault { 
  
  val apps = ZIO.serviceWith[ServiceController] {
    controller => 
      controller.routes
  }

  val program = for {
    app <- apps
    _ <- Server.serve(app @@ Middleware.debug)
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = 
    program.provide(
      Server.defaultWithPort(8080),
      ServiceController.live,
      StockRepo.live,
      ScheduleRepo.live,
      CrawlStatusRepo.live,
      Quill.Mysql.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("StockMysqlAppConfig")
    )
}
