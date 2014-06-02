package models

//case class TestSet(email: String, name: String)

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

case class TestSet(id: ObjectId, // = new ObjectId,
                   key: String,
                   description: String)

object TestSet extends TestSetDAO with TestSetJson{
  // Queries
  def findTestSetByKey(key: String): Option[TestSet] = dao.findOne(MongoDBObject("key" -> key))

  def listTestSetsByUserId(userId: String): List[TestSet] = {
    val testSets = TestSet.findAll().toList
    testSets
  }
}

trait TestSetDAO extends ModelCompanion[TestSet, ObjectId] {
  def collection = mongoCollection("testset")
  val dao = new SalatDAO[TestSet, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("key" -> 1), "key_1", unique = true)



}

/**
 * Trait used to convert to and from json
 */
trait TestSetJson {

  implicit val TestSetJsonWrite = new Writes[TestSet] {
    def writes(o: TestSet): JsValue = {
      Json.obj(
        "id" -> o.id,
        "key" -> o.key,
        "description" -> o.description
      )
    }
  }
  implicit val TestSetJsonRead = (
      (__ \ 'id).read[ObjectId] ~
      (__ \ 'key).read[String] ~
      (__ \ 'description).read[String]
    )(TestSet.apply _)
}

