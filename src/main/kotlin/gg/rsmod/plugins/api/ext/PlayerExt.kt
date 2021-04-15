package gg.rsmod.plugins.api.ext

import com.google.common.collect.EvictingQueue
import com.google.common.primitives.Ints
import gg.rsmod.game.fs.def.ItemDef
import gg.rsmod.game.fs.def.VarbitDef
import gg.rsmod.game.message.impl.*
import gg.rsmod.game.model.World
import gg.rsmod.game.model.attr.*
import gg.rsmod.game.model.bits.BitStorage
import gg.rsmod.game.model.bits.StorageBits
import gg.rsmod.game.model.container.ContainerStackType
import gg.rsmod.game.model.container.ItemContainer
import gg.rsmod.game.model.container.key.ContainerKey
import gg.rsmod.game.model.entity.Entity
import gg.rsmod.game.model.entity.Pawn
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.interf.DisplayMode
import gg.rsmod.game.model.item.Item
import gg.rsmod.game.model.timer.SKULL_ICON_DURATION_TIMER
import gg.rsmod.game.model.timer.TELEBLOCK_TIMER
import gg.rsmod.game.sync.block.UpdateBlockType
import gg.rsmod.plugins.api.*
import gg.rsmod.plugins.api.cfg.Vars.SAFETY_BORDER_VARBIT
import gg.rsmod.plugins.service.marketvalue.ItemMarketValueService
import gg.rsmod.util.BitManipulation
import java.util.*

/**
 * The interface key used by inventory overlays
 */
const val INVENTORY_INTERFACE_KEY = 93

/**
 * The id of the script used to initialise the interface overlay options. The 'big' variant of this script
 * is used as it supports up to eight options rather than five.
 *
 * https://github.com/RuneStar/cs2-scripts/blob/master/scripts/[clientscript,interface_inv_init_big].cs2
 */
const val INTERFACE_INV_INIT_BIG = 150

fun Player.openShop(shop: String) {
    val s = world.getShop(shop)
    if (s != null) {
        attr[CURRENT_SHOP_ATTR] = s
        shopDirty = true

        openInterface(interfaceId = 300, dest = InterfaceDestination.MAIN_SCREEN)
        openInterface(interfaceId = 301, dest = InterfaceDestination.INVENTORY)
        setInterfaceEvents(interfaceId = 300, component = 16, range = 0..s.items.size, setting = 1086)
        setInterfaceEvents(interfaceId = 301, component = 0, range = 0 until inventory.capacity, setting = 1086)
        runClientScript(1074, 13, s.name)
    } else {
        World.logger.warn { "Player \"$username\" is unable to open shop \"$shop\" as it does not exist." }
    }
}

fun Player.message(
    message: String,
    type: ChatMessageType = ChatMessageType.CONSOLE,
    username: String? = null,
    messageColor: MessageColor? = null
) {
    write(MessageGameMessage(
        type = type.id,
        message = message.run { if(messageColor != null) { "<col=${messageColor.colorCode}>$message</col>" } else this},
        username = username
    ))
}

fun Player.devMessage(message: String) {
    if ("dev" in this.privilege.powers) this.message(
        message = message,
        messageColor = MessageColor.MIDNIGHT_BLUE
    )
}


fun Player.filterableMessage(message: String) {
    write(MessageGameMessage(type = ChatMessageType.SPAM.id, message = message, username = null))
}

fun Player.runClientScript(id: Int, vararg args: Any) {
    write(RunClientScriptMessage(id, *args))
}

fun Player.focusTab(tab: GameframeTab) {
    runClientScript(915, tab.id)
}

fun Player.setInterfaceUnderlay(color: Int, transparency: Int) {
    runClientScript(2524, color, transparency)
}

fun Player.setInterfaceEvents(interfaceId: Int, component: Int, from: Int, to: Int, setting: Int) {
    write(
        IfSetEventsMessage(
            hash = ((interfaceId shl 16) or component),
            fromChild = from,
            toChild = to,
            setting = setting
        )
    )
}

