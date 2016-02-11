package tabula.test

import tabula._
import Tabula._
import Column._
import shapeless._
import shapeless.HList._
import org.specs2.mutable._
import org.joda.time._
import org.apache.commons.lang3.text.WordUtils.capitalize
import org.apache.commons.lang3.RandomStringUtils.randomAscii
import scalaz._
import Scalaz._

// a pretend data model

case class UselessItem(name: String, price: Double, tags: Map[String, String] = Map.empty)
case class PretentiousPurveyor(name: String, location: String)
case class Purchase(item: UselessItem, date: Option[DateTime], from: PretentiousPurveyor)

// some test data

object Items {
  val justSomeCoatRack = UselessItem("honest abe", 90.39, tags = Map("foo" -> randomAscii(10)))
  val cheeseParkingSpot = UselessItem("fancy cheese board", 39.95, tags = Map("foo" -> randomAscii(10), "bar" -> randomAscii(10)))
  val whatIsThis = UselessItem("faux professional tool pouch", 48.00, tags = Map("foo" -> randomAscii(10), "quux" -> randomAscii(10)))
}

object PlacesNormalPeopleDoNotGo {
  val SchizophrenicMonkey = PretentiousPurveyor("Tinkering Monkey", "SF Bay Area")
  val BrooklynSlateWtf = PretentiousPurveyor("Brooklyn Slate Co.", "Brokelyn")
  val HeritageInsanityCo = PretentiousPurveyor("Heritage Leather Co.", "Somewhere in Cali")
}

object Purchases {
  import Items._
  import PlacesNormalPeopleDoNotGo._

  val * = {
    Purchase(item = justSomeCoatRack, date = Some(DateTime.now), from = SchizophrenicMonkey) ::
      Purchase(item = cheeseParkingSpot, date = None, from = BrooklynSlateWtf) ::
      Purchase(item = whatIsThis, date = Some(DateTime.now), from = HeritageInsanityCo) ::
      Nil
  }
}

// column descriptions

// what we bought
object ItemName extends Column((_: Purchase).item.name)

// how much we paid
object ItemPrice extends Column((_: Purchase).item.price)

// where we bought it
object PurchaseLocation extends Column((_: Purchase).from.location)

// date of purchase
object DateOfPurchase extends Column((_: Purchase).date)

// tags
case class Tag(name: String) extends Column((_: Purchase).item.tags.get(name))
object Tags extends ListColumn(
  Tag("foo") @@ "tag foo" ::
    Tag("bar") @@ "tag bar" ::
    Tag("baz") @@ "tag baz" ::
    Tag("quux") @@ "tag quux" :: Nil
)

// transformer column: capitalize words
object Capitalize extends Column(capitalize)

// object TotalPaid extends Fold(ItemPrice)(0)(_ + _)

// unlimited extensibility via type classes!
object Extensibility {
  import scala.xml._

  // we'll output cells of type Cell[NodeSeq] via a function that
  // procudes objects of type HTML
  case class HTML(nodes: NodeSeq)

  // provide a way of lazily converting HTML => Cell[NodeSeq]
  implicit object HTMLNodeSeqCellulizer extends Cellulizer[HTML, NodeSeq](_.nodes)

  // here's a column that produces HTML from Purchase-s
  object Title extends Column((p: Purchase) => HTML(<title>{ p.item.name }</title>)) with Namer

  // create custom console output that overrides some default formats and
  // implements conversion of NodeSeq-s to text
  object MyConsole extends Console(minWidth = 15) {
    implicit object NodeSeqFormatter extends SimpleFormatter[NodeSeq] {
      def apply(value: Option[NodeSeq]) = value.map(_.toString) :: Nil
    }
  }
}

import Extensibility._

object ShowcaseSpec {
  object columns extends Columns(
    (ItemName | Capitalize) @@ "Item Name" ::
      Title ::
      ItemPrice @@ "Item Price" ::
      PurchaseLocation @@ "Bought At" ::
      DateOfPurchase @@ "Date of Purchase" ::
      Tags ::
      HNil
  )
}

// let's do it!
class ShowcaseSpec extends Specification {
  "a purchase history" should {
    import ShowcaseSpec._
    "print out a list of things we've bought" in {
      columns.write(MyConsole)(_.toConsole())(Purchases.*.iterator)
      success
    }
  }
}
