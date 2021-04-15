package gg.rsmod.plugins.api

/**
 * @author Tom <rspsmods@gmail.com>
 */
enum class HitType(val id: Int, val hasTint: Boolean = false) {
    POISON(id = 2),
    YELLOW(id = 3), //NOTE: find real use for this and name it accordingly
    DISEASE(id = 4),
    VENOM(id = 5),
    HEAL(id = 6),
    BLOCK_ME(id = 12),
    BLOCK_OTHER(id = 13),
    DAMAGE_ME(16),
    DAMAGE_OTHER(17),
    DAMAGE_ME_CYAN(id = 18),
    DAMAGE_OTHER_CYAN(id = 19),
    DAMAGE_ME_ORANGE(id = 20),
    DAMAGE_OTHER_ORANGE(id = 21),
    DAMAGE_ME_YELLOW(id = 22),
    DAMAGE_OTHER_YELLOW(id = 23),
    DAMAGE_ME_WHITE(id = 24),
    DAMAGE_OTHER_WHITE(id = 25),
}