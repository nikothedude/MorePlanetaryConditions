package data.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class niko_MPC_ids {

    public static final String niko_MPC_modId = "niko_morePlanetaryConditions";
    public static final String niko_MPC_masterConfig = "niko_MPC_settings.json";

    /**
     * Stored value of the defense satellite luddic path suppressor structure id. Here for ease of use and modification.
     */
    public static final String luddicPathSuppressorStructureId = "niko_MPC_antiAsteroidLuddicPathSuppressor";
    /**
     * A list of all possible satellite condition Ids. PLEASE UPDATE THIS IF YOU ADD A NEW ONE
     */
    public static final List<String> satelliteConditionIds = new ArrayList<>(Arrays.asList("niko_MPC_antiAsteroidSatellites"));
    /**
     * Stored value of the MemoryAPI key associated with the satellite tracker. Here for ease of use and modification.
     */
    public static final String satelliteTrackerId = "$niko_MPC_satelliteTracker";
    public static final String isSatelliteFleetId = "$niko_MPC_isSatelliteFleet";
    public static final String satelliteBarrageTerrainId = "niko_MPC_defenseSatelliteBarrage";
    public static final String niko_MPC_campaignPluginId = "niko_MPC_campaignPlugin";


    public static final String satelliteMarketId = "$niko_MPC_satelliteMarket";
    public static final String satelliteHandlerId = "$niko_MPC_satelliteHandler";

    public static final String defenseSatelliteImpactId = "niko_MPC_defenseSatelliteImpact";
    public static final String defenseSatelliteImpactReasonString = "Defense Satellite Artillery Impact";

    public static final String niko_MPC_isSatelliteHullId = "niko_MPC_isSatelliteHull";
    public static final float satellitePlayerVictoryIncrement = 10f;
    public static final float satelliteVictoryGraceIncrement = 40f;
    public static final String satelliteBattleTrackerId = "$niko_MPC_satelliteBattleTracker";
    public static final String satelliteFleetHostileReason = "$niko_MPC_satelliteFleetHostileReason";
    public static final String satelliteFactionId = "niko_MPC_satelliteFaction";
    public static final String temporaryFleetDespawnerId = "$niko_MPC_temporaryFleetDespawner";
    public static final String derelictSatelliteFakeFactionId = "derelictSatelliteBuilder";
}
