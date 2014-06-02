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

case class Query(
                 id: ObjectId, // = new ObjectId,
                 testSetId: ObjectId,
                 key: String,
                 description: String,
                 url: String
                 )

object Query extends QueryDAO with QueryJson{

  def findQueryById(queryIdStr: String): Option[Query] = {
    val queryId= new ObjectId(queryIdStr)
    dao.findOneById(queryId) //MongoDBObject("id" -> queryId))
  }


  def listQueriesByTestSetId(testSetIdStr: String): List[Query] = {
    val testSetId = new ObjectId(testSetIdStr)
    val sortField = MongoDBObject("id" -> 1)
    val queries = Query.findAll().sort(orderBy = sortField).withFilter(q => q.testSetId == testSetId).toList
    queries
  }
}

trait QueryDAO extends ModelCompanion[Query, ObjectId] {
  def collection = mongoCollection("queries")
  val dao = new SalatDAO[Query, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("key" -> 1), "key_1", unique = true)

  // Queries
  //def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("name" -> username))
}

/**
 * Trait used to convert to and from json
 */
trait QueryJson {

  implicit val queryJsonWrite = new Writes[Query] {
    def writes(q: Query): JsValue = {
      Json.obj(
        "id" -> q.id,
        "testSetId" -> q.testSetId,
        "key" -> q.key,
        "description" -> q.description,
        "url" -> q.url
      )
    }
  }

  implicit val queryJsonRead = (
      (__ \ 'id).read[ObjectId] ~
      (__ \ 'testSetKey).read[ObjectId] ~
      (__ \ 'key).read[String] ~
      (__ \ 'description).read[String] ~
      (__ \ 'url).read[String]
    )(Query.apply _)
}

