import org.w3c.dom.CanvasRenderingContext2D
import kotlin.browser.window
import kotlin.math.absoluteValue

fun <T, U> cartesianProduct(a: Iterable<T>, b: Iterable<U>): List<Pair<T, U>> =
    a.flatMap { va ->
        b.map { vb -> Pair(va, vb) }
    }

infix fun <T, U> Iterable<T>.x(other: Iterable<U>) = cartesianProduct(this, other)

class AnimatedHexGrid : Animator {
    private var isFlat = true

    private val size = Size(20.0, 20.0)
    private val origin = Point(0.0, 0.0)

    private val pointyLayout = Layout(Orientation.Pointy, size, origin)
    private val flatLayout = Layout(Orientation.Flat, size, origin)

    private val hexes = ((-20..45) x (0..30)).map { (q, r) -> Hex(q, r) }

    private fun List<Hex>.toGrid(layout: Layout) = map { layout.polygonCorners(it) }

    private val pointyGrid = hexes.toGrid(pointyLayout)
    private val flatGrid = hexes.toGrid(flatLayout)

    private fun drawCenter(ctx: CanvasRenderingContext2D, point: Point) {
        ctx.moveTo(point.x - 1, point.y - 1)
        ctx.arc(point.x, point.y, 1.0, 0.0, kotlin.math.PI * 2.0)
    }

    private fun drawSides(ctx: CanvasRenderingContext2D, corners: List<Point>) {
        val start = corners.first()
        ctx.moveTo(start.x, start.y)
        corners.drop(1).forEach { ctx.lineTo(it.x, it.y) }
        ctx.lineTo(start.x, start.y)
    }

    private var timestamps = mutableListOf<Double>()
    private val timestampsLength = 100

    private fun Double.equalsDelta(other: Double) = (this / other - 1.0).absoluteValue < 0.000001

    private fun drawFps(ctx: CanvasRenderingContext2D) {
        timestamps.add(window.performance.now() - timestamp)
        timestamp = window.performance.now()

        if (timestamps.size >= timestampsLength) {
            timestamps = timestamps.takeLast(timestampsLength).toMutableList()
        }
        val avg = timestamps.sum() / timestamps.size
        val fps = (if (avg.equalsDelta(0.0)) 0.0 else 1000.0 / avg).toInt()

        ctx.save()
        ctx.beginPath()
        ctx.fillStyle = "white"
        ctx.fillRect(0.0, 0.0, 80.0, 50.0)
        ctx.closePath()
        ctx.fillStyle = "black"
        ctx.font = "bold 48px monospace"
        ctx.fillText("fps $fps", 5.0, 40.0, 70.0)
        ctx.restore()
    }

    private var timestamp = window.performance.now()

    override fun draw(ctx: CanvasRenderingContext2D) {
        val grid = if (isFlat) flatGrid else pointyGrid
//        isFlat = !isFlat

        val width = ctx.canvas.width.toDouble()
        val height = ctx.canvas.height.toDouble()

        ctx.beginPath()
        ctx.clearRect(0.0, 0.0, width, height)

        grid.filter { (center, _) -> center.x in 0.0..width && center.y in .0..height }
            .forEach { (center, corners) ->
                drawSides(ctx, corners)
                drawCenter(ctx, center)
            }

        ctx.stroke()

        drawFps(ctx)
    }
}