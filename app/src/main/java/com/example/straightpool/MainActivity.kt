package com.example.straightpool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.straightpool.scorer.ScorerViewModel
import com.example.straightpool.ui.AppNav   // <- your single Nav host lives in ui/AppNav.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ScorerViewModel = viewModel()
            MaterialTheme {
                Surface { AppNav(vm) }
            }
        }
    }
}
