package data.codex

import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.codex.CodexDataV2
import data.niko_MPC_modPlugin
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings

object CodexData {
    fun linkCodexEntries() {
        val overgrownCodex = CodexDataV2.getEntry(CodexDataV2.getConditionEntryId("niko_MPC_overgrownNanoforgeCondition"))
        val overgrownIndustry = CodexDataV2.getEntry(CodexDataV2.getIndustryEntryId("niko_MPC_overgrownNanoforge"))
        val overgrownItem = CodexDataV2.getEntry(CodexDataV2.getItemEntryId("niko_MPC_overgrownNanoforgeItem"))

        overgrownItem.addRelatedEntry(overgrownCodex)
        overgrownCodex.addRelatedEntry(overgrownItem)
        overgrownCodex.addRelatedEntry(overgrownIndustry)
        overgrownIndustry.addRelatedEntry(overgrownCodex)

        /*createReciprocalLink(overgrownItem.id, CodexDataV2.getIndustryEntryId(Industries.HEAVYINDUSTRY))
        createReciprocalLink(overgrownItem.id, CodexDataV2.getIndustryEntryId(Industries.ORBITALWORKS))
        createReciprocalLink(overgrownItem.id, CodexDataV2.getConditionEntryId(Conditions.HABITABLE))
        createReciprocalLink(overgrownItem.id, CodexDataV2.getConditionEntryId(Conditions.POLLUTION))*/

        val toteliacCodex = CodexDataV2.getEntry(CodexDataV2.getConditionEntryId("niko_MPC_carnivorousFauna"))
        createReciprocalLink(toteliacCodex.id, CodexDataV2.getIndustryEntryId(Industries.PATROLHQ))
        createReciprocalLink(toteliacCodex.id, CodexDataV2.getIndustryEntryId(Industries.MILITARYBASE))
        createReciprocalLink(toteliacCodex.id, CodexDataV2.getIndustryEntryId(Industries.HIGHCOMMAND))

        if (niko_MPC_settings.AOTD_vaultsEnabled) {
            //createReciprocalLink(toteliacCodex.id, CodexDataV2.getIndustryEntryId("militarygarrison"))
        }

        val ultraMagneticFieldId = CodexDataV2.getConditionEntryId("niko_MPC_ultraMagneticField")
        createReciprocalLink(ultraMagneticFieldId, CodexDataV2.getIndustryEntryId(Industries.MINING))
        createReciprocalLink(ultraMagneticFieldId, CodexDataV2.getIndustryEntryId(Industries.REFINING))
    }

    private fun createReciprocalLink(entryIdOne: String, entryIdTwo: String) {
        val entryOne = CodexDataV2.getEntry(entryIdOne)
        val entryTwo = CodexDataV2.getEntry(entryIdTwo)

        entryOne.addRelatedEntry(entryTwo)
        entryTwo.addRelatedEntry(entryOne)
    }
}