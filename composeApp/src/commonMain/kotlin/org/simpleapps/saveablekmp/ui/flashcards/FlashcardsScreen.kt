package org.simpleapps.saveablekmp.ui.flashcards

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.domain.srs.ReviewDifficulty
import org.simpleapps.saveablekmp.domain.srs.SpacedRepetition
import org.simpleapps.saveablekmp.ui.main.Base64Image
import org.simpleapps.saveablekmp.ui.theme.*

@Composable
fun FlashcardsScreen(
    viewModel: FlashcardsViewModel,
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        when {
            state.isStudying -> StudyScreen(state, viewModel)
            state.isAddingCard -> AddEditCardScreen(state, viewModel)
            state.selectedDeck != null -> DeckScreen(state, viewModel)
            else -> DecksListScreen(state, viewModel)
        }

        // Toast
        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                viewModel.onEvent(FlashcardsEvent.ClearToast)
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedMedium)
                        .background(AppColors.Bg3)
                        .border(1.dp, AppColors.Border2, RoundedMedium)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(msg, style = AppTypography.bodySmall.copy(color = AppColors.Text))
                }
            }
        }
    }
}

// ── Список колод ──────────────────────────────────────────────────────────────

@Composable
private fun DecksListScreen(
    state: FlashcardsState,
    viewModel: FlashcardsViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Флеш-картки", style = AppTypography.titleLarge)
                Text("${state.decks.size} колод", style = AppTypography.subtitle)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (state.decks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🃏", fontSize = 48.sp)
                    Text("Немає колод", style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    Text(
                        "Додайте флеш-картку з головного екрану,\nобравши категорію «Флеш-картка»",
                        style = AppTypography.caption,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.decks, key = { it.id }) { deck ->
                    DeckCard(deck = deck, onClick = {
                        viewModel.onEvent(FlashcardsEvent.SelectDeck(deck))
                    })
                }
            }
        }
    }
}

@Composable
private fun DeckCard(deck: DeckInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedLarge)
            .background(AppColors.Bg2)
            .border(1.dp, AppColors.Border, RoundedLarge)
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(deck.name, style = AppTypography.body.copy(fontWeight = FontWeight.Medium))
            Text("→", style = AppTypography.body.copy(color = AppColors.Text3))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${deck.totalCards} карток",
                style = AppTypography.caption,
            )
            if (deck.dueCards > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.GreenDim)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${deck.dueCards} до повторення",
                        style = AppTypography.caption.copy(color = AppColors.Green),
                    )
                }
            } else {
                Text("Все повторено ✓", style = AppTypography.caption.copy(color = AppColors.Text3))
            }
        }
    }
}

// ── Картки в колоді ───────────────────────────────────────────────────────────

