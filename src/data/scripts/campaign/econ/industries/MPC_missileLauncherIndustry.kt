package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil
import data.scripts.campaign.econ.industries.missileLauncher.MPC_missileEntityPlugin
import data.scripts.campaign.econ.industries.missileLauncher.MPC_orbitalMissileLauncher

abstract class MPC_missileLauncherIndustry: baseNikoIndustry() {

    var rocketHandler: MPC_orbitalMissileLauncher? = null

    override fun buildingFinished() {
        super.buildingFinished()

        setupDefenseGrid()
    }

    override fun apply() {
        super.apply(true)

        if (missilesEnabled()) {
            applyImpl()

            updateDefenseGrid()
        } else {
            dismantleDefenseGrid()
        }
    }

    abstract fun applyImpl()
    abstract fun missilesEnabled(): Boolean

    abstract fun updateDefenseGrid()
    abstract fun getRelevantDeficitMult(): Float

    protected fun setupDefenseGrid() {
        if (rocketHandler != null) return

        createNewDefenseGrid()
        rocketHandler?.missileReloadInterval = IntervalUtil(getBaseReloadRate(), getBaseReloadRate())
        updateDefenseGrid()
        rocketHandler?.start()
    }

    abstract fun createNewDefenseGrid()

    private fun dismantleDefenseGrid() {
        rocketHandler?.delete()
        rocketHandler = null
    }

    abstract fun getMinSensorProfile(): Float
    abstract fun getBaseReloadRate(): Float

    abstract fun getMaxRockets(): Float
    abstract fun getMaxRange(): Float
    open fun missileCreated(missile: MPC_missileEntityPlugin, target: SectorEntityToken) {}

}