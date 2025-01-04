package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.utilities.niko_MPC_ids
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
        SEARCH_FOR_AGENT,
        UPGRADE_FUEL_PROD,
        INFILTRATE_AND_UPGRADE_UMBRA,
        WAIT,
        GIVE_CORES,
        DONE,
        FAILED;
    }
    var state: State = State.FIND_CORE

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
                State.SEARCH_FOR_AGENT -> {
                    info.addPara(
                        "Search for %s on %s",
                        initPad, getMacario().faction.color,
                        getAgent().nameString, "Volturn"
                    ).setHighlightColors(
                        getMacario().faction.color,
                        Global.getSector().economy.getMarket("volturn")?.faction?.color
                    )
                }
                State.INFILTRATE_AND_UPGRADE_UMBRA -> {
                    info.addPara(
                        "Infiltrate %s",
                        initPad, getUmbra().faction.color,
                        "Umbra"
                    )

                    info.addPara(
                        "Ensure %s is exporting at least %s units of volatiles",
                        5f,
                        getUmbra().faction.color,
                        "Umbra", "eight"
                    ).setHighlightColors(getUmbra().faction.color, Misc.getHighlightColor())
                }
                State.FAILED -> {
                    info.addPara(
                        "Failed",
                        initPad
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
            State.SEARCH_FOR_AGENT -> {
                info.addPara(
                    "A %s agent named %s has gone AWOL, and was last reported on %s. You are to find her and return her to %s for use in the infiltration of %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "Diktat", getAgent().nameString, "Volturn", getMacario().nameString, "Umbra"
                ).setHighlightColors(
                    factionForUIColors.color, factionForUIColors.color, Global.getSector().economy.getMarket("volturn").faction.color, factionForUIColors.color, Global.getSector().economy.getMarket("volturn").faction.color
                )
            }
            State.DONE -> {
                info.addPara("You have successfully armed the %s with heavy industry, and in doing so, drove a wedged between them and the %s.",
                5f,
                Misc.getHighlightColor(),
                "path", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }
            State.FAILED -> {
                info.addPara("You've failed to separate the diktat from the IAIIC.", 5f)
            }
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        if (state == State.FIND_CORE) {
            return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? PlanetAPI
        }
        return null
    }
}