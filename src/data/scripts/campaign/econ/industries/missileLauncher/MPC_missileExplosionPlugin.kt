package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin

class MPC_missileExplosionPlugin: ExplosionEntityPlugin() {

    fun getAlreadyDamaged(): LinkedHashSet<String> = damagedAlready

}