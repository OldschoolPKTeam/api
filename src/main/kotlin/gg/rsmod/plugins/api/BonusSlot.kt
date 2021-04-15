package gg.rsmod.plugins.api

import gg.rsmod.game.model.combat.CombatStyle

/**
 * @author Tom <rspsmods@gmail.com>
 */
enum class BonusSlot(val id: Int) {
    ATTACK_STAB(id = 0),
    ATTACK_SLASH(id = 1),
    ATTACK_CRUSH(id = 2),
    ATTACK_MAGIC(id = 3),
    ATTACK_RANGED(id = 4),

    DEFENCE_STAB(id = 5),
    DEFENCE_SLASH(id = 6),
    DEFENCE_CRUSH(id = 7),
    DEFENCE_MAGIC(id = 8),
    DEFENCE_RANGED(id = 9);

    companion object {

        /**
         * Retrieves the [BonusSlot] for the given [combatStyle]
         */
        fun getBonusSlotForStyle(combatStyle: CombatStyle, defence: Boolean): BonusSlot {
            return when(combatStyle) {
                CombatStyle.STAB -> if(defence) DEFENCE_STAB else ATTACK_STAB
                CombatStyle.SLASH -> if(defence) DEFENCE_SLASH else ATTACK_SLASH
                CombatStyle.CRUSH -> if(defence) DEFENCE_CRUSH else ATTACK_CRUSH
                CombatStyle.MAGIC -> if(defence) DEFENCE_MAGIC else ATTACK_MAGIC
                CombatStyle.RANGED -> if(defence) DEFENCE_RANGED else ATTACK_RANGED
                else -> ATTACK_STAB
            }
        }

    }

}