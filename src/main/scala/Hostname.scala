import scala.util.{Random, Try}
import scala.util.control.NonFatal

object Host:
  val name =
    try {
      val p = Runtime.getRuntime.exec(Array("hostname"))
      p.inputReader().readLine()
    } catch case NonFatal(_) => s"RANDOM${Random.between(10_000, 100_000)}"

  val baseLoad: Int = System.getenv("BASE_LOAD") match
    case "RANDOM" => Random.nextInt(16)
    case str      => Try(str.toInt) getOrElse 0

case class ProbeResponse(count: CallCount, timeInMillis:Long, load:Double, host: String)
case class ProbeRequest(
    minTimeMillis: Long,
    maxTimeMillis: Long = 0,
    cpuSensitivity: Double = 1.0,
    burn: Boolean = true
) {
  def time = zio.ZIO.succeed(
    if maxTimeMillis <= minTimeMillis then minTimeMillis
    else {
      val diff = maxTimeMillis - minTimeMillis
      val rand =  Math.pow(Random.nextDouble(),3) * diff

      rand.ceil.toLong + minTimeMillis

    }
  )
}
