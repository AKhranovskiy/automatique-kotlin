import org.w3c.dom.CanvasRenderingContext2D
import kotlin.browser.window

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

    private val hexes = ((0..10) x (0..10)).map { (q, r) -> Hex(q, r) }

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

    private fun drawFps(ctx: CanvasRenderingContext2D) {
        val diff = window.performance.now() - timestamp
        timestamp = window.performance.now()

        ctx.save()
        ctx.beginPath()
        ctx.fillStyle = "white"
        ctx.fillRect(0.0, 0.0, 80.0, 50.0)
        ctx.closePath()
        ctx.fillStyle = "black"
        ctx.font = "bold 48px monospace"
        ctx.fillText("fps ${(1000 / diff).toInt()}", 5.0, 40.0, 70.0)
        ctx.restore()
    }

    private var timestamp = window.performance.now()

    override fun draw(ctx: CanvasRenderingContext2D) {
        val grid = if (isFlat) flatGrid else pointyGrid
        isFlat = !isFlat

        val width = ctx.canvas.width.toDouble()
        val height = ctx.canvas.height.toDouble()

        ctx.beginPath()
        ctx.clearRect(0.0, 0.0, width, height)

        grid.forEach { (center, corners) ->
            drawSides(ctx, corners)
            drawCenter(ctx, center)
        }

        ctx.stroke()

        drawFps(ctx)
    }
}