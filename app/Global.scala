import com.mongodb.casbah.Imports._
import play.api._
import libs.ws.WS
import models._
import se.radley.plugin.salat._
import se.radley.plugin.salat.Binders._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    //if (User.count(DBObject(), Nil, Nil) == 0) {
    try {
      Logger.info("Loading Testdata")

      val user = User.save(User(
        name = "stefan",
        email = "savev@ccs.neu.edu"
      ))
      //val userId = user.getField("_id").asInstanceOf[ObjectId]
      val testSetId = new ObjectId()
      val testSet = TestSet.save(TestSet(
        id = testSetId,
        key = "ComputerScienceResearchers",
        description = "A set of computer science researchers"
      ))
      //val testSetId = testSet.getField("_id").asInstanceOf[ObjectId]
      println("----------" + testSetId)
      val queryId1 = new ObjectId()
      val query1 = Query.save(Query(id = queryId1,
                                   testSetId = testSetId,
                                   key = "George Mooney",
                                   description = "George_Mooney",
                                   url = "http://george"
                        ))

      Document.save(Document(
        queryId = queryId1,
        key = "what_is_map_reduce",
        description = "What is map reduce",
        url = "http://questions/map_reduce"
      ))

      Document.save(Document(
        queryId = queryId1,
        key = "differences_between_svm_rank_and_ranking_svm",
        description = "Differences between svm rank and ranking svm",
        url = "http://questions/svm_rank"
      ))

      val queryId2 = new ObjectId()
      val query2 = Query.save(Query(id = queryId2,
        testSetId = testSetId,
        key = "Gene Smith",
        description = "Gene_Smith",
        url = "http://gene"
      ))



      //val queryId = query.getField("id").asInstanceOf[ObjectId]

    }
    catch {
      case e: Throwable => e.printStackTrace()
    }
  }

}
