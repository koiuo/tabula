package tabula

import shapeless._
import shapeless.ops.hlist._
import Tabula._

abstract class Columns[F, T, C, Tail <: HList, O <: HList](val columns: Column[F, T, C] :: Tail)(
    implicit
    tl: ToList[Column[F, T, C] :: Tail, Column[_, _, _]],
    aa: ApplyAll[F, Column[F, T, C] :: Tail, ColumnAndCell[F, T, C] :: O]
) {
  private val cellsF = cells(columns)
  def write[Fmt <: Format](fmt: Fmt)(f: fmt.Factory => fmt.Writer)(xs: Iterator[F])(
    implicit
    lf: LeftFolder[ColumnAndCell[F, T, C] :: O, fmt.Row, fmt.type]
  ) =
    writer(fmt)(f).write(xs.map(cellsF(_).row(fmt)))
  def writer[Fmt <: Format](fmt: Fmt)(f: fmt.Factory => fmt.Writer) =
    f(fmt.writer(columns))
}
