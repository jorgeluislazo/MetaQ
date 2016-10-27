package controllers

import libs.SearchLib

import scala.io.Source
import java.io._

import play.api.libs.json.{JsObject, JsValue}
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
    val results = SearchLib.get(query,request)
    Ok(results)
  }

  def clusterSearch(query: String): Action[AnyContent] = Action{ implicit request =>
    val results = SearchLib.cluster(query,request)
    Ok(results)
  }

  def exportData(query: String): Action[AnyContent] = Action{ implicit request =>
    println(request)
    val file = new java.io.File("test.txt")
    val writer = new PrintWriter(file) //prepare the file + writer
    val data = SearchLib.get(query,request)
    val results = (data\"results").get.as[List[JsObject]] //get results
    writer.write("ORFID" + "\tstart" + "\tend" + "\tstrand_sense" + "\ttaxonomy" + "\trpkm" + "\tCOGID" + "\tKEGGID" + "\textended_desc" + "\n")
    for(result <- results){ //can loop through results, but not for each field, would need a matcher
      writer.write((result\"ORFID").get.toString().replace("\"", "") + "\t")
      writer.write((result\"ORF_len").get.toString().replace("\"", "") + "\t")
      writer.write((result\"start").get.toString().replace("\"", "") + "\t")
      writer.write((result\"end").get.toString().replace("\"", "") + "\t")
      writer.write((result\"strand_sense").get.toString().replace("\"", "") + "\t")
      writer.write((result\"taxonomy").get.toString().replace("\"", "") + "\t")
      writer.write((result\"product").get.toString().replace("\"", "") + "\t")
      writer.write((result\"rpkm").get.toString().replace("\"", "") + "\t")
      writer.write((result\"COGID").get.toString().replace("\"", "") + "\t")
      writer.write((result\"KEGGID").get.toString().replace("\"", "") + "\t")
      writer.write((result\"extended_desc").get.toString().replace("\"", "") + "\t") //TODO: strip off the "[ ]" why was it a list again...?
      writer.write("\n")
    }
    writer.close()

    Ok.sendFile( //send the file back to the user
      content = file,
      fileName = _ => "MetaQ_" + query + "_search.txt",
      inline = false
    )
  }

  def test(): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.get( "product:protein" ,request)
    Ok(results)
  }

}