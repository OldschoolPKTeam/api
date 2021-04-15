package gg.rsmod.plugins.api.ext

import gg.rsmod.game.model.EntityType
import gg.rsmod.game.model.Hit
import gg.rsmod.game.model.attr.*
import gg.rsmod.game.model.combat.CombatClass
import gg.rsmod.game.model.combat.PawnHit
import gg.rsmod.game.model.container.key.ContainerKey
import gg.rsmod.game.model.entity.GameObject
import gg.rsmod.game.model.entity.Npc
import gg.rsmod.game.model.entity.Pawn
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.item.Item
import gg.rsmod.game.model.timer.ACTIVE_COMBAT_TIMER
import gg.rsmod.game.model.timer.FROZEN_TIMER
import gg.rsmod.game.model.timer.STUN_TIMER
import gg.rsmod.game.model.timer.TELEBLOCK_TIMER
import gg.rsmod.plugins.api.*
import java.lang.ref.WeakReference
import java.util.*

fun Pawn.getCommandArgs(): Array<String> = attr[COMMAND_ARGS_ATTR]!!

fun Pawn.getInteractingSlot(): Int = attr[INTERACTING_SLOT_ATTR]!!

fun Pawn.getLastInteractingSlot(): Int = attr[LAST_INTERACTING_SLOT_ATTR]!!

fun Pawn.getInteractingItemContainerKey(): ContainerKey = attr[INTERACTING_ITEM_CONTAINER_KEY]!!

fun Pawn.getInteractingItem(): Item = attr[INTERACTING_ITEM]!!.get()!!

fun Pawn.getOtherInteractingItem(): Item = attr[OTHER_ITEM_ATTR]!!.get()!!

fun Pawn.getInteractingItemId(): Int = attr[INTERACTING_ITEM_ID]!!

fun Pawn.getOtherInteractingItemId(): Int = attr[OTHER_ITEM_ID_ATTR]!!

fun Pawn.getInteractingItemSlot(): Int = attr[INTERACTING_ITEM_SLOT]!!

fun Pawn.getOtherInteractingItemSlot(): Int = attr[OTHER_ITEM_SLOT_ATTR]!!

fun Pawn.getInteractingOption(): Int = attr[INTERACTING_OPT_ATTR]!!

fun Pawn.getInteractingGameObj(): GameObject = attr[INTERACTING_OBJ_ATTR]!!.get()!!

fun Pawn.getInteractingNpc(): Npc = attr[INTERACTING_NPC_ATTR]!!.get()!!

fun Pawn.getInteractingPlayer(): Player = attr[INTERACTING_PLAYER_ATTR]!!.get()!!

fun Pawn.getCombatTarget(): Pawn? = attr[COMBAT_TARGET_FOCUS_ATTR]?.get()

fun Pawn.getLastPawnHit(): PawnHit? = attr[LAST_PAWNHIT]?.get()

fun Pawn.getLastPawnHitReceived(): PawnHit? = attr[LAST_PAWNHIT_RECEIVED]?.get()

fun Pawn.isAttacking(): Boolean = attr[COMBAT_TARGET_FOCUS_ATTR]?.get() != null

fun Pawn.isBeingAttacked(): Boolean = timers.has(ACTIVE_COMBAT_TIMER)

fun Pawn.removeCombatTarget() = attr.remove(COMBAT_TARGET_FOCUS_ATTR)

fun Pawn.hasPrayerIcon(icon: PrayerIcon): Boolean = prayerIcon == icon.id

fun Pawn.getBonus(slot: BonusSlot): Int = equipmentBonuses[slot.id]

fun Pawn.getCmbLevel(combatSkill: CombatSkill): Int = when(this) {
    is Player -> this.getSkills().getCurrentLevel(combatSkill.npcId)
    is Npc -> this.stats.getCurrentLevel(combatSkill.playerId)
    else -> 0
}

