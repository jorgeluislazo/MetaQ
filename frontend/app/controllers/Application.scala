package controllers

import libs.SearchLib
import java.io._
import javax.inject.Inject

import play.api.libs.json.JsObject
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class Application @Inject() (ws: WSClient) extends Controller {

  def homePage = Action {
    Ok(views.html.home())
  }

  def geneExplorer(query: String) = Action {
    Ok(views.html.geneExplorer(query))
  }

  def pwayExplorer(query: String) = Action {
    Ok(views.html.pwayExplorer(query))
  }

  def geneSearch(query: String): Action[AnyContent] = Action { implicit request =>
    if (request.getQueryString("searchField").get == "pway"){
      // first find list of orfs associated with that pathway
      // through a 1 time blocking search to pway explorer
      val orfs = getOrfsAssociatedWithPway(query)
      //now call the data for those orfs, return them to client
      val results = SearchLib.select(orfs,request, "gene")
      Ok(results)
    }else{
      //get the ORFs associated with this search
      val results = SearchLib.select(query,request, "gene")
      Ok(results)
    }
  }

  def pwaySearch(query: String): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.select(query, request, "pway")
    Ok(results)
  }

  def clusterGeneSearch(query: String): Action[AnyContent] = Action{ implicit request =>
    if (request.getQueryString("searchField").get == "pway") {
      // first find list of orfs associated with that pathway
      // through a 1 time blocking search to pway explorer
      val orfs = getOrfsAssociatedWithPway(query)
      //now call the data for those orfs, return them to client
      val results = SearchLib.cluster(orfs,request)
      Ok(results)
    }else{
      val results = SearchLib.cluster(query,request)
      Ok(results)
    }
  }

  def getOrfsAssociatedWithPway(pwayID: String) : String = {
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val url = "http://localhost:9000/searchPway?query=" + pwayID + "&searchField=pway_id&page=1"
    val result = Await.result(ws.url(url).get(), 3 second) //wait up to 3 seconds for this response to return
    val orfs = ((result.json \ "results") (0) \ "orfs").as[Array[String]].mkString(",").replaceAll("\\s", "") //extract orfs
    orfs
  }

  def exportData(query: String): Action[AnyContent] = Action{ implicit request =>
    println(request)
    val queryValue = query.substring(0,query.indexOf("&"))
    val file = new java.io.File("/tmp/request")
    try{
      val writer = new PrintWriter(file) //prepare the file + writer

      val data = SearchLib.select(queryValue,request, "gene")
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
    }catch{
      case fnfe: FileNotFoundException => println("FileNotFoundException (No such File or directory)")
    }

    Ok.sendFile( //send the file back to the user
      content = file,
      fileName = _ => "MetaQ_" + queryValue + "_search.txt",
      inline = false
    )
  }

  def test(): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.select( "product:protein" ,request, "gene")
    Ok(results)
  }

}