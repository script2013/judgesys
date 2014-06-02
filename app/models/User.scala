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

case class User(id: ObjectId = new ObjectId,
                name: String,
                email: String
               )

object User extends UserDAO with UserJson

trait UserDAO extends ModelCompanion[User, ObjectId] {
  def collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("name" -> 1), "name_1", unique = true)

  // Queries
  def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("name" -> username))
}

/**
 * Trait used to convert to and from json
 */
trait UserJson {

  implicit val userJsonWrite = new Writes[User] {
    def writes(u: User): JsValue = {
      Json.obj(
        "id" -> u.id,
        "name" -> u.name,
        "email" -> u.email
      )
    }
  }
  implicit val userJsonRead = (
    (__ \ 'id).read[ObjectId] ~
      (__ \ 'name).read[String] ~
      (__ \ 'email).read[String]
    )(User.apply _)
}

