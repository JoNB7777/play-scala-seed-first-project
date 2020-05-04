package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.stream.ActorMaterializer
import models.DataModel
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.FakeRequest
import play.api.http.Status
import repositories.DataRepository
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.commands.{LastError, WriteResult}
import play.api.http.Status._
import reactivemongo.core.errors.GenericDriverException

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val mockDataRepository: DataRepository = mock[DataRepository]
  implicit val system: ActorSystem = ActorSystem("Sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestApplicationController extends ApplicationController (
    controllerComponents, mockDataRepository, executionContext
  )

  "ApplicationController .index" should {

    val dataModel: DataModel = DataModel(
      "abcd",
      "test name",
      "test description",
      100
    )

    val jsonBody: JsObject = Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )

    when(mockDataRepository.find(any())(any()))
      .thenReturn(Future(List(dataModel)))


    val result = TestApplicationController.index()(FakeRequest())

    "return the correct JSON body" in {
      await(jsonBodyOf(result)) shouldBe Json.arr(jsonBody)
    }

    "return OK 200 status" in {
      status(result) shouldBe OK
    }
  }

  "ApplicationController .create" when {

    "the json body is valid" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      val writeResult: WriteResult = LastError(ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None)

      when(mockDataRepository.create(any()))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

      "return Created" in {
        status(result) shouldBe CREATED
      }
    }

    "the json body is not valid" should {
      val invalidJsonBody: JsObject = Json.obj(
      )
      val writeResult: WriteResult = LastError(
        ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None
      )
      when(mockDataRepository.create(any()))
        .thenReturn(Future(writeResult))
      val result = TestApplicationController.create()(FakeRequest().withBody(invalidJsonBody))
      "return an error " in {
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "ApplicationController .read()" when {

    "the JSON body is valid" should {

      val dataModel: DataModel = DataModel(
        "abcd",
        "test name",
        "test description",
        100
      )

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      when(mockDataRepository.read(any()))
        .thenReturn(Future(dataModel))

      val result = TestApplicationController.read(dataModel._id)(FakeRequest())

      "return OK status 200" in {
        status(result) shouldBe 200
      }

      "return the correct JSON body" in {
        await(jsonBodyOf(result)) shouldBe jsonBody
      }
    }



  }

  "ApplicationController .update()" should {

    val jsonBody: JsObject = Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )

    val dataModel: DataModel = DataModel(
      "abcd",
      "test name",
      "test description",
      100
    )

    when(mockDataRepository.update(any()))
      .thenReturn(Future(dataModel))

    val result = TestApplicationController.update(dataModel._id)(FakeRequest().withBody(jsonBody))

    "return the correct JSON" in {
      await(jsonBodyOf(result)) shouldBe jsonBody
    }


    "the supplied json is invalid" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "Test description"
      )

      val result = TestApplicationController.update(dataModel._id)(FakeRequest().withBody(jsonBody))

      "return a BadRequest" in {
        status(result) shouldBe BAD_REQUEST
      }
    }

  }

  "ApplicationController .delete()" should {

    "return accepted" in {
      val writeResult: WriteResult = LastError(
        ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None
      )
      when(mockDataRepository.delete(any()))
        .thenReturn(Future(writeResult))
      val result = TestApplicationController.delete(any())(FakeRequest())
      status(result) shouldBe ACCEPTED
    }

  }


  "the mongo data creation failed" should {

    val jsonBody: JsObject = Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )

    when(mockDataRepository.create(any()))
      .thenReturn(Future.failed(GenericDriverException("Error")))

    "return an error" in {

      val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      await(bodyOf(result)) shouldBe Json.obj("message" -> "Error adding item to Mongo").toString()
    }
  }


  "reading the mongo data failed" should {

    "return an error" in {

      when(mockDataRepository.read(any()))
        .thenReturn(Future.failed(GenericDriverException("Error")))

      val invalidResult = TestApplicationController.read(any())(FakeRequest())

      status(invalidResult) shouldBe Status.INTERNAL_SERVER_ERROR
      await(bodyOf(invalidResult)) shouldBe Json.obj("message" -> "Error reading from database").toString
    }
  }


  "the mongo data update failed" should {

    val jsonBody: JsObject = Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )

    val dataModel: DataModel = DataModel(
      "abcd",
      "test name",
      "test description",
      100
    )

    when(mockDataRepository.update(any()))
      .thenReturn(Future.failed(GenericDriverException("Error")))

    "return an error" in {

      val result = TestApplicationController.update(dataModel._id)(FakeRequest().withBody(jsonBody))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      await(bodyOf(result)) shouldBe Json.obj("message" -> "Error updating item in Mongo database").toString()
    }
  }


  "the mongo data deletion failed" should {

    "return an error" in {

      when(mockDataRepository.delete(any()))
        .thenReturn(Future.failed(GenericDriverException("Error")))

      val result = TestApplicationController.delete(any())(FakeRequest())

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      await(bodyOf(result)) shouldBe Json.obj("message" -> "Error deleting item from Mongo database").toString()
    }
  }

}
