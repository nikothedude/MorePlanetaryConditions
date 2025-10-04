package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import kotlin.math.ceil

class MPC_missileExplosionPlugin: ExplosionEntityPlugin() {

    var source: MPC_aegisMissileEntityPlugin? = null

    fun getAlreadyDamaged(): LinkedHashSet<String> = damagedAlready

    override fun applyDamageToFleet(fleet: CampaignFleetAPI?, damageMult: Float) {
        super.applyDamageToFleet(fleet, damageMult)
        if (fleet == null) return
        if (damageMult <= 0f) return
        //if (params.damage == ExplosionFleetDamage.NONE) return

        val origin = source?.params?.origin ?: return
        if (!origin.isPlayerFleet) return

        if (fleet.knowsWhoPlayerIs() && !fleet.faction.isPlayerFaction) {
            val baseRepLoss = source!!.params.spec.getBaseRepLoss()
            val actualLoss = ceil(baseRepLoss * damageMult).coerceAtMost(baseRepLoss)
            if (actualLoss <= 0f) return
            val impact = CoreReputationPlugin.CustomRepImpact()
            impact.delta = (-actualLoss * 0.01f)
            impact.limit = RepLevel.HOSTILE
            Global.getSector().adjustPlayerReputation(
                RepActionEnvelope(RepActions.CUSTOM, impact, null, false),
                fleet.faction.id
            )
        }
    }

}