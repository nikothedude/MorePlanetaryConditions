package data.scripts.campaign.econ.conditions.overgrownNanoforge

import data.scripts.campaign.econ.conditions.niko_MPC_industryAddingCondition
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_settings

class niko_MPC_overgrownNanoforge : niko_MPC_industryAddingCondition() {

    init {
        industryIds.add(niko_MPC_industryIds.overgrownNanoforgeIndustryId)
    }

    override fun apply(id: String) {
        super.apply(id)
    }

}