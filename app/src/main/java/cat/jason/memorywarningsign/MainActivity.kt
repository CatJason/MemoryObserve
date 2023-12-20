package cat.jason.memorywarningsign

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import cat.jason.performance.MemoryWarningSign

class MainActivity : AppCompatActivity() {
    private var isMemoryInfoShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val showMemoryButton: Button = findViewById(R.id.showMemoryButton)
        showMemoryButton.setOnClickListener {
            if (isMemoryInfoShown) {
                MemoryWarningSign.hide(this)
            } else {
                MemoryWarningSign.show(this)
            }
            isMemoryInfoShown = !isMemoryInfoShown
        }
    }
}