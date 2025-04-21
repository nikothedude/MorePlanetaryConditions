package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyMilitaristicHouseEventIntel.Companion.addStartLabel
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeNonStoryCritical
import java.awt.Color

class MPC_hegemonyContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_hegemonyContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_hegemonyContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_hegemonyContributionIntel
        }

        fun retaliate(panel: TextPanelAPI? = null) {
            val intel = MPC_IAIICFobIntel.Companion.get() ?: return
            intel.retaliate(
                MPC_IAIICFobIntel.RetaliateReason.TURNING_HOUSES_AGAINST_HEGEMONY,
                panel
            )
        }

        const val KEY = "\$MPC_hegeContributionIntel"
        const val HOUSES = 4
    }

    var housesTurned = 0
    var currentHouse = TargetHouse.NONE
        set(value) {
            field.unapply()
            value.apply()

            field = value
        }

    enum class State {
        GO_TO_EVENTIDE_INIT,
        CONVINCE_HOUSES,
        DONE,
        FAILED
    }
    enum class TargetHouse(val stringName: String) {
        NONE("None"),
        ALOOF("ONE"),
        OPPORTUNISTIC("TWO"),
        MILITARISTIC("Mellour") {
            override fun apply(text: TextPanelAPI?) {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get(true)!!
                if (text != null) {
                    intel.sendUpdateIfPlayerHasIntel(null, text)
                }
            }
            override fun unapply(text: TextPanelAPI?) {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get() ?: return
                intel.endAfterDelay()
            }
        },
        HONORABLE("FOUR");

        open fun apply(text: TextPanelAPI? = null) {}
        open fun unapply(text: TextPanelAPI? = null) {}
    }

    fun setNewHouse(house: TargetHouse, text: TextPanelAPI) {
        currentHouse.unapply()
        currentHouse = house
        house.apply(text)

        if (house != TargetHouse.MILITARISTIC) {
            sendUpdateIfPlayerHasIntel(house, text)
        }
    }

    var state: State = State.GO_TO_EVENTIDE_INIT

    var cooldownActive = false
    var cooldownDays = IntervalUtil(30f, 30f)

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.HEGEMONY).crest
    }

    override fun getName(): String = "Hegemony involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.HEGEMONY, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        when (listInfoParam) {
            State.GO_TO_EVENTIDE_INIT -> {
                info.addPara(
                    "Go to %s",
                    5f,
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                    "eventide"
                )
            }
            State.CONVINCE_HOUSES -> {
                info.addPara(
                    "Convince %s to turn against the %s",
                    0f,
                    Misc.getHighlightColor(),
                    "noble houses",
                    "hegemony"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    factionForUIColors.baseUIColor
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )

                info.addPara(
                    "Current target: %s",
                    5f,
                    Misc.getHighlightColor(),
                    currentHouse.stringName
                )
            }
            TargetHouse.MILITARISTIC -> {
                info.addPara(
                    "Disrupt %s or destroy %s",
                    10f,
                    factionForUIColors.baseUIColor,
                    "hegemony industries", "hegemony fleets"
                )
            }
            "HOUSES_TURNED" -> {
                info.addPara(
                    "%s turned against the %s",
                    0f,
                    Misc.getHighlightColor(),
                    currentHouse.stringName,
                    "Hegemony"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    factionForUIColors.baseUIColor
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )
            }
            "COOLDOWN_EXPIRED" -> {
                info.addPara(
                    "Next mission now available",
                    5f
                )
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.HEGEMONY)
    }

    fun getAloofRep(): PersonAPI = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR)

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the hegemony may be involved in the IAIIC.",
            5f
        )

        when (state) {
            State.GO_TO_EVENTIDE_INIT -> {
                info.addPara(
                    "Following your fruitless meeting with the high hegemon, a strange individual claiming to be a eventide aristocrat " +
                    "claimed to have %s of %s involvement with the %s, and asked you to meet them at %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "evidence", "IAIIC", "hegemony", "eventide"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                    factionForUIColors.baseUIColor,
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                )
            }
            State.CONVINCE_HOUSES -> {
                val aloofRep = getAloofRep()
                info.addPara(
                    "You've met with %s - an aristocrat from the Eventide house %s, who has tasked you with turning %s against the %s. " +
                    "Supposedly, these houses are each uniquely positioned to be able to %s on the %s to pull out from the %s.",
                    5f,
                    Misc.getHighlightColor(),
                    aloofRep.nameString, aloofRep.name.last, "noble houses", "Hegemony", "put pressure", "Hegemony", "IAIIC"
                ).setHighlightColors(
                    aloofRep.faction.baseUIColor, aloofRep.faction.baseUIColor, Misc.getHighlightColor(), factionForUIColors.baseUIColor,
                    Misc.getHighlightColor(), factionForUIColors.baseUIColor, Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )

                info.addPara("She listed the following targets: ", 5f)
                info.setBulletedListMode(BaseIntelPlugin.BULLET)
                info.addPara("Youn", 0f).color = Misc.getHighlightColor()
                info.addPara("Lindunberg", 0f).color = Misc.getHighlightColor()
                info.addPara("Mellour", 0f).color = Misc.getHighlightColor()
                info.addPara("Alotera", 0f).color = Misc.getHighlightColor()
                info.setBulletedListMode(null)

                info.addPara(
                    "These houses can be contacted through the %s comms panel - but only %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "eventide", "one at a time"
                ).setHighlightColors(
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                    Misc.getHighlightColor()
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )

                info.addSectionHeading("Operations", Alignment.MID, 10f)

                if (currentHouse != TargetHouse.NONE) {
                    info.addPara(
                        "Your current target is %s.",
                        5f,
                        factionForUIColors.baseUIColor,
                        currentHouse.stringName
                    )
                }
                info.addSpacer(5f)

                when (currentHouse) {
                    TargetHouse.NONE -> {
                        info.addPara(
                            "You are not currently dealing with %s house.",
                            5f,
                            Misc.getHighlightColor(),
                            "any"
                        )

                        if (cooldownActive) {
                            info.addPara(
                                "INTSEC is watching eventide nobility closely for any signs of defection. It would be unwise to engage any house at the moment.",
                                5f,
                                Misc.getNegativeHighlightColor()
                            )
                        }
                    }
                    TargetHouse.ALOOF -> TODO()
                    TargetHouse.OPPORTUNISTIC -> TODO()
                    TargetHouse.MILITARISTIC -> {
                        addStartLabel(info)
                    }
                    TargetHouse.HONORABLE -> TODO()
                }
            }
            State.FAILED -> {
                info.addPara("You have failed to drive a wedge between the IAIIC and the hegemony.", 5f)
            }
            State.DONE -> {
                info.addPara("The %s has been forced to disengage from the %s, delivering a crippling blow to their operations.",
                    5f,
                    Misc.getHighlightColor(),
                    "hegemony", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null

        Global.getSector().economy.getMarket("eventide")?.makeNonStoryCritical("\$MPC_IAIICEvent")

    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        if (cooldownActive && state == State.CONVINCE_HOUSES) {
            cooldownDays.advance(Misc.getDays(amount))
            if (cooldownDays.intervalElapsed()) {
                cooldownActive = false
                sendUpdateIfPlayerHasIntel(
                    "COOLDOWN_EXPIRED",
                    false
                )
            }
        }
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        return when (state) {
            State.GO_TO_EVENTIDE_INIT -> Global.getSector().economy.getMarket("eventide").primaryEntity
            State.CONVINCE_HOUSES -> {
                when (currentHouse) {
                    TargetHouse.NONE -> null
                    TargetHouse.ALOOF -> null
                    TargetHouse.OPPORTUNISTIC -> null
                    TargetHouse.MILITARISTIC -> null
                    TargetHouse.HONORABLE -> null
                }
            }
            State.DONE -> null
            State.FAILED -> null
        }
    }

    fun turnedHouse(text: TextPanelAPI?) {
        housesTurned++

        if (text == null) {
            sendUpdateIfPlayerHasIntel(
                "HOUSES_TURNED",
                false
            )
        } else {
            sendUpdateIfPlayerHasIntel(
                "HOUSES_TURNED",
                text
            )
        }

        if (housesTurned == 3) {
            text?.addPara("Suddenly, MilSec lights up with alarming reports of IAIIC activity - it seems INTSEC is not happy about your attempts to sabotage their efforts.")
            retaliate(text)
        }

        currentHouse = TargetHouse.NONE
    }

}