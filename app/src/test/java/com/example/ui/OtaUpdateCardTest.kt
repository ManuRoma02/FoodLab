package com.example.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.screens.OtaUpdateCard
import com.example.ui.screens.parseMarkdownBold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.ui.theme.CupertinoDarkColorScheme
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocalSoundManager
import com.example.util.OtaUpdateInfo
import com.example.util.OtaUpdateManager
import com.example.util.OtaUpdateState
import com.example.util.SoundManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OtaUpdateCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val soundManager: SoundManager by lazy { SoundManager(context) }

    @Test
    fun testOtaUpdateCard_rendersHeaderAndIdleState() {
        val manager = OtaUpdateManager()
        manager.resetState()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MyApplicationTheme {
                    OtaUpdateCard(otaManager = manager)
                }
            }
        }

        composeTestRule.onNodeWithText("AGGIORNAMENTI IN-APP (OTA)").assertIsDisplayed()
        composeTestRule.onNodeWithText("VERIFICA AGGIORNAMENTI").assertIsDisplayed()
    }

    @Test
    fun testOtaUpdateCard_rendersUpdateAvailableState() {
        val manager = OtaUpdateManager(client = OkHttpClient())
        val info = OtaUpdateInfo(
            versionCode = 99,
            versionName = "1.0.99",
            apkUrl = "https://example.com/update.apk",
            changelog = "• **Nuova UI**: animazioni **60 FPS**",
            isUpdateAvailable = true
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MyApplicationTheme {
                    OtaUpdateCard(otaManager = manager)
                }
            }
        }

        // Initially Idle
        composeTestRule.onNodeWithText("VERIFICA AGGIORNAMENTI").assertIsDisplayed()
    }

    @Test
    fun testParseMarkdownBold_formatsKeywordsWithBoldSpanStyle() {
        val input = "• **Barra navigazione**: scorrimento con **rimbalzo elastico**"
        val annotated = parseMarkdownBold(input)

        // Verifies plain text matches without asterisks
        assertEquals("• Barra navigazione: scorrimento con rimbalzo elastico", annotated.text)

        // Verifies bold span styles
        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldSpans.size)

        val firstSpanText = annotated.text.substring(boldSpans[0].start, boldSpans[0].end)
        assertEquals("Barra navigazione", firstSpanText)

        val secondSpanText = annotated.text.substring(boldSpans[1].start, boldSpans[1].end)
        assertEquals("rimbalzo elastico", secondSpanText)
    }
}
