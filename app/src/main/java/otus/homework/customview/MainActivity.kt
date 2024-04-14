package otus.homework.customview

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.lang.reflect.Type
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var payloads: List<PayloadItem> = emptyList()

    private lateinit var pieChartView: PieChartView
    private lateinit var detailChartView: DetailChartView

    private var isShowDetail: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isShowDetail = savedInstanceState?.getBoolean(IS_SHOW_DETAIL_KEY) ?: false
        payloads = getPayloads()
        setContentView(R.layout.activity_main)

        pieChartView = findViewById(R.id.pieChart)
        detailChartView = findViewById(R.id.chartDetail)

        pieChartView.setOnSectorClick { category ->
            isShowDetail = true
            Toast.makeText(this@MainActivity, category, Toast.LENGTH_SHORT).show()
            detailChartView.setData(getDetailChartData(category))
            updateVisibility()
        }

        if (savedInstanceState == null) {
            pieChartView.setData(getPieChartData())
        }

        updateVisibility()
    }

    private fun updateVisibility() {
        detailChartView.isVisible = isShowDetail
        pieChartView.isVisible = !isShowDetail
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SHOW_DETAIL_KEY, isShowDetail)
    }

    override fun onBackPressed() {
        if (detailChartView.isVisible) {
            detailChartView.isVisible = false
            pieChartView.isVisible = true
        } else {
            super.onBackPressed()
        }
    }

    private fun getPieChartCategoryColor(category: String): Int {
        return when (category) {
            "Продукты" -> Color.YELLOW
            "Здоровье" -> Color.RED
            "Кафе и рестораны" -> Color.CYAN
            "Алкоголь" -> Color.MAGENTA
            "Доставка еды" -> Color.BLUE
            "Транспорт" -> Color.GREEN
            "Спорт" -> Color.GRAY
            else -> Color.BLACK
        }
    }

    private fun getPayloads(): List<PayloadItem> {
        val raw = resources.openRawResource(R.raw.payload)
        val reader: Reader = BufferedReader(InputStreamReader(raw))

        val gson = Gson()
        val itemsType: Type = object : TypeToken<ArrayList<PayloadItem?>?>() {}.type
        val items: List<PayloadItem> = gson.fromJson(reader, itemsType)
        return items
    }

    private fun getDetailChartData(category: String): ChartDetailData {
        val items = payloads
            .filter { it.category == category }
            .sortedBy { it.time }
            .map {
                val date = Date(TimeUnit.SECONDS.toMillis(it.time)).apply {
                    time = it.time
                }
                ChartDetailItem(
                    amount = it.amount.toFloat(),
                    day = date.day.toString()
                )
            }

        return ChartDetailData(
            items = items,
            color = getPieChartCategoryColor(category)
        )
    }

    private fun getPieChartData(): List<PieChartSectorData> {
        val map = mutableMapOf<String, Int>()
        payloads.forEach {
            val amount = map[it.category]
            map[it.category] = (amount ?: 0) + it.amount
        }

        return map.map {
            PieChartSectorData(
                value = it.value,
                name = it.key,
                color = getPieChartCategoryColor(it.key)
            )
        }
    }

    data class PayloadItem(
        val id: Int,
        val name: String,
        val amount: Int,
        val category: String,
        val time: Long
    )

    companion object {
        const val IS_SHOW_DETAIL_KEY = "IS_SHOW_DETAIL_KEY"
    }
}


