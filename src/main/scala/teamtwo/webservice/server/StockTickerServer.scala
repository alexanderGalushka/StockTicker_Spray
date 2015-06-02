package teamtwo.webservice.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.io.IO
import akka.io.Tcp.CommandFailed
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import teamtwo.config.HttpConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StockTickerServer extends App {

  val ApplicationName = "StockTickerServer"

  implicit val actorSystem = ActorSystem(s"$ApplicationName-system")
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  actorSystem.registerOnTermination({
    actorSystem.log.info(s"$ApplicationName actors shutting down")
  })

  // Initialize the Spray based web server which will process incoming requests in a non-blocking manner
  initializeSpray(HttpConfig.interface, HttpConfig.port) onFailure {
    case e =>
      actorSystem.shutdown()
      throw e
  }

  def initializeSpray(interface: String, port: Int)(implicit actorSystem: ActorSystem): Future[Http.Bound] = {
    val service = actorSystem.actorOf(StockTickerServiceActor.props, "stock-ticker-service")
    IO(Http) ? Http.Bind(service, interface = HttpConfig.interface, port = HttpConfig.port) flatMap {
      case b: Http.Bound => Future.successful(b)
      case CommandFailed(b: Http.Bind) => Future.failed(new RuntimeException(s"Unable to bind to port ${HttpConfig.port} on interface ${HttpConfig.interface}"))
    }
  }
}
