package data.scripts.campaign.magnetar.optimizedFit

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource

/** A custom ship loadout, with support for smods and for an officer as well. */
class MPC_optimizedFit(
    val variantId: String,
    val createOfficer: () -> PersonAPI
) {
    fun create(): FleetMemberAPI {
        val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId)
        val officer = createOfficer()

        /*val clonedVariant = member.variant.clone()
        clonedVariant.source = VariantSource.REFIT
        for (smod in smods) {
            clonedVariant.addPermaMod(smod, true)
        }
        member.setVariant(clonedVariant, false, true)*/

        member.captain = officer
        return member
    }
}