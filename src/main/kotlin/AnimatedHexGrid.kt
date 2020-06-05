import controllers.SelectionController
import layers.CoordinatesLayer
import layers.GridLayer
import layers.SelectionLayer
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.MIDDLE
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.createElement
import kotlin.math.absoluteValue

class AnimatedHexGrid : Animator {
    private val hexMap = HexMap()
    private val gridLayer = GridLayer(hexMap)
    private val coordinatesLayer = CoordinatesLayer(hexMap)

    private val selectionLayer = SelectionLayer(hexMap)
    private val selectionController =
        SelectionController { selectionLayer.selection = it }

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
                    fill(PathUtil.toPolygon(points))
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

//        offscreenCanvasPond?.copyOn(ctx)
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
