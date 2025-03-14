import zio.*
import zio.http.{RoutePattern, Routes, Server}
import zio.http.endpoint.Endpoint
import zio.schema.annotation.description
import zio.schema.{DeriveSchema, Schema}

type CallCount = Long

object Run extends ZIOAppDefault:

  given Schema[ProbeRequest] = DeriveSchema.gen
  given Schema[ProbeResponse] = DeriveSchema.gen
  private val end = Endpoint(RoutePattern.POST / "test")
    .in[ProbeRequest]("input")
    .out[ProbeResponse]
  private val handler = end.implementHandler(http.handler(function))

  def burn(time: Duration) = ZIO
    .attemptBlocking {
      var ii = 0L
      while ii <= 100_000
      do ii += 1
    }
    .ignore
    .forever
    .timeout(time)
    .unit
  def function(in: ProbeRequest) = ZIO
    .acquireReleaseWith(
      Cpu.get(in.cpuSensitivity)
    )(_ => Cpu.drop(in.cpuSensitivity)) { inuse =>
      for {
        count <- InvocationCounter.incrementAndGet
        _ <- ZIO.log(s"$count: $in @ $inuse + ${Host.baseLoad}")
        time <- in.time.map(_.milliseconds)
        elapsed <-
          if in.burn then Burner.burn(time)
          else
            ZIO
              .sleep(time * (inuse + Host.baseLoad) * in.cpuSensitivity)
              .timed
              .map(_._1)

      } yield ProbeResponse(count, elapsed.toMillis, inuse, Host.name)
    }
    .tap(r => ZIO.log(r.toString))

  def run = (for {
    _ <- Server.serve(Routes(handler))
  } yield 1).provide(
    InvocationCounter.counter,
    Cpu.cpu,
    Burner.live,
    Server.default
  )

trait InvocationCounter:
  def incrementAndGet: UIO[Long]

object InvocationCounter:
  case class Impl(ref: Ref[Long]) extends InvocationCounter:
    override def incrementAndGet: UIO[Long] = ref.incrementAndGet

  def counter = ZLayer(Ref.make[Long](0).map(Impl.apply))

  def incrementAndGet = ZIO.serviceWithZIO[InvocationCounter](_.incrementAndGet)

trait Cpu:
  def acquire(cpu: Double): UIO[Double]
  def release(cpu: Double): UIO[Unit]

object Cpu:
  case class Impl(ref: Ref[Double]) extends Cpu:
    override def acquire(cpu: Double) = ref.updateAndGet(_ + cpu)
    override def release(cpu: Double) = ref.update(_ - cpu)

  def cpu = ZLayer(Ref.make[Double](0).map(Impl.apply))

  def get(cpu: Double) = ZIO.serviceWithZIO[Cpu](_.acquire(cpu))
  def drop(cpu: Double) = ZIO.serviceWithZIO[Cpu](_.release(cpu))

trait Burner:
  def burn(time: Duration): UIO[Unit]

object Burner:

  def burnRun(cyles: Long) = {
    var acc = 0L
    var ii = cyles
    while (ii > 0)
    do
      ii -= 1
      acc += ii
  }
  case class Impl(perms: Double) extends Burner:
    override def burn(time: zio.Duration): UIO[Unit] =
      ZIO.attemptBlocking(burnRun((time * perms).toMillis)).ignore
  private val loop = 10
  private def median(count: Long) = ZIO
    .foreach(0 to (loop + loop)) { _ =>
      ZIO.attemptBlocking(burnRun(count)).timed.map(_._1.toMillis)
    }
    .map(_.sorted.drop(loop).head)
  val live = ZLayer(for {
    size <- Ref.make(1L << 19)
    millis <- size.updateAndGet(_ << 1).flatMap(median).repeatUntil(_ > 128L)
    neededSize <- size.get
    perms = neededSize.toDouble / millis
    _ <- ZIO.log(s"need $perms/ms")
  } yield Impl(perms))

  def burn(time: Duration) =
    ZIO.serviceWithZIO[Burner](_.burn(time)).timed.map(_._1)
