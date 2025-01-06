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

trait Job[T] {
  def run: ZIO[Any, Throwable, T]
}

//TODO
class StrJob(item: String) extends Job[String] {
  // excute job : crawl and processing and then save to db
  def run: ZIO[Any, Throwable, String] = 
    ZIO.succeedBlocking{
      println("-> run joblet proceessing : " + item + " at " + java.time.LocalDateTime.now)
      "Succeed"
    }
}

@deprecated
class UnitJob extends Job[String] {
    def run: ZIO[Any, Throwable, String] = 
      ZIO.succeedBlocking{
        println("->joblet proceessing : Hello World at " + java.time.LocalDateTime.now)
        "Succeed"
    }
  }

trait JobProducer[T] {
  def unitProduce: ZIO[Any, Nothing, List[T]]
}
//TODO
class JobProducerImpl() extends JobProducer[String] {
  // extract target items from db and then return list of items
  def unitProduce: ZIO[Any, Nothing, List[String]] = (for {
    cnt <- Random.nextIntBetween(0, 3)
    res <- Random.nextIntBetween(0, 101)
  } yield List.fill(cnt)("R-" + res))
  
  // .map { res =>
  //   res.foreach(r => jobScheduler.addJob(new UnitJob))
  //   res
  // }
}

object UnitJobScheduler extends ZIOAppDefault {
  // def runFunc: String = "Hello World at " + java.time.LocalDateTime.now


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
  
  // public   
  def autoProducer(queueRef: Ref[List[Queue[Job[String]]]]) = {
    for {
      queue <- queueRef.get
      addRes <- inputDataAndExtendQueue(queue)(new UnitJob)
      _ <- addRes match {
        case true => ZIO.unit
        case false => for {
          _ <- Console.printLine("--> create new worker queue ... " + queue.size)
          q <- Queue.dropping[Job[String]](3)
          _ <- queueRef.update(_ :+ q)
          _ <- unitQueueWorker(q, queue.size).fork
        } yield ()
      }
    } yield ()
  }

  def addJob(queueRef: Ref[List[Queue[Job[String]]]], job: Job[String]) = 
    for {
      queue <- queueRef.get
      addRes <- inputDataAndExtendQueue(queue)(job)
      _ <- addRes match {
        case true => ZIO.unit
        case false => for {
          _ <- Console.printLine("--> create new worker queue ... " + queue.size)
          q <- Queue.dropping[Job[String]](3)
          _ <- queueRef.update(_ :+ q)
          _ <- unitQueueWorker(q, queue.size).fork
        } yield ()
      }
    } yield ()

  private def unitQueueWorker(queue: Queue[Job[String]], workerId: Int = -1): ZIO[Any, Nothing, Unit] = 
    (for {
      job <- queue.take
      res <- job.run.catchAll(e => ZIO.succeed(e.getMessage())) // 
      _ <- Console.printLine(res + " by worker #" + workerId).ignore
    } yield ()).repeat(Schedule.spaced(1.seconds)).unit  
  
  def streamProducer(queueRef: Ref[List[Queue[Job[String]]]]) = 
    ZIO.foreach(1 to 30) { _ =>
      for {
        _ <- autoProducer(queueRef)
        _ <- ZIO.sleep(Duration(500, TimeUnit.MILLISECONDS))
      } yield ()
    }

  val app =  for {
    queueRef <- Ref.make(List.empty[Queue[Job[String]]])
    _ <- streamProducer(queueRef)
    _ <- ZIO.succeed(println("process completed ..."))
  } yield ()

  def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = app.exitCode
}
