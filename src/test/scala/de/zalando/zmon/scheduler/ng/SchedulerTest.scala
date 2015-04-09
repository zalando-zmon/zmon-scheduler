import org.scalatest._
import scala.collection.convert.wrapAsJava._
import de.zalando.zmon.scheduler.ng._

class OverlapSpec extends FlatSpec with Matchers {

  def jm(m : Map[String, String]) : java.util.Map[String, String] = {
    mapAsJavaMap(m)
  }

  "Entity" should "match by type" in {
    val entityMap = Map("host"->"host1","type"->"host")
    val filterMap = Map("type"->"host")

    filter.overlaps(jm(filterMap), jm(entityMap)) should be (true)
  }

  "Entity" should "not match by type" in {
    val entityMap = Map("host"->"host1","type"->"host")
    val filterMap = Map("type"->"database")

    filter.overlaps(jm(filterMap), jm(entityMap)) should be (false)
  }

  "Entity" should "not match no type" in {
    val entityMap = Map("host"->"host1")
    val filterMap = Map("type"->"database")

    filter.overlaps(jm(filterMap), jm(entityMap)) should be (false)
  }

}