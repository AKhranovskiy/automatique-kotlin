package layers

import HexMap
import PathUtil
import org.w3c.dom.Path2D

class GridLayer(private val hexMap: HexMap, private val showCenter: Boolean = false) :
    CanvasLayer() {
    override fun onDraw() {
        Path2D().apply {
            hexMap.grid.values.filter { (center, _) -> rect.contains(center) }.let {
                it.forEach { (_, corners) ->
                    addPath(
                        PathUtil.toPolygon(
                            corners
                        )
                    )
                }
                if (showCenter) it.forEach { (center, _) ->
                    addPath(
                        PathUtil.toCircle(
                            center
                        )
                    )
                }
            }.let {
                ctx.stroke(this)
            }
        }
    }
}