package zim

import javax.inject._
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ZimService @Inject()(repo: ZimRepository)(implicit ec: ExecutionContext) {

  def fetchSaying(): Future[ZimQuote] = {
    Logger.info("Fetching saying")

    repo.fetchSayings().map(_.head)
  }
}
