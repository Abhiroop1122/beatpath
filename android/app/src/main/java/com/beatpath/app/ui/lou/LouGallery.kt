package com.beatpath.app.ui.lou

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Developer screen (launch with `--ez lou_gallery true`) to review every mood side by side. */
@Composable
fun LouGallery() {
    Column(Modifier.fillMaxSize().background(Color(0xFFE8F6FF)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LouMood.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Lou(Modifier.fillMaxWidth().aspectRatio(171f / 144f), mood = m, beatPhase = if (m == LouMood.Dance) 0.1f else null)
                        Text(m.name)
                    }
                }
            }
        }
    }
}
