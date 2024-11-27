package x.yg.crawl.utils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DataUtil {
  def convertNumTextToInt(text: String): Int = {
    // 1,000,000 -> 1000000
    text.replaceAll(",", "").toInt
  }

  def getYYYYMMDD(offset: Int = 0): String = {
    import java.time.LocalDate
    val today = LocalDate.now
    val target = today.minusDays(offset)
    target.toString
  }

  //2024-11-19 16:10:49
  def getCurrentTimestamp(): String = {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    now.format(formatter)
  }

  def stockTimestamp(): String = {
    val curTs = getCurrentTimestamp() 
    curTs.substring(0, 8) match {
      case a if a > "161009" => curTs.substring(0, 8) + "161009"
      case _ => curTs
    }
  }
  //2024-11-19 16:10:49 --> 20241119161049  --> 20241127181614
  

  def main(v: Array[String]) = {
    // println(convertNumTextToInt("1,000,000"))
    // println(getYYYYMMDD())
    println(stockTimestamp()) //2024-11-27 16:10:09
  }
}
