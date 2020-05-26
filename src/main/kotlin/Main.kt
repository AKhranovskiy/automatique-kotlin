import kotlinx.css.BorderStyle
import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.backgroundColor
import kotlinx.css.properties.borderRight
import kotlinx.css.display
import kotlinx.css.em
import kotlinx.css.height
import kotlinx.css.vh
import kotlinx.css.vw
import kotlinx.css.width
import kotlinx.css.flex
import kotlinx.css.pct
import kotlinx.css.properties.border
import kotlinx.css.properties.borderLeft
import kotlinx.css.px
import kotlin.browser.document
import react.dom.canvas
import react.dom.p
import react.dom.render
import styled.css
import styled.styledCanvas
import styled.styledDiv

fun main() {
    render(document.getElementById("root")) {
        styledDiv {
            css {
                width = 100.vw
                height = 100.vh
                display = Display.flex
            }

            styledDiv {
                css {
                    backgroundColor = Color.lightGreen
                    borderRight(1.px, BorderStyle.solid, Color.gray)
                    flex(0.0, 0.0, 25.em)
                }
                p { +"Control panel" }
            }
            styledDiv {
                css {
                    backgroundColor = Color.lemonChiffon
                    flex(flexGrow = 1.0)
                }
                styledCanvas {
                    css {
                        width = 100.pct
                        height = 99.pct
                    }
                }
            }
        }
    }
}
