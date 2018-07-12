package login

import java.nio.charset.StandardCharsets
import java.util.Base64

import auth.JwtHelper
import javax.inject._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class LoginResponse(token: String)
case class LoginError(error: String)

@Singleton
class LoginController @Inject()(jwtHelper: JwtHelper, cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  implicit val responseFormat: OFormat[LoginResponse] = Json.format[LoginResponse]
  implicit val errorFormat: OFormat[LoginError] = Json.format[LoginError]

  def login: Action[AnyContent] = Action.async { req =>
    Future.successful {
      extractCredentials(req.headers.get("Authorization"))
        .map {
          case Array(user, pass) => Ok(Json.toJson(LoginResponse(jwtHelper.createToken(user, "common_user"))))
        }
        .getOrElse(
          Unauthorized(Json.toJson(LoginError("Invalid credentials provided")))
        )
    }
  }

  def extractCredentials(authHeader: Option[String]): Option[Array[String]] = {
    authHeader.map(_.split("""\s"""))
      .map {
        case Array("Basic", creds) => new String(Base64.getDecoder.decode(creds), StandardCharsets.UTF_8)
      }
      .map(_.split(":"))
      .orElse(None)
  }

}