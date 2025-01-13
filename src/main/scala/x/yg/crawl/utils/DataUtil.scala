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
    val now = LocalDateTime.now().plusDays(0)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    now.format(formatter)
  }

  def stockTimestamp(dayOffset: Int = 0): String = {
    val now = LocalDateTime.now().plusDays(dayOffset)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    val formattedNow = now.format(formatter)
    val fixedTime = "161049"

    val dayOfWeek = now.getDayOfWeek
    val isWeekend = dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY

    if (isWeekend) {
      val friday = now.minusDays(dayOfWeek.getValue - java.time.DayOfWeek.FRIDAY.getValue)
      friday.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + fixedTime
    } else {
      //now
      if (formattedNow.substring(8).toInt > fixedTime.toInt) {
        formattedNow.substring(0, 8) + fixedTime
      } else {
        formattedNow
      }
    }
  }

  def main(v: Array[String]) = {
    // println(convertNumTextToInt("1,000,000"))
    // println(getYYYYMMDD())
    println(getCurrentTimestamp() + " vs " + stockTimestamp()) //2024-11-27 16:10:09
    // 20241213161147
    // 20241213161049
  }
}
