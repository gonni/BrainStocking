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


class CrawlJobScheduler(
	queueRef: Ref[List[Queue[String]]], 
	jobProducer: JobProducer[String],
	stockRepo: StockRepo, 
	crawler: MinStockCrawler) extends UnitJobScheduler5(queueRef, jobProducer) {
	override def processJob(queue: Queue[String], workerId: Int = -1): ZIO[Any, Nothing, Unit] = 
		(for {
			itemCode <- queue.take
			cd <- crawler.crawl(itemCode)
			_ <- Console.printLine(itemCode + " by worker #" + workerId).ignore	// log
		} yield ()).repeat(Schedule.spaced(1.seconds))
		.provide(
			Client.customized,
			NettyClientDriver.live,
			ZLayer.succeed(NettyConfig.default),
			DnsResolver.default,
			DataDownloader.live,
			StockRepo.live,
			Quill.Mysql.fromNamingStrategy(SnakeCase),
			Quill.DataSource.fromPrefix("StockMysqlAppConfig")
		)
		.unit.catchAll(e => Console.printLine(e.getMessage()).ignore)
}


abstract class UnitJobScheduler5 (
	queueRef: Ref[List[Queue[String]]], 
	jobProducer: JobProducer[String], 
	queueSize: Int = 10) {

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

	// private def unitQueueWorker(queue: Queue[Job[String]], workerId: Int = -1): ZIO[Any, Nothing, Unit] = 
	// 	(for {
	// 		job <- queue.take
	// 		res <- job.run.catchAll(e => ZIO.succeed(e.getMessage())) // ----------------> Rrocessing job
	// 		_ <- Console.printLine(res + " by worker #" + workerId).ignore	// log
	// 	} yield ()).repeat(Schedule.spaced(1.seconds)).unit  
	
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
	def autoJobProduce = {
		(for {
			data <- jobProducer.unitProduce	// ----------------> Producing Job
			_ <- Console.printLine("produce data: " + data)
			_ <- ZIO.foreach(data){r => addJob(r)}
		} yield ()).repeat(Schedule.spaced(1.seconds)).unit  
	}	
}
