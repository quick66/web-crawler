package server

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.Materializer
import com.google.inject.Guice
import guice.AppModule
import net.codingwell.scalaguice.InjectorExtensions._

object Main {

    def main(args: Array[String]) {

        val injector = Guice.createInjector(new AppModule)

        implicit val system: ActorSystem = injector.instance[ActorSystem]
        implicit val materializer: Materializer = injector.instance[Materializer]
        val router = injector.instance[Router]

        injector.instance[HttpExt].bindAndHandle(router.routes, "localhost", 8080)
    }

}
