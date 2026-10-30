package com.brainrot.rebuild.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.brainrot.rebuild.R
import com.brainrot.rebuild.app.RebuildApp
import java.time.LocalDate

/** App-open counter widget chip (original: AppOpenCounterWidgetReceiver). */
class AppOpenCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AppOpenCounterWidget()
}

class AppOpenCounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RebuildApp
        val c = try {
            app.db.countersDao().dayCounters(LocalDate.now().toString())
        } catch (_: Exception) { null }
        val limit = try {
            app.thresholds.flow.first().openLimit
        } catch (_: Exception) { 40 }
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier.background(
                        androidx.glance.unit.ColorProvider(R.color.brand_accent)
                    ).padding(8.dp)
                ) {
                    Column {
                        Text(
                            "Opens ${c?.appOpens ?: 0}",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = androidx.glance.unit.ColorProvider(R.color.brand_dark)
                            )
                        )
                        Text(
                            "of $limit",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = androidx.glance.unit.ColorProvider(R.color.brand_dark)
                            )
                        )
                    }
                }
            }
        }
    }
}
