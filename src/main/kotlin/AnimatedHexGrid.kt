import kotlinx.css.Contain
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HANGING
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.MIDDLE
import org.w3c.dom.Path2D
import org.w3c.dom.TOP
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.createElement
import kotlin.math.absoluteValue
import kotlin.math.max

fun <T, U> cartesianProduct(a: Iterable<T>, b: Iterable<U>): List<Pair<T, U>> =
    a.flatMap { va ->
        b.map { vb -> Pair(va, vb) }
    }

infix fun <T, U> Iterable<T>.x(other: Iterable<U>) = cartesianProduct(this, other)

class AnimatedHexGrid : Animator {
    private val size = Size(40.0, 40.0)
    private val origin = Point(0.0, 0.0)

    private val pointyLayout = Layout(Orientation.Pointy, size, origin)
    private val flatLayout = Layout(Orientation.Flat, size, origin)

    private val hexes = ((-20..45) x (-30..30)).map { (q, r) -> Hex(q, r) }

    private fun List<Hex>.toGrid(layout: Layout) = map { layout.polygonCorners(it) }

    private val layout = flatLayout
    private val grid = hexes.toGrid(layout)

    private fun drawCenter(point: Point): Path2D {
        val path = Path2D()
        path.moveTo(point.x - 1, point.y - 1)
        path.arc(point.x, point.y, 1.0, 0.0, kotlin.math.PI * 2.0)
        return path
    }

    private fun drawSides(corners: List<Point>): Path2D {
        val start = corners.first()
        val path = Path2D()
        path.moveTo(start.x, start.y)
        corners.drop(1).forEach { path.lineTo(it.x, it.y) }
        path.lineTo(start.x, start.y)
        return path
    }

    private fun formatHexCoordinate(name: String, value: Int): String = when {
        value == 0 -> name
        value > 0 -> "+$value"
        else -> value.toString()
    }

    private fun drawCoordinates(ctx: CanvasRenderingContext2D, hex: Hex) {
        val center = layout.toPixel(hex)
        ctx.font = "bold ${layout.size.x / 3}pt monospace"
        val q = formatHexCoordinate("q", hex.q)
        val r = formatHexCoordinate("r", hex.r)
        val s = formatHexCoordinate("s", hex.s)

        ctx.textBaseline = CanvasTextBaseline.TOP

        ctx.fillStyle = "green"
        ctx.fillText(
            q,
            center.x - size.x * 5 / 8,
            center.y - size.y / 2
        )

        val rx = ctx.measureText(r)
        ctx.fillStyle = "blue"
        ctx.fillText(
            r,
            center.x + size.x * 5 / 8 - rx.width,
            center.y - size.y / 2
        )

        val sx = ctx.measureText(s)
        ctx.fillStyle = "red"
        ctx.fillText(
            s,
            center.x - sx.width / 2,
            center.y + size.y / 4
        )
    }

    private var offscreenCanvasCoordinates: HTMLCanvasElement? = null

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
        offscreenCanvasPond = null
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

                val path = Path2D()

                grid.filter { (center, _) -> center.x in 0.0..width && center.y in .0..height }
                    .forEach { (center, corners) ->
                        path.addPath(drawSides(corners))
                        path.addPath(drawCenter(center))
                    }

                stroke(path)
            }
        }

        if (offscreenCanvasCoordinates == null) {
            offscreenCanvasCoordinates = createOffscreenCanvas(canvasSize) {
                hexes.forEach { drawCoordinates(this, it) }
            }
        }

        if (selectedHex.isNotEmpty() && offscreenCanvasSelection == null) {
            offscreenCanvasSelection = createOffscreenCanvas(canvasSize) {
                strokeStyle = "lightgreen"
                lineWidth = 4.0

                val path = Path2D()
                selectedHex.map { layout.polygonCorners(it).second }.forEach {
                    path.addPath(drawSides(it))
                }
                stroke(path)

                selectedHex.forEach { drawCoordinates(this, it) }
            }
        }

        if (pond.isNotEmpty() && offscreenCanvasPond == null) {
            offscreenCanvasPond = createOffscreenCanvas(canvasSize) {
                pond.map { (hex, power) ->
                    layout.polygonCorners(hex).second to power.coerceIn(0, 100) / 100.0
                }.forEach { (points, alpha) ->
                    val path = drawSides(points)
                    fillStyle = "rgb(255, 0, 0, $alpha)"
                    fill(path)

                }
                pond.forEach { (hex, power) ->
                    fillStyle = "black"
                    font = "bold ${layout.size.x / 3}pt monospace"
                    val t = power.toString()
                    val tx = measureText(t)
                    val center = layout.toPixel(hex)
                    textBaseline = CanvasTextBaseline.MIDDLE
                    fillText(
                        t,
                        center.x - tx.width / 2,
                        center.y
                    )
                }
            }
        }

        ctx.fillStyle = "#fff"
        ctx.fillRect(0.0, 0.0, canvasSize.x, canvasSize.y)

        offscreenCanvasPond?.copyOn(ctx)
//        offscreenCanvasGrid?.copyOn(ctx)
//        offscreenCanvasCoordinates?.copyOn(ctx)
        offscreenCanvasSelection?.copyOn(ctx)

        drawFps(ctx)
    }

    private fun HTMLCanvasElement.copyOn(ctx: CanvasRenderingContext2D) =
        ctx.drawImage(this, 0.0, 0.0)

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
    private val selectedHex = mutableSetOf<Hex>()

    private fun onEventClick(event: MouseEvent) {
        offscreenCanvasSelection = null
        val hex =
            event.let { Point(it.offsetX, it.offsetY) }.let { layout.toHex(it).round() }

        when {
            hex in selectedHex -> selectedHex -= hex
            // Ctrl click calls context menu.
            event.ctrlKey -> Unit
            event.shiftKey && event.altKey -> {
                spoil(hex)
                offscreenCanvasPond = null
            }
            event.shiftKey -> selectedHex += hex
            event.altKey -> {
                pond.clear()
                spoil(hex)
                offscreenCanvasPond = null
            }
            else -> selectedHex.apply {
                clear()
                add(hex)
            }
        }
//        selectedHex.forEach { h ->
//            pond[h]?.let { power ->
//                console.log("$h->$power")
//            }
//        }
    }

    private val pond = mutableMapOf<Hex, Int>()
    private var offscreenCanvasPond: HTMLCanvasElement? = null

    private fun spoil(root: Hex) {
        val newPond = mutableMapOf<Hex, Int>()
        newPond[root] = 100
        (0..10).forEach { _ ->
            newPond += newPond.flatMap { (hex, power) ->
                buildPond(
                    hex,
                    (power - 5).coerceAtLeast(0)
                )
            }.filterNot { it.first in newPond }
        }

        pond += newPond.filterNot { it.key in pond }
    }

    private fun buildPond(root: Hex, power: Int) =
        root.neighbors().map { it to (0..power).random() }.filter { it.second > 0 }
}
