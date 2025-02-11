package x.yg.crawl.core

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.checkerframework.checker.units.qual.s
import scalaz.Tags.Min
import x.yg.crawl.DataDownloader
import x.yg.crawl.data.StockRepo
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver

import java.util.concurrent.TimeUnit
import x.yg.crawl.data.CrawlStatusRepo
import x.yg.crawl.utils.DataUtil
import java.time.LocalTime


trait JobProducer[T] {
  def unitProduce: ZIO[Any, Nothing, List[T]]
}


abstract class UnitJobScheduler (
	queueRef: Ref[List[Queue[String]]], 
	queueSize: Int = 60) extends JobProducer[String] {

	private def inputDataAndExtendQueue(queue: List[Queue[String]])(effect : => String): ZIO[Any, Nothing, Boolean] = 
		for {
			result <- ZIO.suspendSucceed {
			queue match {
				case Nil => ZIO.succeed(false)
				case x :: xs => 
					for {
						result <- x.offer(effect)
						res <- result match {
							case true => ZIO.succeed(true)
							case false => 
								inputDataAndExtendQueue(xs)(effect)
						}
					} yield res
				}
			}
		} yield result
	
	def processJob(queue: Queue[String], workerId: Int = -1): ZIO[Any, Nothing, Unit]

	// add new job	
	def addJob(job: String) = 
		for {
			queue <- queueRef.get
			addRes <- inputDataAndExtendQueue(queue)(job)
			_ <- addRes match {
				case true => ZIO.unit
				case false => for {
					_ <- Console.printLine("--> create new worker queue ... " + queue.size)
					q <- Queue.dropping[String](queueSize)
					_ <- queueRef.update(_ :+ q)
					_ <- processJob(q, queue.size).fork
				} yield ()
			}
		} yield ()
	
	// produce continouse jobs
	def startProduce() = {
		(for {
      //TODO check this time is whether holiday or not
			data <- unitProduce	// ----------------> Producing Job
			_ <- Console.printLine("produce job to crawl: " + data)
			_ <- ZIO.foreach(data){r => addJob(r)}
		} yield ()).repeat(Schedule.spaced(1.seconds)).unit  
	}	
}


class CrawlJobScheduler(
	queueRef: Ref[List[Queue[String]]], 
	stockRepo: StockRepo, 
	crawler: MinStockCrawler,
	crawlStatus: CrawlStatusRepo) extends UnitJobScheduler(queueRef) {

	//TODO need to changed logic to check time check 09:00 ~ 15:30
	override def unitProduce: ZIO[Any, Nothing, List[String]] = (for {
		_ <- ZIO.when(DataUtil.getCurrentTimestamp() != DataUtil.stockTimestamp(0))(
			ZIO.log("Out of stockDay") *> ZIO.fail(new Exception("---> Out of stock time of Today")))
		_ <- ZIO.whenZIO(!isStockTime())(
			ZIO.log("Out of stockTime range") *> ZIO.fail(new Exception("---> Out of stock time")))
		queue <- queueRef.get
		cntEndItemCodes <- queue.lastOption match {
			case Some(q) => q.size
			case None => ZIO.succeed(0)
		}	
		expiredItemCodes <- crawlStatus.getExpiredItemCode(10 * 60 * 1000 - cntEndItemCodes * 1000)
		_ <- ZIO.foreachPar(expiredItemCodes){itemCode => crawlStatus.syncCrawlStatus(itemCode, "PEND")}
		_ <- ZIO.log("Create target itemCodes : " + expiredItemCodes)
	} yield expiredItemCodes)
	.catchAll(e => ZIO.log(e.getMessage()) *> ZIO.succeed(List.empty[String]))

	private def isStockTime(): ZIO[Any, Throwable, Boolean] = {
		ZIO.succeed {
      val now = LocalTime.now()
      val start = LocalTime.of(9, 0)
      val end = LocalTime.of(15, 30)
      !now.isBefore(start) && !now.isAfter(end)
    }
	}

	override def processJob(queue: Queue[String], workerId: Int = -1): ZIO[Any, Nothing, Unit] = 
		(for {
			itemCode <- queue.take
			cd <- crawler.crawl(itemCode)
			res <- stockRepo.insertStockMinVolumeSerialBulk(cd)
			_ <- crawlStatus.syncCrawlStatus(itemCode, "SUSP")
			_ <- ZIO.foreach(cd){r => Console.printLine(r.toString())}
			_ <- Console.printLine(s"Inserted data ${cd.size} for $itemCode by worker #$workerId").ignore	// log
		} yield ()).repeat(Schedule.spaced(1.seconds))
		.provide(
			Client.customized,
			NettyClientDriver.live,
			ZLayer.succeed(NettyConfig.default),
			DnsResolver.default,
			DataDownloader.live,
		)
		.unit.catchAll(e => ZIO.log(e.getMessage()).ignore)
}