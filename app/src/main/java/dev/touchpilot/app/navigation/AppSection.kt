package dev.touchpilot.app.navigation

import androidx.annotation.DrawableRes
import dev.touchpilot.app.R

enum class AppSection(val label: String, @DrawableRes val iconRes: Int) {
    CHAT("Chat", R.drawable.ic_chat),
    PRODUCT("Use", R.drawable.ic_product),
    LOGS("Logs", R.drawable.ic_logs),
    SETTINGS("Settings", R.drawable.ic_settings)
}