fun Player.setInterfaceEvents(interfaceId: Int, component: Int, range: IntRange, setting: Int) {
    write(
        IfSetEventsMessage(
            hash = ((interfaceId shl 16) or component),
            fromChild = range.first,
            toChild = range.last,
            setting = setting
        )
    )
}

fun Player.setComponentText(interfaceId: Int, component: Int, text: String) {
    write(IfSetTextMessage(interfaceId, component, text))
}

fun Player.setComponentHidden(interfaceId: Int, component: Int, hidden: Boolean) {
    write(IfSetHideMessage(hash = ((interfaceId shl 16) or component), hidden = hidden))
}

fun Player.setComponentItem(interfaceId: Int, component: Int, item: Int, amountOrZoom: Int) {
    write(IfSetObjectMessage(hash = ((interfaceId shl 16) or component), item = item, amount = amountOrZoom))
}

fun Player.setComponentNpcHead(interfaceId: Int, component: Int, npc: Int) {
    write(IfSetNpcHeadMessage(hash = ((interfaceId shl 16) or component), npc = npc))
}

fun Player.setComponentPlayerHead(interfaceId: Int, component: Int) {
    write(IfSetPlayerHeadMessage(hash = ((interfaceId shl 16) or component)))
}

fun Player.setComponentAnim(interfaceId: Int, component: Int, anim: Int) {
    write(IfSetAnimMessage(hash = ((interfaceId shl 16) or component), anim = anim))
}

/**
 * Use this method to open an interface id on top of an [InterfaceDestination]. This
 * method should always be preferred over
 *
 * ```
 * openInterface(parent: Int, child: Int, component: Int, type: Int, isMainComponent: Boolean)
 * ```
 *
 * as it holds logic that must be handled for certain [InterfaceDestination]s.
 */
fun Player.openInterface(interfaceId: Int, dest: InterfaceDestination, fullscreen: Boolean = false) {
    val displayMode = if (!fullscreen || dest.fullscreenChildId == -1) interfaces.displayMode else DisplayMode.FULLSCREEN
    val child = getChildId(dest, displayMode)
    val parent = getDisplayComponentId(displayMode)

    /*
     * Close interfaces from competing destinations
     */
    val close = dest.closeDestinationOnInterfaceOpened()
    close.forEach { closeInterfaceIfOpen(it) }

    openInterface(
        parent,
        child,
        interfaceId,
        if (dest.clickThrough) 1 else 0,
        isModal = dest == InterfaceDestination.MAIN_SCREEN
    )
}

/**
 * Use this method to "re-open" an [InterfaceDestination]. This method should always
 * be preferred over
 *
 * ```
 * openInterface(parent: Int, child: Int, interfaceId: Int, type: Int, mainInterface: Boolean)
 * ````
 *
 * as it holds logic that must be handled for certain [InterfaceDestination]s.
 */
fun Player.openInterface(dest: InterfaceDestination, autoClose: Boolean = false) {
    val displayMode = if (!autoClose || dest.fullscreenChildId == -1) interfaces.displayMode else DisplayMode.FULLSCREEN
    val child = getChildId(dest, displayMode)
    val parent = getDisplayComponentId(displayMode)

    /*
     * Close interfaces from competing destinations
     */
    val close = dest.closeDestinationOnInterfaceOpened()
    close.forEach { closeInterfaceIfOpen(it) }

    openInterface(
        parent,
        child,
        dest.interfaceId,
        if (dest.clickThrough) 1 else 0,
        isModal = dest == InterfaceDestination.MAIN_SCREEN || dest == InterfaceDestination.MAIN_SCREEN_ALTERNATE
    )
}

