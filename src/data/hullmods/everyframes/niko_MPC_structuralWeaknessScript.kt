package data.hullmods.everyframes

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import data.utilities.niko_MPC_debugUtils.displayError

class niko_MPC_structuralWeaknessScript(var holder: ShipAPI?, var modulesToDestroy: MutableSet<ShipAPI>?) :
    BaseEveryFrameCombatPlugin() {
    var engine: CombatEngineAPI? = null
    override fun init(engine: CombatEngineAPI) {
        this.engine = engine
        if (!ensureVariablesNotNull()) {
            removeSelfFromEngine()
            return
        }
    }

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        val destroyModules = !holder!!.isAlive
        var isDone = false
        val iterator = modulesToDestroy!!.iterator()
        while (iterator.hasNext()) {
            val module = iterator.next()
            if (!module.isAlive) {
                iterator.remove()
                continue
            }
            if (destroyModules) {
                destroyModule(module)
                iterator.remove()
                isDone = true
            }
        }
        if (isDone || modulesToDestroy!!.isEmpty()) {
            removeSelfFromEngine()
        }
    }

    fun destroyModule(module: ShipAPI) {
        module.hitpoints = 0.0000000000000000001f
        while (module.isAlive) {
            val damagePoint = module.location
            val damageAmount = 100f
            engine!!.applyDamage(module, damagePoint, damageAmount, DamageType.ENERGY, 0f, true, false, null, false)
        }
    }

    fun removeSelfFromEngine() {
        engine!!.removePlugin(this)
        prepareForGarbageCollection()
    }

    private fun ensureVariablesNotNull(): Boolean {
        var result = true
        if (engine == null) {
            displayError("engine null on structural weakness script init")
            result = false
        }
        if (holder == null) {
            displayError("structural weakness null holder on init")
            result = false
        }
        if (modulesToDestroy == null) {
            displayError("structural weakness modulesToDestroy null on init, holder: $holder")
            result = false
        } else {
            if (modulesToDestroy!!.size == 0) {
                displayError("structural weakness modulesToDestroy.size() == 0 on init, holder: $holder")
                result = false
            }
        }
        return result
    }

    fun prepareForGarbageCollection() {
        holder = null
        modulesToDestroy = null
        engine = null
    }
}