package controllers

import libs.SearchLib
import play.api._
import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Search application is ready."))
  }
  
  def get(query: String): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.get(query,request)
    Ok(results)
  }

}