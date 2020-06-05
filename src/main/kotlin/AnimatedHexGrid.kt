import controllers.PondController
import controllers.SelectionController
import layers.CoordinatesLayer
import layers.GridLayer
import layers.PondLayer
import layers.SelectionLayer
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.window
import kotlin.math.absoluteValue

class AnimatedHexGrid : Animator {
    private val hexMap = HexMap()

    private val gridLayer = GridLayer(hexMap)
    private val coordinatesLayer = CoordinatesLayer(hexMap)
    private val pondLayer = PondLayer(hexMap)
    private val selectionLayer = SelectionLayer(hexMap)

    private val layerContainer =
        LayerContainer(pondLayer, gridLayer, coordinatesLayer, selectionLayer)

    private val pondController =
        PondController { pondLayer.pond = it }
    private val selectionController = SelectionController { selectionLayer.selection = it }

    // TODO FPS counter
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
    /// -----

    private var canvasSize: Size by distinctObservable(Size(0.0, 0.0)) { _, _ ->
        hexMap.size = canvasSize
        layerContainer.resize(canvasSize)
    }

    private val CanvasRenderingContext2D.size: Size
        get() = Size(canvas.width.toDouble(), canvas.height.toDouble())

    override fun draw(ctx: CanvasRenderingContext2D) {
        canvasSize = ctx.size

        ctx.fillStyle = "#fff"
        ctx.fillRect(0.0, 0.0, canvasSize.x, canvasSize.y)

        layerContainer.draw(ctx)

        drawFps(ctx)
    }

    fun onEvent(event: Event) = when (event.type) {
        "click" -> onEventClick(event as MouseEvent)
        else -> Unit
    }

    private fun onEventClick(event: MouseEvent) = event
        .let { Point(it.offsetX, it.offsetY) }
        .let { hexMap.layout.toHex(it).round() }.let { hex ->
            selectionController.onClick(event, hex)
            pondController.onClick(event, hex)
        }
}
