package gg.rsmod.plugins.api

enum class CombatSkill(val playerId: Int, val npcId: Int) {

    ATTACK(Skills.ATTACK, NpcSkills.ATTACK),
    STRENGTH(Skills.STRENGTH, NpcSkills.STRENGTH),
    DEFENCE(Skills.DEFENCE, NpcSkills.DEFENCE),
    RANGED(Skills.RANGED, NpcSkills.RANGED),
    PRAYER(Skills.PRAYER, -1),
    MAGIC(Skills.MAGIC, NpcSkills.MAGIC);

}