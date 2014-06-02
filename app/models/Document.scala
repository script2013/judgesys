package models

//case class User(email: String, name: String)

import play.api.Play.current
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import se.radley.plugin.salat._
import se.radley.plugin.salat.Binders._
import mongoContext._


case class Document( id: ObjectId = new ObjectId,
                     queryId: ObjectId,
                     key: String,
                     description: String,
                     url: String,
                     relevance: String = null
                    ){
  def getRelevance () = {
    if (relevance == null){
      "unjudged"
    }
    else{
      relevance
    }
  }
}

object Document extends DocumentDAO with DocumentJson{
  //https://github.com/novus/salat/wiki/SalatDAO
  def getNextDoc(queryId: ObjectId, docId: ObjectId): Option[Document] = {
    val sortField = MongoDBObject("id" -> 1)
    val seqDocs =
      Document.findAll().
               sort(orderBy = sortField).
               withFilter(doc => doc.queryId == queryId).
               withFilter(doc => doc.id.compareTo(docId) > 0).
               toSeq
    seqDocs.headOption
  }

  def getPrevDoc(queryId: ObjectId, docId: ObjectId): Option[Document] = {
    val sortField = MongoDBObject("id" -> 1)
    val seqDocs =
      Document.findAll().
        sort(orderBy = sortField).
        withFilter(doc => doc.queryId == queryId).
        withFilter(doc => doc.id.compareTo(docId) < 0).
        toSeq
    seqDocs.headOption
  }

  def saveRelevance(docId: ObjectId, relevance: String): Boolean = {
    //val writeRes = dao.update(MongoDBObject("id" -> docId), MongoDBObject("relevance" -> relevance), false, false,  new WriteConcern)

    val doc = findDocumentById(docId.toString).get
    val writeRes = dao.save(doc.copy(relevance = relevance))

    writeRes.getLastError().ok()
  }

  def findDocumentById(docIdStr: String): Option[Document] = {
    val docId = new ObjectId(docIdStr)
    println("find doc: " + docId)
    val res = dao.findOneById(docId) //MongoDBObject("id" -> docId))
    println("found resdoc: " + res)
    res
  }

  def listDocumentsByQueryId(queryIdStr: String): List[Document] = {
    val queryId = new ObjectId(queryIdStr)
    val sortField = MongoDBObject("id" -> 1)
    val docs = Document.findAll().sort(orderBy = sortField).withFilter(d => d.queryId == queryId).toList
    docs
  }

  def listPossibleLabels(): List[Label] = {
    List( Label(name = "Highly Relevant"),
          Label(name = "Relevant"),
          Label(name = "Non-relevant"),
          Label(name = "Unjudged")
    )
  }

}

trait DocumentDAO extends ModelCompanion[Document, ObjectId] {
  def collection = mongoCollection("documents")
  val dao = new SalatDAO[Document, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("key" -> 1), "key_1", unique = true)

  // Queries
  //def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("name" -> username))
}

/**
 * Trait used to convert to and from json
 */
trait DocumentJson {

  implicit val DocumentJsonWrite = new Writes[Document] {
    def writes(d: Document): JsValue = {
      Json.obj(
        "id" -> d.id,
        "queryId" -> d.queryId,
        "key" -> d.key,
        "description" -> d.description,
        "url" -> d.url,
        "relevance" -> d.relevance
      )
    }
  }

  implicit val DocumentJsonRead = (
    (__ \ 'id).read[ObjectId] ~
      (__ \ 'queryId).read[ObjectId] ~
      (__ \ 'key).read[String] ~
      (__ \ 'description).read[String] ~
      (__ \ 'url).read[String] ~
      (__ \ 'relevance).read[String]
    )(Document.apply _)
}

