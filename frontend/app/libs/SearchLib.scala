package libs
import libs.solr.scala.QueryBuilder
import libs.solr.scala.SolrClient
import libs.solr.scala.MapQueryResult
import libs.solr.scala.MapClusterQueryResult
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsString
import play.api._
import play.api.mvc._
import play.api.libs.json.JsBoolean

import scala.collection.immutable.ListMap

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
    val queryBuilder = this.buildSelectQuery(query, request)
    var resultsInfo = Json.obj(
      "EmptyResponse" -> true)

    try {
      // Get Results from Solr.
      val results = queryBuilder.getResultAsMap()
      // prepare results
      resultsInfo = this.prepareSearchResults(results, request)
    } catch {
      case e: Exception =>
        println("query: " + query)
        println("request URL:" + request)
        println("exception caught: " + e)
        resultsInfo = Json.obj(
          "noOfResults" -> 0,
          "error" -> e.toString)
    }

    resultsInfo
  }

  def cluster(query: String, request: Request[AnyContent]) : JsObject = {
    val queryBuilder = this.buildClusterQuery(query, request)
    var resultsInfo = Json.obj(
      "num_of_clusters" -> 0,
      "clusters" -> List[JsString]())

    try {
      // Get Results from Solr.
      val results = queryBuilder.getClustersAsMap()

      // prepare results
      resultsInfo = this.prepareClusterResults(results, request)
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

  def buildSelectQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    // Checking URL Parameters
    val searchSettings = "{!df=" + request.getQueryString("searchField").getOrElse("product") + "}"
    val highQualOnly = request.getQueryString("highQualOnly").getOrElse(false)
    val minRPKM = request.getQueryString("minRPKM").getOrElse("0")
    val isFilterSearch = if(request.getQueryString("facetFilter").isEmpty) false else true
    val filterSearchQuery = request.getQueryString("facetFilter").getOrElse("")

    val page = Integer.parseInt(request.getQueryString("page").getOrElse(1).toString)
    val resultsPerPage = Integer.parseInt(request.getQueryString("noOfResults").getOrElse(100).toString)

    //"http://localhost:8983/solr/ORFDocs"
    //"http://ec2-54-153-99-252.us-west-1.compute.amazonaws.com:8983/solr/ORFDocs"
    val client = new SolrClient("http://localhost:8983/solr/ORFDocs")

    var offset: Int = 0
    if (request.getQueryString("offset").isDefined) {
      offset = Integer.parseInt(request.getQueryString("offset").getOrElse(0).toString)
    } else {
      offset = (page - 1) * resultsPerPage
    }

    var queryBuilder = client.query(searchSettings + query)
      .start(offset)
      .rows(resultsPerPage)
      .addFilterQuery("rpkm:[" + minRPKM + " TO *]")

    if(isFilterSearch){
      queryBuilder = queryBuilder.addFilterQuery(filterSearchQuery)
    } else {
      queryBuilder = queryBuilder.facetFields("COGID")
        .facetFields("KEGGID")
        .setParameter("facet.limit","25")
        .setParameter("facet.mincount", "1")
    }

    if(highQualOnly.equals("true")){
      val fq = "KEGGID:[* TO *] OR COGID:[* TO *]"
      queryBuilder = queryBuilder.addFilterQuery(fq)
    }

    queryBuilder
  }

  def buildClusterQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    val searchSettings = "{!df=" + request.getQueryString("searchField").getOrElse("product") + "}"
    val highQualOnly = request.getQueryString("highQualOnly").getOrElse(false)
    val minRPKM = request.getQueryString("minRPKM").getOrElse("0")

    val client = new SolrClient("http://ec2-54-153-99-252.us-west-1.compute.amazonaws.com:8983/solr/ORFDocs")

    var queryBuilder = client.query(searchSettings + query)
      .addFilterQuery("rpkm:[" + minRPKM + " TO *]")
      .setParameter("qt", "/clustering")

    if(highQualOnly.equals("true")){
      val fq = "KEGGID:[* TO *] OR COGID:[* TO *]"
      queryBuilder = queryBuilder.addFilterQuery(fq)
    }

    queryBuilder
  }

  /**
    * Prepare the results and build mapping between Solr and Application Level
    */
  def prepareSearchResults(results: MapQueryResult, request: Request[AnyContent]): JsObject = {
    var resultsInfo = List[JsObject]()

    val isFilterSearch = if(request.getQueryString("facetFilter").isEmpty) false else true
    val isClusterFilter = if(request.getQueryString("clusterFilter").isEmpty) false else true

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

    resultsInfo = resultsInfo.reverse

    if(!isFilterSearch){
      //sort the facet fields and add them back
      val sortedKEGG = ListMap(results.facetFields("KEGGID").toSeq.sortWith(_._2 > _._2):_*)
      val sortedCOG = ListMap(results.facetFields("COGID").toSeq.sortWith(_._2 > _._2):_*)
      val sortedFacets = Map( "COGID" -> sortedCOG, "KEGGID" -> sortedKEGG)
    }

    val resultsJson = Json.obj(
      "noOfResults" -> results.numFound,
      "start" -> results.start,
      "results" -> resultsInfo,
      "facetFields" -> results.facetFields,
      "isFilterSearch" -> isFilterSearch,
      "isClusterFilter" -> isClusterFilter)
    resultsJson
  }

  def prepareClusterResults(results: MapClusterQueryResult, request: Request[AnyContent]): JsObject = {
    val resultsJson = Json.obj(
      "noOfResults" -> results.numOfClusters,
      "clusters" -> results.clusters)
    resultsJson
  }




}