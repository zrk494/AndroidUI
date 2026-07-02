package com.example.toolpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.toolpack.feature.pet.ui.PetScreen
import com.example.toolpack.ui.theme.ToolPackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolPackTheme {
                PetScreen()
            }
        }
    }
}