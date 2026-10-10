package com.signde.androidvideooverlay

/** Pure key-pair tracking, independent of Android so repeat/cancel handling is testable. */
class OverlayShortcut {
    data class Result(val consumed: Boolean, val toggle: Boolean = false)
    private var pressedKey: Int? = null

    fun handle(key: Int, down: Boolean, repeat: Int, durationMs: Long,
               cancelled: Boolean, selectedKey: Int, longPress: Boolean): Result {
        // Android TV's configurable Favorites key is MACRO_1 on the onn remote,
        // while the original SEI remote uses BOOKMARK. Treat both (and BUTTON_1) as Favorites.
        val matches = selectedKey != 0 && (key == selectedKey || (selectedKey == 174 && (key == 313 || key == 188)))
        if (!matches && pressedKey != key) return Result(false)
        if (down) {
            if (repeat == 0) pressedKey = key
            return Result(true)
        }
        val paired = pressedKey == key
        pressedKey = null
        return Result(true, paired && matches && !cancelled && (!longPress || durationMs >= 750))
    }

    fun reset() { pressedKey = null }
}