fun Pawn.hit(
    damage: Int, type: HitType = if (damage == 0) HitType.BLOCK_ME else HitType.DAMAGE_ME,
    delay: Int = 0, attacker: Optional<WeakReference<Pawn>> = Optional.of(WeakReference(this)),
    combatClass: CombatClass? = null
): Hit {
    val splashHit = damage == 0 && combatClass == CombatClass.MAGIC && entityType == EntityType.PLAYER

    val builder = Hit.Builder()
        .setDamageDelay(delay)
        .addHit(damage = if(splashHit) -1 else damage, type = type.id, attacker = attacker)
        .setHitbarMaxPercentage(HitbarType.NORMAL.pixelsWide)

    if(splashHit) builder.hideHitbar()

    val hit = builder.build()

    addHit(hit)
    return hit
}

fun Pawn.doubleHit(damage1: Int, damage2: Int, delay: Int = 0,
                   type1: HitType = if (damage1 == 0) HitType.BLOCK_ME else HitType.DAMAGE_ME,
                   type2: HitType = if (damage2 == 0) HitType.BLOCK_ME else HitType.DAMAGE_ME,
                   attacker: Optional<WeakReference<Pawn>> = Optional.of(WeakReference(this))): Hit {
    val hit = Hit.Builder()
        .setDamageDelay(delay)
        .addHit(damage = damage1, type = type1.id, attacker = attacker)
        .addHit(damage = damage2, type = type2.id, attacker = attacker)
        .setHitbarMaxPercentage(HitbarType.NORMAL.pixelsWide)
        .build()

    addHit(hit)
    return hit
}

fun Pawn.showHitbar(percentage: Int, type: HitbarType) {
    addHit(Hit.Builder().onlyShowHitbar().setHitbarType(type.id).setHitbarPercentage(percentage).setHitbarMaxPercentage(type.pixelsWide).build())
}

/**
 * Determines if the [Pawn] is teleblocked or not.
 */
fun Pawn.isTeleblocked(): Boolean = this.timers.has(TELEBLOCK_TIMER)

fun Pawn.freeze(cycles: Int, onFreeze: () -> Unit) {
    if (timers.has(FROZEN_TIMER)) {
        return
    }
    stopMovement()
    timers[FROZEN_TIMER] = cycles
    onFreeze()
}

fun Pawn.freeze(cycles: Int) {
    freeze(cycles) {
        if (this is Player) {
            this.message("<col=ef1020>You have been frozen!</col>")
        }
    }
}

fun Pawn.stun(cycles: Int, onStun: () -> Unit): Boolean {
    if (timers.has(STUN_TIMER)) {
        return false
    }
    stopMovement()
    timers[STUN_TIMER] = cycles
    onStun()
    return true
}

fun Pawn.stun(cycles: Int) {
    stun(cycles) {
        graphic(245, 124)
        if (this is Player) {
            resetInteractions()
            interruptQueues()
            message("You have been stunned!")
        }
    }
}

/**
 * Drains a [Pawn]s [combatSkill] by a certain amount
 * @param combatSkill The [CombatSkill] to be drained
 * @param drainAmount The amount tod drain the skill by
 * @param capped Determines whether the skill should drain based on the max level
 */
fun Pawn.drainCombatSkill(combatSkill: CombatSkill, drainAmount: Int, capped: Boolean = true): Int {
    return when(this) {
        is Npc -> {
            if(combatSkill == CombatSkill.PRAYER) { return 0 }
            this.drainSkill(combatSkill.npcId, drainAmount, capped)
        }
        is Player -> this.drainSkill(combatSkill.playerId, drainAmount, capped)
        else -> 0
    }
}

/**
 * Drains a [Pawn]s [combatSkill] by a certain percentage
 * @param combatSkill The [CombatSkill] to be drained
 * @param drainPercentage The percentage amount tod drain the skill by
 * @param capped Determines whether the skill should drain based on the max level
 */
fun Pawn.drainCombatSkill(combatSkill: CombatSkill, drainPercentage: Double, capped: Boolean = true): Int {
    return when(this) {
        is Npc -> {
            if(combatSkill == CombatSkill.PRAYER) { return 0 }
            this.drainSkill(combatSkill.npcId, drainPercentage, capped)
        }
        is Player -> this.drainSkill(combatSkill.playerId, drainPercentage, capped)
        else -> 0
    }
}