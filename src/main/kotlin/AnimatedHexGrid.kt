import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
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
import kotlin.math.sqrt
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T, U> cartesianProduct(a: Iterable<T>, b: Iterable<U>): List<Pair<T, U>> =
    a.flatMap { va ->
        b.map { vb -> Pair(va, vb) }
    }

infix fun <T, U> Iterable<T>.x(other: Iterable<U>) = cartesianProduct(this, other)

data class HexConfig(
    val orientation: Orientation = Orientation.Flat,
    val hexSize: Size = Size(40.0, 40.0)
)

inline fun <T> distinctObservable(
    initialValue: T,
    crossinline onChange: (oldValue: T, newValue: T) -> Unit
): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        if (oldValue != newValue) onChange(oldValue, newValue)
    }
}

typealias HexGrid = Map<Hex, HexPolygon>

class HexMap {
    var config: HexConfig by distinctObservable(HexConfig()) { _, _ -> updateLayout() }

    var size: Size by distinctObservable(Size(0.0, 0.0), { _, _ ->
        val width = config.hexSize.x * 2
        val horizontalSpacing = width * 3 / 4
        val minQ = 0
        val maxQ = (size.x / horizontalSpacing).toInt()

        val height = config.hexSize.y * sqrt(3.0f)

        // Every q-column takes half of r-column.
        val minR = -maxQ / 2
        val maxR = (size.y / height).toInt()

        hexes = ((minQ..maxQ) x (minR..maxR)).map { (q, r) -> Hex(q, r) }
    })

    var origin: Point by distinctObservable(Point(0.0, 0.0)) { _, _ -> updateLayout() }

    var layout: Layout by distinctObservable(Layout(config.orientation, config.hexSize, origin))
    { _, _ -> updateGrid() }
        private set

    var hexes: List<Hex> by distinctObservable(emptyList()) { _, _ -> updateGrid() }
        private set

    var grid: HexGrid = emptyMap()
        private set

    private fun updateLayout() {
        layout = Layout(config.orientation, config.hexSize, origin)
    }

    private fun updateGrid() {
        grid = hexes.map { it to layout.polygonCorners(it) }.toMap()
    }
}

private fun HTMLCanvasElement.copyOn(ctx: CanvasRenderingContext2D) =
    ctx.drawImage(this, 0.0, 0.0)

data class Rect(val x: Double, val y: Double, val width: Double, val height: Double) {
    fun contains(point: Point) = point.x in (x..x + width) && point.y in (y..y + height)
}

abstract class CanvasLayer {
    var size: Size by distinctObservable(Size(0.0, 0.0)) { _, _ ->
        canvas.width = size.x.toInt()
        canvas.height = size.y.toInt()
        invalidate()
    }

    val rect get() = Rect(0.0, 0.0, size.x, size.y)

    private val canvas = document.createElement("canvas") {} as HTMLCanvasElement
    protected val ctx get() = canvas.getContext("2d") as CanvasRenderingContext2D

    private var isInvalidated = true

    fun invalidate() {
        ctx.clearRect(rect.x, rect.y, rect.width, rect.height)
        isInvalidated = true
    }

    fun draw(destination: CanvasRenderingContext2D) {
        if (isInvalidated) {
            onDraw()
            isInvalidated = false
        }
        canvas.copyOn(destination)
    }

    abstract fun onDraw()
}

class GridLayer(private val hexMap: HexMap, private val showCenter: Boolean = false) :
    CanvasLayer() {
    override fun onDraw() {
        Path2D().apply {
            hexMap.grid.values
                .filter { (center, _) -> rect.contains(center) }
                .forEach { (center, corners) ->
                    addPath(corners.toPolygon())
                    addPath(center.toCircle())
                }
        }.let {
            ctx.stroke(it)
        }
    }

    private fun List<Point>.toPolygon() = Path2D().apply {
        if (isNotEmpty()) {
            val start = first()
            moveTo(start.x, start.y)
            drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(start.x, start.y)
        }
    }

    private fun Point.toCircle(radius: Int = 1) = Path2D().apply {
        if (showCenter) {
            moveTo(x - radius, y - radius)
            arc(x, y, radius.toDouble(), 0.0, kotlin.math.PI * 2.0)
        }
    }
}

class AnimatedHexGrid : Animator {
    private val hexMap = HexMap()
    private val gridLayer = GridLayer(hexMap)

