package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICInspectionPrepFactor
import data.utilities.niko_MPC_ids

class MPC_IAIICInspectionPrepIntel(val fobIntel: MPC_IAIICFobIntel): BaseEventIntel() {

    enum class Stage(val stageName: String) {
        START("Start"),
        END("New investigation launched");
    }
    enum class PreparingStates(val isPreparing: Boolean = false) {
        PREPARING(true) {
            override fun createDesc(tooltip: TooltipMakerAPI, progress: Int) {
                tooltip.addPara(
                    "The IAIIC is preparing for a new inspection at a rate of %s per month.",
                    5f,
                    Misc.getHighlightColor(),
                    progress.toString()
                )
            }
        },
        INSPECTION_UNDERWAY {
            override fun createDesc(tooltip: TooltipMakerAPI, progress: Int) {
                tooltip.addPara(
                    "An inspection is already underway, and the IAIIC will not prepare for a new one until the current one is finished.",
                    5f
                )
            }
        },
        NO_CORES {
            override fun createDesc(tooltip: TooltipMakerAPI, progress: Int) {
                tooltip.addPara(
                    "The IAIIC lacks any valid targets in the %s system, and as such, will not launch any inspections.",
                    5f,
                    Misc.getHighlightColor(),
                    "${getTargetSystem()?.name}"
                )
            }
        },
        NO_TARGET {
            override fun createDesc(tooltip: TooltipMakerAPI, progress: Int) {
                tooltip.addPara(
                    "you shouldnt see this",
                    5f
                )
            }
        };

        abstract fun createDesc(tooltip: TooltipMakerAPI, progress: Int)
    }

    companion object {
        const val KEY = "\$MPC_IAIICPrepIntel"
        const val MAX_PROGRESS = 100
        const val REMOVE_TARGET_MULT_THRESH = 1
        const val FRACTAL_TARGET_MULT = 0.25f
        const val BASE_INSPECTION_FP = 200f

        fun get(): MPC_IAIICInspectionPrepIntel? {
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICInspectionPrepIntel
        }

        fun getAICores(system: StarSystemAPI): MutableMap<MarketAPI, MutableList<String>> {
            val cores = HashMap<MarketAPI, MutableList<String>>()

            for (market in Global.getSector().economy.getMarkets(system)) {
                if (!market.isPlayerOwned) continue
                val list = ArrayList<String>()

                list.addAll(MPC_IAIICInspectionIntel.getAICores(market))
                cores[market] = list
            }

            return cores
        }

        fun getTargetSystem(): StarSystemAPI? {
            return MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem
        }

    }

    var activeInspection: MPC_IAIICInspectionIntel? = null
    var inspectionsUndergone: Int = 0

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this

        setMaxProgress(MAX_PROGRESS)

        factors.clear()
        stages.clear()

        addFactor(MPC_IAIICInspectionPrepFactor())

        addStage(Stage.START, 0)
        addStage(Stage.END, 0)
        isImportant = true

        Global.getSector().intelManager.addIntel(this, true, null)
        //Global.getSector().addListener(this)
    }

    fun getPreparingState(): PreparingStates {
        if (activeInspection != null) return PreparingStates.INSPECTION_UNDERWAY
        val fractalSystem = getTargetSystem() ?: return PreparingStates.NO_TARGET
        if (getAICores(fractalSystem).values.all { it.isEmpty() }) return PreparingStates.NO_CORES

        return PreparingStates.PREPARING
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return

        val small = 0f

        val stage = stageId as? Stage ?: return
        if (isStageActive(stageId)) {
            addStageDesc(info, stage, small, false)
        }
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId !is Stage) return null

        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val opad = 10f

                tooltip.addTitle(stageId.stageName)

                addStageDesc(tooltip, stageId, opad, true)
            }
        }
    }

    override fun notifyStageReached(stage: EventStageData?) {
        if (stage == null) return

        when (stage.id) {
            Stage.START -> {}
            Stage.END -> {
                val target = getNewTargetColony() ?: return
                val FOB = MPC_IAIICFobIntel.getFOB() ?: return

                MPC_IAIICInspectionIntel(FOB, target, BASE_INSPECTION_FP)
            }
        }
    }

    private fun getNewTargetColony(): MarketAPI? {
        val system = getTargetSystem() ?: return null
        val aiCores = getAICores(system)
        val picker = WeightedRandomPicker<MarketAPI>()

        for (entry in aiCores.entries) {
            val cores = entry.value
            if (cores.isEmpty()) continue
            val market = entry.key
            var weight = 10f
            if (inspectionsUndergone < REMOVE_TARGET_MULT_THRESH && cores.contains(niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID)) {
                weight *= FRACTAL_TARGET_MULT
            }
            picker.add(market, weight)
        }

        return picker.pick()
    }

    private fun addStageDesc(info: TooltipMakerAPI, stage: Stage, initPad: Float, forTooltip: Boolean) {
        when (stage) {
            Stage.START -> {}
            Stage.END -> {
                info.addPara(
                    "The IAIIC will launch a new investigation on a random colony with AI cores.",
                    5f
                )
            }
        }
    }

    override fun getStageIconImpl(stageId: Any?): String? {
        val esd = getDataFor(stageId) ?: return null
        return Global.getSettings().getSpriteName("events", "MPC_IAIIC_PREP" + (esd.id as Stage).name)
    }

    override fun getIcon(): String? {
        return Global.getSettings().getSpriteName("events", "MPC_IAIIC_PREP_START")
    }

    fun inspectionEnded(inspection: MPC_IAIICInspectionIntel) {
        activeInspection = null
    }
}
