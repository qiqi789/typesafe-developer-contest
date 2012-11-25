/*
 *  Author: Qi Qi
 *  Title: Ph.D
 *  Affiliation: University of Missouri
 *  Email: qqi@missouri.edu
 */

package org.contest

import scala.util.matching.Regex
import scala.io.Source
import java.io.PrintWriter
import java.io.File
import sys.process._

import org.contest.KnowledgeBase._

object MapUtils {

  /**
   * generate a map of all pathway IDs and its name and number of gene mappings
   *  input: a file from kegg contains all mapped pathways listing
   *  output: a map from pathway ID to its info. Map[String, (String, String)]
   *
   */
  def GetMapPathways(filename: String): Map[String, (String, String)] = {
    val filesource = Source.fromFile(filename)
    //val mat_pattern = """(map\d+) ([a-zA-Z-/, ']*)(\(\d+\))""".r
    val mat_pattern = """(map\d+) ([a-zA-Z-/, '\(\)\d]*) (\(\d+\))$""".r
    def roll(map: Map[String, (String, String)], line: String): Map[String, (String, String)] = {
      mat_pattern.findFirstMatchIn(line) match {
        case None => map
        case Some(mat_pattern(mapid, name, rank)) => map + (mapid -> (name, rank))
      }
    }

    filesource.getLines./:(Map[String, (String, String)]())(roll)
  }

  /**
   * generate a map of k# -> [gen#], a list of gen# mapped
   *
   */
  def GetMapGenes(filename: String) = {
    val filesource = Source.fromFile(filename)
    val regex_pat = """(\w+):([A-Z\d]+)[ \t](K\d+)""".r
    //val test = "sce:YBR158W	K15081"
    def roll(map: Map[String, List[String]], line: String): Map[String, List[String]] = {
      regex_pat.findFirstMatchIn(line) match {
        case None => map
        case Some(regex_pat(org, genId, knum)) =>
          if (map.contains(knum))
            map + (knum -> map(knum).::(genId)) // this knum pair will replace the previous version.
          else map + (knum -> List(genId))
      }
    }
    filesource.getLines./:(Map[String, List[String]]())(roll)

  }

  /**
   * generate a map of gen# -> [k#], actually only one k#
   *
   */
  def GetMapGenesPlain(filename: String) = {
    val filesource = Source.fromFile(filename)
    val regex_pat = """(\w+):([A-Z\d]+)[ \t](K\d+)""".r
    //val test = "sce:YBR158W	K15081"
    def roll(map: Map[String, List[String]], line: String): Map[String, List[String]] = {
      regex_pat.findFirstMatchIn(line) match {
        case None => map
        case Some(regex_pat(org, genId, knum)) =>
          if (map.contains(genId))
            map + (genId -> map(genId).::(knum))
          else map + (genId -> List(knum))
      }
    }
    filesource.getLines./:(Map[String, List[String]]())(roll)

  }

  /**
   * Handle with a KO# and a Kegg pathway XML file
   *
   */
  def one_knum_xml(testknum: String, filename: String) = {

    val root = scala.xml.XML.loadFile(filename)

    val entries = root \ "entry"

    // entries containing the queried k#
    val queries = entries.filter(e => (e \ "@name").text.contains(testknum))

    // set of IDs on the queried k#
    def roll_ids(query: scala.xml.Node, set: Set[String]): Set[String] = set.+((query \ "@id").text)
    val idSet = queries.:\(Set[String]())(roll_ids)

    val relations = root \ "relation"
    // parallel collection
    //      val relations = (root \ "relation").par

    // queried k#'s relations
    val relationsFiltered = relations filter (rel => idSet.contains((rel \ "@entry1").text) || idSet.contains((rel \ "@entry2").text))

    // queried k#'s reactions
    val reactionsFiltered = (root \ "reaction") filter (reac => idSet.contains((reac \ "@id").text))

    def roll_relations(rel: scala.xml.Node, rlist: Relations): Relations = {

      def rolle(entry: String, dir: String): Relations = {
        // should only have one matched query for a given ID in entries
        val qui = entries.filter(q => (q \ "@id").text == (rel \ entry).text).head

        val subnames = (rel \ "subtype")./:(List[RelSubType]())((list: RelSubTypes, sub: scala.xml.Node) =>
          if ((sub \ "@name").text != "compound") list.::((sub \ "@name").text)
          else {
            val en = entries.filter(q => (q \ "@id").text == (sub \ "@value").text).head
            list.::((en \ "@name").text)
          })
        rlist.::((dir, (qui \ "@name").text.split(" ").toList, (rel \ "@type").text, subnames))
      }

      (idSet.contains((rel \ "@entry1").text), idSet.contains((rel \ "@entry2").text)) match {
        case (true, false) =>
          {
            rolle("@entry2", "to")
          } case (false, true) =>
          {
            rolle("@entry1", "from")
          }
        case (true, true) =>
          {
            rolle("@entry1", "from") ++ rolle("@entry2", "to")
          } case _ => rlist
      }
    }
    val reList = relationsFiltered.:\(List[Relation]())(roll_relations)

    def roll_reactions(list: Reactions, react: scala.xml.Node): Reactions = {
      val substrates = (react \ "substrate") map (sub => (sub \ "@name").text)
      val products = (react \ "product") map (pro => (pro \ "@name").text)
      (substrates zip products).toList
    }
    val reactList = reactionsFiltered./:(List[Reaction]())(roll_reactions)

    (reList.distinct, reactList.distinct)
  }

  /**
   * download to local of all the kegg pathways KGML files according to all the keys in the map
   *
   */
  def DownloadAllMaps(allMaps: Map[String, (String, String)]): Unit = {
    // download all the xml files
    // use parallel collection, it is fast for here.
    allMaps.par.foreach(item => {
      val knum = item._1.substring(3)
      val url = "http://www.genome.jp/kegg-bin/download?entry=ko" + knum + "&format=kgml"
      "wget -O " + "ko" + knum + ".xml " + url.toString() !
    })
  }

}
