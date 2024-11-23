package x.yg.crawl.utils

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
}
