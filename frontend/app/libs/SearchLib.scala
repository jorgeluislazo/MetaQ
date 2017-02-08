package libs
import libs.solr.scala.QueryBuilder
import libs.solr.scala.SolrClient
import libs.solr.scala.MapQueryResults
import libs.solr.scala.MapClusterQueryResult
import play.api.libs.json._
import play.api.mvc._
/**
  * Class Search performs many functions about information retrieval .
  */
object SearchLib {

  val baseURL = "http://localhost:8983/solr/"
  /**
    * is a main function that handling search /select process.
    */
  def select(query: String, request: Request[AnyContent], selectType: String): JsObject = {
    // Construct the solr query depending on the type and handling the parameters
    println(s"searchLib select - queryString: $query, requestURI: $request, selectType: $selectType)")
    val queryBuilder = if (selectType == "gene"){
      buildGeneSelectQuery(query, request)}
    else {
      buildPwaySelectQuery(query, request)
    }

    var resultsInfo = Json.obj(
      "EmptyResponse" -> true)

    try {
      // Get Results from Solr.
      val results = queryBuilder.getResultAsMap(selectType)
      println("Solr select/ results: " + results)
      // prepare results
      resultsInfo = if (selectType == "gene")
        this.prepareGeneSearchResults(results, request)
      else
        this.preparePwaySearchResults(results, request)

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

  // main function that handles /clustering searches
  def cluster(query: String, request: Request[AnyContent]): JsObject = {
    println(s"searchLib cluster - queryString: $query, requestURI: $request)")
    val queryBuilder = this.buildGeneClusterQuery(query, request)
    var resultsInfo = Json.obj(
      "num_of_clusters" -> 0,
      "clusters" -> List[JsString]())

    try {
      // Get Results from Solr.
      val results = queryBuilder.getClustersAsMap()
      println("Solr cluster/ results: " + results)

      // prepare results
      resultsInfo = this.prepareGeneClusterResults(results, request)
    } catch {
      case e: Exception =>
        println("exception caught from clustering: " + e);
    }
    resultsInfo
  }

  /**
    * constructQuery : constructs the gene query and handling the user params
    */

  def buildGeneSelectQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    // Checking URL Parameters
    // if its a module linking search, do search for IDs (list has been passed as query)

    val queryName = request.getQueryString("query").getOrElse(query)
    val searchSettings = if(request.getQueryString("searchField").getOrElse("product") == "pway")
      "{!terms f=ORFID}" else "{!df=" + request.getQueryString("searchField").getOrElse("product") + "}"
    val highQualOnly = request.getQueryString("highQualOnly").getOrElse(false)
    val minRPKM = request.getQueryString("minRPKM").getOrElse("0")
    val isTaxonomyBuilder = if (request.getQueryString("treeBuilder").isEmpty) false else true //Taxonomy builder (for tree)
    val filterSearchQuery = request.getQueryString("facetFilter") //facet COG/KEGG filter upon click
        .getOrElse(request.getQueryString("taxonomyFilter").getOrElse("")) //taxonomy filter

    val page = Integer.parseInt(request.getQueryString("page").getOrElse(1).toString)
    val resultsPerPage = Integer.parseInt(request.getQueryString("rows").getOrElse(100).toString)

    // http://localhost:8983/solr/ORFDocs
    // http://ec2-52-53-226-52.us-west-1.compute.amazonaws.com:8983/solr/ORFDocs
    val client = new SolrClient(baseURL + "ORFDocs")

//    println(request + " " + isFilterSearch)
    //set the offset
    var offset: Int = 0
    if (request.getQueryString("offset").isDefined) {
      offset = Integer.parseInt(request.getQueryString("offset").getOrElse(0).toString)
    } else {
      offset = (page - 1) * resultsPerPage
    }

    println(resultsPerPage)

    var queryBuilder = client.query(searchSettings + queryName)
      .start(offset)
      .rows(resultsPerPage)
      .addFilterQuery("rpkm:[" + minRPKM + " TO *]")

    if (filterSearchQuery != "") {
      //clicked on a facet, or taxonomy node, add the filter query
      queryBuilder = queryBuilder.addFilterQuery(filterSearchQuery)
    } else {
      if(isTaxonomyBuilder){
        //is it a taxonomy tree builder query?
        queryBuilder = queryBuilder.facetFields("taxonomyID")
          .setParameter("facet.limit", "20")
          .setParameter("facet.mincount", "1") //todo: set to log(numCountResults)*2
      }else{
        //normal query with COGID KEGGID facets returned
        queryBuilder = queryBuilder.facetFields("COGID")
          .facetFields("KEGGID")
          .setParameter("facet.limit", "25")
          .setParameter("facet.mincount", "1")
      }
    }

    if (highQualOnly.equals("true")) {
      val fq = "KEGGID:[* TO *] OR COGID:[* TO *]"
      queryBuilder = queryBuilder.addFilterQuery(fq)
    }
    queryBuilder
  }

  def buildPwaySelectQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    val queryName = request.getQueryString("query").getOrElse(query)
    val searchSettings = "{!df=" + request.getQueryString("searchField").getOrElse("pway_name") + "}"
    val page = Integer.parseInt(request.getQueryString("page").getOrElse(1).toString)
    val resultsPerPage = Integer.parseInt(request.getQueryString("noOfResults").getOrElse(100).toString)

    // http://localhost:8983/solr/PwayDocs
    // http://ec2-52-53-226-52.us-west-1.compute.amazonaws.com:8983/solr/PwayDocs
    val client = new SolrClient(baseURL + "PwayDocs")

    var offset: Int = 0
    if (request.getQueryString("offset").isDefined) {
      offset = Integer.parseInt(request.getQueryString("offset").getOrElse(0).toString)
    } else {
      offset = (page - 1) * resultsPerPage
    }

    val queryBuilder = client.query(searchSettings + queryName)
      .start(offset)
      .rows(resultsPerPage)

    queryBuilder
  }

