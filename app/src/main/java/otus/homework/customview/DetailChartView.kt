package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

class DetailChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: ChartDetailData = ChartDetailData(items = emptyList(), color = 0)

    private val defaultItemWidth = context.toPx(24)
    private var itemWidth = defaultItemWidth
    private val itemRect = RectF()
    private val chartRect = RectF()
    private var maxAmount: Float = 0F
    private var amountGroups: List<String> = emptyList()

    private val dayTextBound = Rect()
    private val dayTextBoundPaddingTop = context.toPx(4)
    private var dayTextHeight = 0

    private val amountTextBound = Rect()
    private val amountTextPaddingEnd = context.toPx(4)

    private val itemFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.RED
        isAntiAlias = true
    }

    private val itemStrokePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 1f
    }

    private val paintStroke = Paint().apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        color = Color.GRAY
        isAntiAlias = true
        strokeWidth = 1f
    }

    private val textPaint = Paint().apply {
        color = Color.GRAY
        isAntiAlias = true
        textSize = context.toPx(8)
    }

    init {
        if (isInEditMode) {
            setData(getDataForEditMode())
        }
    }

    fun setData(data: ChartDetailData) {
        this.data = data
        maxAmount = if (data.items.isNotEmpty()) data.items.maxOf { it.amount } else 0f

        amountGroups = if (maxAmount > 0) {
            listOf(
                maxAmount.toInt().toString(),
                ((maxAmount / 100f) * 75).toInt().toString(),
                ((maxAmount / 100f) * 50).toInt().toString(),
                ((maxAmount / 100f) * 25).toInt().toString()
            )
        } else {
            emptyList()
        }

        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        textPaint.getTextBounds("1", 0, 1, dayTextBound)
        dayTextHeight = dayTextBound.height()

        val maxAmountText = maxAmount.toInt().toString()
        textPaint.getTextBounds(maxAmountText, 0, maxAmountText.length, amountTextBound)

        val requestedHeight = MeasureSpec.getSize(heightMeasureSpec)
        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val requestedWidthMode = MeasureSpec.getMode(widthMeasureSpec)

        val chartPaddingLeft =
            paddingStart.toFloat() + amountTextBound.width() + amountTextPaddingEnd
        val chartPaddingRight = paddingEnd.toFloat()
        val chartPaddingBottom = paddingBottom.toFloat() + (dayTextHeight + dayTextBoundPaddingTop)
        chartRect.left = chartPaddingLeft
        chartRect.top = paddingTop.toFloat()
        chartRect.right = requestedWidth - chartPaddingRight
        chartRect.bottom = requestedHeight - chartPaddingBottom

        val items = data.items

        var resultWidth = 0
        when (requestedWidthMode) {
            MeasureSpec.EXACTLY -> {
                itemWidth = chartRect.width() / items.size
                resultWidth = requestedWidth.toInt()
            }

            MeasureSpec.AT_MOST -> {
                val needWidth = defaultItemWidth * items.size
                if (needWidth > chartRect.width()) {
                    itemWidth = chartRect.width() / items.size
                    resultWidth = requestedWidth.toInt()
                } else {
                    itemWidth = defaultItemWidth
                    chartRect.right = chartRect.left + needWidth
                    resultWidth = (chartPaddingLeft + needWidth + chartPaddingRight).toInt()
                }
            }

            else -> {
                val needWidth = defaultItemWidth * items.size
                chartRect.right = chartRect.left + needWidth
                itemWidth = defaultItemWidth
                resultWidth = (chartPaddingLeft + needWidth + chartPaddingRight).toInt()
            }
        }

        setMeasuredDimension(resultWidth, requestedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawAmountGroups(canvas)
        drawChart(canvas)
    }

    private fun drawChart(canvas: Canvas) {
        itemRect.left = chartRect.left
        itemRect.top = 0f
        itemRect.right = 0f
        itemRect.bottom = chartRect.bottom

        for ((index, item) in data.items.withIndex()) {
            itemRect.right = itemRect.left + itemWidth

            val percent = item.amount / maxAmount
            itemRect.top =
                itemRect.bottom - (((chartRect.height())) * percent)

            canvas.drawLine(
                itemRect.left,
                chartRect.top,
                itemRect.left,
                chartRect.bottom,
                paintStroke
            )
            if (index == data.items.size - 1) {
                canvas.drawLine(
                    itemRect.right,
                    chartRect.top,
                    itemRect.right,
                    chartRect.bottom,
                    paintStroke
                )
            }
            canvas.drawRect(itemRect, itemFillPaint)
            canvas.drawRect(itemRect, itemStrokePaint)

            textPaint.getTextBounds(item.day, 0, item.day.length, dayTextBound)
            canvas.drawText(
                item.day,
                itemRect.left + (itemWidth / 2) - (dayTextBound.width() / 2),
                chartRect.bottom + dayTextHeight + dayTextBoundPaddingTop,
                textPaint
            )

            itemRect.left = itemRect.right
        }
    }

    private fun drawAmountGroups(canvas: Canvas) {
        for ((index, amount) in amountGroups.withIndex()) {
            val textStartY =
                chartRect.top + ((chartRect.height() / amountGroups.size) * index)
            canvas.drawText(
                amount,
                chartRect.left - amountTextBound.width() - amountTextPaddingEnd,
                textStartY + amountTextBound.height(),
                textPaint
            )
            canvas.drawLine(
                chartRect.left,
                textStartY,
                chartRect.right,
                textStartY,
                paintStroke
            )
        }
    }

    private fun getDataForEditMode(): ChartDetailData {
        val items = mutableListOf<ChartDetailItem>()
        var amount = 25f
        for (i in 1..30) {
            items += ChartDetailItem(
                amount = amount,
                day = i.toString()
            )
            amount += 100f
        }
        return ChartDetailData(items = items, color = Color.RED)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        return SaveState(
            superState = superState,
            data = data
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SaveState) {
            super.onRestoreInstanceState(state.superState)
            setData(state.data)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    @Parcelize
    private data class SaveState(
        val superState: Parcelable?,
        val data: ChartDetailData
    ) : Parcelable
}

@Parcelize
data class ChartDetailData(
    val items: List<ChartDetailItem>,
    @ColorInt val color: Int
) : Parcelable

@Parcelize
data class ChartDetailItem(
    val amount: Float,
    val day: String
) : Parcelable

fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    resources.displayMetrics
)