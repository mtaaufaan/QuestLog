package com.rds.questlog.presentation.addarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.model.AddArticlePayload
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SAVE_DELAY_MS = 650L
private const val PASTE_MESSAGE_MS = 2_600L

/**
 * Form "Simpan Guide" (S2) dengan dua tab. Props dan callback mengikuti component-contract.md §2; state form
 * dipegang di sini (seperti prototipe) dan dipulihkan saat rotasi. Penyimpanan nyata menyusul di Tahap 2.
 */
@Composable
fun AddArticleContent(
    mode: ScrapeMode,
    targetArticleId: Long?,
    games: List<GameUiModel>,
    articles: List<ArticleUiModel>,
    isPremium: Boolean,
    onBack: () -> Unit,
    onSave: (AddArticlePayload) -> Unit,
    onOpenUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    submitError: String? = null,
    sharedUrl: String? = null,
    initialGameId: Long? = null,
    readClipboard: suspend () -> String? = { null },
    detectSource: suspend (DetectRequest) -> DetectOutcome = ::dummyDetect,
    incomingSharedUrl: String? = null,
    onIncomingHandled: () -> Unit = {},
) {
    var form by rememberSaveable(stateSaver = AddArticleFormSaver) {
        mutableStateOf(
            AddArticleForm(
                mode = mode,
                targetId = targetArticleId,
                urls = listOfNotNull(sharedUrl).ifEmpty { listOf("") },
            ),
        )
    }
    PrefillGame(initialGameId, games, canPrefill = form.gameInput.isBlank() && mode == ScrapeMode.NEW) {
        form = form.withPickedGame(it.id, it.name)
    }
    var pasteOutcome by remember { mutableStateOf<PasteOutcome?>(null) }
    // URL dari "Bagikan" yang jadi penanda Hal. 1, dan URL yang menunggu konfirmasi karena daftar sudah berisi.
    var sharedMarker by rememberSaveable { mutableStateOf(sharedUrl) }
    var confirmShared by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val detector = rememberDetector(detectSource, onOpenUnlock)
    LaunchedEffect(pasteOutcome) {
        if (pasteOutcome != null) {
            delay(PASTE_MESSAGE_MS)
            pasteOutcome = null
        }
    }

    val target = targetArticle(form, articles)
    val errors = if (form.showErrors) validate(form, articles) else FormErrors()
    val urlErrors = urlErrors(form.urls, urlScope(form, articles), form.showErrors)
    val limitText = when {
        articleLimitReached(form, articles, isPremium) -> R.string.add_article_limit_article
        gameLimitReached(form, games, isPremium) -> R.string.add_article_limit_game
        else -> null
    }

    IncomingSharedUrl(
        url = incomingSharedUrl,
        hasFilledUrls = form.filledUrlCount > 0,
        onApply = {
            form = form.withSharedUrl(it)
            sharedMarker = it
        },
        onNeedsConfirm = { confirmShared = it },
        onHandled = onIncomingHandled,
    )

    LaunchedEffect(form.saving) {
        if (form.saving) {
            delay(SAVE_DELAY_MS)
            val payload = buildPayload(form, articles)
            form = form.copy(saving = false)
            onSave(payload)
        }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().imePadding()) {
        AddArticleHeader(onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ModeTabs(form.mode) { form = form.withMode(it) }
            if (form.mode == ScrapeMode.NEW) {
                GameField(
                    form = form,
                    games = games,
                    articles = articles,
                    isPremium = isPremium,
                    error = errors.game,
                    onInput = { form = form.withGameInput(it) },
                    onPick = { form = form.withPickedGame(it.id, it.name) },
                    onCreate = { form = form.withNewGame() },
                    onOpenUnlock = onOpenUnlock,
                )
                TitleField(form.title, errors.title) { form = form.withTitle(it) }
            } else {
                AppendTargetSection(articles, games, form.targetId, errors.target) { form = form.withTarget(it) }
            }
            UrlSection(
                form = form,
                target = target,
                errors = urlErrors,
                onChange = { index, text -> form = form.withUrlChanged(index, text) },
                onAdd = { form = form.withUrlAdded() },
                onRemove = { form = form.withUrlRemoved(it) },
                onMove = { index, delta -> form = form.withUrlMoved(index, delta) },
                showSharedBanner = sharedMarker != null && form.urls.firstOrNull() == sharedMarker,
                pasteOutcome = pasteOutcome,
                onPaste = {
                    scope.launch {
                        val (next, outcome) = form.withPasted(readClipboard())
                        form = next
                        pasteOutcome = outcome
                    }
                },
                trailing = { DetectBlock(form, articles, isPremium, detector, { form = it }, onOpenUnlock) },
            )
        }
        confirmShared?.let { url ->
            ReplaceUrlsDialog(
                onConfirm = {
                    form = form.withSharedUrl(url)
                    sharedMarker = url
                    confirmShared = null
                },
                onDismiss = { confirmShared = null },
            )
        }
        AddArticleFooter(
            limitText = limitText?.let { stringResource(it) },
            submitError = submitError,
            isNew = form.mode == ScrapeMode.NEW,
            saving = form.saving,
            saveEnabled = !form.saving && limitText == null,
            onUpgrade = onOpenUnlock,
            onSave = {
                val attempted = form.copy(showErrors = true)
                form = if (validate(attempted, articles).isValid) attempted.copy(saving = true) else attempted
            },
        )
    }
}

@Composable
private fun AddArticleHeader(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (pressed) QuestLogTheme.colors.hover else Color.Transparent)
                    .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    QlIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.add_article_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(R.string.add_article_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = CormorantGaramond, fontSize = 23.sp),
            )
        }
        HorizontalDivider(color = QuestLogTheme.colors.divider)
    }
}

