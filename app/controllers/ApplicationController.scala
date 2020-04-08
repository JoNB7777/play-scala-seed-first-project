package controllers

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index(): Action[AnyContent] = TODO

  def create(): Action[AnyContent] = TODO

  def read(id: String): Action[AnyContent] = TODO

  def update(id: String): Action[AnyContent] = TODO

  def delete(id: String): Action[AnyContent] = TODO
}
