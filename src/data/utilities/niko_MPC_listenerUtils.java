package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import data.scripts.campaign.listeners.niko_MPC_satelliteCleanupListener;
import data.scripts.campaign.listeners.niko_MPC_satelliteSpawnListener;

public class niko_MPC_listenerUtils {

    public static void addSatelliteCleanupListenerIfNonePresent() {
        if (!Global.getSector().getListenerManager().hasListenerOfClass(niko_MPC_satelliteCleanupListener.class)) {
            Global.getSector().addListener(new niko_MPC_satelliteCleanupListener(false)); //permaregister automatically does addlistener. why
        }
    }
    public static void addSatelliteSpawnListenerIfNonePresent() {
        if (!Global.getSector().getListenerManager().hasListenerOfClass(niko_MPC_satelliteSpawnListener.class)) {
            Global.getSector().addListener(new niko_MPC_satelliteSpawnListener(false)); //permaregister automatically does addlistener. why
        }
    }

}
