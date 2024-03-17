package otus.homework.customview

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.lang.reflect.Type

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pieChartView = findViewById<PieChartView>(R.id.pieChart)
        pieChartView.setOnSectorClick {
            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
        }

        if (savedInstanceState == null) {
            pieChartView.setData(getPieChartData())
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

    private fun getPieChartData(): List<PieChartSectorData> {
        val raw = resources.openRawResource(R.raw.payload)
        val reader: Reader = BufferedReader(InputStreamReader(raw))

        val gson = Gson()
        val itemsType: Type = object : TypeToken<ArrayList<PayloadItem?>?>() {}.type
        val items: List<PayloadItem> = gson.fromJson(reader, itemsType)

        val map = mutableMapOf<String, Int>()
        items.forEach {
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
}


