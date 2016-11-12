package libs.solr.scala

/**
  * The result of query which is executed by QueryBuilder#getResultAsMap().
  * and similar functions
*/

abstract class MapQueryResults{
  def numFound: Long
  def documents: List[Map[String, Any]]
  def facetFields: Map[String, Map[String, Long]]
  def facetDates: Map[String, Map[String, Long]]
  def start: Int
}


case class MapGeneQueryResults(
                                numFound: Long,
                                documents: List[Map[String, Any]],
                                facetFields: Map[String, Map[String, Long]],
                                facetDates: Map[String, Map[String, Long]],
                                start: Int) extends MapQueryResults

case class MapPwayQueryResults(
                                numFound: Long,
                                documents: List[Map[String, Any]],
                                start: Int) extends MapQueryResults {
  override def facetFields: Map[String, Map[String, Long]] = null
  override def facetDates: Map[String, Map[String, Long]] = null
}

case class MapClusterQueryResult(
                                  numOfClusters: Int,
                                  clusters : List[Map[String , List[String]]])

case class CaseClassQueryResult[T](
                                    numFound: Long,
                                    documents: List[T],
                                    facetFields: Map[String, Map[String, Long]],
                                    facetDates: Map[String, Map[String, Long]])
