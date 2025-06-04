package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecution
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.magnetar.crisis.MPC_TTBMCacheDefenderSpawnScript
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContributionChangeData
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_TTContributionIntel
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.fadeAndExpire
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeNonStoryCritical
import org.magiclib.kotlin.makeStoryCritical
import kotlin.math.abs

class MPC_IAIICTriTachCMD: BaseCommandPlugin() {

    companion object {
        fun doingSearch(): Boolean {
            if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_startedSearchForTTBM")) return false
            val intel = MPC_IAIICFobIntel.get() ?: return false
            if (!intel.factionContributions.any { it.factionId == Factions.TRITACHYON }) return false
            return true
        }
        fun generateCache(): StarSystemAPI? {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystem"] != null) return Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystem"] as StarSystemAPI?

            var targetSystem: StarSystemAPI? = null
            var backupSystem: StarSystemAPI? = null
            for (system in Global.getSector().starSystems) {
                if (system.hasTag(Tags.THEME_SPECIAL) || system.hasTag(Tags.THEME_UNSAFE) || system.hasTag(Tags.SYSTEM_ABYSSAL) || system.hasTag(Tags.THEME_HIDDEN)) continue
                if (!system.isProcgen) continue
                if (abs(system.location.x) < CACHE_HYPERSPACE_MIN_DIST || abs(system.location.y) < CACHE_HYPERSPACE_MIN_DIST_Y) continue

                backupSystem = targetSystem // in case every valid system has been entered
                if (!system.isEnteredByPlayer) continue
                if (!system.hasSystemwideNebula()) continue
                if (system.planets.isEmpty()) continue

                targetSystem = system
            }
            if (targetSystem == null) targetSystem = backupSystem
            if (targetSystem == null) return null

            val target = targetSystem.planets.randomOrNull() ?: targetSystem.star
            val cache = targetSystem.addCustomEntity("MPC_BMDataCache", "Data cache", "MPC_BMDataCache", Factions.NEUTRAL)
            cache.makeImportant("MPC_BMDataCache")
            cache.setCircularOrbitWithSpin(target, MathUtils.getRandomNumberInRange(0f, 360f), target.radius + 300f, 90f, 30f, 40f)
            cache.isDiscoverable = true
            cache.sensorProfile = 75f

            Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystem"] = targetSystem
            Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystemName"] = targetSystem.nameWithNoType
            MPC_TTBMCacheDefenderSpawnScript(targetSystem).start()
            return targetSystem
        }

        const val DOWN_PAYMENT = 250000
        const val CACHE_HYPERSPACE_MIN_DIST = 50000f
        const val CACHE_HYPERSPACE_MIN_DIST_Y = 25000f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val interactionTarget = dialog.interactionTarget

        val command = params[0].getString(memoryMap)

