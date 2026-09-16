package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class WidgetLayoutTest {
    private val widgetSizeDp = 110

    @Test
    fun twoByTwoWidgetsFitAtMinimumDeclaredSize() {
        verifyAndCapture(R.layout.widget_pet_status, 110, "widget_pet_status_2x2.png")
        verifyAndCapture(R.layout.widget_daily_calories, 110, "widget_daily_calories_2x2.png")
    }

    @Test
    fun twoByTwoWidgetsFillCommonLauncherAllocation() {
        verifyAndCapture(R.layout.widget_pet_status, 144, "widget_pet_status_2x2_launcher.png", assertFilled = true)
        verifyAndCapture(R.layout.widget_daily_calories, 144, "widget_daily_calories_2x2_launcher.png", assertFilled = true)
    }

    @Test
    fun petWidgetKeepsMacroReadoutMinimal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.widget_pet_status, null)
        val values = listOf(
            root.findViewById<TextView>(R.id.protein_text).text.toString(),
            root.findViewById<TextView>(R.id.carbs_text).text.toString(),
            root.findViewById<TextView>(R.id.fat_text).text.toString()
        )

        values.forEach { value ->
            assertTrue("Macro '$value' must be a compact gram value", value.matches(Regex("\\d+g")))
            assertFalse("Macro '$value' must not show a consumed/target pair", value.contains('/'))
        }
    }

    private fun verifyAndCapture(
        layoutRes: Int,
        widgetSizeDp: Int,
        outputName: String,
        assertFilled: Boolean = false
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        val sizePx = (widgetSizeDp * density).toInt()
        val root = LayoutInflater.from(context).inflate(layoutRes, null) as ViewGroup
        val exactSize = View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)

        root.measure(exactSize, exactSize)
        root.layout(0, 0, sizePx, sizePx)

        assertEquals(sizePx, root.measuredWidth)
        assertEquals(sizePx, root.measuredHeight)
        capture(root, sizePx, outputName)
        assertChildrenStayInside(root, "root")
        assertTextIsNotClipped(root)
        if (assertFilled) assertUsesAvailableHeight(root)
    }

    private fun assertUsesAvailableHeight(root: ViewGroup) {
        val visibleChildren = (0 until root.childCount)
            .map(root::getChildAt)
            .filter { it.visibility != View.GONE }
        val first = visibleChildren.first()
        val last = visibleChildren.last()
        assertTrue("Content leaves unused space above", first.top <= root.paddingTop + 1)
        assertTrue(
            "Content leaves unused space below: bottom=${last.bottom}, root=${root.height}",
            last.bottom >= root.height - root.paddingBottom - 1
        )
    }

    private fun assertChildrenStayInside(parent: ViewGroup, path: String) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child.visibility != View.VISIBLE) continue
            val childPath = "$path/${child.resources.getResourceEntryNameOrFallback(child.id, index)}"
            assertTrue("$childPath starts before the left edge", child.left >= 0)
            assertTrue(
                "$childPath top=${child.top}, bottom=${child.bottom}, height=${child.height} in parent height=${parent.height}; siblings=${parent.childBounds()}",
                child.top >= 0
            )
            assertTrue("$childPath right=${child.right} exceeds parent width=${parent.width}", child.right <= parent.width)
            assertTrue("$childPath bottom=${child.bottom} exceeds parent height=${parent.height}", child.bottom <= parent.height)
            assertTrue("$childPath has negative width", child.width >= 0)
            assertTrue("$childPath has negative height", child.height >= 0)
            if (child is ViewGroup) assertChildrenStayInside(child, childPath)
        }
    }

    private fun assertTextIsNotClipped(view: View) {
        if (view is TextView && view.text.isNotEmpty()) {
            val availableWidth = view.width - view.totalPaddingLeft - view.totalPaddingRight
            val desiredWidth = Layout.getDesiredWidth(view.text, view.paint)
            assertTrue(
                "Text '${view.text}' needs ${desiredWidth}px but only ${availableWidth}px are available",
                desiredWidth <= availableWidth + 1f
            )
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) assertTextIsNotClipped(view.getChildAt(index))
        }
    }

    private fun capture(view: View, sizePx: Int, outputName: String) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val reportDir = File("build/reports/widget-layout").apply { mkdirs() }
        FileOutputStream(File(reportDir, outputName)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun ViewGroup.childBounds(): String =
        (0 until childCount).joinToString { index ->
            val child = getChildAt(index)
            "#$index(${child.left},${child.top}-${child.right},${child.bottom}; ${child.width}x${child.height})"
        }

    private fun android.content.res.Resources.getResourceEntryNameOrFallback(id: Int, index: Int): String =
        if (id == View.NO_ID) "child-$index" else runCatching { getResourceEntryName(id) }.getOrDefault("child-$index")
}
