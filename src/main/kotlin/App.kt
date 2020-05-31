import kotlinx.css.BorderStyle
import kotlinx.css.Color
import kotlinx.css.Display
import kotlinx.css.backgroundColor
import kotlinx.css.display
import kotlinx.css.em
import kotlinx.css.flex
import kotlinx.css.height
import kotlinx.css.properties.borderRight
import kotlinx.css.px
import kotlinx.css.vh
import kotlinx.css.vw
import kotlinx.css.width
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.p
import react.dom.render
import styled.css
import styled.styledDiv
import kotlin.browser.document

fun renderReactApp() {
    render(document.getElementById("root")) {
        child(MainComponent::class) {}
    }
}

class MainComponent : RComponent<RProps, RState>() {
    private val animator = AnimatedHexGrid()

    override fun RBuilder.render() {
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
                attrs.onClickFunction = { animator.onEvent(it.asDynamic().nativeEvent as Event) }
                child(Animation::class) {
                    this.attrs.animator = animator
                }
            }
        }
    }
}