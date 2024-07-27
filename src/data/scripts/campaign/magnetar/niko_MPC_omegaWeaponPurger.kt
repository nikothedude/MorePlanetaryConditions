package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import data.utilities.niko_MPC_battleUtils.getContainingLocation
import data.utilities.niko_MPC_ids

class niko_MPC_omegaWeaponPurger: BaseCampaignEventListener(false) {
    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)
        if (plugin == null || loot == null) return

        val battle = plugin.battle
        for (fleet in battle.bothSides) {
            if (fleet.faction.id == niko_MPC_ids.OMEGA_DERELICT_FACTION_ID) {
                purgeOmegaWeapons(loot)
            }
        }
    }

    companion object {
        fun purgeOmegaWeapons(loot: CargoAPI) {
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