  /**
    * Prepare the results and build mapping between Solr and Application Level
    */
  def prepareGeneSearchResults(results: MapQueryResults, request: Request[AnyContent]): JsObject = {
    var resultsInfo = List[JsObject]()

    val isFilterSearch = if (request.getQueryString("facetFilter").isDefined) "facetFilter" //tell client is facet filter
      else if (request.getQueryString("taxonomyFilter").isDefined) "taxonomyFilter" //tell client is taxonomy  filter
      else if (request.getQueryString("clusterFilter").isDefined) "clusterFilter" // tell client is cluster filter
      else "empty" //facet COG/KEGG filter upon click
    val isTaxonomyBuilder = if (request.getQueryString("treeBuilder").isEmpty) false else true //Taxonomy builder (for tree)

    if(isTaxonomyBuilder){
      //only ask for ORFIDs
      results.documents.foreach {
        doc =>
          var resultJsonDoc = Json.obj(
            "ORFID" -> doc("ORFID").toString
          )
          resultsInfo ::= resultJsonDoc
      }

      val resultsJson = Json.obj(
        "noOfResults" -> results.numFound,
        "start" -> results.start,
        "results" -> resultsInfo,
        "facetFields" -> results.facetFields,
        "isFilterSearch" -> isFilterSearch)

      resultsJson

    }else{
      //normal search, include all information
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
          resultsInfo ::= resultJsonDoc
      }

      resultsInfo = resultsInfo.reverse

//      if (isFilterSearch == "facetFilter") {
//        //sort the facet fields and add them back
//        val sortedKEGG = ListMap(results.facetFields("KEGGID").toSeq.sortWith(_._2 > _._2): _*)
//        val sortedCOG = ListMap(results.facetFields("COGID").toSeq.sortWith(_._2 > _._2): _*)
//        val sortedFacets = Map("COGID" -> sortedCOG, "KEGGID" -> sortedKEGG)
//      }

      val resultsJson = Json.obj(
        "noOfResults" -> results.numFound,
        "start" -> results.start,
        "results" -> resultsInfo,
        "facetFields" -> results.facetFields,
        "isFilterSearch" -> isFilterSearch)

      resultsJson
    }
  }

  def preparePwaySearchResults(results: MapQueryResults, request: Request[AnyContent]): JsObject = {
    var resultsInfo = List[JsObject]()

    results.documents.foreach {
      doc =>
        var resultJsonDoc = Json.obj(
          "pway_id" -> doc("pway_id").toString,
          "pway_name" -> doc("pway_name").toString,
          "rxn_total" -> doc("rxn_total").toString,
          "sample_runs" -> covertStringListToJsValue(doc("sample_runs").toString),
          "orfs" -> covertStringListToJsValue(doc("orfs").toString)
        )
        resultsInfo ::= resultJsonDoc
    }
    resultsInfo = resultsInfo.reverse

    val resultsJson = Json.obj(
      "noOfResults" -> results.numFound,
      "start" -> results.start,
      "results" -> resultsInfo)
    resultsJson
  }


  def buildGeneClusterQuery(query: String, request: Request[AnyContent]): QueryBuilder = {
    val searchSettings = if(request.getQueryString("searchField").getOrElse("product") == "pway")
      "{!terms f=ORFID}" else "{!df=" + request.getQueryString("searchField").getOrElse("product") + "}"
    val highQualOnly = request.getQueryString("highQualOnly").getOrElse(false)
    val minRPKM = request.getQueryString("minRPKM").getOrElse("0")

    // http://localhost:8983/solr/ORFDocs
    // http://ec2-52-53-226-52.us-west-1.compute.amazonaws.com:8983/solr/ORFDocs
    val client = new SolrClient(baseURL + "ORFDocs")

    var queryBuilder = client.query(searchSettings + query)
      .addFilterQuery("rpkm:[" + minRPKM + " TO *]")
      .setParameter("qt", "/clustering")

    if (highQualOnly.equals("true")) {
      val fq = "KEGGID:[* TO *] OR COGID:[* TO *]"
      queryBuilder = queryBuilder.addFilterQuery(fq)
    }
    queryBuilder
  }

  def prepareGeneClusterResults(results: MapClusterQueryResult, request: Request[AnyContent]): JsObject = {
    val resultsJson = Json.obj(
      "noOfResults" -> results.numOfClusters,
      "clusters" -> results.clusters)
    resultsJson
  }

  def covertStringListToJsValue(result: String): JsValue = {
    val resultArray = result.stripPrefix("[").stripSuffix("]").trim.split(",")
    Json.toJson(resultArray)
  }

}