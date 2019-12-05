package zio.logging

import zio._

object Examples extends zio.App {
  val correlationId = ContextKey[String]("correlationId", "undefined-correlation-id")

  val env =
    for {
      ctxMap <- ContextMap.empty
    } yield {
      val stringFormat = "[correlation-id = %s] %s"
      new Logging with LoggingContext with LoggingFormat[String] {
        self =>
        override def logging: AbstractLogging.Service[Any] = new Slf4jLogger()

        override def loggingContext: LoggingContext.Service[Any] = ctxMap

        override def format(message: String): ZIO[Any, Nothing, String] =
          loggerContext
            .get(correlationId)
            .map(correlationId => stringFormat.format(correlationId, message))
            .provide(self)
      }
    }

  override def run(args: List[String]) =
    (for {
      fiber <- loggerContext.set(correlationId, "1234").fork
      _     <- logger.info("info message without correlation id")
      _     <- fiber.join
      _     <- loggerContext.set(correlationId, "1234111")
      _     <- logger.info("info message with correlation id")
      _     <- logger.info("another info message with correlation id").fork
    } yield 1).provideSomeM(env)
}