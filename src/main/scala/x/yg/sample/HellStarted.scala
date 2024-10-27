package x.yg.sample

import zio.*
import java.io.IOException

object HellStarted extends ZIOAppDefault {

  val app = for {
    _ <- Console.printLine("Start ZIO Application ..")
    svc <- ZIO.service[DisplayService]
    _ <- svc.printHello()
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = //Console.printLine("Hello119")
    app.provide(HelloDisply.layer)
}

trait DisplayService {
    def printHello(): ZIO[Any, Throwable, Unit]
}

class HelloDisplay extends DisplayService {
  override def printHello() = 
    ZIO.succeed(println("Hello"))
}

object HelloDisply {
    val layer: ZLayer[Any, Nothing, DisplayService] =
        ZLayer.succeed(
            new HelloDisplay()
        )
}
