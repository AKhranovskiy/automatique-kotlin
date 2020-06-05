package layers

import Hex
import HexMap
import Point
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.TOP

class CoordinatesLayer(private val hexMap: HexMap) : CanvasLayer() {
    override fun onDraw() {
        ctx.textBaseline = CanvasTextBaseline.TOP
        ctx.font = "bold ${hexMap.config.hexSize.x / 3}pt monospace"

        hexMap.grid
            .filter { (_, polygon) -> rect.contains(polygon.center) }
            .keys.forEach { drawCoordinates(it) }
    }

    private fun drawCoordinates(hex: Hex) {
        val center = hexMap.layout.toPixel(hex)
        drawQ(center, hex)
        drawR(center, hex)
        drawS(center, hex)
    }

    private fun drawQ(center: Point, hex: Hex) {
        ctx.fillStyle = "green"
        ctx.fillText(
            formatHexCoordinate("q", hex.q),
            center.x - hexMap.config.hexSize.x * 5 / 8,
            center.y - hexMap.config.hexSize.y / 2
        )
    }

    private fun drawR(center: Point, hex: Hex) {
        val r = formatHexCoordinate("r", hex.r)
        val rx = ctx.measureText(r)

        ctx.fillStyle = "blue"
        ctx.fillText(
            r,
            center.x + hexMap.config.hexSize.x * 5 / 8 - rx.width,
            center.y - hexMap.config.hexSize.y / 2
        )
    }

    private fun drawS(center: Point, hex: Hex) {
        val s = formatHexCoordinate("s", hex.s)
        val sx = ctx.measureText(s)
        ctx.fillStyle = "red"
        ctx.fillText(
            s,
            center.x - sx.width / 2,
            center.y + hexMap.config.hexSize.y / 4
        )
    }

    private fun formatHexCoordinate(name: String, value: Int): String = when (value) {
        0 -> name
        else -> value.toSignedString()
    }

    private fun Int.toSignedString(): String = if (this > 0) "+$this" else "$this"
}