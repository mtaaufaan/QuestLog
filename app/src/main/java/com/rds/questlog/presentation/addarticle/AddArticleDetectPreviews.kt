package com.rds.questlog.presentation.addarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rds.questlog.presentation.theme.QuestLogTheme

private const val SAMPLE_BASE = "https://gamefaqs.gamespot.com/ps/dq7/faqs/walkthrough?page=1"
private val SampleFound = dummyContinuationUrls(SAMPLE_BASE)

@Composable
private fun DetectPreview(
    state: DetectState = DetectState.Idle,
    premium: Boolean = true,
    base: String? = SAMPLE_BASE,
    slots: Int = 9,
) {
    QuestLogTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            DetectSection(premium, state, base, slots, filled = MAX_URLS - slots, DetectActions({}, {}, {}, {}, {}))
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectFreePreview() = DetectPreview(premium = false)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectIdlePreview() = DetectPreview()

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectNoUrlPreview() = DetectPreview(base = null)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectFullPreview() = DetectPreview(slots = 0)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectLoadingPreview() = DetectPreview(DetectState.Loading)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectFoundPreview() = DetectPreview(
    DetectState.Found(SampleFound.take(4), SampleFound.take(4).toSet()),
    slots = 9,
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectFoundLimitedPreview() =
    DetectPreview(DetectState.Found(SampleFound, SampleFound.take(3).toSet()), slots = 3)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectNoneSelectedPreview() = DetectPreview(DetectState.Found(SampleFound.take(3), emptySet()))

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectNoResultsPreview() = DetectPreview(DetectState.NoResults)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectFailedPreview() = DetectPreview(DetectState.Failed)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DetectAddedPreview() = DetectPreview(DetectState.Added(4))
