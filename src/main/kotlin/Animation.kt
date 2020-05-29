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

interface Animator {
    fun draw(ctx: CanvasRenderingContext2D)
}

external interface AnimationProps : RProps {
    var animator: Animator
}

external interface AnimationState : RState {
    var key: Double
}

class Animation : RComponent<AnimationProps, AnimationState>() {
    private var rAF: Int = 0

    override fun componentDidMount() {
        rAF = window.requestAnimationFrame(::updateAnimationState)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun updateAnimationState(time: Double) {
        rAF = window.requestAnimationFrame(::updateAnimationState)
        setState {
            key = time
        }

    }

    override fun componentWillUnmount() {
        window.cancelAnimationFrame(rAF)
    }

    override fun RBuilder.render() {
        child(Canvas::class) {
            attrs.animator = props.animator
        }
    }
}

external interface CanvasProps : RProps {
    var animator: Animator
}

class Canvas : RComponent<CanvasProps, RState>() {
    private var context: CanvasRenderingContext2D? = null

    private fun saveContext(element: dynamic) {
        context = element as? CanvasRenderingContext2D?
    }

    override fun componentDidUpdate(prevProps: CanvasProps, prevState: RState, snapshot: Any) {
        context?.let { ctx ->
            ctx.save()
            props.animator.draw(ctx)
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
