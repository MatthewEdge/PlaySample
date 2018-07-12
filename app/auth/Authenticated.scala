package auth

import javax.inject.Inject
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Authenticated @Inject()(parser: BodyParsers.Default, jwtHelper: JwtHelper)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    val jwtToken = request.headers.get("X-Auth-Token").getOrElse("")

    jwtHelper.verifyToken(jwtToken) // TODO more validation
      .map(_ => block(request))
      .getOrElse(Future.successful(Unauthorized("Invalid token passed")))
  }
}

class AdminOnly @Inject() (parser: BodyParsers.Default, jwtHelper: JwtHelper)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    val jwtToken = request.headers.get("X-Auth-Token").getOrElse("")

    jwtHelper.verifyHasRole(jwtToken, "admin_user")
      .map(_ => block(request))
      .getOrElse(Future.successful(Unauthorized("Invalid token passed")))
  }
}