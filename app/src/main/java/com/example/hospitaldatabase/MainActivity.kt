package com.example.hospitaldatabase

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button




class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main) // Set the layout for this activity

        // Find the button by its ID from the layout
        val btnGoToLogin: Button = findViewById(R.id.button)

        // Set an OnClickListener for the button
        btnGoToLogin.setOnClickListener {
            // Create an Intent to navigate from MainActivity to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            // Start the LoginActivity
            startActivity(intent)
        }

    }
}