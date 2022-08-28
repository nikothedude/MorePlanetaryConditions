package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.List;

public class niko_MPC_generalUtils {

    public static boolean isValidMemoryKey(MemoryAPI memory, String key) {

        return (memory.contains(key)) || ((memory.get(key))) != null;
    }

    /**
     * Checks to see if the given key is present and not null in memory.
     * If the key IS null, it will set the given object as the value for the key.
     * If it is not, the object's value will be set to null, to allow for GC.
     *
     * @param memory The memoryAPI object we are accessing.
     * @param key    The key we will be checking and instantiating.
     * @param object The object to set as the key's value if the key is invalid.
     * @return
     */
    public static void instantiateMemoryKeyIfInvalid(MemoryAPI memory, String key, Object object) {
        if (!(isValidMemoryKey(memory, key))) { //if it isn't valid,
            instantiateMemoryKey(memory, key, object);
        }
    }

    public static void instantiateMemoryKey(MemoryAPI memory, String key, Object object) { //this method only exists for clarity
        memory.set(key, object);
    }

    public static List<EveryFrameScript> getScriptsOfClass(Class<?> clazz) {
        SectorAPI sector = Global.getSector();
        List<EveryFrameScript> scriptsOfClass = new ArrayList<>();

        for (EveryFrameScript script : sector.getScripts()) {
            if (script.getClass() == clazz) {
                scriptsOfClass.add(script);
            }
        }
        return scriptsOfClass;
    }

    public static void deleteMemoryKey(MemoryAPI memory, String key) {
        memory.set(key, null);
        memory.unset(key);
    }

}
