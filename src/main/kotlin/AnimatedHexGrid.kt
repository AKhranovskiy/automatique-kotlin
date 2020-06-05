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
            ctx.save()
            onDraw()
            ctx.restore()
            isInvalidated = false
        }
        canvas.copyOn(destination)
    }

    protected abstract fun onDraw()
}

class PathUtil {
    companion object {
        fun toPolygon(points: List<Point>) = Path2D().apply {
            if (points.isNotEmpty()) {
                val start = points.first()
                moveTo(start.x, start.y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(start.x, start.y)
            }
        }

        fun toCircle(point: Point, radius: Int = 1) = Path2D().apply {
            moveTo(point.x - radius, point.y - radius)
            arc(point.x, point.y, radius.toDouble(), 0.0, kotlin.math.PI * 2.0)
        }
    }
}

class GridLayer(private val hexMap: HexMap, private val showCenter: Boolean = false) :
    CanvasLayer() {
    override fun onDraw() {
        Path2D().apply {
            hexMap.grid.values.filter { (center, _) -> rect.contains(center) }.let {
                it.forEach { (_, corners) -> addPath(PathUtil.toPolygon(corners)) }
                if (showCenter) it.forEach { (center, _) -> addPath(PathUtil.toCircle(center)) }
            }.let {
                ctx.stroke(this)
            }
        }
    }
}

class CoordinatesLayer(private val hexMap: HexMap) : CanvasLayer() {
    override fun onDraw() {
        ctx.textBaseline = CanvasTextBaseline.TOP
        ctx.font = "bold ${hexMap.config.hexSize.x / 3}pt monospace"

        hexMap.grid
            .filter { (_, polygon) -> rect.contains(polygon.center) }
            .keys.forEach { drawCoordinates(it) }
    }

    private fun drawCoordinates(hex: Hex) {
        val center = hexMap.layout.toPixel(hex)
        drawQ(center, hex)
        drawR(center, hex)
        drawS(center, hex)
    }

    private fun drawQ(center: Point, hex: Hex) {
        ctx.fillStyle = "green"
        ctx.fillText(
            formatHexCoordinate("q", hex.q),
            center.x - hexMap.config.hexSize.x * 5 / 8,
            center.y - hexMap.config.hexSize.y / 2
        )
    }

    private fun drawR(center: Point, hex: Hex) {
        val r = formatHexCoordinate("r", hex.r)
        val rx = ctx.measureText(r)

        ctx.fillStyle = "blue"
        ctx.fillText(
            r,
            center.x + hexMap.config.hexSize.x * 5 / 8 - rx.width,
            center.y - hexMap.config.hexSize.y / 2
        )
    }

    private fun drawS(center: Point, hex: Hex) {
        val s = formatHexCoordinate("s", hex.s)
        val sx = ctx.measureText(s)
        ctx.fillStyle = "red"
        ctx.fillText(
            s,
            center.x - sx.width / 2,
            center.y + hexMap.config.hexSize.y / 4
        )
    }

    private fun formatHexCoordinate(name: String, value: Int): String = when (value) {
        0 -> name
        else -> value.toSignedString()
    }

    private fun Int.toSignedString(): String = if (this > 0) "+$this" else "$this"
}

class SelectionLayer(private val hexMap: HexMap) :
    CanvasLayer() {

    var selection: Set<Hex> by distinctObservable(emptySet()) { _, _ -> invalidate() }

    override fun onDraw() {
        ctx.strokeStyle = "lightgreen"
        ctx.lineWidth = 4.0

        Path2D().apply {
            selection.mapNotNull { hexMap.grid[it]?.corners }.forEach {
                addPath(PathUtil.toPolygon(it))
            }
        }.let {
            ctx.stroke(it)
        }
    }
}

class LayerContainer(vararg layers: CanvasLayer) {
    private val container = layers.toMutableList()

    fun resize(size: Size) {
        container.forEach { it.size = size }
    }

    fun draw(destination: CanvasRenderingContext2D) = container.forEach { it.draw(destination) }

    fun invalidate() = container.forEach { it.invalidate() }

    fun add(layer: CanvasLayer) {
        require(!container.contains(layer)) { "Layer is already added." }
        container += layer
        layer.invalidate()
    }

    fun remove(layer: CanvasLayer) {
        require(container.contains(layer)) { "Layer is not added." }
        container -= layer
        layer.invalidate()
    }
}

class SelectionController(private val onSelectionChanged: (selection: Set<Hex>) -> Unit) {
    private val selection = mutableSetOf<Hex>()

    fun onClick(event: MouseEvent, hex: Hex) {
        when {
            // Ctrl click calls context menu.
            event.ctrlKey -> Unit
            event.shiftKey -> when (hex) {
                in selection -> selection -= hex
                else -> selection += hex
            }.also {
                onSelectionChanged(selection)
            }
            else -> selection.apply {
                clear()
                add(hex)
            }.also {
                onSelectionChanged(selection)
            }
        }
    }
}

class AnimatedHexGrid : Animator {
    private val hexMap = HexMap()
    private val gridLayer = GridLayer(hexMap)
    private val coordinatesLayer = CoordinatesLayer(hexMap)

    private val selectionLayer = SelectionLayer(hexMap)
    private val selectionController = SelectionController { selectionLayer.selection = it }

    private val layerContainer = LayerContainer(gridLayer, coordinatesLayer, selectionLayer)

    private var timestamps = mutableListOf<Double>()
    private val timestampsLength = 100

    private fun Double.equalsDelta(other: Double) =
        (this / other - 1.0).absoluteValue < 0.000001

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
        layerContainer.resize(canvasSize)

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
        layerContainer.draw(ctx)

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

    private fun onEventClick(event: MouseEvent) {
        val hex = event
            .let { Point(it.offsetX, it.offsetY) }
            .let { hexMap.layout.toHex(it).round() }

        selectionController.onClick(event, hex)

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