fun Player.openInterface(parent: Int, child: Int, interfaceId: Int, type: Int = 0, isModal: Boolean = false) {
    if (isModal) {
        interfaces.openModal(parent, child, interfaceId)
    } else {
        interfaces.open(parent, child, interfaceId)
    }
    write(IfOpenSubMessage(parent, child, interfaceId, type))
}

fun Player.closeInterface(interfaceId: Int) {
    if (interfaceId == interfaces.getModal()) {
        interfaces.setModal(-1)
    }
    val hash = interfaces.close(interfaceId)
    if (hash != -1) {
        write(IfCloseSubMessage(hash))
    }
}

fun Player.closeInterface(dest: InterfaceDestination) {
    val displayMode = interfaces.displayMode
    val child = getChildId(dest, displayMode)
    val parent = getDisplayComponentId(displayMode)
    val hash = interfaces.close(parent, child)
    if (hash != -1) {
        write(IfCloseSubMessage((parent shl 16) or child))
    }
}

fun Player.closeInterfaceIfOpen(dest: InterfaceDestination, interfaceId: Int = -1) {
    val displayMode = interfaces.displayMode
    val child = getChildId(dest, displayMode)
    val parent = getDisplayComponentId(displayMode)
    if (interfaceId == -1 && interfaces.isOccupied(parent, child)
            || interfaceId != -1 && interfaces.getInterfaceAt(parent, child) != -1) {
        closeInterface(dest)
    }
}

fun Player.closeComponent(parent: Int, child: Int) {
    interfaces.close(parent, child)
    write(IfCloseSubMessage((parent shl 16) or child))
}

fun Player.closeDialogue() = this.closeComponent(parent = 162, child = CHATBOX_CHILD)

fun Player.closeInputDialog() {
    write(TriggerOnDialogAbortMessage())
}

fun Player.getInterfaceAt(dest: InterfaceDestination): Int {
    val displayMode = interfaces.displayMode
    val child = getChildId(dest, displayMode)
    val parent = getDisplayComponentId(displayMode)
    return interfaces.getInterfaceAt(parent, child)
}

fun Player.isInterfaceVisible(interfaceId: Int): Boolean = interfaces.isVisible(interfaceId)

fun Player.toggleDisplayInterface(newMode: DisplayMode) {
    if (interfaces.displayMode != newMode) {
        val oldMode = interfaces.displayMode
        interfaces.displayMode = newMode

        openOverlayInterface(newMode)
        runClientScript(3998, newMode.id)

        InterfaceDestination.values.filter { it.isSwitchable() }.forEach { pane ->
            val fromParent = getDisplayComponentId(oldMode)
            val fromChild = getChildId(pane, oldMode)
            val toParent = getDisplayComponentId(newMode)
            val toChild = getChildId(pane, newMode)

            /*
             * Remove the interfaces from the old display mode's chilren and add
             * them to the new display mode's children.
             */
            if (interfaces.isOccupied(parent = fromParent, child = fromChild)) {
                val oldComponent = interfaces.close(parent = fromParent, child = fromChild)
                if (oldComponent != -1) {
                    if (pane != InterfaceDestination.MAIN_SCREEN) {
                        interfaces.open(parent = toParent, child = toChild, interfaceId = oldComponent)
                    } else {
                        interfaces.openModal(parent = toParent, child = toChild, interfaceId = oldComponent)
                    }
                }
            }

            write(IfMoveSubMessage(from = (fromParent shl 16) or fromChild, to = (toParent shl 16) or toChild))
        }

        if (newMode.isResizable()) {
            setInterfaceUnderlay(color = -1, transparency = -1)
        }
        if (oldMode.isResizable()) {
            // TODO figure out what this does. it fixed the issue where you can't click when switch from resizeable to fixed.
            //openInterface(parent = getDisplayComponentId(newMode),child = getChildId(InterfaceDestination.MAIN_SCREEN, newMode), interfaceId = 60, type = 0)
        }
    }
}