@Composable
private fun DeckScreen(
    state: FlashcardsState,
    viewModel: FlashcardsViewModel,
) {
    val deck = state.selectedDeck ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedMedium)
                    .background(AppColors.Bg3)
                    .border(1.dp, AppColors.Border, RoundedMedium)
                    .clickable { viewModel.onEvent(FlashcardsEvent.BackToDecks) },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", style = AppTypography.body.copy(color = AppColors.Text2))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(deck.name, style = AppTypography.titleLarge)
                Text("${state.cardsInDeck.size} карток", style = AppTypography.subtitle)
            }
            AppButton(
                text = "Вчити (${deck.dueCards})",
                onClick = { viewModel.onEvent(FlashcardsEvent.StartStudy) },
                enabled = deck.dueCards > 0,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            AppButton(
                text = "+ Картка",
                onClick = { viewModel.onEvent(FlashcardsEvent.AddCard) },
            )
        }

        Spacer(Modifier.height(10.dp))

        if (state.cardsInDeck.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Немає карток у цій колоді", style = AppTypography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(state.cardsInDeck, key = { it.id }) { card ->
                    FlashcardListItem(
                        card = card,
                        onEdit = { viewModel.onEvent(FlashcardsEvent.StartEditCard(card)) },
                        onDelete = { viewModel.onEvent(FlashcardsEvent.DeleteCard(card.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardListItem(
    card: SavedItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val nextReview = SpacedRepetition.getNextReviewLabel(card.nextReviewAt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedMedium)
            .background(AppColors.Bg2)
            .border(1.dp, AppColors.Border, RoundedMedium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                card.value.take(60),
                style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            if (card.description.isNotBlank()) {
                Text(
                    card.description.take(60),
                    style = AppTypography.caption,
                    maxLines = 1,
                )
            }
            Text(
                "Повторення: $nextReview",
                style = AppTypography.caption.copy(
                    color = if (card.nextReviewAt <= Clock.now().toEpochMilliseconds())
                        AppColors.Green else AppColors.Text3
                ),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton28(onClick = onEdit) {
                Text("✎", style = AppTypography.caption.copy(color = AppColors.Text2))
            }
            IconButton28(onClick = onDelete) {
                Text("✕", style = AppTypography.caption.copy(color = AppColors.PriorityHigh))
            }
        }
    }
}

// ── Режим навчання ────────────────────────────────────────────────────────────

@Composable
private fun StudyScreen(
    state: FlashcardsState,
    viewModel: FlashcardsViewModel,
) {
    if (state.studySessionDone) {
        StudyDoneScreen(state, viewModel)
        return
    }

    val currentCard = state.studyQueue.getOrNull(state.currentCardIndex) ?: return
    val progress = (state.currentCardIndex + 1).toFloat() / state.studyQueue.size

    // Анімація перевороту
    val rotation by animateFloatAsState(
        targetValue = if (state.isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Прогрес
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton("✕ Завершити", onClick = { viewModel.onEvent(FlashcardsEvent.FinishStudy) })
            Text(
                "${state.currentCardIndex + 1} / ${state.studyQueue.size}",
                style = AppTypography.bodySmall,
            )
        }

        // Прогрес-бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.Border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.Green),
            )
        }

        // Картка з анімацією перевороту
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedLarge)
                .background(AppColors.Bg2)
                .border(1.dp, AppColors.Border, RoundedLarge)
                .clickable { viewModel.onEvent(FlashcardsEvent.FlipCard) }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12 * density
                },
            contentAlignment = Alignment.Center,
        ) {
            // Передня сторона
            if (rotation <= 90f) {
                CardFace(
                    content = currentCard.value,
                    label = "Передня сторона",
                    hint = "Натисни щоб перевернути",
                )
            } else {
                // Задня сторона (відзеркалена назад)
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }.fillMaxSize()) {
                    CardFace(
                        content = currentCard.description.ifBlank { "—" },
                        label = "Задня сторона",
                        hint = null,
                    )
                }
            }
        }

        // Кнопки оцінки (тільки після перевороту)
        if (state.isFlipped) {
            val hardResult = SpacedRepetition.calculate(ReviewDifficulty.HARD, currentCard.easeFactor, currentCard.interval, currentCard.repetitions)
            val normalResult = SpacedRepetition.calculate(ReviewDifficulty.NORMAL, currentCard.easeFactor, currentCard.interval, currentCard.repetitions)
            val easyResult = SpacedRepetition.calculate(ReviewDifficulty.EASY, currentCard.easeFactor, currentCard.interval, currentCard.repetitions)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RateButton(
                    label = "Складно",
                    sublabel = hardResult.nextReviewLabel,
                    color = AppColors.PriorityHigh,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(FlashcardsEvent.RateCard(ReviewDifficulty.HARD)) },
                )
                RateButton(
                    label = "Нормально",
                    sublabel = normalResult.nextReviewLabel,
                    color = AppColors.PriorityMedium,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(FlashcardsEvent.RateCard(ReviewDifficulty.NORMAL)) },
                )
                RateButton(
                    label = "Легко",
                    sublabel = easyResult.nextReviewLabel,
                    color = AppColors.Green,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(FlashcardsEvent.RateCard(ReviewDifficulty.EASY)) },
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Натисни на картку щоб побачити відповідь",
                    style = AppTypography.caption,
                )
            }
        }
    }
}

