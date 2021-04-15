package gg.rsmod.plugins.api.ext

import gg.rsmod.game.model.entity.Player

fun tryCommand(player: Player, args: Array<String>, failMessage: String, tryUnit: Function1<Array<String>, Unit>) {
    try {
        tryUnit.invoke(args)
    } catch (e: Exception) {
        player.message(failMessage)
        e.printStackTrace()
    }
}

fun tryCommand(player: Player, failMessage: String, tryUnit: Function1<Array<String>, Unit>) {
    tryCommand(player, player.getCommandArgs(), failMessage, tryUnit)
}