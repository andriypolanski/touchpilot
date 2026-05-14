package dev.touchpilot.app

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.touchpilot.app.androidcontrol.AccessibilityBridge
import dev.touchpilot.app.tools.ToolExecutionLog

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var outputView: TextView
    private lateinit var executionLogView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "TouchPilot"
            textSize = 30f
        }

        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }

        val enableButton = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val observeButton = Button(this).apply {
            text = "Observe Current Screen"
            setOnClickListener {
                refreshStatus()
                val snapshot = AccessibilityBridge.observeScreen()
                ToolExecutionLog.record(
                    name = "observe_screen",
                    args = "",
                    ok = AccessibilityBridge.isConnected(),
                    message = "snapshot length=${snapshot.length}"
                )
                outputView.text = snapshot
                refreshExecutionLog()
            }
        }

        val appInput = EditText(this).apply {
            hint = "App package or launcher label"
            setSingleLine(true)
        }

        val openAppButton = Button(this).apply {
            text = "Open App"
            setOnClickListener {
                val target = appInput.text.toString()
                val ok = openApp(target)
                recordAndRender("open_app", "target=\"$target\"", ok, "openApp")
            }
        }

        val targetInput = EditText(this).apply {
            hint = "Visible text to tap"
            setSingleLine(true)
        }

        val tapButton = Button(this).apply {
            text = "Tap Text"
            setOnClickListener {
                val target = targetInput.text.toString()
                val ok = AccessibilityBridge.tapByText(target)
                refreshStatus()
                recordAndRender("tap", "text=\"$target\"", ok, "tapByText")
            }
        }

        val typeInput = EditText(this).apply {
            hint = "Text to type into focused field"
            setSingleLine(true)
        }

        val typeButton = Button(this).apply {
            text = "Type Into Focused Field"
            setOnClickListener {
                val value = typeInput.text.toString()
                val ok = AccessibilityBridge.typeIntoFocusedField(value)
                refreshStatus()
                recordAndRender("type_text", "text_length=${value.length}", ok, "typeIntoFocusedField")
            }
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val backButton = Button(this).apply {
            text = "Back"
            setOnClickListener {
                val ok = AccessibilityBridge.pressBack()
                refreshStatus()
                recordAndRender("press_back", "", ok, "pressBack")
            }
        }

        val homeButton = Button(this).apply {
            text = "Home"
            setOnClickListener {
                val ok = AccessibilityBridge.pressHome()
                refreshStatus()
                recordAndRender("press_home", "", ok, "pressHome")
            }
        }

        actionRow.addView(backButton, rowButtonParams())
        actionRow.addView(homeButton, rowButtonParams())

        val scrollRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val scrollForwardButton = Button(this).apply {
            text = "Scroll Down"
            setOnClickListener {
                val ok = AccessibilityBridge.scrollForward()
                refreshStatus()
                recordAndRender("scroll", "direction=\"forward\"", ok, "scrollForward")
            }
        }

        val scrollBackwardButton = Button(this).apply {
            text = "Scroll Up"
            setOnClickListener {
                val ok = AccessibilityBridge.scrollBackward()
                refreshStatus()
                recordAndRender("scroll", "direction=\"backward\"", ok, "scrollBackward")
            }
        }

        scrollRow.addView(scrollForwardButton, rowButtonParams())
        scrollRow.addView(scrollBackwardButton, rowButtonParams())

        val waitInput = EditText(this).apply {
            hint = "Text to wait for"
            setSingleLine(true)
        }

        val waitButton = Button(this).apply {
            text = "Wait For Text"
            setOnClickListener {
                val expectedText = waitInput.text.toString()
                outputView.text = "Waiting for \"$expectedText\"..."
                Thread {
                    val ok = AccessibilityBridge.waitForText(expectedText, timeoutMs = 5_000L)
                    runOnUiThread {
                        refreshStatus()
                        recordAndRender(
                            "wait_for_ui",
                            "text=\"$expectedText\", timeout_ms=5000",
                            ok,
                            "waitForText"
                        )
                    }
                }.start()
            }
        }

        outputView = TextView(this).apply {
            text = "Enable TouchPilot Control, then observe a screen."
            textSize = 13f
            setPadding(0, 24, 0, 0)
        }

        val executionLogTitle = TextView(this).apply {
            text = "Tool Execution Log"
            textSize = 18f
            setPadding(0, 32, 0, 8)
        }

        executionLogView = TextView(this).apply {
            text = ToolExecutionLog.render()
            textSize = 13f
        }

        root.addView(titleView)
        root.addView(statusView)
        root.addView(enableButton)
        root.addView(observeButton)
        root.addView(appInput)
        root.addView(openAppButton)
        root.addView(targetInput)
        root.addView(tapButton)
        root.addView(typeInput)
        root.addView(typeButton)
        root.addView(actionRow)
        root.addView(scrollRow)
        root.addView(waitInput)
        root.addView(waitButton)
        root.addView(outputView)
        root.addView(executionLogTitle)
        root.addView(executionLogView)

        setContentView(ScrollView(this).apply {
            addView(root)
        })

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun openApp(target: String): Boolean {
        if (target.isBlank()) return false

        val exactLaunchIntent = packageManager.getLaunchIntentForPackage(target)
        if (exactLaunchIntent != null) {
            startActivity(exactLaunchIntent)
            return true
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val matches = packageManager.queryIntentActivities(launcherIntent, 0)
        val match = matches.firstOrNull { info ->
            info.launcherLabel().equals(target, ignoreCase = true)
        } ?: matches.firstOrNull { info ->
            info.launcherLabel().contains(target, ignoreCase = true)
        } ?: return false

        val intent = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return false
        startActivity(intent)
        return true
    }

    private fun ResolveInfo.launcherLabel(): String {
        return loadLabel(packageManager)?.toString().orEmpty()
    }

    private fun recordAndRender(name: String, args: String, ok: Boolean, message: String) {
        ToolExecutionLog.record(name, args, ok, message)
        outputView.text = "$name($args) -> $ok"
        refreshExecutionLog()
    }

    private fun refreshExecutionLog() {
        executionLogView.text = ToolExecutionLog.render()
    }

    private fun refreshStatus() {
        statusView.text = if (AccessibilityBridge.isConnected()) {
            "Accessibility service: connected"
        } else {
            "Accessibility service: not connected"
        }
    }

    private fun rowButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
}
