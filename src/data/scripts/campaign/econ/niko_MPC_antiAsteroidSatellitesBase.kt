package data.scripts.campaign.econ

import data.utilities.niko_MPC_industryIds

abstract class niko_MPC_antiAsteroidSatellitesBase : niko_MPC_industryAddingCondition() {

    init {
        industryIds.add(niko_MPC_industryIds.luddicPathSuppressorStructureId)
    }

}