fun Player.openOverlayInterface(displayMode: DisplayMode) {
    if (displayMode != interfaces.displayMode) {
        interfaces.setVisible(
            parent = getDisplayComponentId(interfaces.displayMode),
            child = getChildId(InterfaceDestination.MAIN_SCREEN, interfaces.displayMode),
            visible = false
        )
    }
    val component = getDisplayComponentId(displayMode)
    interfaces.setVisible(parent = getDisplayComponentId(displayMode), child = 0, visible = true)
    write(IfOpenTopMessage(component))
}

fun Player.sendItemContainer(key: Int, items: Array<Item?>) {
    write(UpdateInvFullMessage(containerKey = key, items = items))
}

fun Player.sendItemContainer(interfaceId: Int, component: Int, items: Array<Item?>) {
    write(UpdateInvFullMessage(interfaceId = interfaceId, component = component, items = items))
}

fun Player.sendItemContainer(interfaceId: Int, component: Int, key: Int, items: Array<Item?>) {
    write(UpdateInvFullMessage(interfaceId = interfaceId, component = component, containerKey = key, items = items))
}

fun Player.sendItemContainer(key: Int, container: ItemContainer) = sendItemContainer(key, container.rawItems)

fun Player.sendItemContainer(interfaceId: Int, component: Int, container: ItemContainer) =
    sendItemContainer(interfaceId, component, container.rawItems)

fun Player.sendItemContainer(interfaceId: Int, component: Int, key: Int, container: ItemContainer) =
    sendItemContainer(interfaceId, component, key, container.rawItems)

fun Player.updateItemContainer(interfaceId: Int, component: Int, oldItems: Array<Item?>, newItems: Array<Item?>) {
    write(
        UpdateInvPartialMessage(
            interfaceId = interfaceId,
            component = component,
            oldItems = oldItems,
            newItems = newItems
        )
    )
}

fun Player.updateItemContainer(
    interfaceId: Int,
    component: Int,
    key: Int,
    oldItems: Array<Item?>,
    newItems: Array<Item?>
) {
    write(
        UpdateInvPartialMessage(
            interfaceId = interfaceId,
            component = component,
            containerKey = key,
            oldItems = oldItems,
            newItems = newItems
        )
    )
}

fun Player.updateItemContainer(key: Int, oldItems: Array<Item?>, newItems: Array<Item?>) {
    write(UpdateInvPartialMessage(containerKey = key, oldItems = oldItems, newItems = newItems))
}

/**
 * Sends a container type referred to as 'invother' in CS2, which is used for displaying a second container with
 * the same container key. An example of this is the trade accept screen, where the list of items being traded is stored
 * in container 90 for both the player's container, and the partner's container. A container becomes 'invother' when it's
 * component hash is less than -70000, which internally translates the container key to (key + 32768). We can achieve this by either
 * sending a component hash of less than -70000, or by setting the key ourselves. I feel like the latter makes more sense.
 *
 * Special thanks to Polar for explaining this concept to me.
 *
 * https://github.com/RuneStar/cs2-scripts/blob/a144f1dceb84c3efa2f9e90648419a11ee48e7a2/scripts/script768.cs2
 */
fun Player.sendItemContainerOther(key: Int, container: ItemContainer) {
    write(UpdateInvFullMessage(containerKey = key + 32768, items = container.rawItems))
}

fun Player.sendRunEnergy(energy: Int) {
    write(UpdateRunEnergyMessage(energy))
}

fun Player.playSound(id: Int, volume: Int = 1, delay: Int = 0) {
    write(SynthSoundMessage(sound = id, volume = volume, delay = delay))
}

fun Player.playSong(id: Int) {
    write(MidiSongMessage(id))
}

fun Player.getVarp(id: Int): Int = varps.getState(id)

fun Player.setVarp(id: Int, value: Int) {
    varps.setState(id, value)
}

/**
 * Adds an [amount] to the varp given the [id]
 */
fun Player.addVarp(id: Int, amount: Int) {
    val old = this.getVarp(id)
    this.setVarp(id, old + amount)
}

