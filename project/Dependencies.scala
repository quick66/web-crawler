import sbt._

object Dependencies {
    
    val akkaVersion = "2.5.22"
    val akkaHttpVersion = "10.1.8"
    
    val list: Seq[ModuleID] = Seq(
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

        // Logging
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "ch.qos.logback" % "logback-classic" % "1.2.3",

        // DI
        "net.codingwell" %% "scala-guice" % "4.2.3",

        // Parsing
        "org.jsoup" % "jsoup" % "1.12.1"
    )
}
