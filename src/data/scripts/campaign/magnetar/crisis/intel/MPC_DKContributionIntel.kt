package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.rulecmd.MPC_IAIICDKCMD
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color


class MPC_DKContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_DKContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_DKContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_DKContributionIntel
        }

        const val KEY = "\$MPC_DKContributionIntel"
        const val SECT_NAME = "Arrow of Ludd"
    }

    enum class State {
        FIND_CORE,
        RETURN_WITH_CORE,
        WAIT_FOR_MACARIO,
        RETURN_TO_MACARIO,
        GO_TO_AGENT,
        GET_CACHE,
        GOT_CACHE,
        PLANT_EVIDENCE,
        QM_DEPOSED,
        INFILTRATE_AND_UPGRADE_UMBRA,
        RETURN_TO_MACARIO_CAUSE_DONE,
        DONE,
        FAILED,
        SEARCH_FOR_AGENT // UNUSED;
    }
    var state: State = State.FIND_CORE
    val interval = IntervalUtil(1f, 1f)

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.DIKTAT).crest
    }

    override fun getName(): String = "Diktat involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.DIKTAT, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is String) {
            info.addPara(listInfoParam as String, initPad)
        }
        if (listInfoParam is State) {
            when (listInfoParam) {
                State.FIND_CORE -> {
                    info.addPara(
                        "Find the %s", initPad,
                        Misc.getHighlightColor(), "unique synchrotron core"
                    )
                    /*val label = info.addPara(
                        "Give %s a %s", 5f,
                        Misc.getHighlightColor(),
                        "Macario", "alpha core"
                    ).setHighlightColors(
                        factionForUIColors.color, Misc.getHighlightColor()
                    )*/
                }
                State.RETURN_WITH_CORE -> {
                    info.addPara(
                        "Return to %s", initPad,
                        getMacario().faction.color, getMacario().nameString
                    )
                }
                State.WAIT_FOR_MACARIO -> {
                    info.addPara(
                        "Wait %s",
                        initPad, Misc.getHighlightColor(),
                        "one week"
                    )
                }
                State.RETURN_TO_MACARIO -> {
                    info.addPara(
                        "Return to %s",
                        initPad, getMacario().faction.color,
                        "Macario"
                    )
                }
                State.GO_TO_AGENT -> {
                    info.addPara(
                        "Make contact with %s on %s",
                        initPad, getMacario().faction.color,
                        getAgent().nameString, "Umbra"
                    ).setHighlightColors(
                        getMacario().faction.color,
                        Global.getSector().economy.getMarket("umbra")?.faction?.color
                    )
                }
                State.GET_CACHE -> {
                    info.addPara(
                        "Go to %s and retrieve %s's %s",
                        initPad,
                        getAgent().faction.color,
                        "Sindria", getAgent().nameString, "cache"
                    ).setHighlightColors(
                        Global.getSector().economy.getMarket("sindria").faction.color,
                        getAgent().faction.color,
                        Misc.getHighlightColor()
                    )
                }
                State.GOT_CACHE -> {
                    info.addPara(
                        "Return to %s",
                        initPad,
                        getAgent().faction.color,
                        getAgent().nameString
                    )
                }
                State.PLANT_EVIDENCE -> {
                    info.addPara(
                        "Plant evidence on %s",
                        initPad,
                        getUmbra().faction.color,
                        getUmbra().name
                    )
                }
                State.QM_DEPOSED -> {
                    info.addPara(
                        "Return to %s",
                        initPad,
                        getAgent().faction.color,
                        getAgent().nameString
                    )
                }
                State.INFILTRATE_AND_UPGRADE_UMBRA -> {
                    info.addPara(
                        "Ensure %s is exporting at least %s units of volatiles",
                        5f,
                        getUmbra().faction.color,
                        "Umbra", MPC_IAIICDKCMD.getVolatileDemand().toInt().toString()
                    ).setHighlightColors(getUmbra().faction.color, Misc.getHighlightColor())
                }
                State.RETURN_TO_MACARIO_CAUSE_DONE -> {
                    info.addPara(
                        "Volatile production satisfied",
                        5f
                    )
                    info.addPara(
                        "Return to %s",
                        0f,
                        getMacario().faction.color,
                        "Macario"
                    )
                }
                State.FAILED -> {
                    info.addPara(
                        "Failed",
                        initPad
                    )
                }
                State.DONE -> {
                    info.addPara(
                        "Success", initPad
                    )
                }
            }
        }
    }

    fun getMacario(): PersonAPI = Global.getSector().importantPeople.getPerson(People.MACARIO)
    fun getAgent(): PersonAPI = Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR)
    fun getUmbra(): MarketAPI = Global.getSector().economy.getMarket("umbra")
    fun getCorePlanet(): SectorEntityToken? = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? SectorEntityToken

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.DIKTAT)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (state == State.INFILTRATE_AND_UPGRADE_UMBRA) {
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                MPC_IAIICDKCMD.checkDemandAndUpdate()
            }
        }
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the Diktat is assisting the IAIIC.",
            5f
        )
        info.addPara(
            "You've had a meeting with %s, who has agreed to help you with IAIIC as long as you do him a favor.",
            5f,
            getMacario().faction.color,
            getMacario().nameString
        )

        when (state) {
            State.FIND_CORE -> {
                val planet = getCorePlanet() ?: return
                info.addPara(
                    "You've been directed to recover a %s from %s, in the %s system. It is rumored to be %s, even moreso than the base model.",
                    5f,
                    Misc.getHighlightColor(),
                    "special synchrotron core", planet.name, planet.starSystem.name, "highly effective"
                )
            }
            State.RETURN_WITH_CORE -> {
                info.addPara(
                    "You've retrieved the %s, and now must return to %s with it.",
                    5f,
                    Misc.getHighlightColor(),
                    "KINH-Pattern Synchrotron Core", getMacario().nameString
                ).setHighlightColors(Misc.getHighlightColor(), getMacario().faction.color)
            }
            State.WAIT_FOR_MACARIO -> {
                info.addPara(
                    "%s is brainstorming a way for you to infiltrate %s and ensure delivery of %s to %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "Macario", "Umbra", "volatiles", "Sindria"
                ).setHighlightColors(
                    getMacario().faction.color, Global.getSector().economy.getMarket("umbra").faction.color, Misc.getHighlightColor(), Global.getSector().economy.getMarket("sindria").faction.color
                )
            }
            State.RETURN_TO_MACARIO -> {
                info.addPara(
                    "You've recieved a summons from %s.",
                    5f,
                    getMacario().faction.color,
                    "Macario"
                )
            }
            State.GO_TO_AGENT -> {
                info.addPara(
                    "You are to meet up with a %s agent named %s on %s, and begin a plot to depose %s's %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "Diktat", getAgent().nameString, "Umbra", "Umbra", "quartermaster"
                ).setHighlightColors(
                    factionForUIColors.color, factionForUIColors.color, Global.getSector().economy.getMarket("umbra").faction.color, Global.getSector().economy.getMarket("umbra").faction.color, Misc.getHighlightColor()
                )
            }
            State.GET_CACHE -> {
                info.addPara(
                    "Apparently, %s left a %s on %s that will help implicate %s's %s as being a %s of the %s.",
                    5f,
                    getAgent().faction.color,
                    getAgent().nameString, "cache", "Sindria", "Umbra", "quartermaster", "agent", "diktat"
                ).setHighlightColors(
                    getAgent().faction.color,
                    Misc.getHighlightColor(),
                    Global.getSector().economy.getMarket("sindria").faction.color,
                    Global.getSector().economy.getMarket("umbra").faction.color,
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor(),
                    factionForUIColors.color
                )
            }
            State.GOT_CACHE -> {
                info.addPara(
                    "You've looted the cache on %s, and obtained a plethora of %s. Now you must return to %s on %s.",
                    5f,
                    getAgent().faction.color,
                    "Sindria", "framing tools", getAgent().nameString, "Umbra"
                ).setHighlightColors(
                    Global.getSector().economy.getMarket("sindria").faction.color,
                    Misc.getHighlightColor(),
                    getAgent().faction.color,
                    Global.getSector().economy.getMarket("umbra").faction.color,
                )
            }
            State.PLANT_EVIDENCE -> {
                info.addPara(
                    "You've been instructed to %s on %s.",
                    5f,
                    getUmbra().faction.color,
                    "plant evidence", "Umbra"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    getUmbra().faction.color
                )
            }
            State.QM_DEPOSED -> {
                info.addPara(
                    "You've successfully \"dealt with\" the %s %s, leaving the way open for %s.",
                    5f,
                    getAgent().faction.color,
                    "Umbra", "Quartermaster", getAgent().nameString
                ).setHighlightColors(
                    Global.getSector().economy.getMarket("umbra").faction.color,
                    Misc.getHighlightColor(),
                    getAgent().faction.color,
                )
            }
            State.INFILTRATE_AND_UPGRADE_UMBRA -> {
                info.addPara(
                    "%s is now the %s of %s, and it's now up to you to ensure %s is exporting at least %s units of volatiles.",
                    5f,
                    getAgent().faction.color,
                    getAgent().nameString, "quartermaster", "Umbra", "Umbra", MPC_IAIICDKCMD.getVolatileDemand().toInt().toString()
                ).setHighlightColors(
                    getAgent().faction.color,
                    Misc.getHighlightColor(),
                    getUmbra().faction.color,
                    getUmbra().faction.color,
                    Misc.getHighlightColor()
                )
            }
            State.RETURN_TO_MACARIO_CAUSE_DONE -> {
                info.addPara(
                    "You've secured a internal volatiles source of the %s, and now must return to %s.",
                    5f,
                    getMacario().faction.color,
                    "Diktat", "Macario"
                )
            }
            State.DONE -> {
                info.addPara("You have successfully secured the %s a internal volatiles source, and in doing so, drove a wedge between them and the %s.",
                5f,
                Misc.getHighlightColor(),
                "Diktat", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }
            State.FAILED -> {
                info.addPara("You've failed to separate the diktat from the IAIIC.", 5f)
            }

            else -> {}
        }
    }

    fun getVolatileDemand(): Float {
        return Global.getSector().economy.getMarket("sindria")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) }?.getDemand(Commodities.VOLATILES)?.quantity?.modifiedValue ?: 0f
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
        (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? PlanetAPI)?.makeUnimportant("\$MPC_IAIICDKSyncroPlanet")
        Global.getSector().importantPeople.getPerson(People.MACARIO).makeUnimportant("\$MPC_macarioDuringDKContribution")
        Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR).makeUnimportant("\$MPC_umbraInfiltrator")
        Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR).makeUnimportant("\$MPC_umbraInfiltrator")
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        if (state == State.FIND_CORE) {
            return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? PlanetAPI
        }
        return null
    }
}