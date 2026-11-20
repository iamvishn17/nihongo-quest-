package com.nihongoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nihongoquest.data.LearningDatabase
import com.nihongoquest.ui.NihongoTheme

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val dao = LearningDatabase.get(this).learningDao()
  setContent { NihongoTheme(false) { AppRoot() } }
 }
}

@Composable
private fun AppRoot() { Text("NIHONGO QUEST — 日本語を学ぼう") }
