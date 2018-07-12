package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import javax.inject.Inject
import play.api.Configuration

import scala.util.Try

class JwtHelper @Inject()(config: Configuration) {

  val algorithm: Algorithm = Algorithm.HMAC256(config.get[String]("jwt.secret"))
  val issuer: String = config.get[String]("jwt.issuer")

  val ROLE_KEY = "role"
  val USER_KEY = "user"

  /**
   * @param token String JWT token
   * @return Option[String] Some(tokenPayload) if valid, None if validation failed
   */
  def verifyToken(token: String): Option[String] = {
    Try(
      JWT.require(algorithm)
        .withIssuer(issuer)
        .build()
        .verify(token)
    )
      .fold({ ex =>
        println("Token validation failed", ex)
        None
      }, { decoded => Some(decoded.getPayload) })
  }

  /**
   * @param token String JWT token
   * @return Option[String] Some(tokenPayload) if valid, None if validation failed
   */
  def verifyHasRole(token: String, role: String): Option[String] = {
    Try(
      JWT.require(algorithm)
        .withIssuer(issuer)
        .build()
        .verify(token)
    )
      .fold({ ex =>
        println("Token validation failed", ex)
        None
      }, { decoded =>
        println(decoded.getPayload)
        Some(decoded.getPayload)
      })
  }

  /**
   * @param user String
   * @param role String user role
   * @return String Signed JWT Token
   */
  def createToken(user: String, role: String): String = {
    JWT.create()
      .withIssuer(issuer)
      .withClaim(USER_KEY, user)
      .withClaim(ROLE_KEY, role)
      .sign(algorithm)
  }
}
