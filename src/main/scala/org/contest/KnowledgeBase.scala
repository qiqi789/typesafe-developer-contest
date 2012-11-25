/*
 *  Author: Qi Qi
 *  Title: Ph.D
 *  Affiliation: University of Missouri
 *  Email: qqi@missouri.edu
 */

package org.contest

object KnowledgeBase {
  /** types related to knowledge base development */
  type EntryID = String
  type EntryIDs = List[String]
  type RelDir = String
  type RelType = String
  type RelSubType = String
  type RelSubTypes = List[RelSubType]
  type ReaSubstrate = String
  type ReaProduct = String
  type Relation = (RelDir, EntryIDs, RelType, RelSubTypes)
  type Relations = List[Relation]
  type Substrate = String
  type Product = String
  type Reaction = (Substrate, Product)
  type Reactions = List[Reaction]
  
  /** knowledge entry class  */
  class Entry(relations: Relations, reactions: Reactions) {
    var relationsL = relations
    var reactionsL = reactions
      
    def toStringFile(file: java.io.PrintWriter) = {
      relations.foreach(rel => file.println(rel._1+"::"+rel._2.mkString("[", " ", "]")+"::"+rel._3 + "::" + rel._4.mkString("[", ";", "]") ) )
      reactions.foreach(rea => file.println(rea))
    }
  }
}