/**
 * Decreases the varp given the [id] by an [amount]
 */
fun Player.decreaseVarp(id: Int, amount: Int) {
    val old = this.getVarp(id)
    this.setVarp(id, old - amount)
}

/**
 * Increments the varp by one given the [id]
 */
fun Player.incrVarp(id: Int) {
    val old = this.getVarp(id)
    this.setVarp(id, old + 1)
}

/**
 * Decrements the varp by one given the [id]
 */
fun Player.decrVarp(id: Int) {
    val old = this.getVarp(id)
    this.setVarp(id, old - 1)
}

fun Player.toggleVarp(id: Int) {
    varps.setState(id, varps.getState(id) xor 1)
}

fun Player.syncVarp(id: Int) {
    setVarp(id, getVarp(id))
}

fun Player.getVarbit(id: Int): Int {
    val def = world.definitions.get(VarbitDef::class.java, id)
    return varps.getBit(def.varp, def.startBit, def.endBit)
}

fun Player.setVarbit(id: Int, value: Int) {
    val def = world.definitions.get(VarbitDef::class.java, id)
    varps.setBit(def.varp, def.startBit, def.endBit, value)
}

/**
 * Add an [amount] to the varbit given the [id]
 */
fun Player.addVarbit(id: Int, amount: Int) {
    val old = this.getVarbit(id)
    this.setVarbit(id, old + amount)
}

/**
 * Decrease the varbit given the [id] by an [amount]
 */
fun Player.decreaseVarbit(id: Int, amount: Int) {
    val old = this.getVarbit(id)
    this.setVarbit(id, old - amount)
}

/**
 * Increments the varbit by one given the [id]
 */
fun Player.incrVarbit(id: Int) {
    val old = this.getVarbit(id)
    this.setVarbit(id, old + 1)
}

/**
 * Decrements the varbit by one given the [id]
 */
fun Player.decrVarbit(id: Int) {
    val old = this.getVarbit(id)
    this.setVarbit(id, old - 1)
}

/**
 * Write a varbit message to the player's client without actually modifying
 * its varp value in [Player.varps].
 */
fun Player.sendTempVarbit(id: Int, value: Int) {
    val def = world.definitions.get(VarbitDef::class.java, id)
    val state = BitManipulation.setBit(varps.getState(def.varp), def.startBit, def.endBit, value)
    val message = if (state in -Byte.MAX_VALUE..Byte.MAX_VALUE) VarpSmallMessage(def.varp, state) else VarpLargeMessage(
        def.varp,
        state
    )
    write(message)
}

fun Player.toggleVarbit(id: Int) {
    val def = world.definitions.get(VarbitDef::class.java, id)
    varps.setBit(def.varp, def.startBit, def.endBit, getVarbit(id) xor 1)
}

fun Player.setMapFlag(x: Int, z: Int) {
    write(SetMapFlagMessage(x, z))
}

fun Player.clearMapFlag() {
    setMapFlag(255, 255)
}

fun Player.sendOption(option: String, id: Int, leftClick: Boolean = false) {
    check(id in 1..options.size) { "Option id must range from [1-${options.size}]" }
    val index = id - 1
    options[index] = option
    write(SetOpPlayerMessage(option, index, leftClick))
}

/**
 * Checks if the player has an option with the name [option] (case-sensitive).
 */
fun Player.hasOption(option: String, id: Int = -1): Boolean {
    check(id == -1 || id in 1..options.size) { "Option id must range from [1-${options.size}]" }
    return if (id != -1) options.any { it == option } else options[id - 1] == option
}

/**
 * Removes the option with [id] from this player.
 */
fun Player.removeOption(id: Int) {
    check(id in 1..options.size) { "Option id must range from [1-${options.size}]" }
    val index = id - 1
    write(SetOpPlayerMessage("null", index, false))
    options[index] = null
}