@Composable
private fun CardFace(content: String, label: String, hint: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = AppTypography.caption)
        Spacer(Modifier.height(16.dp))
        if (content.startsWith("data:image")) {
            Base64Image(
                dataUrl = content,
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Text(
                content,
                style = AppTypography.body.copy(fontSize = 20.sp, textAlign = TextAlign.Center),
                textAlign = TextAlign.Center,
            )
        }
        hint?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = AppTypography.caption)
        }
    }
}

@Composable
private fun RateButton(
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedMedium)
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedMedium)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = AppTypography.bodySmall.copy(color = color, fontWeight = FontWeight.Medium))
        Text(sublabel, style = AppTypography.caption.copy(color = color.copy(alpha = 0.7f)))
    }
}

@Composable
private fun StudyDoneScreen(state: FlashcardsState, viewModel: FlashcardsViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Сесію завершено!", style = AppTypography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Повторено ${state.studyQueue.size} карток",
            style = AppTypography.bodySmall.copy(color = AppColors.Text2),
        )
        Spacer(Modifier.height(32.dp))
        AppButton(
            text = "Готово",
            onClick = { viewModel.onEvent(FlashcardsEvent.FinishStudy) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Додавання/редагування картки ─────────────────────────────────────────────

@Composable
private fun AddEditCardScreen(
    state: FlashcardsState,
    viewModel: FlashcardsViewModel,
) {
    val categories = remember { mutableStateOf<List<org.simpleapps.saveablekmp.data.model.Category>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedMedium)
                    .background(AppColors.Bg3)
                    .border(1.dp, AppColors.Border, RoundedMedium)
                    .clickable { viewModel.onEvent(FlashcardsEvent.CancelEdit) },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", style = AppTypography.body.copy(color = AppColors.Text2))
            }
            Text(
                if (state.editingCard != null) "Редагувати картку" else "Нова картка",
                style = AppTypography.titleLarge,
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Передня сторона
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("ПЕРЕДНЯ СТОРОНА", style = AppTypography.caption)
                BasicTextField(
                    value = state.inputFront,
                    onValueChange = { viewModel.onEvent(FlashcardsEvent.FrontChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .background(AppColors.Bg2, RoundedMedium)
                        .border(1.dp, AppColors.Border, RoundedMedium)
                        .padding(12.dp),
                    textStyle = AppTypography.body,
                    cursorBrush = SolidColor(AppColors.Green),
                    decorationBox = { inner ->
                        if (state.inputFront.isEmpty()) Text("Питання або термін...", style = AppTypography.body.copy(color = AppColors.Text3))
                        inner()
                    }
                )
            }

            // Задня сторона
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("ЗАДНЯ СТОРОНА", style = AppTypography.caption)
                BasicTextField(
                    value = state.inputBack,
                    onValueChange = { viewModel.onEvent(FlashcardsEvent.BackChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .background(AppColors.Bg2, RoundedMedium)
                        .border(1.dp, AppColors.Border, RoundedMedium)
                        .padding(12.dp),
                    textStyle = AppTypography.body,
                    cursorBrush = SolidColor(AppColors.Green),
                    decorationBox = { inner ->
                        if (state.inputBack.isEmpty()) Text("Відповідь або пояснення...", style = AppTypography.body.copy(color = AppColors.Text3))
                        inner()
                    }
                )
            }

            AppButton(
                text = if (state.editingCard != null) "Зберегти зміни" else "+ Додати картку",
                onClick = { viewModel.onEvent(FlashcardsEvent.SaveCard) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Імпорт Clock для використання в CardFace
private val Clock get() = kotlinx.datetime.Clock.System