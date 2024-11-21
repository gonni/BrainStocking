package x.yg.sample

import zio._

import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.Header.UserAgent

object HttpsClient extends ZIOAppDefault {
  val url     = URL.decode("https://finance.naver.com/item/sise_time.naver?code=205470&thistime=20241119161049&page=1").toOption.get
  // val url = URL.decode("https://m.naver.com").toOption.get
  val headers = Headers(Header.UserAgent(UserAgent.ProductOrComment.Product("Mozilla", Some("5.0"))))

  val sslConfig = ClientSSLConfig.FromTrustStoreResource(
    trustStorePath = "truststore.jks",
    trustStorePassword = "changeit",
  )

  val clientConfig = ZClient.Config.default.ssl(sslConfig)

  val program = for {
    data <- ZClient.batched(Request.get(url).addHeaders(headers))
    _    <- Console.printLine(data.body.asString)
  } yield ()

  val run =
    program.provide(
      ZLayer.succeed(clientConfig),
      Client.customized,
      NettyClientDriver.live,
      DnsResolver.default,
      ZLayer.succeed(NettyConfig.default),
    )

}