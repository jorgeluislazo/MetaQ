package controllers

import libs.SearchLib
import java.io._
import java.net.URL
import javax.inject.Inject

import models.UserForm
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import org.apache.commons.codec.binary.Base64

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.Source
import sys.process._

class Application @Inject() (ws: WSClient) extends Controller {

  val userList = Source.fromFile("taxonomyData/users").getLines().toArray
  var username : String = ""
  var password : String = ""

  def homePage = Action { implicit request =>
    val username = request.session.get("username").orNull
    val password = request.session.get("password").orNull

    if(password == null){
      Ok(views.html.home()).withNewSession
    }else{
      Ok(views.html.home()).withSession("username" -> username, "password" -> password)
    }
  }

  def geneExplorer(query: String) = Action { implicit request =>
//    println("query: " + query)
    Ok(views.html.geneExplorer(query))
  }

  def pwayExplorer(query: String) = Action { implicit request =>
    Ok(views.html.pwayExplorer(query))
  }

  def geneSearch(query: String): Action[AnyContent] = Action { implicit request =>
    println(s"searchLib cluster - requestURI: $request)")

    var queryString = ""

    val results = if(request.getQueryString("searchField").get == "pway") {
      // first find list of orfs associated with that pathway
      // through a 1 time blocking search to pway explorer
      queryString = getOrfsAssociatedWithPway(query)
      //now call the data for those orfs, return them to client
      //      println("orfs: " + orfs)
      SearchLib.select(queryString, request, "gene")
    }else{
      //Normal search: get the ORFs associated with this search
      queryString = query
      SearchLib.select(query,request, "gene")
    }

    if (request.getQueryString("treeBuilder").getOrElse("f") == "t"){
      //get taxonomy IDs as facets and write taxonomy tree lineages
      val solrResults = SearchLib.select(queryString,request, "gene")
      val taxonomyMap = (solrResults \ "facetFields" \ "taxonomyID").get.as[Map[String,Int]] //taxID -> value
      val fileIDs = new java.io.File("taxonomyData/exampleTaxIDs")
      val buffWriter = new BufferedWriter(new FileWriter(fileIDs))

      for (id <- taxonomyMap.keySet){
        buffWriter.write("^"+ id + "\n")
      }
      buffWriter.close()
      println("H1 - TaxIDs written, size=" + taxonomyMap.keySet.size)

      val runScript = "bash taxonomyData/script".! //todo: add params, concurrency
      //final JS Array result to send back to client
      var jsFinalResult = JsArray()
      //we will discard lineages seen before
      var seenLineageSet : Set[String] = Set()
      //for each lineage we find in our list
      println("H2 - Lineages obtained")

      for(line <- Source.fromFile("/tmp/exampleTaxLineages.txt").getLines()){
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
       //normal saerch, return results
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
    val result = Await.result(ws.url(url).get(), 4 second) //wait up to 3 seconds for this response to return
    val orfs = ((result.json \ "results") (0) \ "orfs").as[Array[String]].mkString(",").replaceAll("\\s", "") //extract orfs
    orfs
  }

  def exportData(query: String): Action[AnyContent] = Action{ implicit request =>
//    println(request)
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

  def login = Action { implicit request =>
    val data : (String, String) = Tuple2("", "")
    Ok(views.html.login.input(credentials.fill(data), "Please input credentials"))
  }

  def logout = Action { implicit request =>
    SearchLib.userName = ""
    Redirect(routes.Application.homePage()).withNewSession
  }

  def authenticate(credentials :Tuple2[String, String]) = Action { implicit request =>
    Ok(views.html.home()).withSession( "username" -> credentials._1, "id" -> credentials._2)
  }

  def parseCredentials = Action { implicit request =>
       credentials.bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("inside error")
        BadRequest(views.html.login.input(formWithErrors, "Error: please review below"))
      },
      data => {
          if (isUserAuth(data)) {
            this.username = data._1
            this.password = data._2
            SearchLib.userName = this.username
            Redirect(routes.Application.homePage()).withSession("username" -> username, "password" -> password)
          } else {
            Ok(views.html.login.input(credentials.fill(data), "Error: wrong username/password."))
          }
      }
    )
  }

  def isUserAuth(credentials : (String, String)) : Boolean = {
    var result : Boolean = false

    val connection = new URL("http://137.82.19.141:8443/solr/admin/authentication").openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(credentials._1, credentials._2))
    try{
      val resp = Source.fromInputStream(connection.getInputStream)
      result = true
    }catch{
      //this means a 401 was thrown, user unauthenticated
      case ioe: IOException => result = false
    }
    result
  }

  //api to check user exists
  def checkUser(user : String): Action[AnyContent] = Action { implicit request =>
    println(user)
      if(userList.contains(user)){
        Ok(user + " found")
      }else{
        Ok("user not found")
      }
  }

  def newUser = Action {
    val dummyClient = UserForm("","","","","","")
    Ok(views.html.login.userCreate(userForm.fill(dummyClient), "Please fill the required fields below"))
  }

  def saveUser = Action { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.login.userCreate(formWithErrors, "Error: see below"))
      },
      user => {
        if(userList.contains(user.username)){
          BadRequest(views.html.login.userCreate(userForm.fill(user), "Error: username already exists"))
        }else{
          if(save(user)){
            println("new user : " + user.username)
            this.username = user.username
            this.password = user.password
            SearchLib.userName = this.username
            Redirect(routes.Application.homePage()).withSession("username" -> username, "password" -> password)
          }else{
            BadRequest(views.html.login.userCreate(userForm.fill(user), "Error: could not save that username"))
          }
        }
      }
    )
  }

  def save(form: UserForm): Boolean ={
    var result : Boolean = false
    val connection = new URL("http://137.82.19.141:8443/solr/admin/authentication").openConnection()

    connection.setDoOutput(true) //set to POST
    connection.setRequestProperty("Content-Type", "application/json") //send data as json
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader("Jorge-Admin", "Jakiro29!")) //need admin to accept

    val message = "{\"set-user\": {\"" + form.username + "\" : \"" + form.password + "\"}}"

    try{
      //save the user using solr API call, and then save the user data in the webapp
      val output = connection.getOutputStream
      output.write(message.getBytes())

      val resp = connection.getInputStream

      val users = new java.io.File("taxonomyData/users")
      var buffWriter = new BufferedWriter(new FileWriter(users, true))

      buffWriter.write(form.username + "\n")
      userList.+(form.username)
      buffWriter.close()

      val saveList = new java.io.File("taxonomyData/saveList")
      buffWriter = new BufferedWriter(new FileWriter(saveList, true))
      buffWriter.write(form.username + "\t" + form.fullName + "\t"
        + form.institution + "\t" + form.city + "\t" + form.postalCode + "\n")
      buffWriter.close()
      result = true

    }catch{
      case ioe: IOException => result = false
    }
    result
  }

  object HttpBasicAuth {
    val BASIC = "Basic"
    val AUTHORIZATION = "Authorization"

    def encodeCredentials(username: String, password: String): String = {
      new String(Base64.encodeBase64String((username + ":" + password).getBytes))
    }
    def getHeader(username: String, password: String): String =
      BASIC + " " + encodeCredentials(username, password)
  }

  private val userForm: Form[UserForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "fullName" -> nonEmptyText,
      "institution" -> text,
      "city" -> text,
      "postalCode" -> text
    ) (UserForm.apply)(UserForm.unapply)
  )

  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]

  /// User Auth stuff ///

  private val credentials : Form[(String, String)] = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(Tuple2.apply)(Tuple2.unapply)
  )

}