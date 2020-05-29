import org.w3c.dom.CanvasRenderingContext2D

class AnimatedHexGrid : Animator {
    private var angle = 0

    override fun draw(ctx: CanvasRenderingContext2D) {
//        val orientation = when (props.angle % 2) {
//            0 -> Orientation.Pointy
//            else -> Orientation.Flat
//        }
//
//        val layout = Layout(orientation, Size(20.0, 20.0), Point(0.0, 0.0))
//
        val width = ctx.canvas.width.toDouble()
        val height = ctx.canvas.height.toDouble()

        ctx.beginPath()
        ctx.clearRect(0.0, 0.0, width, height)
        ctx.translate(width / 2, height / 2)
        ctx.rotate((++angle * kotlin.math.PI) / 180)
        ctx.fillStyle = "#4397AC"
        ctx.fillRect(
            -width / 4,
            -height / 4,
            width / 2,
            height / 2
        )
//
//            ctx.clearRect(0.0, 0.0, width, height)
//
//            ctx.beginPath()
//
//            (-50..50).forEach { q ->
//                (-50..50).forEach { r ->
//                    layout.polygon_corners(Hex(q, r)).let { (_, points) ->
//                        ctx.moveTo(points[0].x, points[0].y)
//                        points.drop(1).forEach {
//                            ctx.lineTo(it.x, it.y)
//                        }
//                        ctx.lineTo(points[0].x, points[0].y)
//                    }
//                }
//            }
//
//            ctx.closePath()
//            ctx.stroke()
    }
}