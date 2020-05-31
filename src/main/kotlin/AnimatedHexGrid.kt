import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.window
import kotlin.browser.document
import kotlin.dom.createElement
import kotlin.math.absoluteValue

fun <T, U> cartesianProduct(a: Iterable<T>, b: Iterable<U>): List<Pair<T, U>> =
    a.flatMap { va ->
        b.map { vb -> Pair(va, vb) }
    }

infix fun <T, U> Iterable<T>.x(other: Iterable<U>) = cartesianProduct(this, other)

class AnimatedHexGrid : Animator {
    private val size = Size(20.0, 20.0)
    private val origin = Point(0.0, 0.0)

    private val pointyLayout = Layout(Orientation.Pointy, size, origin)
    private val flatLayout = Layout(Orientation.Flat, size, origin)

    private val hexes = ((-20..45) x (-30..30)).map { (q, r) -> Hex(q, r) }

    private fun List<Hex>.toGrid(layout: Layout) = map { layout.polygonCorners(it) }

    private val layout = flatLayout
    private val grid = hexes.toGrid(layout)

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

    private var canvasSize = Size(0.0, 0.0)
        set(value) {
            if (field != value) {
                onResize()
            }
            field = value
        }

    private fun onResize() {
        offscreenCanvasGrid = null
        offscreenCanvasSelection = null
    }

    private val CanvasRenderingContext2D.size: Size
        get() = Size(canvas.width.toDouble(), canvas.height.toDouble())

    private fun createOffscreenCanvas(
        size: Size,
        onContext2D: CanvasRenderingContext2D.() -> Unit
    ) = document.createElement("canvas") {}.let {
        it as HTMLCanvasElement
    }.apply {
        width = size.x.toInt()
        height = size.y.toInt()
    }.apply {
        onContext2D(getContext("2d") as CanvasRenderingContext2D)
    }

    override fun draw(ctx: CanvasRenderingContext2D) {
        canvasSize = ctx.size

        if (offscreenCanvasGrid == null) {
            offscreenCanvasGrid = createOffscreenCanvas(canvasSize) {
                val width = canvas.width.toDouble()
                val height = canvas.height.toDouble()

                beginPath()

                grid.filter { (center, _) -> center.x in 0.0..width && center.y in .0..height }
                    .forEach { (center, corners) ->
                        drawSides(this, corners)
                        drawCenter(this, center)
                    }

                stroke()
            }
        }

        if (selectedHex != null && offscreenCanvasSelection == null) {
            offscreenCanvasSelection = createOffscreenCanvas(canvasSize) {
                fillStyle = "green"
                drawSides(this, layout.polygonCorners(selectedHex!!).second)
                fill()
            }
        }

        ctx.clearRect(0.0, 0.0, canvasSize.x, canvasSize.y)
        offscreenCanvasSelection?.let { ctx.drawImage(it, 0.0, 0.0) }
        offscreenCanvasGrid?.let { ctx.drawImage(it, 0.0, 0.0) }

        drawFps(ctx)
    }

    private var offscreenCanvasGrid: HTMLCanvasElement? = null

    fun onEvent(event: Event) {
        when (event.type) {
            "click" -> {
                onEventClick(event as MouseEvent)
            }
            else -> Unit
        }
    }

    private var offscreenCanvasSelection: HTMLCanvasElement? = null
    private var selectedHex: Hex? = null
        set(value) {
            console.log("Change selection $field->$value")
            field = value
            offscreenCanvasSelection = null
        }

    private fun onEventClick(event: MouseEvent) {
        selectedHex =
            event.let { Point(it.offsetX, it.offsetY) }.let { layout.toHex(it).round() }
    }
}