fun Player.getStorageBit(storage: BitStorage, bits: StorageBits): Int = storage.get(this, bits)

fun Player.hasStorageBit(storage: BitStorage, bits: StorageBits): Boolean = storage.get(this, bits) != 0

fun Player.setStorageBit(storage: BitStorage, bits: StorageBits, value: Int) {
    storage.set(this, bits, value)
}

fun Player.toggleStorageBit(storage: BitStorage, bits: StorageBits) {
    storage.set(this, bits, storage.get(this, bits) xor 1)
}

fun Player.heal(amount: Int, capValue: Int = 0) {
    getSkills().alterCurrentLevel(skill = Skills.HITPOINTS, value = amount, capValue = capValue)
}

fun Player.getWeaponType(): Int = getVarbit(357)

fun Player.getAttackStyle(): Int = getVarp(43)

fun Player.hasWeaponType(type: WeaponType, vararg others: WeaponType): Boolean =
    getWeaponType() == type.id || others.isNotEmpty() && getWeaponType() in others.map { it.id }

fun Player.hasEquipped(slot: EquipmentType, vararg items: Int): Boolean {
    check(items.isNotEmpty()) { "Items shouldn't be empty." }
    return items.any { equipment.hasAt(slot.id, it) }
}

fun Player.hasEquipped(items: IntArray) = items.all { equipment.contains(it) }

fun Player.hasAnyEquipped(items: IntArray) = items.any { equipment.contains(it) }

fun Player.hasItemEquipped(item: Int) = equipment.contains(item)

fun Player.getEquipment(slot: EquipmentType): Item? = equipment[slot.id]

fun Player.hasItemInInventory(item: Int) = inventory.contains(item)

fun Player.setSkullIcon(icon: SkullIcon) {
    skullIcon = icon.id
    addBlock(UpdateBlockType.APPEARANCE)
}

fun Player.skull(icon: SkullIcon, durationCycles: Int) {
    check(icon != SkullIcon.NONE)
    setSkullIcon(icon)
    timers[SKULL_ICON_DURATION_TIMER] = durationCycles
}

fun Player.hasSkullIcon(icon: SkullIcon): Boolean = skullIcon == icon.id

fun Player.isClientResizable(): Boolean =
    interfaces.displayMode == DisplayMode.RESIZABLE_CLASSIC || interfaces.displayMode == DisplayMode.RESIZABLE_MODERN

fun Player.inWilderness(): Boolean = getInterfaceAt(InterfaceDestination.PVP_OVERLAY) != -1

fun Player.sendWorldMapTile() {
    runClientScript(1749, tile.as30BitInteger)
}

fun Player.sendCombatLevelText() {
    setComponentText(593, 3, "Combat Lvl: $combatLevel")
}

fun Player.sendWeaponComponentInformation() {
    val weapon = getEquipment(EquipmentType.WEAPON)

    val name: String
    val panel: Int

    if (weapon != null) {
        val definition = world.definitions.get(ItemDef::class.java, weapon.id)
        name = definition.name

        panel = Math.max(0, definition.weaponType)
    } else {
        name = "Unarmed"
        panel = 0
    }

    setComponentText(593, 1, name)
    setVarbit(357, panel)
}

fun Player.calculateAndSetCombatLevel(): Boolean {
    val old = combatLevel

    val attack = getSkills().getMaxLevel(Skills.ATTACK)
    val defence = getSkills().getMaxLevel(Skills.DEFENCE)
    val strength = getSkills().getMaxLevel(Skills.STRENGTH)
    val hitpoints = getSkills().getMaxLevel(Skills.HITPOINTS)
    val prayer = getSkills().getMaxLevel(Skills.PRAYER)
    val ranged = getSkills().getMaxLevel(Skills.RANGED)
    val magic = getSkills().getMaxLevel(Skills.MAGIC)

    val base = Ints.max(strength + attack, magic * 2, ranged * 2)

    combatLevel = ((base * 1.3 + defence + hitpoints + prayer / 2) / 4).toInt()

    val changed = combatLevel != old
    if (changed) {
        runClientScript(389, combatLevel)
        sendCombatLevelText()
        addBlock(UpdateBlockType.APPEARANCE)
        return true
    }

    return false
}

