package it.insubria.prova0

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    var count=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
    fun funzione1(view: View){
            count+=1
            val tvCount = findViewById<TextView>(R.id.textView)
            tvCount.text = "Hai cliccato $count volte"
    }
}