package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.List;

public class niko_MPC_memoryUtils {
    /**
     * @param memory The memory to scan for the key.
     * @param key The key to check for.
     * @return True if memory both contains the key and if the value of key is not null. False otherwise.
     */
    public static boolean isValidMemoryKey(MemoryAPI memory, String key) {
        return (memory.contains(key)) && ((memory.get(key))) != null;
    }

    /**
     * Instantiates a new memoryKey in memory with object if isValidMemoryKey(memory, key) returns false.
     *
     * @param memory The memoryAPI object we are accessing.
     * @param key    The key we will be checking and instantiating.
     * @param object The object to set as the key's value if the key is invalid.
     */
    public static void instantiateMemoryKeyIfInvalid(MemoryAPI memory, String key, Object object) {
        if (!(isValidMemoryKey(memory, key))) { //if it isn't valid,
            instantiateMemoryKey(memory, key, object);
        }
    }

    /**
     * Simple wrapper for memory.set(key, object)
     */
    public static void instantiateMemoryKey(MemoryAPI memory, String key, Object object) { //this method only exists for clarity
        memory.set(key, object);
    }

    /**
     * Sets key to null in memory, then unsets the key.
     * @param memory The memory to delete from.
     * @param key The key to delete.
     */
    public static void deleteMemoryKey(MemoryAPI memory, String key) {
        memory.set(key, null);
        memory.unset(key);
    }

    public static niko_MPC_satelliteBattleTracker createNewSatelliteTracker() {
        MemoryAPI globalMemory = Global.getSector().getMemory();

        niko_MPC_satelliteBattleTracker battleTracker = new niko_MPC_satelliteBattleTracker();
        globalMemory.set(niko_MPC_ids.satelliteBattleTrackerId, battleTracker);

        return battleTracker;
    }
}
