scalaVersion := "3.3.3"

name := "hello-world"
organization := "ch.epfl.scala"
version := "1.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.1.6",
  "dev.zio"       %% "zio-json"            % "0.6.2",
  "dev.zio"       %% "zio-http"            % "3.0.1",
  "io.getquill"   %% "quill-zio"           % "4.8.0",
  "io.getquill"   %% "quill-jdbc-zio"      % "4.8.0",
  "mysql" % "mysql-connector-java" % "8.0.33",
  "dev.zio"       %% "zio-config"          % "4.0.0-RC16",
  "dev.zio"       %% "zio-config-typesafe" % "4.0.0-RC16",
  "dev.zio"       %% "zio-config-magnolia" % "4.0.0-RC16",
  "dev.zio"       %% "zio-logging"       % "2.1.15",
  "dev.zio"       %% "zio-logging-slf4j" % "2.1.15",
  "org.slf4j"      % "slf4j-simple"      % "2.0.9",
  "dev.zio" %% "zio" % "2.1.6",
  "dev.zio" %% "zio-test" % "2.1.6" % Test,          
  "dev.zio" %% "zio-test-sbt" % "2.1.6" % Test  
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.1"
libraryDependencies += "dev.zio" %% "zio-macros" % "2.1.6"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"
// https://mvnrepository.com/artifact/com.influxdb/influxdb-client-scala
libraryDependencies += "com.influxdb" % "influxdb-client-scala_2.13" % "7.2.0"