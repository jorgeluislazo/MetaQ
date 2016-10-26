package controllers

import libs.SearchLib
import play.api._
import play.api.mvc._

class Application extends Controller {

  def homePage = Action {
    Ok(views.html.home())
  }

  def geneExplorer(query: String) = Action {
    Ok(views.html.geneExplorer(query))
  }

  def pwayExplorer(query: String) = Action {
    Ok(views.html.pwayExplorer(query))
  }
  
  def simpleSearch(query: String): Action[AnyContent] = Action { implicit request =>
    println(query)
    val results = SearchLib.get(query,request)
    Ok(results)
  }

  def clusterSearch(query: String): Action[AnyContent] = Action{ implicit request =>
    val results = SearchLib.cluster(query,request)
    Ok(results)
  }

  def test(): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.get( "product:protein" ,request)
    Ok(results)
  }

}