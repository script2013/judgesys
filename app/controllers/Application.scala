package controllers

import play.api._
import play.api.mvc._
import com.mongodb.WriteConcern
import models._
import org.bson.types.ObjectId

case class MyValue(value: String)

case class ViewTestData(testSet: TestSet, user: User)

case class ViewQueryData(query: Query, user: User)

case class ViewDocumentData(document: Document, query: Query, user: User)

object ViewTestData{

  def fromInput(user: User, testSetId: String): Option[ViewTestData] = {
    for(
      testSet <- TestSet.findTestSetById(testSetId)
    ) yield ViewTestData(testSet, user)
  }

}

object ViewDocumentData{

  def fromInput(user: User, docId: String): Option[ViewDocumentData] = {
    for(
      doc <- Document.findDocumentById(docId);
      query <- Query.findQueryById(doc.queryId.toString)
    ) yield ViewDocumentData(doc, query, user)
  }

}

object ViewQueryData{
  def fromInput(user: User, queryId: String): Option[ViewQueryData] = {
    for(
      query <- Query.findQueryById(queryId)
    ) yield ViewQueryData(query, user)
  }

  def fromViewDocumentData(viewDocData: ViewDocumentData): ViewQueryData = {
    ViewQueryData(viewDocData.query, viewDocData.user)
  }
}

object Application extends Controller {
  import play.api.data._
  import play.api.data.Forms._
  import play.api.data.format.Formats._

  import play.api.data._
  import play.api.data.Forms._
  import play.api.data.format.Formats._

  import play.api.data.validation.Constraints._
  import models.User
  import models.Query
  import play.api.mvc._
  import play.core._
  import play.api.libs.iteratee._
  import play.api.libs.concurrent._
  import play.api.http._
  import play.api.libs.json._
  import play.api.http.Status._
  import play.api.http.HeaderNames._

  val connected = "connected"

  def userApply(email: String, name: String) = User(id =null, email = email, name = name)
  def userUnApply(user: User) = Some(user.email, user.name)

  val userForm: Form[User] = Form(
    mapping(
      "email" -> text, // verifying(required),
      "name" -> nonEmptyText(minLength = 3) // verifying(required)
    )(userApply)(userUnApply)
  )



  def answerFormApply(value: String): MyValue = MyValue(value)

  def answerFormUnapply(value: MyValue) = Some(value.value)

  val answerForm: Form[MyValue] = Form(
    mapping ("relevance" -> text) (answerFormApply)(answerFormUnapply) )

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def queries() = Action {
    val queries = Query.findAll().toList
    Ok(Json.toJson(queries))
  }

  def users() = Action {
    val users = User.findAll().toList
    Ok(Json.toJson(users))
  }

  def redirectToIndexPage() = Redirect(routes.Application.index2().url)

  def withUser[A, T <: Result](continueWithF: User => T) (implicit request: Request[A]): Result = {
    request.session.get(connected).map { userName =>
      User.findOneByUsername(userName) match {
        case None => redirectToIndexPage()
        case Some(user) => continueWithF(user)
      }
    }.getOrElse {
      redirectToIndexPage()
    }
  }

  def withOption[A](maybeValue: Option[A])(f: A => Result): Result = {
    maybeValue.fold({redirectToIndexPage()})(value => f(value))
  }

  def index2 = Action { request =>
    request.session.get(connected).map { user =>
      Ok(views.html.index2(user))
    }.getOrElse {
      //Unauthorized("Oops, you are not connected")
      Ok(views.html.login(userForm))
    }
  }

  def save = Action { implicit request =>
    userForm.bindFromRequest.fold(
      badForm => Ok(views.html.login(badForm)),
      user => {
        User.findOneByUsername(user.name) match {
          case None => User.save(user, WriteConcern.SAFE)
          case Some(_) => ()
        }

        val result = Redirect(routes.Application.index2().url) //Ok("Connected: " + user.name + " " + user.email)
        result.withSession(request.session + (connected -> user.name))
      }
    )
  }

  /*
  def saveAnswer2(queryId: String)  = Action { implicit request =>
    val res = answerForm.bindFromRequest.get
    Ok("Submitted queryId " + queryId + " " + res)
  }
  */

  def saveAnswer(docId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption (ViewDocumentData.fromInput(user, docId)) (viewDocData => {
        val res = answerForm.bindFromRequest.get
        if (!Document.saveRelevance(new ObjectId(docId), res.value)){
          throw new Exception("cannot save")
        }

        val docIdObj = new ObjectId(docId)
        val maybeNextDocId = Document.getNextDoc(viewDocData.query.id, docIdObj)
        maybeNextDocId match {
          case Some(nextDocId) =>
            withOption (ViewDocumentData.fromInput(user, nextDocId.id.toString)) (nextDocData => {
              Ok(views.html.viewDocument(nextDocData))
            })
          case None => Ok(views.html.viewDocuments(ViewQueryData.fromViewDocumentData(viewDocData)))
        }
      })
    }
  }

  //def viewQuery(queryId: String) = Action{ request =>
  //  Ok(views.html.viewQuery(queryId))
  //}

  def viewTestSet(setId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption (ViewTestData.fromInput(user, setId)) (viewTestData => {
        Ok(views.html.viewTestSet(viewTestData))
      })
    }
  }

  def viewDocuments(queryId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption (ViewQueryData.fromInput(user, queryId)) (viewData => {
        Ok(views.html.viewDocuments(viewData))
      })
    }
  }

  def nextDoc(docId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption (ViewDocumentData.fromInput(user, docId)) (viewDocData => {
        val docIdObj = new ObjectId(docId)
        val maybeNextDocId = Document.getNextDoc(viewDocData.query.id, docIdObj)
        maybeNextDocId match {
          case Some(nextDocId) =>
            withOption (ViewDocumentData.fromInput(user, nextDocId.id.toString)) (nextDocData => {
              Ok(views.html.viewDocument(nextDocData))
            })
          case None => Ok(views.html.viewDocuments(ViewQueryData.fromViewDocumentData(viewDocData)))
        }
      })
    }
  }

  def prevDoc(docId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption (ViewDocumentData.fromInput(user, docId)) (viewDocData => {
        val docIdObj = new ObjectId(docId)
        val maybeNextDocId = Document.getPrevDoc(viewDocData.query.id, docIdObj)
        maybeNextDocId match {
          case Some(nextDocId) =>
            withOption (ViewDocumentData.fromInput(user, nextDocId.id.toString)) (nextDocData => {
              Ok(views.html.viewDocument(nextDocData))
            })
          case None => Ok(views.html.viewDocuments(ViewQueryData.fromViewDocumentData(viewDocData)))
        }
      })
    }
  }

  def viewDocument(docId: String) = Action{implicit request =>
    withUser { (user: User) =>
      withOption(ViewDocumentData.fromInput(user, docId))(viewDocData => Ok(views.html.viewDocument(viewDocData)))
    //  Ok(user.name + " " + docId)
    }
  }

 /*
 Action{ request =>
    withUser(request)
    Ok(views.html.viewDocument(docId))
  }
*/

  def logout = Action { request =>
    request.session.get(connected).map { user =>
      val result = Redirect(routes.Application.index2().url)
      result.withSession(request.session - (connected))
    }.getOrElse {
      //Unauthorized("Oops, you are not connected")
      Redirect(routes.Application.index2().url)
    }
  }


}