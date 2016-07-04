package libs
import libs.solr.scala.QueryBuilderBase
import libs.solr.scala.QueryBuilder
import libs.solr.scala.SolrClient
import libs.solr.scala.MapQueryResult
import org.apache.solr.client.solrj.SolrQuery
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsString
import play.api._
import play.api.mvc._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue
import play.api.libs.json.JsBoolean
import play.api.db.DB
import play.api.Play.current

import util.control.Breaks._

/**
 * Class Search performs many functions about information retrieval .
  *
 * @version 0.1
 */
object SearchLib {

  /**
   * is a main function that handling search process.
    *
    * @param query
   * @param request
   * @version 0.1
   */
  def get(query: String,request: Request[AnyContent]): JsObject = {
    // Construct the solr query and handling the parameters
    val queryBuilder = this.buildQuery(query, request)
    var resultsInfo = Json.obj(
      "num_of_results" -> 0,
      "results" -> List[JsString]())

    try {
      // Get Results from Solr.
      val results = queryBuilder.getResultAsMap()

      // prepare results
      resultsInfo = this.prepareResults(results, request)
    } catch {
      case e: Exception =>
        println("exception caught: " + e);
    }

    resultsInfo
  }

  /**
   *
   * constructQuery : constructs the query and handling the user params
    *
    * @param  query
   * @param  request
   */

  def buildQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    // Checking URL Parameters
    val settings = "{!df=" + request.getQueryString("searchField").getOrElse("product") + "}"
    val highQualOnly = request.getQueryString("highQualOnly").getOrElse(false)
    val minRPKM = request.getQueryString("minRPKM").getOrElse("0")

    val page = Integer.parseInt(request.getQueryString("page").getOrElse(1).toString)
    val mm = request.getQueryString("mm").getOrElse("<NULL>").replace("\"", "")
    val resultsPerPage = Integer.parseInt(request.getQueryString("noOfResults").getOrElse(200).toString)
    val sort = request.getQueryString("sort").getOrElse("<NULL>")

    val client = new SolrClient("http://localhost:8983/solr/ORFDocs")

    var offset: Int = 0
    if (request.getQueryString("offset").isDefined) {
      offset = Integer.parseInt(request.getQueryString("offset").getOrElse(0).toString)
    } else {
      offset = (page - 1) * resultsPerPage
    }


    var queryBuilder = client.query(settings + query)
      .start(offset)
      .rows(resultsPerPage)
      .addFilterQuery("rpkm:[" + minRPKM + " TO *]")

    if(highQualOnly.equals("true")){
      val fq = "KEGGID:[* TO *] OR COGID:[* TO *]"
      queryBuilder = queryBuilder.addFilterQuery(fq)
    }

    // When you assign mm (Minimum 'Should' Match), we remove q.op
    // becuase we can't set two params to the same function
    // q.op=AND == mm=100% | q.op=OR == mm=0%
    if (!mm.equals("<NULL>")) {
      queryBuilder = queryBuilder.setParameter("mm", "100%")
    } else {
      //queryBuilder = queryBuilder.setParameter("q.op", queryOperator)
    }

    if (query.equals("*:*")) {
      queryBuilder = queryBuilder.setParameter("q", "product:protein")
    }

    queryBuilder
  }

  /**
   * Prepare the results and build mapping between Solr and Application Level
   */
  def prepareResults(results: MapQueryResult, request: Request[AnyContent]): JsObject = {
    var resultsInfo = List[JsObject]()
    results.documents.foreach {
      doc =>
        var resultJsonDoc = Json.obj(
          "ORFID" -> doc("ORFID").toString,
          "ORF_len" -> doc("ORF_len").toString,
          "start" -> doc("start").toString,
          "end" -> doc("end").toString,
          "strand_sense" -> doc("strand_sense").toString,
          "taxonomy" -> doc("taxonomy").toString,
          "product" -> doc("product").toString,
          "rpkm" -> doc("rpkm").toString,
          "COGID" -> doc.getOrElse("COGID", "N/A").toString,
          "KEGGID" -> doc.getOrElse("KEGGID", "N/A").toString,
          "extended_desc" -> doc.getOrElse("extended_desc", "N/A").toString
        )
        resultsInfo::=resultJsonDoc
    }

    val resultsJson = Json.obj(
      "noOfResults" -> results.numFound,
      "results" -> resultsInfo)
    resultsJson
  }


  

}