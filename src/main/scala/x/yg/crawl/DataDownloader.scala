package x.yg.crawl

import zio._
import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.Header.UserAgent
import org.scalameta.data.data
import java.nio.charset.Charset

trait DataDownloader {
	def download(url: String): ZIO[Client, Throwable, String]
}

final class DataDownloaderLive extends DataDownloader {
	val headers = Headers(Header.UserAgent(UserAgent.ProductOrComment.Product("Mozilla", Some("5.0"))))
	override def download(url: String): ZIO[Client, Throwable, String] = 
		for {
			_ <- ZIO.log("downloading data")
			data <- ZClient.batched(Request.get(url).addHeaders(headers))
			res <- data.body.asString(Charset.forName("EUC-KR"))
		} yield res
}

object DataDownloader {
	val sslConfig = ClientSSLConfig.FromTrustStoreResource(
		trustStorePath = "truststore.jks",
		trustStorePassword = "changeit"
	)

	val clientConfig = ZClient.Config.default.ssl(sslConfig)
	// Client.customized,
	// 		NettyClientDriver.live,
	// 		DnsResolver.default,
	// 		ZLayer.succeed(NettyConfig.default),
	val live = ZLayer.succeed(clientConfig) ++ ZLayer.succeed(new DataDownloaderLive)
}