fun Player.calculateDeathContainers(): DeathContainers {
    var keepAmount = if (hasSkullIcon(SkullIcon.WHITE)) 0 else 3
    if (getVarbit(599) == 1) keepAmount++
    if (attr[PROTECT_ITEM_ATTR] == true) {
        keepAmount++
    }

    val keptContainer = ItemContainer(world.definitions, keepAmount, ContainerStackType.NO_STACK)
    val lostContainer =
        ItemContainer(world.definitions, inventory.capacity + equipment.capacity, ContainerStackType.NORMAL)

    var totalItems = inventory.rawItems.filterNotNull() + equipment.rawItems.filterNotNull()
    val valueService = world.getService(ItemMarketValueService::class.java)

    totalItems = if (valueService != null) {
        totalItems.sortedBy { it.id }.sortedWith(compareByDescending { valueService.get(it.id) })
    } else {
        totalItems.sortedBy { it.id }
            .sortedWith(compareByDescending { world.definitions.get(ItemDef::class.java, it.id).cost })
    }

    totalItems.forEach { item ->
        if (keepAmount > 0 && !keptContainer.isFull) {
            val add = keptContainer.add(item, assureFullInsertion = false)
            keepAmount -= add.completed
            if (add.getLeftOver() > 0) {
                lostContainer.add(item.id, add.getLeftOver())
            }
        } else {
            lostContainer.add(item)
        }
    }

    return DeathContainers(kept = keptContainer, lost = lostContainer)
}

// Note: this does not take ground items, that may belong to the player, into account.
fun Player.hasItem(item: Int, amount: Int = 1): Boolean =
    containers.values.firstOrNull { container -> container.getItemCount(item) >= amount } != null

fun Player.isPrivilegeEligible(to: String): Boolean = world.privileges.isEligible(privilege, to)

fun Player.getStrengthBonus(): Int = equipmentBonuses[10]

fun Player.getRangedStrengthBonus(): Int = equipmentBonuses[11]

fun Player.getMagicDamageBonus(): Int = equipmentBonuses[12]

fun Player.getPrayerBonus(): Int = equipmentBonuses[13]

fun Player.openUrl(url: String) {
    write(OpenUrlMessage(url))
}

fun Player.nothingMessage() {
    message(Entity.NOTHING_INTERESTING_HAPPENS)
}

fun Player.isTeleBlocked(): Boolean = this.getVarbit(4163) > 0

fun Player.isInFeroxSafetyBorder(): Boolean = this.getVarbit(SAFETY_BORDER_VARBIT) == 1

fun Player.getKiller(): Optional<Pawn> = Optional.ofNullable(this.attr[KILLER_ATTR]?.get())

fun Player.getRecentKills(): EvictingQueue<String> = this.attr.getOrDefault(RECENT_KILLS, EvictingQueue.create(3))

fun Player.getBountyHunterPoints(): Int = this.attr.getOrDefault(BOUNTY_HUNTER_POINTS, 0)

fun Player.drainRunEnergy(amount: Int) {
    this.runEnergy -= amount
    if (this.runEnergy <= 0)
        this.runEnergy = 0.0
    sendRunEnergy(runEnergy.toInt())
}

fun Player.restoreRunEnergy(amount: Int) {
    this.runEnergy += amount
    if (this.runEnergy > 100)
        this.runEnergy = 100.0
    sendRunEnergy(runEnergy.toInt())
}

/**
 * Drains a [Player]s skill and returns the amount that was drained
 * @param skillId The skill that is being trained
 * @param drainPercentage The percentage amount to drain the skill by
 * @param capped Determines whether the skill should drain based on the max level
 */
