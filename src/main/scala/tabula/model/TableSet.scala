package tabula.model

import org.apache.poi.hssf.usermodel._
import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

@Salat
trait AsTableSet extends AsCSV with AsXLS {

  val name: String
  val tables: List[Table]

  def asCSV = tables.map(_.asCSV).mkString("\n\n\n")

  def asXLS: HSSFWorkbook = asXLS(false)

  def asXLS(autoSize: Boolean = false): HSSFWorkbook = {
    val workbook = new HSSFWorkbook
    val DateTimeCellStyle = {
      val style = workbook.createCellStyle
      style.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"))
      style
    }
    tables.foreach {
      case table @ Table(_id, tableSetId, table_name, header, rows, _) => {
        val sheet = workbook.createSheet(table_name)
        (header :: rows.toList ::: Nil).zipWithIndex.foreach {
          case (Row(_id, tableId, columns, _), idx) => {
            val row = sheet.createRow(idx)
            columns.zipWithIndex.foreach {
              case (c, colIdx) => {
                val cell = row.createCell(colIdx)
                c match {
                  case StringColumn(value) => cell.setCellValue(value)
                  case DateTimeColumn(Some(date)) => {
                    cell.setCellStyle(DateTimeCellStyle)
                    cell.setCellValue(date.toDate)
                  }
                  case DateTimeColumn(_) => {}
                  case bdc @ BigDecimalColumn(_, _, _, _) => bdc.scaled.foreach {
                    scaled =>
                      if (scaled != null) cell.setCellValue(scaled.doubleValue)
                  }
                  case EmptyColumn(_)                           => {}
                  case x                                        => throw new IllegalArgumentException("been adding columns, haven't you? %s".format(x.getClass))
                }
              }
            }
          }
        }
        if (autoSize) header.columns.zipWithIndex.foreach {
          case (_, i) => sheet.autoSizeColumn(i)
        }
      }
    }
    workbook
  }

}

object TableSet {
  val empty = TableSet(name = "", tables = Nil)
}

case class TableSet(_id: ObjectId = new ObjectId,
                    name: String,
                    tables: List[Table]) extends AsTableSet
