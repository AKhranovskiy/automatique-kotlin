import kotlin.browser.document

fun main() {
    if (document.body != null) {
        renderReactApp()
    } else {
        document.addEventListener("DOMContentLoaded", { renderReactApp() })
    }
}



