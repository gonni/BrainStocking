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

object UnitJobScheduler2 {
	def create = {
		Ref.make(List.empty[Queue[Job[String]]]).map(new UnitJobScheduler2Impl(_, new JobProducerImpl()))
	}
}

// trait UnitJobScheduler2 {
// 	def addJob(job: Job[String]): ZIO[Any, Throwable, Unit]
// 	// def startWorker: ZIO[Any, Throwable, Unit]
// }

case class UnitCrawlJob(crawlStatusRepo: CrawlStatusRepo, 
	stockRepo: StockRepo, minStockCrawler: MinStockCrawler) extends Job[String] {
	val fixedPeriod = 10
	
	def run: ZIO[Any, Throwable, String] = for {
		expiredItemCodes <- crawlStatusRepo.getExpiredItemCode(fixedPeriod)
		_ <- ZIO.log("Create target itemCodes : " + expiredItemCodes)
		_ <- ZIO.foreach(expiredItemCodes){itemCode => 
			for {
				_ <- crawlStatusRepo.syncCrawlStatus(itemCode, "RUN")
				_ <- ZIO.sleep(Duration(1, TimeUnit.SECONDS))
				_ <- crawlStatusRepo.syncCrawlStatus(itemCode, "WAIT")
			} yield ()
		}
	} yield "Done"
		// ZIO.succeedBlocking{
		// 	println("-> run joblet proceessing : " + java.time.LocalDateTime.now)
		// 	"Succeed"
		// }
}



class UnitJobScheduler2Impl(queueRef: Ref[List[Queue[Job[String]]]], jobProducer: JobProducer[String], queueSize: Int = 10) {

	private def inputDataAndExtendQueue(queue: List[Queue[Job[String]]])(effect : => Job[String]): ZIO[Any, Nothing, Boolean] = 
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

	private def unitQueueWorker(queue: Queue[Job[String]], workerId: Int = -1): ZIO[Any, Nothing, Unit] = 
		(for {
			job <- queue.take
			res <- job.run.catchAll(e => ZIO.succeed(e.getMessage())) // ----------------> Rrocessing job
			_ <- Console.printLine(res + " by worker #" + workerId).ignore	// log
		} yield ()).repeat(Schedule.spaced(1.seconds)).unit  
	
	// add new job	
	def addJob(job: Job[String]) = 
		for {
			queue <- queueRef.get
			addRes <- inputDataAndExtendQueue(queue)(job)
			_ <- addRes match {
				case true => ZIO.unit
				case false => for {
					_ <- Console.printLine("--> create new worker queue ... " + queue.size)
					q <- Queue.dropping[Job[String]](queueSize)
					_ <- queueRef.update(_ :+ q)
					_ <- unitQueueWorker(q, queue.size).fork
				} yield ()
			}
		} yield ()
	
	// produce continouse jobs
	def autoJobProduce = {
		(for {
			data <- jobProducer.unitProduce	// ----------------> Producing Job
			_ <- Console.printLine("produce data: " + data)
			_ <- ZIO.foreach(data){r => addJob(new StrJob(r))}
		} yield ()).repeat(Schedule.spaced(1.seconds)).unit  
	}	
}

object Main0 extends ZIOAppDefault {
	val app = for {
		scheduler <- UnitJobScheduler2.create
		_ <- scheduler.autoJobProduce
	} yield ()
	override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = app.exitCode
}