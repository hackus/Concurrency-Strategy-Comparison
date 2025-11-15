package com.example.concurrency.dbaccess

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.example.concurrency.dbaccess.report.SampleEntry
import com.example.concurrency.dbaccess.UserInfo
import com.zaxxer.hikari.HikariDataSource

import org.slf4j.LoggerFactory

import java.util.concurrent.{ExecutorService, Executors}
import java.util.function.{Consumer, Supplier}
import scala.concurrent.ExecutionContext
import cats.effect.unsafe.implicits.global

class CatsDBManager(
                     executorService: ExecutorService,
                     runtime: IORuntime
                   ) extends DBManager {

  // REAL zero-arg constructor for Cucumber
  def this() = this(
    Executors.newFixedThreadPool(
      java.lang.Runtime.getRuntime.availableProcessors()
    ),
    IORuntime.global
  )

  private val log = LoggerFactory.getLogger(getClass)

  private val ec: ExecutionContext =
    ExecutionContext.fromExecutorService(executorService)

  private def launch(io: IO[Unit]): Unit =
    io.unsafeRunAndForget()

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

    val io: IO[Unit] = IO.defer {
      val startNs = java.lang.System.nanoTime()

      IO.blocking(DBQueries.insertUser(index, s"User_$index", dataSource))
        .evalOn(ec)
        .attempt
        .flatMap {
          case Right(result) =>
            IO.blocking {
              counterSuccess.get()
              val endNs = java.lang.System.nanoTime()
              sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
              log.info(s"Insert completed: ${UserInfo.getAsString(result)}")
            }

          case Left(e) =>
            IO.blocking {
              counterFail.get()
              val endNs = java.lang.System.nanoTime()
              sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
              log.error(s"Insert failed for $index: ${e.toString}")
            }
        }
    }

    launch(io)
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

    val io: IO[Unit] = IO.defer {
      val startNs = java.lang.System.nanoTime()

      val task =
        for {
          index <- IO.blocking(Utils.getRandomUserId(dataSource)).evalOn(ec)
          result <- IO.blocking {
            val name = s"User_updated_$index"
            DBQueries.updateUser(index, name, dataSource)
          }.evalOn(ec)
        } yield (index, result)

      task.attempt.flatMap {
        case Right((index, result)) =>
          IO.blocking {
            counterSuccess.get()
            val endNs = java.lang.System.nanoTime()
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
            log.info(s"Update completed: ${UserInfo.getAsString(result)}")
          }

        case Left(e) =>
          IO.blocking {
            counterFail.get()
            val endNs = java.lang.System.nanoTime()
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
            log.error(s"Update failed: ${e.toString}")
          }
      }
    }

    launch(io)
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

    val io: IO[Unit] = IO.defer {
      val startNs = java.lang.System.nanoTime()

      val task =
        for {
          index <- IO.blocking(Utils.getRandomUserId(dataSource)).evalOn(ec)
          result <- IO.blocking(DBQueries.selectUser(index, dataSource)).evalOn(ec)
        } yield (index, result)

      task.attempt.flatMap {
        case Right((index, result)) =>
          IO.blocking {
            counterSuccess.get()
            val endNs = java.lang.System.nanoTime()
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
            log.info(s"Select completed: ${UserInfo.getAsString(result)}")
          }

        case Left(e) =>
          IO.blocking {
            counterFail.get()
            val endNs = java.lang.System.nanoTime()
            sampleEntryConsumer.accept(new SampleEntry(startNs, endNs - startNs))
            log.error(s"Select failed: ${e.toString}")
          }
      }
    }

    launch(io)
  }

  // --------------------------------------------------------------------------
  // REQUIRED BY DBManager INTERFACE
  // --------------------------------------------------------------------------

  override def getExecutorService(): ExecutorService =
    executorService
}
