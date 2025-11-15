package com.example.concurrency.dbaccess

import com.zaxxer.hikari.HikariDataSource
import com.example.concurrency.dbaccess.report.SampleEntry
import com.example.concurrency.dbaccess.UserInfo

import java.util.function.{Consumer, Supplier}
import java.util.concurrent.{ExecutorService, Executors}
import java.util.Optional

import org.slf4j.LoggerFactory
import zio._

class ZioDBManager() extends DBManager {

  private val log = LoggerFactory.getLogger(getClass)

  // --------------------------------------------------------------------------
  // ZIO EXECUTOR (correct)
  // --------------------------------------------------------------------------

  private val zioExecutor: Executor =
    Executor.fromExecutionContext(
      scala.concurrent.ExecutionContext.fromExecutorService(
        Executors.newFixedThreadPool(
          java.lang.Runtime.getRuntime.availableProcessors()
        )
      )
    )

  // --------------------------------------------------------------------------
  // Real Java ExecutorService required by DBManager interface
  // (we use a second pool because DBManager strictly requires it)
  // --------------------------------------------------------------------------

  private val javaPool: ExecutorService =
    Executors.newFixedThreadPool(
      java.lang.Runtime.getRuntime.availableProcessors()
    )

  override def getExecutorService(): ExecutorService =
    javaPool

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------

  private def fireAndForget(effect: UIO[Unit]): Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(effect.forkDaemon)
    }

  private def measure(
                       effect: ZIO[Any, Throwable, Optional[UserInfo]],
                       counterSuccess: Supplier[Void],
                       counterFail: Supplier[Void],
                       sampleEntryConsumer: Consumer[SampleEntry],
                       indexInfo: => String
                     ): Unit = {

    val zioEffect: UIO[Unit] =
      for {
        start <- ZIO.succeed(java.lang.System.nanoTime())
        _ <- effect
          .onExecutor(zioExecutor)
          .tap { result =>
            ZIO.succeed {
              counterSuccess.get()
              val end = java.lang.System.nanoTime()
              sampleEntryConsumer.accept(new SampleEntry(start, end - start))
              log.info(s"$indexInfo completed: ${UserInfo.getAsString(result)}")
            }
          }
          .tapError { e =>
            ZIO.succeed {
              counterFail.get()
              val end = java.lang.System.nanoTime()
              sampleEntryConsumer.accept(new SampleEntry(start, end - start))
              log.error(s"$indexInfo failed: ${e.toString}")
            }
          }
          .ignore // Ensures ZIO[Any, Throwable, Unit] -> ZIO[Any, Nothing, Unit]
      } yield ()

    fireAndForget(zioEffect)
  }

  // --------------------------------------------------------------------------
  // INSERT
  // --------------------------------------------------------------------------

  override def insertAction(
                             index: java.lang.Long,
                             dataSource: HikariDataSource,
                             counterSuccess: Supplier[Void],
                             counterFail: Supplier[Void],
                             sampleEntryConsumer: Consumer[SampleEntry]
                           ): Unit = {

    val zioCall: ZIO[Any, Throwable, Optional[UserInfo]] =
      ZIO.attempt(DBQueries.insertUser(index, s"User_$index", dataSource))

    measure(zioCall, counterSuccess, counterFail, sampleEntryConsumer, s"Insert($index)")
  }

  // --------------------------------------------------------------------------
  // UPDATE
  // --------------------------------------------------------------------------

  override def updateAction(
                             dataSource: HikariDataSource,
                             counterSuccess: Supplier[Void],
                             counterFail: Supplier[Void],
                             sampleEntryConsumer: Consumer[SampleEntry]
                           ): Unit = {

    val zioCall: ZIO[Any, Throwable, Optional[UserInfo]] =
      for {
        id <- ZIO.attempt(Utils.getRandomUserId(dataSource))
        result <- ZIO.attempt {
          val newName = s"User_updated_$id"
          DBQueries.updateUser(id, newName, dataSource)
        }
      } yield result

    measure(zioCall, counterSuccess, counterFail, sampleEntryConsumer, "Update")
  }

  // --------------------------------------------------------------------------
  // SELECT
  // --------------------------------------------------------------------------

  override def selectAction(
                             dataSource: HikariDataSource,
                             counterSuccess: Supplier[Void],
                             counterFail: Supplier[Void],
                             sampleEntryConsumer: Consumer[SampleEntry]
                           ): Unit = {

    val zioCall: ZIO[Any, Throwable, Optional[UserInfo]] =
      for {
        id <- ZIO.attempt(Utils.getRandomUserId(dataSource))
        result <- ZIO.attempt(DBQueries.selectUser(id, dataSource))
      } yield result

    measure(zioCall, counterSuccess, counterFail, sampleEntryConsumer, "Select")
  }
}
