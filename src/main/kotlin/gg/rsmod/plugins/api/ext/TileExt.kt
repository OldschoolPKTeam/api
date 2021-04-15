package gg.rsmod.plugins.api.ext

import gg.rsmod.game.model.Area
import gg.rsmod.game.model.Direction
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World

val WILDERNESS = Area(2941, 3524, 3392, 3968)
val SAFE_HIGH_RISK_ZONE = Area(1368, 3925, 1380, 3937)

/**
 * The area within the Ferox enclave that is considered to be a safe-spot
 * within the wilderness
 */
val FEROX_ENCLAVE_AREAS = arrayOf(
    Area(3125, 3639, 3139, 3617),
    Area(3123, 3632, 3124, 3622),
    Area(3138, 3646, 3143, 3618),
    Area(3144, 3622, 3150, 3646),
    Area(3151, 3646, 3156, 3636),
    Area(3151, 3635, 3154, 3626)
)

/**
 * The surrounding border of the Ferox Enclave that is considered to be safe-spot
 * within the wilderness when the player is not teleblocked
 */
val FEROX_ENCLAVE_SAFETY_BORDER = arrayOf(
    Area(3155, 3634, 3160, 3638),
    Area(3124, 3640, 3137, 3643),
    Area(3120, 3634, 3124, 3639),
    Area(3118, 3623, 3122, 3634),
    Area(3121, 3620, 3124, 3622),
    Area(3124, 3616, 3124, 3620),
    Area(3124, 3616, 3129, 3616),
    Area(3129, 3610, 3140, 3616),
    Area(3141, 3616, 3144, 3618)
)
val DANGER_HIGH_RISK_ZONE = arrayOf(
        Area(1369, 3938, 1379, 3947),
        Area(1381, 3926, 1390, 3936),
        Area(1369, 3915, 1379, 3924),
        Area(1358, 3926, 1367, 3936)
)

fun Tile.getWildernessLevel(): Int {
    // TODO: Remove, testing only
    // Clan wars FFA area
    if (Area(3274, 4760, 3381, 4853).contains(x, z)) {
        return 60
    }

    if (isInDangerHighRiskZone()) {
        return 60
    }

    if (!inside(WILDERNESS) || this.isWildySafeZone()) {
        return 0
    }

    val z = if (this.z > 6400) this.z - 6400 else this.z
    return (((z - 3525) shr 3) + 1)
}

fun Tile.createArea(radius: Int = 1): Area = Area(this.x - radius, this.z - radius, this.x + radius, this.z + radius)

/**
 * Gets the adjacent tiles based on the source tile
 * @param direction The [Direction] is used to determine
 *          if vertical or horizontal tiles will be used
 * @param amount The amount of tiles to capture
 */
fun Tile.getAdjacentTiles(direction: Direction, amount: Int = 1): List<Tile> {
    val list = mutableListOf<Tile>()
    when(direction) {
        Direction.WEST, Direction.EAST -> {
            for (z in -amount..amount) {
                if (z == 0)
                    continue
                list.add(Tile(this.x, this.z + z, this.height))
            }
        }
        Direction.NORTH, Direction.SOUTH -> {
            for (x in -amount..amount) {
                if(x == 0)
                    continue
                list.add(Tile(this.x + x, this.z, this.height))
            }
        }
        else -> throw UnsupportedOperationException("You cannot get the adjacent tiles for $direction")
    }
    return list
}

fun Tile.isClipped(world: World): Boolean = world.collision.isClipped(this)

fun Tile.isInWilderness(): Boolean = this.getWildernessLevel() > 0

fun Tile.isWildySafeZone(): Boolean = isInFeroxEnclave()

fun Tile.inside(vararg areas: Area): Boolean = areas.any {x >= it.bottomLeft.x && z >= it.bottomLeftZ && x <= it.topRight.x && z <= it.topRight.z }

/**
 * Determines whether the given tile is within the ferox enclave
 */
fun Tile.isInFeroxEnclave(): Boolean = inside(*FEROX_ENCLAVE_AREAS)

/**
 * Determines whether the given tile is within the Ferox safety border
 */
fun Tile.isInFeroxSafetyBorder(): Boolean = inside(*FEROX_ENCLAVE_SAFETY_BORDER)

/**
 * Determines whether the given tile is within safe area of the high risk zone
 */
fun Tile.isInSafeHighRiskZone(): Boolean = inside(SAFE_HIGH_RISK_ZONE)

fun Tile.isInDangerHighRiskZone(): Boolean = inside(*DANGER_HIGH_RISK_ZONE)