package data.utilities

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.everyFrames.niko_MPC_scriptAdder
import data.utilities.niko_MPC_debugUtils.displayError

object niko_MPC_scriptUtils {
    fun addScriptsAtValidTime(script: EveryFrameScript, entityToAddScriptsTo: SectorEntityToken?, allowDuplicates: Boolean) {
        addScriptsAtValidTime(ArrayList(setOf(script)), entityToAddScriptsTo, allowDuplicates)
    }

    /**
     * Required in apply() of conditions due to the fact that scripts variable on entities can be null during loading.
     * @param scriptsToAdd
     * @param entityToAddScriptsTo
     */
    @JvmStatic
    fun addScriptsAtValidTime(scriptsToAdd: List<EveryFrameScript>, entityToAddScriptsTo: SectorEntityToken?, allowDuplicates: Boolean) {
        if (entityToAddScriptsTo == null) return
        if (isValidTimeToAddScripts(entityToAddScriptsTo)) {
            for (script in scriptsToAdd) {
                if (!entityToAddScriptsTo.hasScriptOfClass(script.javaClass)) {
                    entityToAddScriptsTo.addScript(script)
                }
            }
        } else {
            val scriptAdders = getEntityScriptAdderList(entityToAddScriptsTo)
            val scriptAdder = niko_MPC_scriptAdder(scriptsToAdd, entityToAddScriptsTo, allowDuplicates)
            Global.getSector().addScript(scriptAdder)
            scriptAdders?.add(scriptAdder)
        }
    }

    @JvmStatic
    fun isValidTimeToAddScripts(entity: SectorEntityToken): Boolean {
        return entity.id != null || Global.getCurrentState() != GameState.TITLE
    }

    @JvmStatic
    fun forceScriptAdderToAddScriptsIfOneIsPresentAndIfIsValidTime(primaryEntity: SectorEntityToken) {
        if (isValidTimeToAddScripts(primaryEntity)) {
            val scriptAdders: List<niko_MPC_scriptAdder>? = getEntityScriptAdderList(primaryEntity)
            if (!scriptAdders.isNullOrEmpty()) {
                for (scriptAdder in ArrayList(scriptAdders)) {
                    scriptAdder.addScripts()
                }
            }
        }
    }

    @JvmStatic
    fun getEntityScriptAdderList(entity: SectorEntityToken?): MutableList<niko_MPC_scriptAdder>? {
        if (entity == null) {
            displayError("null entity on getEntityScriptAdderList. this shouldnt happen!!!", true)
            return ArrayList()
        }
        val entityMemory = entity.memoryWithoutUpdate ?: return null
        var scriptAdders = entityMemory[niko_MPC_ids.scriptAdderId] as MutableList<niko_MPC_scriptAdder>
        if (scriptAdders !is ArrayList<*>) {
            entityMemory[niko_MPC_ids.scriptAdderId] = ArrayList<niko_MPC_scriptAdder>()
            scriptAdders = entityMemory[niko_MPC_ids.scriptAdderId] as MutableList<niko_MPC_scriptAdder>
        }
        return scriptAdders
    }
}