package controllers

import javax.inject.Inject
import models.DataModel
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import reactivemongo.core.errors.{DatabaseException, DriverException, ReactiveMongoException}
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents, val dataRepository: DataRepository, implicit val ec: ExecutionContext) extends BaseController {

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.find().map(items => Ok(Json.toJson(items)))
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.create(dataModel).map(_ => Created) recover {
          case _: ReactiveMongoException => InternalServerError(Json.obj(
            "message" -> "Error adding item to Mongo"
          ))
        }
      case JsError(_) => Future(BadRequest)
    }
  }

//  def read(id:String) = Action.async { implicit request =>
//    val idConvertedToJson = Json.toJson(id)
//    dataRepository.find("_id"->idConvertedToJson).map(items => Ok(Json.toJson(items))) recover {
//      case _: ReactiveMongoException => InternalServerError(Json.obj(
//        "message" -> "Error adding item to Mongo"
//      ))
//    }
//  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map(item => Ok(Json.toJson(item))) recover {
      case _: DriverException => InternalServerError(Json.obj(
        "message" -> "Error reading from database"
      ))
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(dataModel).map(_ => Accepted(Json.toJson(dataModel))) recover {
          case _: ReactiveMongoException => InternalServerError(Json.obj(
            "message" -> "Error updating item in Mongo database"
          ))
        }
      case JsError(_) => Future(BadRequest)
    }
  }

  def delete(id:String) = Action.async {  implicit request =>
    dataRepository.delete(id).map(item => Status(ACCEPTED)) recover {
      case _: ReactiveMongoException => InternalServerError(Json.obj(
        "message" -> "Error deleting item from Mongo database"
      ))
    }
  }
}
