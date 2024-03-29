package tabula

import Tabula._
import org.joda.time._
import scala.math.{BigDecimal => ScalaBigDecimal}
import java.math.{BigDecimal => JavaBigDecimal}
import cats.Monoid

/** Type class for converting an intermediate representation `T` into
  * a cell type `C`. Instances of this type class will be largely
  * specific to the application using Tabula; however, a few are
  * provided out of the box in order to faciliate use of common JVM
  * primitives and other values.
  *
  * See [[Column]] for a detailed description of `T` and `C`, and the
  * roles these types play in the Tabula design.
  *
  * @param convert function for turning a `T` into a `C`
  */
abstract class Cellulizer[T, C: Manifest](convert: T => C) { cz =>

  /** Lazy implementation of a [[Cell]][C] that defers computation of
    * the `C` until a Tabula component requires it.
    *
    * @param source Some arbitrary `F` - domain type from which cell data is extracted
    * @param f Function for extracting intermediate type `T` from `F`
    */
  class LazyCell[F](source: F, f: F => Option[T]) extends Cell[C] {
    def manifest = implicitly[Manifest[C]]
    lazy val value = f(source).map(convert)
  }

  /** Extract a [[Cell]][C] from some arbitrary `F`. */
  def apply[F](source: F, f: F => Option[T]): Cell[C] = new LazyCell(source, f)

  /** Convert some `T` into a [[Cell]][C]. */
  def apply(value: T): Cell[C] = new LazyCell(value, Some(_: T))

  /** Convert a potentially missing `T` into a [[Cell]][C]. */
  def apply(value: Option[T]): Cell[C] =
    new LazyCell(value, identity: Option[T] => Option[T])
}

/** Derived [[Cellulizer]] that supports [[ListColumn]] as a means of
  * producing [[Cell]][List[C]] cells.
  */
class ListCellulizer[F, T, C: Manifest](implicit mc: Monoid[C])
    extends Cellulizer[List[ColumnAndCell[F, T, C]], List[C]](
      cacs => cacs.map(cac => cac._2.value.getOrElse(mc.empty))
    )

/** Cellulizer starter kit. Contains [[Cellulizer]]s for enabling
  * commonly used conversions, making Tabula immediately useful in an
  * uncomplicated environment that doesn't require additional
  * extensions.
  */
trait Cellulizers {

  /** Cellulizers for primitive types that Tabula understands natively. */
  implicit object StringIdCellulizer
      extends Cellulizer[String, String](identity)
  implicit object BooleanIdCellulizer
      extends Cellulizer[Boolean, Boolean](identity)
  implicit object IntDoubleCellulizer extends Cellulizer[Int, Int](identity)
  implicit object DoubleDoubleCellulizer
      extends Cellulizer[Double, Double](identity)
  implicit object DateTimeDateTimeCellulizer
      extends Cellulizer[DateTime, DateTime](identity)

  /** Cellulizer for `T =:= scala.math.BigDecimal` and `C =:= Double` case. */
  implicit object ScalaBigDecimalDoubleCellulizer
      extends Cellulizer[ScalaBigDecimal, Double](_.doubleValue)

  /** Cellulizer for `T =:= java.math.BigDecimal` and `C =:= Double` case. */
  implicit object JavaBigDecimalDoubleCellulizer
      extends Cellulizer[JavaBigDecimal, Double](_.doubleValue)

  /** Cellulizer for `T =:= Long` and `C =:= Double` case. */
  implicit object LongDoubleCellulizer
      extends Cellulizer[Long, Double](_.doubleValue)

  /** Cellulizer for `T =:= Float` and `C =:= Double` case. */
  implicit object FloatDoubleCellulizer
      extends Cellulizer[Float, Double](_.doubleValue)

  /** Perform ad-hoc short-circuit conversion of some `F` directly to
    * `C`, bypassing the intermediate `T` stage. Supported by
    * implicitly injected [[Cellulizer]][F, C].
    */
  def cellulize[F, C](value: F)(implicit cz: Cellulizer[F, C]): Cell[C] =
    cz(value)

  /** Perform ad-hoc short-circuit conversion of a potentially missing
    * `F` directly to `C`, bypassing the intermediate `T`
    * stage. Supported by implicitly injected [[Cellulizer]][F, C].
    */
  def cellulize[F, C](value: Option[F])(
      implicit cz: Cellulizer[F, C]): Cell[C] = cz(value)

  implicit def lcz[F, T, C: Manifest](implicit mc: Monoid[C]) =
    new ListCellulizer[F, T, C]
}
