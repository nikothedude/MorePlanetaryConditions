package data.utilities

object niko_MPC_ids {
    const val NEX_GROUND_REPORT_PLUGIN_ID = "MPC_groundReportPlugin"
    const val hyperspaceLinkedExitJumppoints = "\$niko_MPC_hyperspaceLinkedExitJumppoints" // used for scanning in hyperspace
    const val hyperspaceLinkedJumpPointDesignationId = "\$niko_MPC_hyperspaceLinkedJumpPoint"
    const val hyperMagneticFieldMemoryId = "\$niko_MPC_hyperMagneticFieldMemoryId"
    const val hyperspaceLinkedTerrainMemoryId = "\$niko_MPC_hyperspaceLinkedTerrain"
    const val hyperspaceLinkedJumpPointEntryMemoryId = "\$niko_MPC_hyperspaceLinkedJumpPointEntry"
    const val hyperspaceLinkedJumpPointExitMemoryId = "\$hyperspaceLinkedJumpPointExitMemoryId"
    const val hyperspaceLinkedSavedCellsMemoryId = "\$hyperspaceLinkedSavedCellsId"

    const val mesonFieldGlobalMemoryId = "\$niko_MPC_mesonFieldGlobalMemoryId"

    const val overgrownNanoforgeConditionId = "niko_MPC_overgrownNanoforgeCondition"
    const val overgrownNanoforgeHandlerMemoryId = "\$niko_MPC_overgrownNanoforgeHandler"
    const val overgrownNanoforgeJunkHandlerMemoryId = "\$niko_MPC_overgrownNanoforgeJunkHandler_"
    const val overgrownNanoforgeItemId = "niko_MPC_overgrownNanoforgeItem"
    const val INTEL_OVERGROWN_NANOFORGES = "Overgrown Nanoforge"
    const val INTEL_OVERGROWN_NANOFORGES_MARKET = "$INTEL_OVERGROWN_NANOFORGES: "
    const val conditionLinkedHandlerMemoryId = "\$conditionLinkedMemoryHandlerId"
    const val niko_MPC_modId = "niko_morePlanetaryConditions"
    const val niko_MPC_masterConfig = "niko_MPC_settings.json"
    const val overgrownNanoforgeFleetScriptListMemoryId = "\$niko_MPC_overgrownFleetSpawnScripts"
    /**
     * A list of all possible satellite condition Ids. PLEASE UPDATE THIS IF YOU ADD A NEW ONE
     */
    val satelliteConditionIds: List<String> = listOf("niko_MPC_antiAsteroidSatellites")

    const val satelliteBarrageTerrainId = "niko_MPC_defenseSatelliteBarrage"

    const val niko_MPC_campaignPluginId = "niko_MPC_campaignPlugin"
    const val overgrownNanoforgeFleetsizeRemovalScriptId = "\$niko_MPC_overgrownNanoforgeFleetsizeRemovalScriptId"
    const val satelliteMarketId = "\$niko_MPC_satelliteMarket"
    const val isSatelliteFleetId = "\$niko_MPC_isSatelliteFleet"
    /**
     * Stored value of the MemoryAPI key associated with the satellite tracker. Here for ease of use and modification.
     */
    const val satelliteHandlersId = "\$niko_MPC_satelliteHandler"
    const val satelliteEntityHandler = "\$niko_MPC_satelliteHandlerForSatelliteEntities"
    const val satelliteHandlerIdAlt = "\$niko_MPC_satelliteHandlerAlt"
    const val defenseSatelliteImpactId = "niko_MPC_defenseSatelliteImpact"
    const val defenseSatelliteImpactReasonString = "Defense Satellite Artillery Impact"
    const val niko_MPC_isSatelliteHullId = "niko_MPC_isSatelliteHull"
    const val satellitePlayerVictoryIncrement = 10f
    const val satelliteVictoryGraceIncrement = 40f
    const val satelliteBattleTrackerId = "\$niko_MPC_satelliteBattleTracker"
    const val satelliteFleetHostileReason = "\$niko_MPC_satelliteFleetHostileReason"
    const val satelliteFactionId = "niko_MPC_satelliteFaction"
    const val temporaryFleetDespawnerId = "\$niko_MPC_temporaryFleetDespawner"
    const val derelictSatelliteFakeFactionId = "derelictSatelliteBuilder"
    const val overgrownNanoforgeFleetFactionId = "overgrownNanoforgeFleet"
    const val satelliteTagId = "niko_MPC_satellite"
    const val scriptAdderId = "\$niko_MPC_scriptAdderId"
    const val satelliteCustomEntityRemoverScriptId = "\$niko_MPC_satelliteCustomEntityRemoverScriptId"
    const val cosmeticSatelliteTagId = "niko_MPC_satellite"
    const val globalSatelliteHandlerListId = "\$niko_MPC_globalSatelliteHandlerList"

    const val isDummyFleetId = "\$niko_MPC_isDummyFleet"

    const val CORONA_RESIST_STATION_TAG = "MPC_coronaResistStationTag"
    const val CORONA_RESIST_DEFENDER = "\$niko_MPC_coronaResistDefenderFleet"
    const val CORONA_RESIST_SYSTEM = "\$niko_MPC_coronaResistSystem"
    const val CORONA_RESIST_STATION = "\$MPC_coronaResistStation"
    const val CORONA_RESIST_DEFENDER_CORE = "\$MPC_coronaResistDefenderFleetCore"
    const val CORONA_RESIST_STATION_DEFENDER_FLEET = "\$MPC_coronaResistStationFleet"
    const val SKULIODA_MEMORY_TAG = "\$MPC_skulioda_person"

    // stored in global mem
    const val CORONA_RESIST_STATION_GLOBAL = "\$MPC_coronaResistStationGlobal"
    const val UNDER_CORONA_RESIST_EFFECT = "\$MPC_underCoronaResistEffect"
}