package layers

import Hex
import HexMap
import PathUtil
import distinctObservable
import org.w3c.dom.Path2D

class SelectionLayer(private val hexMap: HexMap) : CanvasLayer() {

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