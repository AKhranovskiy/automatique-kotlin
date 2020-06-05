import org.w3c.dom.Path2D

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