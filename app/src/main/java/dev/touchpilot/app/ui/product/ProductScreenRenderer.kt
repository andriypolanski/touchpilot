package dev.touchpilot.app.ui.product

import android.app.Activity
import android.widget.LinearLayout
import dev.touchpilot.app.memory.Skill
import dev.touchpilot.app.navigation.AppSection
import dev.touchpilot.app.runtime.ToolExecutionController
import dev.touchpilot.app.ui.TouchPilotTheme as Theme
import dev.touchpilot.app.ui.primaryButton
import dev.touchpilot.app.ui.sectionTitle
import dev.touchpilot.app.ui.summaryCard
import dev.touchpilot.app.ui.timelineCard
import dev.touchpilot.app.ui.withMargins

class ProductScreenRenderer(
    private val activity: Activity,
    private val contentRoot: LinearLayout,
    private val skills: List<Skill>,
    private val toolExecutionController: ToolExecutionController,
    private val openAccessibilitySettings: () -> Unit,
    private val showSection: (AppSection) -> Unit,
    private val openSettingsTools: () -> Unit,
    private val openSkillDetail: (String) -> Unit,
    private val refreshProductScreen: () -> Unit
) {
    fun render() {
        contentRoot.addView(
            activity.summaryCard(
                title = "TouchPilot",
                value = "Things you can use right now",
                chipText = "local-first",
                chipAccent = true
            )
        )

        contentRoot.addView(activity.sectionTitle("Start here"))
        contentRoot.addView(
            activity.timelineCard(
                title = "Ask TouchPilot",
                body = "Open chat for a request, a question, or a step-by-step task.",
                actionHint = "Open Chat"
            ) {
                showSection(AppSection.CHAT)
            }
        )
        contentRoot.addView(
            activity.timelineCard(
                title = "Inspect the device",
                body = "Read the current screen, app, and basic device state.",
                actionHint = "Open Tools"
            ) {
                openSettingsTools()
            }
        )
        contentRoot.addView(
            activity.timelineCard(
                title = "Review activity",
                body = "Check recent runs, approvals, and tool results.",
                actionHint = "Open Logs"
            ) {
                showSection(AppSection.LOGS)
            }
        )
        contentRoot.addView(
            activity.timelineCard(
                title = "Configure host",
                body = "Choose skills, runtime mode, MCP, and other settings.",
                actionHint = "Open Settings"
            ) {
                showSection(AppSection.SETTINGS)
            }
        )

        contentRoot.addView(activity.sectionTitle("Quick actions"))
        val quickActionRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        quickActionRow.addView(
            activity.primaryButton("Read Screen") {
                toolExecutionController.executeAndRender("observe_screen_context", emptyMap())
                refreshProductScreen()
            },
            rowParams()
        )
        quickActionRow.addView(
            activity.primaryButton("Foreground App") {
                toolExecutionController.executeAndRender("get_foreground_app", emptyMap())
                refreshProductScreen()
            },
            rowParams()
        )
        contentRoot.addView(quickActionRow)

        contentRoot.addView(
            activity.primaryButton("Open Accessibility Settings") {
                openAccessibilitySettings()
            }.withMargins(top = 8)
        )

        contentRoot.addView(activity.sectionTitle("Skills you can use"))
        if (skills.isEmpty()) {
            contentRoot.addView(
                activity.timelineCard(
                    title = "No bundled skills are enabled",
                    body = "Enable a skill in Settings to unlock a focused task set.",
                    actionHint = "Open Settings"
                ) {
                    showSection(AppSection.SETTINGS)
                }
            )
        } else {
            skills.forEach { skill ->
                val examples = skill.examples.take(2)
                val body = buildString {
                    append(skill.description)
                    if (examples.isNotEmpty()) {
                        append("\nExamples: ")
                        append(examples.joinToString(separator = ", "))
                    }
                }
                contentRoot.addView(
                    activity.timelineCard(
                        title = skill.title,
                        body = body,
                        actionHint = "Open skill details"
                    ) {
                        openSkillDetail(skill.id)
                    }
                )
            }
        }
    }

    private fun rowParams() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
        setMargins(0, 0, 8, 0)
    }
}
