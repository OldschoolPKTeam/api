package gg.rsmod.plugins.api.ext

import gg.rsmod.game.model.Area
import gg.rsmod.game.model.EntityType
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.collision.ObjectType
import gg.rsmod.game.model.entity.DynamicObject
import gg.rsmod.game.model.entity.GameObject
import gg.rsmod.game.model.entity.Pawn

fun World.openDoor(
    obj: GameObject,
    opened: Int = obj.id + 1,
    invertRot: Boolean = false,
    invertTransform: Boolean = false
): GameObject {
    val oldRot = obj.rot
    val newRot = Math.abs((oldRot + (if (invertRot) -1 else 1)) and 0x3)
    val diagonal = obj.type == ObjectType.DIAGONAL_WALL.value

    val newTile = when (oldRot) {
        0 -> if (diagonal) obj.tile.transform(0, 1) else obj.tile.transform(if (invertTransform) 1 else -1, 0)
        1 -> if (diagonal) obj.tile.transform(1, 0) else obj.tile.transform(0, if (invertTransform) -1 else 1)
        2 -> if (diagonal) obj.tile.transform(0, -1) else obj.tile.transform(if (invertTransform) -1 else 1, 0)
        3 -> if (diagonal) obj.tile.transform(-1, 0) else obj.tile.transform(0, if (invertTransform) 1 else -1)
        else -> throw IllegalStateException("Invalid door rotation: [currentRot=$oldRot, replaceRot=$newRot]")
    }

    val newDoor = DynamicObject(id = opened, type = obj.type, rot = newRot, tile = newTile)
    remove(obj)
    spawn(newDoor)
    return newDoor
}

fun World.closeDoor(
    obj: GameObject,
    closed: Int = obj.id - 1,
    invertRot: Boolean = false,
    invertTransform: Boolean = false
): GameObject {
    val oldRot = obj.rot
    val newRot = Math.abs((oldRot + (if (invertRot) 1 else -1)) and 0x3)
    val diagonal = obj.type == ObjectType.DIAGONAL_WALL.value

    val newTile = when (oldRot) {
        0 -> if (diagonal) obj.tile.transform(1, 0) else obj.tile.transform(0, if (invertTransform) -1 else 1)
        1 -> if (diagonal) obj.tile.transform(0, -1) else obj.tile.transform(if (invertTransform) -1 else 1, 0)
        2 -> if (diagonal) obj.tile.transform(-1, 0) else obj.tile.transform(0, if (invertTransform) 1 else -1)
        3 -> if (diagonal) obj.tile.transform(0, 1) else obj.tile.transform(if (invertTransform) 1 else -1, 0)
        else -> throw IllegalStateException("Invalid door rotation: [currentRot=$oldRot, replaceRot=$newRot]")
    }

    val newDoor = DynamicObject(id = closed, type = obj.type, rot = newRot, tile = newTile)
    remove(obj)
    spawn(newDoor)
    return newDoor
}

/**
 * An effect that targets multiple enemies at once
 * @param target The target pawn to cast the effect on
 * @param tile The [Tile] that will be the source of the effect if set
 * @param radius The radius of effect based on the target's tile
 * @param playersEffected The number of players to effect
 * @param npcsEffected The number of npcs to effect
 * @param effect The amount of pawns to effect in that area
 */
fun World.multiEffect(
    target: Pawn,
    tile: Tile? = null,
    radius: Int = 1,
    playersEffected: Int,
    npcsEffected: Int,
    effectTargetType: Boolean = true,
    effect: (Pawn) -> Unit
) {
    val effectArea = tile?.createArea(radius) ?: target.tile.createArea(radius)

    val players = this.pawnList()
        .filter(areaEffectTargetTypeFilter(target, effectTargetType))
        .filter(areaEffectFilter(target, effectArea))
        .take(playersEffected)

    val npcs = this.pawnList()
        .filter(areaEffectTargetTypeFilter(target, effectTargetType))
        .filter(areaEffectFilter(target, effectArea))
        .take(npcsEffected)

    (players + npcs).distinct().take(playersEffected + npcsEffected).shuffled().forEach(effect)
}

fun areaEffectTargetTypeFilter(target: Pawn, effectTargetType: Boolean): (Pawn) -> Boolean = { pawn: Pawn ->
    if (effectTargetType) pawn.entityType == target.entityType
    else pawn.entityType == EntityType.PLAYER || pawn.entityType == EntityType.NPC
}

fun areaEffectFilter(target: Pawn, effectArea: Area): (Pawn) -> Boolean = { pawn: Pawn ->
    pawn.tile.isMulti(target.world) && effectArea.contains(pawn.tile) && pawn != target
}