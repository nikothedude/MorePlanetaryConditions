package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import data.utilities.niko_MPC_battleUtils.getContainingLocation
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings.MAGNETAR_DROP_OMEGA_WEAPONS

class niko_MPC_omegaWeaponPurger: BaseCampaignEventListener(false) {
    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)

        if (MAGNETAR_DROP_OMEGA_WEAPONS) return

        if (plugin == null || loot == null) {
            niko_MPC_debugUtils.log.info("aborted omega weapon purge, plugin: $plugin, loot: $loot")
            return
        } else {
            niko_MPC_debugUtils.log.info("attempting omega weapon purge")
        }

        val battle = plugin.battle
        for (fleet in battle.bothSides) {
            if (fleet.faction.id == niko_MPC_ids.OMEGA_DERELICT_FACTION_ID && !fleet.hasTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)) {
                purgeOmegaWeapons(loot)
            } else {
                niko_MPC_debugUtils.log.info("did not purge weapons of ${fleet.name}, factionId = ${fleet.faction.id}, hasTag = ${fleet.hasTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)}")
            }
        }
    }

    companion object {
        fun purgeOmegaWeapons(loot: CargoAPI) {
            niko_MPC_debugUtils.log.info("purging post-battle omega weapons")
            for (weapon in loot.weapons.toList()) {
                val item = weapon.item
                val weaponSpec = Global.getSettings().getWeaponSpec(item) ?: continue
                if (weaponSpec.tags.contains("omega")) {
                    loot.removeWeapons(item, weapon.count)
                }
            }
        }
    }
}