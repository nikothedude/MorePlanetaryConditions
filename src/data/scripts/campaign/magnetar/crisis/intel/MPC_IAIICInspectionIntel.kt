package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate

class MPC_IAIICInspectionIntel(system: StarSystemAPI?, faction: FactionAPI?, delegate: RaidDelegate?): RaidIntel(system, faction, delegate), RaidDelegate {

    protected var expectedCores: List<String> = ArrayList()

}