package layers

import Hex
import HexMap
import PathUtil
import distinctObservable
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.MIDDLE

class PondLayer(private val hexMap: HexMap) : CanvasLayer() {
    var pond: Map<Hex, Int> by distinctObservable(emptyMap()) { _, _ -> invalidate() }

    override fun onDraw() {
        pond.mapNotNull { (hex, power) ->
            hexMap.grid[hex]?.corners?.let { it to power.coerceIn(0, 100) / 100.0 }
        }.forEach { (points, alpha) ->
            ctx.fillStyle = "rgb(255, 0, 0, $alpha)"
            ctx.fill(PathUtil.toPolygon(points))
        }

        pond.mapNotNull { (hex, power) ->
            hexMap.grid[hex]?.center?.let { it to power.toString() }
        }.forEach { (center, t) ->
            ctx.fillStyle = "black"
            ctx.font = "bold ${hexMap.config.hexSize.x / 3}pt monospace"
            ctx.textBaseline = CanvasTextBaseline.MIDDLE

            val tx = ctx.measureText(t)
            ctx.fillText(t, center.x - tx.width / 2, center.y + 5)
        }
    }

}