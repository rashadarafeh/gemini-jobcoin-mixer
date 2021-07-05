package com.gemini.jobcoin.controller

import com.gemini.jobcoin.JobcoinBackend
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, PathBindable}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

@Singleton
class JobCoinController @Inject()(jobcoinBackend: JobcoinBackend,cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {
  def createAccounts(addresses: Seq[String]) = {
    // for metrics purposes this call can be wrapped in a timer
    // to report performance
    Action {
      Ok(Json.toJson(
        Map(
          "depositAccount" -> jobcoinBackend.createAccounts(addresses)
        )
      ))
    }
  }
}

object Binders {
  // This binder is so that we can accept a Seq[String]
  // as input to the endpoint in the form of comma seperated
  // strings.
  // e.g. localhost:5432/api/createAddreses/name1,name2,name3
  //
  // I have used binders like these before, and am re-using a
  // favored one here.
  //
  // To make a PathBindable[Seq[T]], we need to accumalate the results
  // from binding T, which returns an Either[something, T]. so we go through
  // the Eithers recursively and accumulate their right values into the Seq[T]
  def traverseEither[A, B, C](values: List[A])(f: A => Either[C, B]): Either[C, List[B]] = {
    @tailrec
    def loop(values: List[A], acc: List[B]): Either[C, List[B]] = {
      values match {
        case head :: tail =>
          f(head) match {
            case Left(error)  => Left(error)
            case Right(value) => loop(tail, value :: acc)
          }
        case Nil => Right(acc)
      }
    }
    loop(values, List.empty).right.map(_.reverse)
  }

  def seqBindable[T: PathBindable](delimiter: String)(implicit p: PathBindable[T]) = new PathBindable[Seq[T]] {

    def bind(key: String, value: String): Either[String, Seq[T]] = {
      val values = value.split(delimiter).toList
      val results = traverseEither(values)(p.bind(key, _))
      results.right.flatMap {
        case Nil   ⇒ Left(s"a value is required for $key")
        case other ⇒ Right(other)
      }
    }

    def unbind(key: String, list: Seq[T]): String =
      list.map(p.unbind(key, _)).mkString(",")
  }
  // The end result: PathBindable[Seq[T]]
  implicit def commaSeparatedSeqBindable[T: PathBindable] = seqBindable[T](",")
}
