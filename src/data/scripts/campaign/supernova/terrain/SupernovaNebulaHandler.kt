package data.scripts.campaign.supernova.terrain

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin
import data.utilities.niko_MPC_ids

object SupernovaNebulaHandler {
    private fun getSystem(): StarSystemAPI? = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.SUPERNOVA_TARGET] as? StarSystemAPI

    fun getNebulae(): MutableSet<NebulaTerrainPlugin>? {
        val sys = getSystem() ?: return null
        val mem = sys.memoryWithoutUpdate
        val one = mem[niko_MPC_ids.SUPERNOVA_NEBULA_ONE_MEMID] as? NebulaTerrainPlugin
        val two = mem[niko_MPC_ids.SUPERNOVA_NEBULA_TWO_MEMID] as? NebulaTerrainPlugin
        val nebulae = HashSet<NebulaTerrainPlugin>()
        one?.let { nebulae += it }
        two?.let { nebulae += it }

        return nebulae
    }

    fun getEditors(): MutableMap<NebulaTerrainPlugin, NebulaEditor>? {
        val nebulae = getNebulae() ?: return null
        val editors = HashMap<NebulaTerrainPlugin, NebulaEditor>()
        for (neb in nebulae) {
            val editor = NebulaEditor(neb)
            editors[neb] = editor
        }

        return editors
    }
}