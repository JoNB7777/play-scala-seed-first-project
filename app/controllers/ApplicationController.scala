package controllers

import javax.inject.Inject
import models.DataModel
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import reactivemongo.core.errors.DatabaseException
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents, val dataRepository: DataRepository, implicit val ec: ExecutionContext) extends BaseController {

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.find().map(items => Ok(Json.toJson(items)))
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.create(dataModel).map(_ => Created)
      case JsError(_) => Future(BadRequest)
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map(_ => Ok)

  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(dataModel).map(_ => Accepted(Json.toJson(dataModel)))
      case JsError(_) => Future(BadRequest)
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.delete(id).map(_ => Status(ACCEPTED))
  }
}
