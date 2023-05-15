package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TextFieldAPI
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import java.lang.NumberFormatException

class overgrownNanoforgeIntelInputScanner(val intel: overgrownNanoforgeIntel) : niko_MPC_baseNikoScript() {

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)

        intel.intensityInput = null
        intel.intensityProgressBar = null

        intel.externalSupportInput = null
        intel.externalSupportProgressBar = null
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        if (!Global.getSector().isPaused) return stop()
        if (intel.intensityInput == null) return stop()

        intel.suppressionIntensity = sanitizeFloat(intel.intensityInput!!, intel.suppressionIntensity)
        intel.externalSupportRating = sanitizeFloat(intel.externalSupportInput!!, intel.externalSupportRating)
    }

    private fun sanitizeFloat(intensityInput: TextFieldAPI, defaultReturn: Float): Float {
        val text = intensityInput.text ?: return defaultReturn
        if (text.isEmpty()) return defaultReturn
        val negative = text.first() == '-'
        val filteredString = (text.filter { it == '.' || it.isDigit() })
        if (filteredString.isEmpty()) return defaultReturn

        var float: Float = try {
            (filteredString.toFloat())
        } catch (ex: NumberFormatException) {
            //niko_MPC_debugUtils.log.log(Level.ERROR, "caught NFE in input scanner sanitize")
            defaultReturn
        }
        if (negative) float *= -1f
        return float
    }

}
