import org.scalatest._
import scala.collection.convert.wrapAsJava._
import de.zalando.zmon.scheduler.ng._
import java.util.List

class OverlapSpec extends FlatSpec with Matchers {

  def jm(m : Map[String, Object]) : java.util.Map[String, Object] = {
    mapAsJavaMap(m)
  }

  def jmS(m : Map[String, String]) : java.util.Map[String, String] = {
    mapAsJavaMap(m)
  }

  "Entity" should "match by type" in {
    val entityMap : Map[String, Object] = Map("host"->"host1","type"->"host")
    val filterMap : Map[String, String] = Map("type"->"host")

    filter.overlaps(jmS(filterMap), jm(entityMap)) should be (true)
  }

  "Entity" should "not match by type" in {
    val entityMap = Map("host"->"host1","type"->"host")
    val filterMap = Map("type"->"database")

    filter.overlaps(jmS(filterMap), jm(entityMap)) should be (false)
  }

  "Entity" should "not match no type" in {
    val entityMap = Map("host"->"host1")
    val filterMap = Map("type"->"database")

    filter.overlaps(jmS(filterMap), jm(entityMap)) should be (false)
  }

  "Entity" should "match team collection" in {
    val teamCollection = new java.util.ArrayList[String]()
    teamCollection.add("Platform/Database")
    teamCollection.add("Platform/Software")
    val entityMap = Map("type"->"host", "team"->teamCollection)
    val filterMap = Map("type"->"host","team"->"Platform/Software")

    filter.overlaps(jmS(filterMap), jm(entityMap)) should be (true)
  }

  "Entity" should "not match team collection" in {
    val teamCollection = new java.util.ArrayList[String]()
    teamCollection.add("Platform/Database")
    teamCollection.add("Platform/Software")
    val entityMap = Map("type"->"host", "team"->teamCollection)
    val filterMap = Map("type"->"host","team"->"Platform/System")

    filter.overlaps(jmS(filterMap), jm(entityMap)) should be (false)
  }
}