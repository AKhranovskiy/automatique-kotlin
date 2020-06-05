import layers.CanvasLayer
import org.w3c.dom.CanvasRenderingContext2D

class LayerContainer(vararg layers: CanvasLayer) {
    private val container = layers.toMutableList()

    fun resize(size: Size) {
        container.forEach { it.size = size }
    }

    fun draw(destination: CanvasRenderingContext2D) = container.forEach { it.draw(destination) }

    fun invalidate() = container.forEach { it.invalidate() }

    fun add(layer: CanvasLayer) {
        require(!container.contains(layer)) { "Layer is already added." }
        container += layer
        layer.invalidate()
    }

    fun remove(layer: CanvasLayer) {
        require(container.contains(layer)) { "Layer is not added." }
        container -= layer
        layer.invalidate()
    }
}