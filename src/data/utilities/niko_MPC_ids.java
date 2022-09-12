package data.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class niko_MPC_ids {

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
    public static final String satelliteParamsId = "$niko_MPC_satelliteParams";

    public static final String defenseSatelliteImpactId = "niko_MPC_defenseSatelliteImpact";
    public static final String defenseSatelliteImpactReasonString = "Defense Satellite Artillery Impact";

    public static final String niko_MPC_isSatelliteHullId = "niko_MPC_isSatelliteHull";
}
