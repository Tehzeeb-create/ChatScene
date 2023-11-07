package com.example.chatscene

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        Handler().postDelayed({
            val intent = Intent(this, NameGenderSelectionActivity::class.java)
            startActivity(intent)
            finish() // Prevent returning to the splash screen
        }, SPLASH_DELAY)
    }
}
