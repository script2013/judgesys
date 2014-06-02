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
                     url: String
                    )

object Document extends DocumentDAO with DocumentJson{

  def findDocumentById(docIdStr: String): Option[Document] = {
    val docId = new ObjectId(docIdStr)
    println("find doc: " + docId)
    val res = dao.findOneById(docId) //MongoDBObject("id" -> docId))
    println("found resdoc: " + res)

    res
  }

  def listDocumentsByQueryId(queryIdStr: String): List[Document] = {
    val queryId = new ObjectId(queryIdStr)
    val docs = Document.findAll().withFilter(d => d.queryId == queryId).toList
    docs
  }

  def listPossibleLabels(): List[Label] = {
    List( Label(name = "Highly Relevant"),
          Label(name = "Relevant"),
          Label(name = "Non-relevant")
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
        "url" -> d.url
      )
    }
  }

  implicit val DocumentJsonRead = (
    (__ \ 'id).read[ObjectId] ~
      (__ \ 'queryId).read[ObjectId] ~
      (__ \ 'key).read[String] ~
      (__ \ 'description).read[String] ~
      (__ \ 'url).read[String]
    )(Document.apply _)
}

