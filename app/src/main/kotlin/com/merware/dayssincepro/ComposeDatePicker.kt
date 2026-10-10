package com.merware.dayssincepro

import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import java.util.TimeZone
import java.util.function.LongConsumer

internal object ComposeDatePicker {
    @JvmStatic
    @OptIn(ExperimentalMaterial3Api::class)
    fun show(
        activity: AppCompatActivity,
        initialSelectionUtcMillis: Long,
        pastOnly: Boolean,
        onDateSelected: LongConsumer,
        onDismiss: Runnable?
    ) {
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val todayUtcMillis = DatePickerSupport.utcMillis(
            todayUtc.get(Calendar.YEAR),
            todayUtc.get(Calendar.MONTH),
            todayUtc.get(Calendar.DAY_OF_MONTH)
        )
        val currentYear = todayUtc.get(Calendar.YEAR)
        val selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                DatePickerSupport.isSelectableDate(utcTimeMillis, pastOnly, todayUtcMillis)

            override fun isSelectableYear(year: Int): Boolean =
                DatePickerSupport.isSelectableYear(year, pastOnly, currentYear)
        }

        val colorScheme = if (ThemeMode.isDark(activity)) {
            darkColorScheme(
                primary = Color(activity.getColor(R.color.ui_accent_primary)),
                onPrimary = Color.White,
                primaryContainer = Color(activity.getColor(R.color.picker_primary_container_dark)),
                onPrimaryContainer = Color(activity.getColor(R.color.picker_on_primary_container_dark)),
                surface = Color(activity.getColor(R.color.ui_surface_dark)),
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(activity.getColor(R.color.ui_accent_primary)),
                onPrimary = Color.White,
                primaryContainer = Color(activity.getColor(R.color.picker_primary_container_light)),
                onPrimaryContainer = Color(activity.getColor(R.color.picker_on_primary_container_light)),
                surface = Color(activity.getColor(R.color.ui_surface_light)),
                onSurface = Color(0xff1b1b1b)
            )
        }
        val confirmLabel = activity.getString(R.string.mini_b_ok)
        val dismissLabel = activity.getString(R.string.Cancel)
        val composeView = ComposeView(activity)
        val content = checkNotNull(activity.findViewById<ViewGroup>(android.R.id.content)) {
            "Activity content root is required to host the Compose date picker."
        }
        content.addView(composeView, ViewGroup.LayoutParams(0, 0))

        var closing = false
        fun closePicker() {
            if (closing) return
            closing = true
            composeView.post {
                composeView.disposeComposition()
                (composeView.parent as? ViewGroup)?.removeView(composeView)
            }
        }

        fun cancelPicker() {
            if (closing) return
            onDismiss?.run()
            closePicker()
        }

        composeView.setContent {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialSelectionUtcMillis,
                initialDisplayedMonthMillis = initialSelectionUtcMillis,
                yearRange = DatePickerSupport.MIN_DATE_YEAR..DatePickerSupport.MAX_DATE_YEAR,
                selectableDates = selectableDates
            )

            MaterialTheme(colorScheme = colorScheme) {
                DatePickerDialog(
                    onDismissRequest = ::cancelPicker,
                    confirmButton = {
                        TextButton(
                            enabled = pickerState.selectedDateMillis != null,
                            onClick = {
                                pickerState.selectedDateMillis?.let(onDateSelected::accept)
                                closePicker()
                            }
                        ) {
                            Text(confirmLabel)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = ::cancelPicker) {
                            Text(dismissLabel)
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    DatePicker(state = pickerState, showModeToggle = true)
                }
            }
        }
    }
}
