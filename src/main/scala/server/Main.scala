package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.Guice
import guice.AppModule
import net.codingwell.scalaguice.InjectorExtensions._

object Main {

    def main(args: Array[String]) {

        val injector = Guice.createInjector(new AppModule)

        implicit val system: ActorSystem = injector.instance[ActorSystem]
        implicit val materializer: Materializer = ActorMaterializer()

        Http().bindAndHandle(injector.instance[Router].routes, "localhost", 8080)
    }

}
