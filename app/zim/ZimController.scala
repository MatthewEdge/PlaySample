package zim

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class ZimController @Inject()(cc: ControllerComponents, service: ZimService)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  import ZimQuote._

  def getQuote = Action.async {
    service.fetchSaying()
      .map(resp => Ok(Json.toJson(resp)))
      .recover {
        case ex => BadRequest(ex.getMessage)
      }
  }

}
