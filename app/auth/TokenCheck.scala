package auth

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class TokenCheck @Inject()(cc: ControllerComponents, authenticated: Authenticated)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def tokenCheck: Action[AnyContent] = authenticated { req =>
    Ok("Good")
  }


}
