package controllers

import libs.SearchLib
import java.io._
import javax.inject.Inject

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import sys.process._

class Application @Inject() (ws: WSClient) extends Controller {

  def homePage = Action {
    Ok(views.html.home())
  }

  def geneExplorer(query: String) = Action {
    println("query: " + query)
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
      if(request.getQueryString("treeBuilder").nonEmpty){
        //get taxonomy IDs as facets and write taxonomy tree lineages
        val solrSresults = SearchLib.select(query,request, "gene")
        val taxonomyMap = (solrSresults \ "facetFields" \ "taxonomyID").get.as[Map[String,Int]] //taxID -> value
        val fileIDs = new java.io.File("/tmp/exampleTaxIDs")
        val buffWriter = new BufferedWriter(new FileWriter(fileIDs))

        for (id <- taxonomyMap.keySet){
          buffWriter.write("^"+ id + "\n")
        }
        buffWriter.close()
        println("H1 - TaxIDs written, size=" + taxonomyMap.keySet.size)
        val runScript = "bash /tmp/script".! //todo: add params, concurrency
        //final JS Array result to send back to client
        var jsFinalResult = JsArray()
        //we will discard lineages seen before
        var seenLineageSet : Set[String] = Set()
        //for each lineage we find in our list
        println("H2 - Lineages obtained")
        for(line <- Source.fromFile("tmp/exampleTaxLineages.txt").getLines()){
          var jsBranchResult = JsArray()
          //extract as a tuple (lineageString, count)
          val tuple = line.split("\t")
          val lineageString = tuple(0)
          //add the unseen lineage to the set
          if(!seenLineageSet.contains(lineageString)){
            seenLineageSet = seenLineageSet + lineageString
            //add the species
            val jsEntry : JsValue = Json.obj(
              "id" -> tuple(0).replace("(miscellaneous)", ""),
              "taxid" -> tuple(1),
              "count" -> taxonomyMap.get(tuple(1)).get
            )
            jsBranchResult = jsBranchResult.+:(jsEntry)

            //now we prepend all the parent branches lineages we havent seen
            val lineageBranches = lineageString.split("\\.")
            var i = lineageBranches.length
            var break = 0
            //work upwards from leaves to root
            while(i > 0){
              val branchSet = lineageBranches.take(i)
              val branchString = branchSet.mkString(".")
                //add that branch if we havent seen it before, otherwise break
              if(!seenLineageSet.contains(branchString)) {
                seenLineageSet = seenLineageSet + branchString
                val jsEntry : JsValue = Json.obj(
                  "id" -> branchString,
                  "taxid" -> 0,
                  "count" -> -1
                )
                jsBranchResult = jsBranchResult.+:(jsEntry) //prepend
              }else{
                break = 1
              }
              i -= 1
            }
            jsFinalResult = jsFinalResult.++(jsBranchResult)
//            println(tuple(0) + ". Count: " + taxonomyMap.get(tuple(1)).get + ". taxID: " + tuple(1))
          }
        }
        //this might be required for D3.js
        val columns : JsValue = Json.arr("id")
//        jsFinalResult = jsFinalResult.:+(columns)
//        seenLineageSet.foreach{println}
        Ok(jsFinalResult)
      }else{
        //Normal search: get the ORFs associated with this search
        val results = SearchLib.select(query,request, "gene")
        Ok(results)
      }
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
    val query = if(request.getQueryString("name").isDefined){
      request.getQueryString("name").get + "_filtered"
    }else{
      request.getQueryString("query").get
    }
//    val query = request.getQueryString("query").get
    val file = new java.io.File("/tmp/request")
    try{
      val writer = new PrintWriter(file) //prepare the file + writer

      val data = SearchLib.select(query,request, "gene")
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
      fileName = _ => "MetaQ_" + query + "_search.txt",
      inline = false
    )
  }

  def dendogram: Action[AnyContent] = Action {
    Ok(views.html.dendogram())
  }

  def test(): Action[AnyContent] = Action { implicit request =>
    val results = SearchLib.select( "product:protein" ,request, "gene")
    Ok(results)
  }

  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]

}