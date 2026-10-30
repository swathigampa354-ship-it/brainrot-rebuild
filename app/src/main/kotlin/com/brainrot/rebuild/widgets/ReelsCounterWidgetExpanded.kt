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
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.brainrot.rebuild.R
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.room.DailyCounterEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Expanded 4x2 variant of the reels counter (original:
 *  feature_widget.presentation.widget.ReelsCounterWidgetExpandedReceiver):
 *  shows today's totals, limit progress and the top per-app split. */
class ReelsCounterWidgetExpanded : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RebuildApp
        val day = LocalDate.now().toString()
        val c: DailyCounterEntity? = try {
            app.db.countersDao().dayCounters(day)
        } catch (_: Exception) { null }
        val splits = try {
            app.db.countersDao().splitFor(day).take(3)
        } catch (_: Exception) { emptyList() }
        val limit = try {
            app.thresholds.flow.first().reelsLimit
        } catch (_: Exception) { 60 }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .background(androidx.glance.unit.ColorProvider(R.color.brand_dark))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "Today",
                        style = TextStyle(fontSize = 11.sp, color = androidx.glance.unit.ColorProvider(R.color.brand_accent))
                    )
                    Text(
                        "${c?.reelsCount ?: 0} reels · ${c?.appOpens ?: 0} opens",
                        style = TextStyle(fontSize = 14.sp, color = androidx.glance.unit.ColorProvider(R.color.brand_accent))
                    )
                    androidx.glance.appwidget.ProgressBar(
                        progress = (
                            (c?.reelsCount ?: 0).toFloat() / limit.toFloat().coerceAtLeast(1f) * 100f
                            ).toInt(),
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    for (s in splits) {
                        Text(
                            "${s.appNameShort()}: ${s.reels} reels / ${s.opens} opens",
                            style = TextStyle(fontSize = 10.sp, color = androidx.glance.unit.ColorProvider(R.color.brand_accent))
                        )
                    }
                }
            }
        }
    }
}

private fun com.brainrot.rebuild.room.AppSplitEntity.appNameShort(): String =
    packageName.substringAfterLast('.').take(12)

class ReelsCounterWidgetExpandedReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ReelsCounterWidgetExpanded()
}
