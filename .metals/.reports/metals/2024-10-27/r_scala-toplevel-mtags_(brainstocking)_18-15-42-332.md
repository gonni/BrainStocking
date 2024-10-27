error id: file://<WORKSPACE>/src/main/scala/x/yg/sample/HellStarted.scala:[252..252) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/x/yg/sample/HellStarted.scala", "package x.yg.sample

import zio.*

object HellStarted extends ZIOAppDefault {

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = Console.printLine("Hello119")
}

trait DisplayService :
    def printHello(): ZIO[Any, Unit, Nothing]

class ")
file://<WORKSPACE>/src/main/scala/x/yg/sample/HellStarted.scala
file://<WORKSPACE>/src/main/scala/x/yg/sample/HellStarted.scala:13: error: expected identifier; obtained eof
class 
      ^
#### Short summary: 

expected identifier; obtained eof