@Composable
private fun ModeTabs(mode: ScrapeMode, onMode: (ScrapeMode) -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(Modifier.fillMaxWidth().height(40.dp).clip(shape).border(1.dp, c.accentLine, shape)) {
        Tab(stringResource(R.string.add_article_tab_new), mode == ScrapeMode.NEW, Modifier.weight(1f)) {
            onMode(ScrapeMode.NEW)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.accentLine))
        Tab(stringResource(R.string.add_article_tab_append), mode == ScrapeMode.APPEND, Modifier.weight(1f)) {
            onMode(ScrapeMode.APPEND)
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) c.accentTint else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.accentText, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp))
    }
}

@Composable
private fun TitleField(title: String, hasError: Boolean, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.add_article_label_title))
        QlTextField(
            value = title,
            onValueChange = onChange,
            placeholder = stringResource(R.string.add_article_title_placeholder),
            errorText = if (hasError) stringResource(R.string.add_article_error_title_empty) else null,
        )
    }
}

@Composable
private fun AddArticleFooter(
    limitText: String?,
    submitError: String?,
    isNew: Boolean,
    saving: Boolean,
    saveEnabled: Boolean,
    onUpgrade: () -> Unit,
    onSave: () -> Unit,
) {
    val c = QuestLogTheme.colors
    Column {
        HorizontalDivider(color = c.divider)
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (limitText != null) LimitBanner(limitText, onUpgrade)
            if (submitError != null) {
                Text(
                    text = submitError,
                    color = QuestLogTheme.colors.danger,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                )
            }
            SaveButton(
                label = when {
                    saving -> stringResource(R.string.add_article_saving)
                    isNew -> stringResource(R.string.add_article_save_new)
                    else -> stringResource(R.string.add_article_save_append)
                },
                saving = saving,
                enabled = saveEnabled,
                onClick = onSave,
            )
            Text(
                text = stringResource(R.string.add_article_footnote),
                color = c.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LimitBanner(text: String, onUpgrade: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(c.accentTint)
            .border(1.dp, c.accentSoft, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            QlIcons.Lock,
            contentDescription = null,
            tint = c.accentText,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text,
                color = c.accentText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            )
            Text(
                text = stringResource(R.string.add_article_upgrade),
                color = c.accentText,
                modifier = Modifier.clickable(role = Role.Button, onClick = onUpgrade),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                ),
            )
        }
    }
}

@Composable
private fun SaveButton(label: String, saving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .alpha(if (enabled || saving) 1f else 0.45f)
            .clip(shape)
            .background(if (pressed && enabled) c.accentTintActive else Color.Transparent)
            .border(1.dp, c.accentLine, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (saving) CircularProgressIndicator(Modifier.size(14.dp), color = c.accentLine, strokeWidth = 1.5.dp)
        Text(label, color = c.accentText, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 1.1.sp),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddArticleNewPreview() {
    PreviewContent(ScrapeMode.NEW, null, PreviewData.articles)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddArticleAppendPreview() {
    PreviewContent(ScrapeMode.APPEND, 1L, PreviewData.articles)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddArticleSharedPreview() {
    PreviewContent(ScrapeMode.NEW, null, PreviewData.articles, sharedUrl = PreviewData.SHARED_URL)
}

@Composable
private fun PreviewContent(
    mode: ScrapeMode,
    target: Long?,
    articles: List<ArticleUiModel>,
    sharedUrl: String? = null,
) {
    QuestLogTheme {
        AddArticleContent(
            mode = mode,
            targetArticleId = target,
            games = PreviewData.games,
            articles = articles,
            isPremium = false,
            onBack = {},
            onSave = {},
            onOpenUnlock = {},
            sharedUrl = sharedUrl,
        )
    }
}

/** Dari Halaman Game, game-nya terisi otomatis sekali saat daftar game sudah termuat (bila isian masih kosong). */
@Composable
private fun PrefillGame(
    initialGameId: Long?,
    games: List<GameUiModel>,
    canPrefill: Boolean,
    onPick: (GameUiModel) -> Unit,
) {
    var done by rememberSaveable { mutableStateOf(initialGameId == null) }
    val prefill by rememberUpdatedState(canPrefill)
    LaunchedEffect(games) {
        val game = games.firstOrNull { it.id == initialGameId }
        if (!done && game != null) {
            if (prefill) onPick(game)
            done = true
        }
    }
}

/** Bagian "Halaman lanjutan" yang terhubung ke form: sumber, slot, dan penambahan hasil ke daftar URL. */
@Composable
private fun DetectBlock(
    form: AddArticleForm,
    articles: List<ArticleUiModel>,
    isPremium: Boolean,
    detector: Detector,
    onForm: (AddArticleForm) -> Unit,
    onOpenUnlock: () -> Unit,
) {
    DetectSection(
        premium = isPremium,
        state = detector.state,
        base = detectBaseUrl(form.urls),
        slots = detectSlots(form),
        filled = form.filledUrlCount,
        actions = DetectActions(
            onDetect = {
                detector.search(
                    base = checkNotNull(detectBaseUrl(form.urls)),
                    slots = detectSlots(form),
                    known = urlScope(form, articles).let { it.stored + it.existing + form.urls },
                )
            },
            onToggle = { detector.toggle(it, detectSlots(form)) },
            onApply = {
                (detector.state as? DetectState.Found)?.let {
                    onForm(applyDetected(form, it))
                    detector.applied(it.selected.size)
                }
            },
            onDismiss = detector::dismiss,
            onOpenUnlock = onOpenUnlock,
        ),
    )
}
