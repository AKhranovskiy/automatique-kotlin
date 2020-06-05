package layers

import Rect
import Size
import distinctObservable
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.dom.createElement

private fun HTMLCanvasElement.copyOn(ctx: CanvasRenderingContext2D) =
    ctx.drawImage(this, 0.0, 0.0)

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