import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class Hex(val q: Int = 0, val r: Int = 0, val s: Int = 0 - q - r) {
    init {
        require(q + r + s == 0) { "q=$q, r=$r and s=$s must add up to 0" }
    }

    val length get() = ((q.absoluteValue + r.absoluteValue + s.absoluteValue) / 2f).roundToInt()

    operator fun plus(other: Hex) = Hex(q + other.q, r + other.r, s + other.s)
    operator fun minus(other: Hex) = Hex(q - other.q, r - other.r, s - other.s)

    fun distance(other: Hex) = (this - other).length

    fun direction(direction: Int): Hex {
        require(direction in (0..5))
        return DIRECTIONS[direction]
    }

    fun neighbor(direction: Int): Hex = this + direction(direction)

    fun neighbors() = DIRECTIONS.indices.map { neighbor(it) }

    fun asVector2() = Vector2(q.toDouble(), r.toDouble())

    companion object {
        val DIRECTIONS = listOf(
            Hex(1, 0, -1), Hex(1, -1, 0), Hex(0, -1, 1),
            Hex(-1, 0, 1), Hex(-1, 1, 0), Hex(0, 1, -1)
        )
    }
}

data class Vector2(val x: Double, val y: Double) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(other: Vector2) = Vector2(x * other.x, y * other.y)
    operator fun div(other: Vector2) = Vector2(x / other.x, y / other.y)
}

data class Matrix2(val m00: Double, val m10: Double, val m01: Double, val m11: Double) {
    operator fun times(vector: Vector2) = Vector2(
        m00 * vector.x + m10 * vector.y,
        m01 * vector.x + m11 * vector.y
    )
}

sealed class Orientation(
    f0: Double, f1: Double, f2: Double, f3: Double,
    b0: Double, b1: Double, b2: Double, b3: Double,
    val start_angle: Double
) {
    companion object {
        val SQRT3 = kotlin.math.sqrt(3.0)
    }

    val forward = Matrix2(f0, f1, f2, f3)
    val backward = Matrix2(b0, b1, b2, b3)

    object Pointy : Orientation(
        SQRT3, SQRT3 / 2.0, 0.0, 3.0 / 2.0,
        SQRT3 / 3.0, -1.0 / 3.0, 0.0, 2.0 / 3.0,
        0.5
    )

    object Flat : Orientation(
        3.0 / 2.0, 0.0, SQRT3 / 2.0, SQRT3,
        2.0 / 3.0, 0.0, -1.0 / 3.0, SQRT3 / 3.0,
        0.0
    )
}

data class Point(val x: Double, val y: Double) {
    constructor(vector: Vector2) : this(vector.x, vector.y)

    fun asVector2() = Vector2(x, y)
}

data class Size(val x: Double, val y: Double) {
    fun asVector2() = Vector2(x, y)
}


data class FractionalHex(val q: Double = 0.0, val r: Double = 0.0, val s: Double = -q - r) {
    fun round(): Hex {
        val diff = { d: Double -> (d - d.roundToInt()).absoluteValue }

        val qi = q.roundToInt()
        val ri = r.roundToInt()
        val si = s.roundToInt()

        val qd = diff(q)
        val rd = diff(r)
        val sd = diff(s)

        return when {
            qd > rd && qd > sd -> Hex(-ri - si, ri, si)
            rd > sd -> Hex(qi, -qi - si, si)
            else -> Hex(qi, ri, -qi - ri)
        }
    }
}

data class HexPolygon(val center: Point, val corners: List<Point>)

data class Layout(val orientation: Orientation, val size: Size, val origin: Point) {
    fun toPixel(hex: Hex): Point =
        Point(orientation.forward * hex.asVector2() * size.asVector2() + origin.asVector2())

    fun toHex(point: Point): FractionalHex =
        ((point.asVector2() - origin.asVector2()) / size.asVector2()).let {
            orientation.backward * it
        }.let {
            FractionalHex(it.x, it.y, -it.x - it.y)
        }

    private fun cornerOffset(corner: Int): Point {
        val angle = 2.0 * kotlin.math.PI * (orientation.start_angle + corner) / 6.0
        return Point(size.asVector2() * Vector2(cos(angle), sin(angle)))
    }

    fun polygonCorners(hex: Hex) = toPixel(hex).let { center ->
        HexPolygon(center,
            (0..5).map { center.asVector2() + cornerOffset(it).asVector2() }.map {
                Point(it)
            })
    }
}


