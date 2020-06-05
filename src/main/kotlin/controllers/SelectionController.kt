package controllers

import Hex
import org.w3c.dom.events.MouseEvent

class SelectionController(private val onSelectionChanged: (selection: Set<Hex>) -> Unit) {
    private val selection = mutableSetOf<Hex>()

    fun onClick(event: MouseEvent, hex: Hex) {
        when {
            // Ctrl click calls context menu.
            event.ctrlKey -> Unit
            event.shiftKey -> when (hex) {
                in selection -> selection -= hex
                else -> selection += hex
            }.also {
                onSelectionChanged(selection.toSet())
            }
            else -> selection.apply {
                clear()
                add(hex)
            }.also {
                onSelectionChanged(selection.toSet())
            }
        }
    }
}