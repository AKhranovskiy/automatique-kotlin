import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.RenderingContext
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.canvas
import react.setState
import kotlin.browser.window

external interface AnimationProps : RProps

external interface AnimationState : RState {
    var angle: Int
}

class Animation : RComponent<AnimationProps, AnimationState>() {
    private var rAF: Int = 0

    override fun AnimationState.init() {
        angle = 0
    }

    override fun componentDidMount() {
        rAF = window.requestAnimationFrame(::updateAnimationState)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun updateAnimationState(time: Double) {
        setState {
            angle += 1
        }
        rAF = window.requestAnimationFrame(::updateAnimationState)
    }

    override fun componentWillUnmount() {
        window.cancelAnimationFrame(rAF)
    }

    override fun RBuilder.render() {
        child(Canvas::class) {
            attrs.angle = state.angle
        }
    }
}


external interface CanvasProps : RProps {
    var angle: Int
}

class Canvas : RComponent<CanvasProps, RState>() {
    private var context: CanvasRenderingContext2D? = null

    private fun saveContext(element: dynamic) {
        context = element as? CanvasRenderingContext2D?
    }

    override fun componentDidUpdate(prevProps: CanvasProps, prevState: RState, snapshot: Any) {
        val width = context?.canvas?.width?.toDouble() ?: return
        val height = context?.canvas?.height?.toDouble() ?: return

        context?.let { ctx ->
            ctx.save()

            ctx.beginPath()
            ctx.clearRect(0.0, 0.0, width, height)
            ctx.translate(width / 2, height / 2)
            ctx.rotate(props.angle * kotlin.math.PI / 180)
            ctx.fillStyle = "#4397AC"
            ctx.fillRect(-width / 4, -height / 4, width / 2, height / 2)

            ctx.restore()
        }
    }

    override fun RBuilder.render() {
        child(PureCanvas::class) {
            attrs.contextRef = ::saveContext
        }
    }
}

external interface PureCanvasProps : RProps {
    var contextRef: (ctx: RenderingContext?) -> Unit
}

class PureCanvas : RComponent<PureCanvasProps, RState>() {
    override fun RBuilder.render() {
        canvas {
            attrs {
                width = 1270.toString()
                height = 940.toString()
            }
            ref { element ->
                (element as? HTMLCanvasElement)?.let {
                    props.contextRef(it.getContext("2d"))
                }

            }

        }
    }
}