        when (command) {
            "doingSearch" -> {
                return doingSearch()
            }
            "canDoOptions" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.TRITACHYON }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_startedSearchForTTBM")) return false
                //if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) return false
                return true
            }
            "beginSearch" -> {
                val person = interactionTarget.activePerson ?: return false
                val intel = MPC_TTContributionIntel.get(true) ?: return false
                intel.activePerson = person
                intel.sendUpdateIfPlayerHasIntel("Search for evidence", dialog.textPanel)
                Global.getSector().memoryWithoutUpdate["\$MPC_startedSearchForTTBM"] = true
                interactionTarget.market.makeStoryCritical("\$MPC_TTSearch")

                return true
            }
            "canSearchCommsRelay" -> {
                if (!doingSearch()) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_startedSearchForTTBM")) return false
                if (interactionTarget.id != "elada_relay") return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_searchedEladaRelay")) return false

                return true
            }
            "canSearchSensorArray" -> {
                if (!doingSearch()) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_startedSearchForTTBM")) return false
                if (interactionTarget.customEntityType != (Entities.SENSOR_ARRAY_MAKESHIFT)) return false
                if (interactionTarget.containingLocation?.id != "hybrasil") return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_searchedHybrasilArray")) return false

                return true
            }
            "incrementEvidence" -> {
                MPC_TTContributionIntel.get()?.incrementEvidence(dialog.textPanel)
            }
            "canDoFirstBarEvent" -> {
                if (!doingSearch()) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_foundTTBMContactInBar")) return false
                if (interactionTarget.containingLocation?.id != "Hybrasil") return false
                if (interactionTarget.faction.id != Factions.TRITACHYON) return false

                return true
            }
            "canDoIndieBMContactBarEvent" -> {
                if (!doingSearch()) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_trickedTriTachBruisers")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_failedIndieBMContact")) return false
                if (interactionTarget.market?.id != "cethlenn") return false
                return true
            }
            "canDoPirateBMContactBarEvent" -> {
                if (!doingSearch()) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_foundPirateBMContactInBar")) return false
                if (interactionTarget.market?.id != "donn") return false
                return true
            }
            "canDoTTBMContactBarEvent" -> {
                if (!doingSearch()) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_leftTTBMContactInBar")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_foundTTBMContactInBar")) return false
                if (interactionTarget.containingLocation?.id != "hybrasil") return false
                if (interactionTarget.faction.id != Factions.TRITACHYON) return false

                return true
            }
            "beginPirateDelivery" -> {
                val intel = MPC_TTContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.PirateTransportState.HAS_PIRATE, dialog.textPanel)
            }
            "canDeliverPirateToGilead" -> {
                if (!doingSearch()) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_foundPirateBMContactInBar")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_deliveredPirateToGilead")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_turnedInGileadPirate")) return false
                if (interactionTarget.market?.id != "gilead") return false

                return true
            }
            "fuckingDie" -> {
                Global.getSector().campaignUI.cmdExitWithoutSaving()
            }
            "handOverPirate" -> {
                val intel = MPC_TTContributionIntel.get(true)
                Global.getSector().memoryWithoutUpdate["\$MPC_turnedInGileadPirate"] = true
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.PirateTransportState.FAILED, dialog.textPanel)
            }
            "getCacheCoords" -> {
                val intel = MPC_TTContributionIntel.get(true)
                Global.getSector().memoryWithoutUpdate["\$MPC_deliveredPirateToGilead"] = true
                Global.getSector().memoryWithoutUpdate["\$MPC_hasPirateDataCoords"] = true
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.PirateTransportState.DELIVERED, dialog.textPanel)
            }
            "didntPay" -> {
                Global.getSector().memoryWithoutUpdate["\$MPC_deliveredPirateToGilead"] = true
                val intel = MPC_TTContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.PirateTransportState.FAILED, dialog.textPanel)
            }
            "canFindCache" -> {
                if (!doingSearch()) return false
                if (interactionTarget.market?.id != "gilead") return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_hasPirateDataCoords")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_searchedPirateDataCache")) return false

                return true
            }
            "lightsOut" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
                Global.getSoundPlayer().pauseMusic()
                val ambiencePlayer = BarCMD.getAmbiencePlayer()
                ambiencePlayer?.stop()
            }
            "awaken" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
            }

            "canReturnWithEvidence" -> {
                val person = MPC_TTContributionIntel.get()?.activePerson ?: return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_evidenceDone")) return false
                if (MPC_TTContributionIntel.get(true)?.state != MPC_TTContributionIntel.State.FIND_EVIDENCE) return false
                if (interactionTarget.activePerson != person) return false
                return true
            }

            "spawnCache" -> {
                val system = generateCache()
            }
            "beginCacheSearch" -> {
                val intel = MPC_TTContributionIntel.get(true)
                intel?.state = MPC_TTContributionIntel.State.FIND_CACHE
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.FIND_CACHE, dialog.textPanel)
            }
            "obtainedCache" -> {
                val intel = MPC_TTContributionIntel.get(true)
                intel?.state = MPC_TTContributionIntel.State.RETURN_TO_CONTACT
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.RETURN_TO_CONTACT, dialog.textPanel)
                interactionTarget.fadeAndExpire(1f)
            }

            "canGiveCache" -> {
                val person = MPC_TTContributionIntel.get()?.activePerson ?: return false
                if (interactionTarget.activePerson != person) return false
                return MPC_TTContributionIntel.get(true)?.state == MPC_TTContributionIntel.State.RETURN_TO_CONTACT
            }
            "cantExtort" -> {
                return (MPC_IAIICFobIntel.getIAIICStrengthInSystem() * 100f) < 60f
            }
            "startWaitForExecs" -> {
                class MPC_waitForExecsScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
                    override fun executeImpl() {
                        val intel = MPC_TTContributionIntel.get()
                        intel?.state = MPC_TTContributionIntel.State.RESOLVE
                        intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.RESOLVE, false, false)
                    }
                }
                MPC_waitForExecsScript(IntervalUtil(30f, 30f)).start()
                val intel = MPC_TTContributionIntel.get()
                intel?.state = MPC_TTContributionIntel.State.WAIT
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.WAIT, dialog.textPanel)
            }
            "upgradeContact" -> {
                val person = MPC_TTContributionIntel.get()?.activePerson ?: return false
                person.importance = PersonImportance.VERY_HIGH
            }

            "isSummoned" -> {
                val person = MPC_TTContributionIntel.get()?.activePerson ?: return false
                if (interactionTarget.activePerson != person) return false
                return (MPC_TTContributionIntel.get(true)?.state == MPC_TTContributionIntel.State.RESOLVE)
            }
            "isPayingUp" -> {
                val person = MPC_TTContributionIntel.get()?.activePerson ?: return false
                if (interactionTarget.activePerson != person) return false
                return (MPC_TTContributionIntel.get(true)?.state == MPC_TTContributionIntel.State.PAY_UP)
            }
            "payingUpNow" -> {
                val intel = MPC_TTContributionIntel.get(true)
                intel?.state = MPC_TTContributionIntel.State.PAY_UP
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.PAY_UP, dialog.textPanel)
            }
            "cantPay" -> {
                return Global.getSector().playerFleet.cargo.getCommodityQuantity(Commodities.ALPHA_CORE) < 3
            }
            "RESOLVED" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                val toRemove = intel.getContributionById(Factions.TRITACHYON) ?: return false
                MPC_IAIICFobIntel.get()?.removeContribution(toRemove, false, dialog)
                val intel2 = MPC_TTContributionIntel.get(true) ?: return false
                intel2.state = MPC_TTContributionIntel.State.OVER
                intel2.endAfterDelay()
                interactionTarget.market.makeNonStoryCritical("\$MPC_TTSearch")
            }
        }
        return false
    }

    override fun doesCommandAddOptions(): Boolean {
        return false
    }

    override fun getOptionOrder(params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Int {
        return params!![0].getFloat(memoryMap).toInt()
    }
}