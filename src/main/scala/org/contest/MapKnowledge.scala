/*
 *  Author: Qi Qi
 *  Title: Ph.D
 *  Affiliation: University of Missouri
 *  Email: qqi@missouri.edu
 */

package org.contest

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._
import java.io.PrintWriter
import java.io.File
import org.contest.KnowledgeBase._
import org.contest.MapUtils._

/** Parallel version based on Akka actors, with enhancement of knowledge base*/
object MapKnowledge extends App {

  /** message class */
  sealed trait sysMessage
  case object Calculate extends sysMessage
  case class Work(quota: List[EntryID]) extends sysMessage
  case class Result(value: Map[EntryID, Entry]) extends sysMessage
  case class combResult(result: Map[EntryID, Entry], duration: Duration)

  /** Worker actor class */
  class Worker extends Actor {

    def calculateQuotaFor(quota: List[EntryID]): Map[EntryID, Entry] = {

      def rolle(map: Map[EntryID, Entry], gen: String) = {
        val knum = gen
        val kon = "ko:" + knum
        val refs = filenames./:((List[Relation](), List[Reaction]()))((pair: (Relations, Reactions), filename: String) =>
          {
            val (relations, reactions) = one_knum_xml(kon, filename)
            (pair._1 ++ relations, pair._2 ++ reactions)
          })
        val eclass = new Entry(refs._1, refs._2)
        map + (knum -> eclass)
      }

      quota./:(Map[EntryID, Entry]())(rolle)
    }

    def receive = {
      case Work(quota) =>
        //println("quota's size:" + quota.length)
        sender ! Result(calculateQuotaFor(quota)) // perform the work
    }
  }

  /** Master actor class */
  class Master(nrOfWorkers: Int, listener: ActorRef) extends Actor {

    var resMap: Map[EntryID, Entry] = Map[EntryID, Entry]()
    var nrOfResults: Int = _
    val start: Long = System.currentTimeMillis

    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

    def receive = {
      case Calculate =>
        for (i <- 0 until nrOfWorkers) {
          workerRouter ! Work(chunkList(mapGenesList, i, nrOfWorkers))
          println("send quota to worker:" + (i + 1))
        }
      case Result(value) =>
        resMap = resMap ++ value
        nrOfResults += 1
        println("results gotten: " + nrOfResults)
        if (nrOfResults == nrOfWorkers) {
          listener ! combResult(resMap, duration = (System.currentTimeMillis - start).millis)
          context.stop(self)
        }
    }
  }

  /** Listener actor class */
  class Listener extends Actor {
    def receive = {
      case combResult(result, duration) =>
        val writer = new PrintWriter(new File("AllRelsOnMappedGenes_parallel.txt"))
        result.foreach(map => {
          writer.println(map._1 + "->")
          map._2.toStringFile(writer)
        })
        writer.close()
        println("\n\tCalculation time: \t%s".format(duration))
        context.system.shutdown()
    }
  }

  def chunkList(list: List[String], n: Int, total: Int) = {
    val size = list.length / total
    if (n == total - 1) list.drop(n * size)
    else list.drop(n * size).take(size)
  }

  def calculate(nrOfWorkers: Int) = {
    // Create an Akka system
    val system = ActorSystem("calSystem")

    // create the result listener, which will print the result and 
    // shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(new Master(nrOfWorkers, listener)), name = "master")

    // start the calculation
    master ! Calculate
  }

  val mapGenes = MapUtils.GetMapGenes("wholelist-and-ko.txt")
  println("mapGenes size: " + mapGenes.size)
  val mapPathways = MapUtils.GetMapPathways("wholelist-maps.txt")
  println("mapPathways size: " + mapPathways.size)
  val path = "kegg/"
  val filenames_0 = mapPathways./:(List[String]())((l: List[String], m: (String, (String, String))) => l.::(path + "ko" + m._1.substring(3) + ".xml"))
  println("filenames size: " + filenames_0.length)

  val filenames = filenames_0 //.take(50)  // for a quick test

  val mapGenesList_0 = mapGenes.keys.toList

  val mapGenesList = mapGenesList_0 //.take(25) // for a quick test

  // use 200 worker threads to calculate.
  calculate(200)

}
