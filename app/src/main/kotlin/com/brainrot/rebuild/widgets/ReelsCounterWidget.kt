package com.brainrot.rebuild.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import com.brainrot.rebuild.R
import androidx.glance.text.Text
import com.brainrot.rebuild.R
import androidx.glance.text.TextStyle
import com.brainrot.rebuild.app.RebuildApp
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class ReelsCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReelsCounterWidget()
}

class ReelsCounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RebuildApp
        val today = LocalDate.now().toString()
        val c = app.db.countersDao().dayCounters(today)
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier.background(
                        androidx.glance.unit.ColorProvider(R.color.brand_dark)
                    ).padding(8.dp)
                ) {
                    Column {
                        Text(
                            "Reels ${c?.reelsCount ?: 0}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = androidx.glance.unit.ColorProvider(R.color.brand_accent)
                            )
                        )
                        Text(
                            "Opens ${c?.appOpens ?: 0}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = androidx.glance.unit.ColorProvider(R.color.brand_dark)
                            )
                        )
                    }
                }
            }
        }
    }
}
