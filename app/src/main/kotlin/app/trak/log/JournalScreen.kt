package app.trak.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trak.R
import app.trak.core.designsystem.component.EmptyView
import app.trak.core.designsystem.component.TrakButton
import app.trak.core.designsystem.component.TrakButtonStyle
import app.trak.core.designsystem.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The Log tab: the daily journal for one date. Phase 1 shows the date picker and an honest
 * placeholder and saves nothing. Phase 2 puts the single-page journal here; the route stays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(initialDate: String?) {
    var date by rememberSaveable { mutableStateOf(initialDate ?: LocalDate.now().toString()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val day = LocalDate.parse(date)

    Column(Modifier.fillMaxSize().testTag("screen_journal")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.headlineSmall)
                Text(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), style = MaterialTheme.typography.bodyLarge)
            }
            TrakButton(stringResource(R.string.journal_change_date), onClick = { picking = true }, style = TrakButtonStyle.TEXT)
        }
        EmptyView(
            title = stringResource(R.string.journal_empty_title),
            body = stringResource(R.string.journal_empty_body),
        )
    }

    if (picking) {
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // Journals are for days that have happened.
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}
