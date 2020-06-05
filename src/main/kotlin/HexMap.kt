import kotlin.math.sqrt

data class HexConfig(
    val orientation: Orientation = Orientation.Flat,
    val hexSize: Size = Size(40.0, 40.0)
)typealias HexGrid = Map<Hex, HexPolygon>

class HexMap {
    var config: HexConfig by distinctObservable(HexConfig()) { _, _ -> updateLayout() }

    var size: Size by distinctObservable(Size(0.0, 0.0), { _, _ ->
        val width = config.hexSize.x * 2
        val horizontalSpacing = width * 3 / 4
        val minQ = 0
        val maxQ = (size.x / horizontalSpacing).toInt()

        val height = config.hexSize.y * sqrt(3.0f)

        // Every q-column takes half of r-column.
        val minR = -maxQ / 2
        val maxR = (size.y / height).toInt()

        hexes = ((minQ..maxQ) x (minR..maxR)).map { (q, r) -> Hex(q, r) }
    })

    var origin: Point by distinctObservable(Point(0.0, 0.0)) { _, _ -> updateLayout() }

    var layout: Layout by distinctObservable(Layout(config.orientation, config.hexSize, origin))
    { _, _ -> updateGrid() }
        private set

    var hexes: List<Hex> by distinctObservable(emptyList()) { _, _ -> updateGrid() }
        private set

    var grid: HexGrid = emptyMap()
        private set

    private fun updateLayout() {
        layout = Layout(config.orientation, config.hexSize, origin)
    }

    private fun updateGrid() {
        grid = hexes.map { it to layout.polygonCorners(it) }.toMap()
    }
}