    private fun formatHexCoordinate(name: String, value: Int): String = when {
        value == 0 -> name
        value > 0 -> "+$value"
        else -> value.toString()
    }

    private fun drawCoordinates(ctx: CanvasRenderingContext2D, hex: Hex) {
        val center = hexMap.layout.toPixel(hex)
        ctx.font = "bold ${hexMap.config.hexSize.x / 3}pt monospace"
        val q = formatHexCoordinate("q", hex.q)
        val r = formatHexCoordinate("r", hex.r)
        val s = formatHexCoordinate("s", hex.s)

        ctx.textBaseline = CanvasTextBaseline.TOP

        ctx.fillStyle = "green"
        ctx.fillText(
            q,
            center.x - hexMap.config.hexSize.x * 5 / 8,
            center.y - hexMap.config.hexSize.y / 2
        )

        val rx = ctx.measureText(r)
        ctx.fillStyle = "blue"
        ctx.fillText(
            r,
            center.x + hexMap.config.hexSize.x * 5 / 8 - rx.width,
            center.y - hexMap.config.hexSize.y / 2
        )

        val sx = ctx.measureText(s)
        ctx.fillStyle = "red"
        ctx.fillText(
            s,
            center.x - sx.width / 2,
            center.y + hexMap.config.hexSize.y / 4
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

    private var canvasSize: Size by distinctObservable(Size(0.0, 0.0)) { _, _ -> onResize() }

    private fun onResize() {
        gridLayer.invalidate()

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
        hexMap.size = ctx.size
        gridLayer.size = ctx.size

        if (offscreenCanvasCoordinates == null) {
            offscreenCanvasCoordinates = createOffscreenCanvas(canvasSize) {
                hexMap.hexes.forEach { drawCoordinates(this, it) }
            }
        }

        if (selectedHex.isNotEmpty() && offscreenCanvasSelection == null) {
            offscreenCanvasSelection = createOffscreenCanvas(canvasSize) {
                strokeStyle = "lightgreen"
                lineWidth = 4.0

                Path2D().apply {
                    selectedHex.mapNotNull { hexMap.grid[it]?.corners }.forEach {
//                        addPath(drawSides(it))
                    }
                }.let {
                    stroke(it)

                }

                selectedHex.forEach { drawCoordinates(this, it) }
            }
        }

        if (pond.isNotEmpty() && offscreenCanvasPond == null) {
            offscreenCanvasPond = createOffscreenCanvas(canvasSize) {
                pond.mapNotNull { (hex, power) ->
                    hexMap.grid[hex]?.corners?.let { it to power.coerceIn(0, 100) / 100.0 }
                }.forEach { (points, alpha) ->
                    fillStyle = "rgb(255, 0, 0, $alpha)"
//                    fill(drawSides(points))
                }

                pond.mapNotNull { (hex, power) ->
                    hexMap.grid[hex]?.center?.let { it to power.toString() }
                }.forEach { (center, t) ->
                    fillStyle = "black"
                    font = "bold ${hexMap.config.hexSize.x / 3}pt monospace"
                    textBaseline = CanvasTextBaseline.MIDDLE

                    val tx = measureText(t)
                    fillText(t, center.x - tx.width / 2, center.y)
                }
            }
        }

        ctx.fillStyle = "#fff"
        ctx.fillRect(0.0, 0.0, canvasSize.x, canvasSize.y)

        offscreenCanvasPond?.copyOn(ctx)
        gridLayer.draw(ctx)
        offscreenCanvasCoordinates?.copyOn(ctx)
        offscreenCanvasSelection?.copyOn(ctx)

        drawFps(ctx)
    }

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
        val hex = event
            .let { Point(it.offsetX, it.offsetY) }
            .let { hexMap.layout.toHex(it).round() }

        when {
            hex in selectedHex -> selectedHex -= hex
            event.shiftKey -> selectedHex += hex
            // Ctrl click calls context menu.
            event.ctrlKey -> Unit
            else -> selectedHex.apply {
                clear()
                add(hex)
            }
        }
        when {
            event.shiftKey && event.altKey -> {
                spoil(hex)
                offscreenCanvasPond = null
            }
            event.altKey -> {
                pond.clear()
                spoil(hex)
                offscreenCanvasPond = null
            }
        }

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
