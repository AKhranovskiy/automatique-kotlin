package controllers

import Hex
import org.w3c.dom.events.MouseEvent

class PondController(private val onUpdate: (pond: Map<Hex, Int>) -> Unit) {
    private val pond = mutableMapOf<Hex, Int>()

    fun onClick(event: MouseEvent, hex: Hex) {
        when {
            event.shiftKey && event.altKey -> {
                spoil(hex)

                onUpdate(pond.toMap())
            }
            event.altKey -> {
                pond.clear()
                spoil(hex)

                onUpdate(pond.toMap())
            }
        }
    }

    private fun spoil(root: Hex) {
        val newPond = mutableMapOf<Hex, Int>()
        newPond[root] = 100
        (0..10).forEach { _ ->
            newPond += newPond.flatMap { (hex, power) ->
                buildPond(
                    hex,
                    (power - 5).coerceAtLeast(0)
                )
            }.filterNot { it.first in newPond }
        }

        pond += newPond.filterNot { it.key in pond }
    }

    private fun buildPond(root: Hex, power: Int) =
        root.neighbors().map { it to (0..power).random() }.filter { it.second > 0 }
}