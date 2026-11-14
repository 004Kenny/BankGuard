package com.example.bankguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bankguard.ui.BankGuardApp
import com.example.bankguard.ui.theme.BankGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BankGuardTheme {
                BankGuardApp()
            }
        }
    }
}