fun Player.drainSkill(skillId: Int, drainPercentage: Double, capped: Boolean = true): Int {
    val currentLevel = this.getSkills().getCurrentLevel(skillId)
    val drainAmount = currentLevel * drainPercentage
    this.getSkills().decrementCurrentLevel(skillId, drainAmount.toInt(), capped)
    return currentLevel - this.getSkills().getCurrentLevel(skillId)
}

/**
 * Drains a [Player]s skill and returns the amount that was drained
 * @param skillId The skill that is being trained
 * @param drainAmount The amount to drain the skill by
 * @param capped Determines whether the skill should drain based on the max level
 */
fun Player.drainSkill(skillId: Int, drainAmount: Int, capped: Boolean = true): Int {
    val currentLevel = this.getSkills().getCurrentLevel(skillId)
    this.getSkills().decrementCurrentLevel(skillId, drainAmount, capped)
    return currentLevel - this.getSkills().getCurrentLevel(skillId)
}
/**
 * Collects a [Set] of [ItemContainer]s based on a set of [containerKeys]
 */
fun Player.getContainers(containerKeys: Set<ContainerKey>): Set<ItemContainer> {
    return this.containers.filterValues { it.key in containerKeys }.values.toSet()
}

/**
 * Identifies which of the two used items is equal to the ID
 */
fun Player.identifyUseItem(itemId: Int): Item? {
    val itemOne = this.getInteractingItemId()
    val itemTwo = this.getOtherInteractingItemId()
    return when {
        itemOne == itemId -> this.getInteractingItem()
        itemTwo == itemId -> this.getOtherInteractingItem()
        else -> null
    }
}

/**
 * Identifies which of the two used slots is equal to the ID
 */
fun Player.identifyUseSlot(itemId: Int): Int? {
    val itemOne = this.getInteractingItemId()
    val itemTwo = this.getOtherInteractingItemId()
    return when {
        itemOne == itemId -> this.getInteractingItemSlot()
        itemTwo == itemId -> this.getOtherInteractingItemSlot()
        else -> null
    }
}

/**
 * Teleblocks a [player] for a certain amount of [time]
 *
 * @param time     The amount of time to teleblock a player for
 */
fun Player.teleblock(time: Int) {
    this.timers[TELEBLOCK_TIMER] = 1
    this.setVarbit(4163, 100 + time)
    this.message("You have been teleblocked!")
}

/**
 * Clears a [player]s teleblock varbits
 */
fun Player.clearTeleblock() {
    this.timers.remove(TELEBLOCK_TIMER)
    this.setVarbit(4163, 0)
}

/**
 * Determines if the [Player] has Vengeance cast
 */
fun Player.hasVengeance(): Boolean = this.getVarbit(2450) == 1

/**
 * Determines if the [Player] has Vengeance cast delay
 */
fun Player.hasVengeanceDelay(): Boolean = this.getVarbit(2451) == 1

/**
 * Clears the [Player]s vengeance
 */
fun Player.clearVengeance() = this.setVarbit(2450, 0)

/**
 * Clears the [Player]s vengeance delay
 */
fun Player.clearVengeanceDelay() = this.setVarbit(2451, 0)

/**
 * Determines if the [Player] is accepting aid or not.
 */
fun Player.isAcceptingAid(): Boolean = this.getVarbit(4180) == 1

/**
 * Determines if the [Player] is fighting another player in single combat
 */
fun Player.isFightingPlayerInSingle(): Boolean {
    val combatTarget = this.getCombatTarget() ?: return false
    return when (combatTarget.entityType.isPlayer) {
        true -> {
            return !this.tile.isMulti(this.world)
        }
        else -> false
    }
}

/**
 * If this player has logged in for the first time on this login.
 */
fun Player.isFirstLogin(): Boolean {
    return attr[NEW_ACCOUNT_ATTR] ?: false
}