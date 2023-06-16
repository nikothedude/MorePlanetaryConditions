package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories

class overgrownNanoforgeSpawnFleetEffect(
    nanoforgeHandler: overgrownNanoforgeHandler,
    val hostile: Boolean,
    val respawnMin: Float,
    val respawnMax: Float,
    val fpMax: Float,
    spawnAll: Boolean = shouldSpawnAll(nanoforgeHandler.market.containingLocation)
): overgrownNanoforgeRandomizedEffect(nanoforgeHandler) {
    val spawningScript: overgrownNanoforgeSpawnFleetScript = overgrownNanoforgeSpawnFleetScript(
        this,
        hostile,
        respawnMin,
        respawnMax,
        fpMax,
        spawnAll)

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (hostile) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    override fun getName(): String {
        var name = "Derelict auto-factory"
        if (isPositive()) name = "Hackable $name"
        return name
    }

    override fun getDescription(): String {
        val clock = Global.getSector().clock
        var desc = "Spawns a derelict fleet every ${clock.convertToDays(respawnMin)} - ${clock.convertToDays(respawnMax)} days, up to a max of $fpMax FP."
        desc += if (isPositive()) "\nThe fleets are of the market's faction, derelict if unowned." else "\nThe fleets are always hostile."
        return desc
    }

    override fun applyEffects() {
        spawningScript.apply()
    }

    override fun unapplyEffects() {
        spawningScript.unapply()
    }

    override fun delete() {
        spawningScript.delete()
        super.delete()
    }

    companion object {
        fun shouldSpawnAll(location: LocationAPI): Boolean {
            return Global.getCurrentState() == GameState.TITLE
        }
    }
}
