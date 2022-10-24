package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.rules.MemoryAPI

object niko_MPC_memoryUtils {
    @JvmStatic
    /**
     * @param memory The memory to scan for the key.
     * @param key The key to check for.
     * @return True if memory both contains the key and if the value of key is not null. False otherwise.
     */
    fun isValidMemoryKey(memory: MemoryAPI, key: String?): Boolean {
        return memory.contains(key) && memory[key] != null
    }

    @JvmStatic
    /**
     * Instantiates a new memoryKey in memory with object if isValidMemoryKey(memory, key) returns false.
     *
     * @param memory The memoryAPI object we are accessing.
     * @param key    The key we will be checking and instantiating.
     * @param object The object to set as the key's value if the key is invalid.
     */
    fun instantiateMemoryKeyIfInvalid(memory: MemoryAPI, key: String?, `object`: Any?) {
        if (!isValidMemoryKey(memory, key)) { //if it isn't valid,
            instantiateMemoryKey(memory, key, `object`)
        }
    }

    @JvmStatic
    /**
     * Simple wrapper for memory.set(key, object)
     */
    fun instantiateMemoryKey(memory: MemoryAPI, key: String?, `object`: Any?) { //this method only exists for clarity
        memory[key] = `object`
    }

    /**
     * Sets key to null in memory, then unsets the key.
     * @param memory The memory to delete from.
     * @param key The key to delete.
     */
    @JvmStatic
    fun deleteMemoryKey(memory: MemoryAPI, key: String?) {
        memory[key] = null
        memory.unset(key)
    }

    /**
     * Creates a new instance of niko_MPC_satelliteBattleTracker, then does
     * globalMemory.set(satelliteTrackerId, tracker).
     * @return The new save-specific instance of the satellite battle tracker.
     */
    @JvmStatic
    fun createNewSatelliteTracker(): niko_MPC_satelliteBattleTracker {
        val globalMemory = Global.getSector().memoryWithoutUpdate
        val battleTracker = niko_MPC_satelliteBattleTracker()
        globalMemory[niko_MPC_ids.satelliteBattleTrackerId] = battleTracker
        return battleTracker
    }
}