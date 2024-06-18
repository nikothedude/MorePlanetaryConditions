package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_coronaResistFleetManagerScript
import data.utilities.*

class niko_MPC_coronaResistStation: BaseCommandPlugin() {

    protected var playerFleet: CampaignFleetAPI? = null
    protected var entity: SectorEntityToken? = null
    protected var playerFaction: FactionAPI? = null
    protected var entityFaction: FactionAPI? = null
    protected var text: TextPanelAPI? = null
    protected var options: OptionPanelAPI? = null
    protected var playerCargo: CargoAPI? = null
    protected var memory: MemoryAPI? = null
    protected var market: MarketAPI? = null
    protected var dialog: InteractionDialogAPI? = null
    protected var memoryMap: Map<String, MemoryAPI>? = null
    protected var faction: FactionAPI? = null

    companion object {
        const val CREDITS_TO_BUY_STATION = 250000f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (ruleId == null || dialog == null || params == null) return false
        this.memoryMap = memoryMap
        this.dialog = dialog

        val command = params[0].getString(memoryMap) ?: return false

        entity = dialog.interactionTarget
        init(entity!!)

        if (memoryMap != null) memory = getEntityMemory(memoryMap)

        text = dialog.textPanel
        options = dialog.optionPanel

        if (command == "genLoot") {
            genLoot()
        } else if (command == "fleetInStation") {
            return fleetInStation()
        } else if (command == "stationFriendly") { // otherwise, they are beligerent
            return stationFriendly()
        } else if (command == "beginInitialConfrontation") {
            return beginConfrontation()
        } else if (command == "beginConfrontation") {
            return beginConfrontation()
        } else if (command == "wouldBeFriendlyIfTOn") {
            stationFriendly(false)
        } else if (command == "toggleTransponder") {
            Global.getSector().playerFleet.isTransponderOn = !Global.getSector().playerFleet.isTransponderOn
        } else if (command == "disarm") {
            val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = false
            fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true)
        } else if (command == "getRidOfFleets") {
            entity!!.removeScriptsOfClass(MPC_coronaResistFleetManagerScript::class.java)
            val coreFleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
            for (fleet in (entity!!.containingLocation.fleets - coreFleet)) {
                if ((fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_DEFENDER] != true)) continue
                for (target in fleet.fleetData.membersListCopy) {
                    fleet.removeFleetMemberWithDestructionFlash(target)
                }
            }
        } else if (command == "hasEnoughCreditsToBuy") {
            val credits = Global.getSector().playerFleet.cargo.credits
            return (credits.get() >= CREDITS_TO_BUY_STATION)
        } else if (command == "deductCredits") {
            val credits = Global.getSector().playerFleet.cargo.credits
            credits.subtract(CREDITS_TO_BUY_STATION)
        } else if (command == "friendlyAction") {
            val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
            fleet.commander.relToPlayer.adjustRelationship(20f, RepLevel.COOPERATIVE)
        } else if (command == "wantsToSell") {
            val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
            if (fleet.commander.memoryWithoutUpdate["\$MPC_defeatedMentally"] == true) return true
            return stationFriendly()
        } else if (command == "leaveToJangala") {
            val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
            memory?.unset(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET)

            entity!!.containingLocation.addEntity(fleet)
            fleet.location.set(entity!!.location)

            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, getTargetForTravel(), 999999f)
            fleet.setFaction(Factions.INDEPENDENT)
        }
        return true
    }

    private fun getTargetForTravel(): SectorEntityToken {
        val jangala = Global.getSector().getEntityById("jangala")
        if (jangala != null) return jangala

        return Global.getSector().economy.marketsCopy.random().primaryEntity
    }

    private fun stationFriendly(considerTransponder: Boolean = true): Boolean {
        val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return true
        if (fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE] == true) return true
        if (considerTransponder && !Global.getSector().playerFleet.isTransponderOn) return false
        return (fleet.faction.relToPlayer.level >= RepLevel.COOPERATIVE)
    }

    private fun beginConfrontation(initial: Boolean = false): Boolean {
        val cachedMemoryMap = memoryMap
        val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return false
        niko_MPC_miscUtils.refreshCoronaDefenderFleetVariables(fleet) // sanity

        val containingLocation = entity!!.containingLocation
        containingLocation.addEntity(fleet)
        fleet.location.set(entity!!.location)

        if (dialog == null) return false

        val entity = dialog!!.interactionTarget
        dialog!!.interactionTarget = fleet
        val config = FleetInteractionDialogPluginImpl.FIDConfig()
        config.impactsEnemyReputation = false
        config.dismissOnLeave = false
        config.printXPToDialog = true

        val plugin = FleetInteractionDialogPluginImpl(config)
        val originalPlugin = dialog!!.plugin
        config.delegate = object : FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            override fun notifyLeave(dialog: InteractionDialogAPI) {
                fleet.containingLocation?.removeEntity(fleet)
                fleet.containingLocation = null

                dialog.plugin = originalPlugin
                dialog.interactionTarget = entity
                if (plugin.context is FleetEncounterContext) {
                    val context = plugin.context as FleetEncounterContext
                    if (context.didPlayerWinEncounterOutright()) {
                        /*for (handler: niko_MPC_satelliteHandlerCore in )
                        incrementSatelliteGracePeriod(
                            Global.getSector().playerFleet,
                            niko_MPC_ids.satellitePlayerVictoryIncrement,
                            entityFocus
                        ) */
                        FireBest.fire(null, dialog, cachedMemoryMap, "MPC_coronaResistStationFleetDefeated")
                    } else {
                        dialog.dismiss()
                    }
                } else {
                    dialog.dismiss()
                }
            }
        }
        dialog!!.plugin = plugin
        plugin.init(dialog)
        return true
    }


    private fun fleetInStation(): Boolean {
        val fleet = memory?.getFleet(niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET) ?: return false
        return fleet.containingLocation == null
    }

    private fun genLoot() {
        val memory = entity!!.memoryWithoutUpdate
        val seed = memory.getLong(MemFlags.SALVAGE_SEED)
        val random = Misc.getRandom(seed, 100)

        var d = DropData()
        d.chances = 3
        d.group = "blueprints"
        entity!!.addDropRandom(d)

        d = DropData()
        d.chances = 1
        d.group = "rare_tech"
        entity!!.addDropRandom(d)

        d = DropData()
        d.chances = 600
        d.group = "basic"
        entity!!.addDropRandom(d)

        d = DropData()
        d.chances = 1
        d.group = "any_hullmod_high"
        entity!!.addDropRandom(d)

        d = DropData()
        d.chances = 1
        d.group = "blueprints_low"
        entity!!.addDropRandom(d)

        d = DropData()
        d.chances = 1
        d.group = "blueprints_guaranteed"
        entity!!.addDropRandom(d)

        val salvage = SalvageEntity.generateSalvage(random, 1f, 1f, 1f, 1f, entity!!.dropValue, entity!!.dropRandom)

        val extra = BaseSalvageSpecial.getExtraSalvage(memoryMap)
        if (extra != null) {
            salvage.addAll(extra.cargo)
            BaseSalvageSpecial.clearExtraSalvage(memoryMap)
        }
        /*salvage.addSpecial(SpecialItemData(Items.INDUSTRY_BP, niko_MPC_industryIds.coronaResistIndustry), 1f)
        salvage.addSpecial(SpecialItemData(Items.MODSPEC, "niko_MPC_fighterSolarShielding"), 1f)*/

        salvage.sort()

        Global.getSector().playerFleet.cargo.addSpecial(SpecialItemData(Items.INDUSTRY_BP, niko_MPC_industryIds.coronaResistIndustry), 1f)
        Global.getSector().playerFleet.cargo.addSpecial(SpecialItemData(Items.MODSPEC, "niko_MPC_fighterSolarShielding"), 1f)
    }

    protected fun init(entity: SectorEntityToken) {
        memory = entity.memoryWithoutUpdate
        this.entity = entity
        playerFleet = Global.getSector().playerFleet
        playerCargo = playerFleet!!.cargo
        playerFaction = Global.getSector().playerFaction
        entityFaction = entity.faction
        faction = entity.faction
        market = entity.market
    }
}
