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
// import javax.xml.crypto.Data


object RealtimeCrawlScheduler extends ZIOAppDefault {

  // def runFunc: ZIO[Any, Throwable, Unit] = 
  //   ZIO.succeedBlockingUnsafe(r => println("Hello World"))

  def runFunc: String = "Hello World at " + java.time.LocalDateTime.now

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
  
  // public   
  def autoProducer(queueRef: Ref[List[Queue[String]]]) = {
    for {
      queue <- queueRef.get
      addRes <- inputDataAndExtendQueue(queue)(runFunc)
      _ <- addRes match {
        case true => ZIO.unit
        case false => for {
          _ <- Console.printLine("--> create new worker queue ... " + queue.size)
          q <- Queue.dropping[String](3)
          _ <- queueRef.update(_ :+ q)
          _ <- unitQueueWorker(q).fork
        } yield ()
      }
    } yield ()
  }
  
  // processing ...
  private def unitQueueWorker(queue: Queue[String]): ZIO[Any, Nothing, Unit] = 
    (for {
      job <- queue.take
      _ <- Console.printLine(job + " by worker").ignore
    } yield ()).repeat(Schedule.spaced(1.seconds)).unit  

  val app =  for {
    queueRef <- Ref.make(List.empty[Queue[String]])
    _ <- ZIO.foreach(1 to 30) { _ =>
      for {
       _ <- autoProducer(queueRef)
       _ <- ZIO.sleep(Duration(500, TimeUnit.MILLISECONDS))
      } yield ()
    }
    _ <- ZIO.succeed(println("process completed ..."))
  } yield ()

  def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = app.exitCode
}