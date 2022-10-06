package data.utilities;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class niko_MPC_settings {

    private static final Logger log = Global.getLogger(niko_MPC_settings.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void loadSettings() throws JSONException, IOException {
        log.debug("reloading settings");

        JSONObject configJson = Global.getSettings().loadJSON(niko_MPC_ids.niko_MPC_masterConfig);

        niko_MPC_settings.DEFENSE_SATELLITES_ENABLED = configJson.getBoolean("enableDefenseSatellites");
        niko_MPC_settings.SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame");
        niko_MPC_settings.PREVENT_SATELLITE_TURN = configJson.getBoolean("preventSatelliteTurning");
        niko_MPC_settings.DISCOVER_SATELLITES_IN_BULK = configJson.getBoolean("discoverSatellitesInBulk");

        niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_BASE = (float) configJson.getDouble("satelliteInterferenceDistanceBase");
        niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_MULT = (float) configJson.getDouble("satelliteInterferenceDistanceMult");

        niko_MPC_settings.BATTLE_SATELLITES_BASE = configJson.getInt("maxBattleSatellitesBase");
        niko_MPC_settings.BATTLE_SATELLITES_MULT = configJson.getDouble("maxBattleSatellitesMult");

        niko_MPC_settings.BARRAGE_WEIGHT = (float) configJson.getDouble("barrage_weight");
        niko_MPC_settings.STANDARD_WEIGHT = (float) configJson.getDouble("standard_weight");
        niko_MPC_settings.SHIELDED_WEIGHT = (float) configJson.getDouble("shielded_weight");
        niko_MPC_settings.BEAMER_WEIGHT = (float) configJson.getDouble("beamer_weight");
        niko_MPC_settings.ORDNANCE_WEIGHT = (float) configJson.getDouble("ordnance_weight");
        niko_MPC_settings.SWARM_WEIGHT = (float) configJson.getDouble("swarm_weight");

    }

    public static boolean XENOLIFE_ENABLED;
    public static boolean XENO_RANCHING_IND_ENABLED;
    public static boolean XENO_HUNTING_IND_ENABLED;
    public static boolean PREVENT_SATELLITE_TURN;
    public static boolean DEFENSE_SATELLITES_ENABLED;
    public static boolean SHOW_ERRORS_IN_GAME;
    public static boolean DISCOVER_SATELLITES_IN_BULK;

    public static float SATELLITE_INTERFERENCE_DISTANCE_BASE;
    public static float SATELLITE_INTERFERENCE_DISTANCE_MULT;
    public static int BATTLE_SATELLITES_BASE;
    public static double BATTLE_SATELLITES_MULT;
    public static Float BARRAGE_WEIGHT;
    public static Float STANDARD_WEIGHT;
    public static Float SHIELDED_WEIGHT;
    public static Float BEAMER_WEIGHT;
    public static Float ORDNANCE_WEIGHT;
    public static Float SWARM_WEIGHT;
}
