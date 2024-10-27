package x.yg.sample

import zio._
import zio.config.typesafe.TypesafeConfigProvider
import zio.http._
import java.io.IOException
import zio.config.magnolia.deriveConfig

case class HttpServerConfig(host: String, port: Int, nThread: Int)

object HttpServerConfig {
  val config: Config[HttpServerConfig] = 
    deriveConfig[HttpServerConfig].nested("HttpServerConfig")
}


// case class HttpServerConfig(host: String, port: Int, nThread: Int)

object ConfigMain extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(
      TypesafeConfigProvider.fromResourcePath()
  )

  private val serverConfig: ZLayer[Any, Config.Error, HttpServerConfig] =
    ZLayer
      .fromZIO(
        ZIO.config[HttpServerConfig](HttpServerConfig.config).map { c =>
          // Server.Config.default.binding(c.host, c.port)
          println("Config info :" + c)
          c
        }
      )

  // val app = 
  //   ZIO.service[HttpServerConfig].flatMap { config =>
  //     Console.printLine(
  //       "Application started with following configuration:\n" +
  //         s"\thost: ${config.host}\n" +
  //         s"\tport: ${config.port}"
  //     )
  //   }



  val workflow: ZIO[HttpServerConfig, IOException, Unit] = {
    ZIO.service[HttpServerConfig].flatMap { config =>
      Console.printLine(
        "Application started with following configuration:\n" +
          s"\thost: ${config.host}\n" +
          s"\tport: ${config.port}"
      )
    }
  }
  
  override def run = workflow.provide(bootstrap, serverConfig) //workflow.provide(serverConfig)
}
