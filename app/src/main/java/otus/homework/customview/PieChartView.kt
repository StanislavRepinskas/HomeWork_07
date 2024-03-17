package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sectorItems: List<PieChartSector> = emptyList()

    private val sectorPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val innerCirclePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    private val viewRect = RectF()
    private val chartOuterRect = RectF()
    private val chartInnerRect = RectF()

    private var onSectorClick: ((name: String) -> (Unit))? = null

    init {
        if (isInEditMode) {
            setData(getDataForEditMode())
        }
    }

    fun setData(dataItems: List<PieChartSectorData>) {
        val total = dataItems.sumBy { it.value }
        var startAngle = -90f
        var sweepAngle = 0f

        val chartItems = mutableListOf<PieChartSector>()
        dataItems.forEach {
            sweepAngle = 360f * (it.value.toFloat() / total)

            chartItems += PieChartSector(
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                data = it
            )

            startAngle += sweepAngle
        }

        this.sectorItems = chartItems

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        viewRect.apply {
            left = paddingStart.toFloat()
            top = paddingTop.toFloat()
            right = (requestedWidth - paddingEnd).toFloat()
            bottom = (requestedHeight - paddingBottom).toFloat()
        }

        val viewWidth = viewRect.width()
        val viewHeight = viewRect.height()
        val chartViewSize = min(viewWidth, viewHeight)
        val wDiff = ((viewWidth - chartViewSize) / 2)
        val hDiff = ((viewHeight - chartViewSize) / 2)

        chartOuterRect.apply {
            left = viewRect.left + wDiff
            top = viewRect.top + hDiff
            right = viewRect.right - wDiff
            bottom = viewRect.bottom - hDiff
        }

        val chartArcSize = (chartViewSize / 100) * 15
        chartInnerRect.apply {
            left = chartOuterRect.left + chartArcSize
            top = chartOuterRect.top + chartArcSize
            right = chartOuterRect.right - chartArcSize
            bottom = chartOuterRect.bottom - chartArcSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        sectorItems.forEach {
            sectorPaint.color = it.data.color
            canvas.drawArc(chartOuterRect, it.startAngle, it.sweepAngle, true, sectorPaint)
        }
        canvas.drawOval(chartInnerRect, innerCirclePaint)
    }

    fun setOnSectorClick(onSectorClick: ((name: String) -> (Unit))?) {
        this.onSectorClick = onSectorClick
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val touchY = event.y

            val isInsideOuterRadius = isInsideRadius(chartOuterRect, touchX, touchY)
            val isInsideInnerRadius = isInsideRadius(chartInnerRect, touchX, touchY)

            if (isInsideOuterRadius && !isInsideInnerRadius) {
                val cx: Float = chartOuterRect.centerX()
                val cy: Float = chartOuterRect.centerY()

                val theta = Math.atan2(
                    (cx - touchX).toDouble(),
                    (touchY - cy).toDouble()
                ) * 180.0 / Math.PI

                // theta для левой половины круга (снизу вверх) 0:180, для правой половины -0:-180
                // Отрисовка секторов от -90f до 270f
                val angle = theta + 90.0

                val sector = sectorItems.firstOrNull {
                    angle in it.startAngle..it.startAngle + it.sweepAngle
                }
                if (sector != null) {
                    onSectorClick?.invoke(sector.data.name)
                }
            }
        }
        return true
    }

    private fun isInsideRadius(rect: RectF, xTouch: Float, yTouch: Float): Boolean {
        val radius = rect.width() / 2f

        val distanceX = xTouch - rect.centerX()
        val distanceY = yTouch - rect.centerY()

        return (distanceX * distanceX) + (distanceY * distanceY) <= radius * radius
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()

        return PieChartSaveState(
            superState = superState,
            data = sectorItems.map {
                it.data
            }
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is PieChartSaveState) {
            super.onRestoreInstanceState(state.superState)
            setData(state.data)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private data class PieChartSector(
        val startAngle: Float,
        val sweepAngle: Float,
        val data: PieChartSectorData
    )

    @Parcelize
    private data class PieChartSaveState(
        val superState: Parcelable?,
        val data: List<PieChartSectorData>
    ) : Parcelable

    companion object {
        private fun getDataForEditMode(): List<PieChartSectorData> {
            return listOf(
                PieChartSectorData(
                    value = 15,
                    name = "Health",
                    color = Color.RED
                ),
                PieChartSectorData(
                    value = 55,
                    name = "Products",
                    color = Color.BLUE
                ),
                PieChartSectorData(
                    value = 15,
                    name = "Transport",
                    color = Color.MAGENTA
                )
            )
        }
    }
}

@Parcelize
data class PieChartSectorData(
    val value: Int,
    val name: String,
    @ColorInt val color: Int
) : Parcelable

