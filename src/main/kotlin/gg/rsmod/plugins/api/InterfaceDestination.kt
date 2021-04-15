package gg.rsmod.plugins.api

import gg.rsmod.game.model.interf.DisplayMode

enum class InterfaceDestination(val interfaceId: Int,
        val fixedChildId: Int,
        val resizeClassicChildId: Int,
        val resizeModernChildId: Int,
        val fullscreenChildId: Int = -1,
        val clickThrough: Boolean = true
) {

    CHAT_BOX(interfaceId = 162, fixedChildId = 27, resizeClassicChildId = 32, resizeModernChildId = 34, fullscreenChildId = 1),

    // Full fixed viewport
    // IfMoveSubMessage From 548:23, To 164:15
    // IfMoveSubMessage From 548:23, To 161:15
    // IfMoveSubMessage From 548:23, To 165:8

    WALKABLE(interfaceId = -1, fixedChildId = 14, resizeClassicChildId = 3, resizeModernChildId = 4),

    // Underneath total xp, might be a tracker?
    // IfMoveSubMessage From 548:17, To 164:7
    // IfMoveSubMessage From 548:17, To 161:7
    // IfMoveSubMessage From 548:17, To 165:5

    TAB_AREA(interfaceId = -1, fixedChildId = 67, resizeClassicChildId = 69, resizeModernChildId = 69, fullscreenChildId = 9, clickThrough = false),

    ATTACK(interfaceId = 593, fixedChildId = 69, resizeClassicChildId = 71, resizeModernChildId = 71, fullscreenChildId = 10),

    SKILLS(interfaceId = 320, fixedChildId = 70, resizeClassicChildId = 72, resizeModernChildId = 72, fullscreenChildId = 11),

    QUEST_ROOT(interfaceId = 629, fixedChildId = 71, resizeClassicChildId = 73, resizeModernChildId = 73, fullscreenChildId = 12),

    INVENTORY(interfaceId = 149, fixedChildId = 72, resizeClassicChildId = 74, resizeModernChildId = 74, fullscreenChildId = 13),

    EQUIPMENT(interfaceId = 387, fixedChildId = 73, resizeClassicChildId = 75, resizeModernChildId = 75, fullscreenChildId = 14),

    PRAYER(interfaceId = 541, fixedChildId = 74, resizeClassicChildId = 76, resizeModernChildId = 76, fullscreenChildId = 15),

    MAGIC(interfaceId = 218, fixedChildId = 75, resizeClassicChildId = 77, resizeModernChildId = 77, fullscreenChildId = 16),

    CLAN_CHAT(interfaceId = 7, fixedChildId = 76, resizeClassicChildId = 78, resizeModernChildId = 78, fullscreenChildId = 17),

    ACCOUNT_MANAGEMENT(interfaceId = 109, fixedChildId = 77, resizeClassicChildId = 79, resizeModernChildId = 79, fullscreenChildId = 18),

    SOCIAL(interfaceId = 429, fixedChildId = 78, resizeClassicChildId = 80, resizeModernChildId = 80, fullscreenChildId = 19),

    LOG_OUT(interfaceId = 182, fixedChildId = 79, resizeClassicChildId = 81, resizeModernChildId = 81, fullscreenChildId = 20),

    SETTINGS(interfaceId = 116, fixedChildId = 80, resizeClassicChildId = 82, resizeModernChildId = 82, fullscreenChildId = 21),

    EMOTES(interfaceId = 216, fixedChildId = 81, resizeClassicChildId = 83, resizeModernChildId = 83, fullscreenChildId = 22),

    MUSIC(interfaceId = 239, fixedChildId = 82, resizeClassicChildId = 84, resizeModernChildId = 84, fullscreenChildId = 23),

    PVP_OVERLAY(interfaceId = -1, fixedChildId = 15, resizeClassicChildId = 4, resizeModernChildId = 4, fullscreenChildId = 24),

    USERNAME(interfaceId = 163, fixedChildId = 20, resizeClassicChildId = 11, resizeModernChildId = 11, fullscreenChildId = 25),

    MINI_MAP(interfaceId = 160, fixedChildId = 11, resizeClassicChildId = 31, resizeModernChildId = 31, fullscreenChildId = 26),

    XP_COUNTER(interfaceId = 122, fixedChildId = 18, resizeClassicChildId = 8, resizeModernChildId = 8, fullscreenChildId = 6),

    // Full viewport height
    // IfMoveSubMessage From 548:19, To 164:9
    // IfMoveSubMessage From 548:19, To 161:9
    // IfMoveSubMessage From 548:19, To 165:7

    MAIN_SCREEN(interfaceId = -1, fixedChildId = 23, resizeClassicChildId = 15, resizeModernChildId = 15, fullscreenChildId = 30, clickThrough = false),

    // Middle left of viewport
    // IfMoveSubMessage From 548:16, To 164:5
    // IfMoveSubMessage From 548:16, To 161:5
    // IfMoveSubMessage From 548:16, To 165:2

    // Used for the 'all settings' interface and world map
    MAIN_SCREEN_ALTERNATE(interfaceId = -1, fixedChildId = 24, resizeClassicChildId = 16, resizeModernChildId = 16, fullscreenChildId = 30),

    WORLD_MAP_FULL(interfaceId = -1, fixedChildId = 29, resizeClassicChildId = 29, resizeModernChildId = 29, fullscreenChildId = 29, clickThrough = false),

    ;

    fun isSwitchable(): Boolean = when (this) {
        CHAT_BOX, MAIN_SCREEN, WALKABLE, TAB_AREA,
        ATTACK, SKILLS, QUEST_ROOT, INVENTORY, EQUIPMENT,
        PRAYER, MAGIC, CLAN_CHAT, ACCOUNT_MANAGEMENT,
        SOCIAL, LOG_OUT, SETTINGS, EMOTES, MUSIC, PVP_OVERLAY,
        USERNAME, MINI_MAP, XP_COUNTER, MAIN_SCREEN_ALTERNATE -> true
        else -> false
    }

    /**
     * Gets a list of [InterfaceDestination] that should be cleared
     * of all interfaces when an interface in the destination is opened.
     * @return [List] of [InterfaceDestination]
     */
    fun closeDestinationOnInterfaceOpened(): List<InterfaceDestination> {
        return when (this) {
            MAIN_SCREEN -> listOf(MAIN_SCREEN_ALTERNATE)
            MAIN_SCREEN_ALTERNATE -> listOf(MAIN_SCREEN)
            else -> emptyList()
        }
    }

    companion object {
        val values = enumValues<InterfaceDestination>()

        fun getModals() = values.filter { pane -> pane.interfaceId != -1 }
    }
}

fun getDisplayComponentId(displayMode: DisplayMode) = when (displayMode) {
    DisplayMode.FIXED -> 548
    DisplayMode.RESIZABLE_CLASSIC -> 161
    DisplayMode.RESIZABLE_MODERN -> 164
    DisplayMode.FULLSCREEN -> 165
    else -> throw RuntimeException("Unhandled display mode.")
}

fun getChildId(pane: InterfaceDestination, displayMode: DisplayMode): Int = when (displayMode) {
    DisplayMode.FIXED -> pane.fixedChildId
    DisplayMode.RESIZABLE_CLASSIC -> pane.resizeClassicChildId
    DisplayMode.RESIZABLE_MODERN -> pane.resizeModernChildId
    DisplayMode.FULLSCREEN -> pane.fullscreenChildId
    else -> throw RuntimeException("Unhandled display mode.")
}