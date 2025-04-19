package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.BattleCreationContext
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin
import com.fs.starfarer.api.util.Misc

class MPC_SalvageDefenderInteractionWithObjectives: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token?>?,
        memoryMap: Map<String?, MemoryAPI?>
    ): Boolean {
        if (dialog == null) return false

        val entity = dialog.interactionTarget
        val memory = getEntityMemory(memoryMap)

        val defenders = memory.getFleet("\$defenderFleet") ?: return false

        dialog.interactionTarget = defenders

        val config = FIDConfig()
        config.leaveAlwaysAvailable = true
        config.showCommLinkOption = false
        config.showEngageText = false
        config.showFleetAttitude = false
        config.showTransponderStatus = false
        config.showWarningDialogWhenNotHostile = false
        config.alwaysAttackVsAttack = true
        config.impactsAllyReputation = true
        config.impactsEnemyReputation = false
        config.pullInAllies = false
        config.pullInEnemies = false
        config.pullInStations = false
        config.lootCredits = false

        config.firstTimeEngageOptionText = "Engage the automated defenses"
        config.afterFirstTimeEngageOptionText = "Re-engage the automated defenses"
        config.noSalvageLeaveOptionText = "Continue"

        config.dismissOnLeave = false
        config.printXPToDialog = true

        val seed = memory.getLong(MemFlags.SALVAGE_SEED)
        config.salvageRandom = Misc.getRandom(seed, 75)


        val plugin = FleetInteractionDialogPluginImpl(config)

        val originalPlugin = dialog.plugin
        config.delegate = object : BaseFIDDelegate() {
            override fun notifyLeave(dialog: InteractionDialogAPI) {
                // nothing in there we care about keeping; clearing to reduce savefile size
                defenders.memoryWithoutUpdate.clear()
                // there's a "standing down" assignment given after a battle is finished that we don't care about
                defenders.clearAssignments()
                defenders.deflate()

                dialog.plugin = originalPlugin
                dialog.interactionTarget = entity


                //Global.getSector().getCampaignUI().clearMessages();
                if (plugin.context is FleetEncounterContext) {
                    val context = plugin.context as FleetEncounterContext
                    if (context.didPlayerWinEncounterOutright()) {
                        val p = SDMParams()
                        p.entity = entity
                        p.factionId = defenders.faction.id

                        val plugin = Global.getSector().genericPlugins.pickPlugin(
                            SalvageDefenderModificationPlugin::class.java, p
                        )
                        plugin?.reportDefeated(p, entity, defenders)

                        memory.unset("\$hasDefenders")
                        memory.unset("\$defenderFleet")
                        memory["\$defenderFleetDefeated"] = true
                        entity.removeScriptsOfClass(FleetAdvanceScript::class.java)
                        FireBest.fire(null, dialog, memoryMap, "BeatDefendersContinue")
                    } else {
                        var persistDefenders = false
                        if (context.isEngagedInHostilities) {
                            persistDefenders = persistDefenders or !Misc.getSnapshotMembersLost(defenders).isEmpty()
                            for (member in defenders.fleetData.membersListCopy) {
                                if (member.status.needsRepairs()) {
                                    persistDefenders = true
                                    break
                                }
                            }
                        }

                        if (persistDefenders) {
                            if (!entity.hasScriptOfClass(FleetAdvanceScript::class.java)) {
                                defenders.isDoNotAdvanceAI = true
                                defenders.containingLocation = entity.containingLocation
                                // somewhere far off where it's not going to be in terrain or whatever
                                defenders.setLocation(1000000f, 1000000f)
                                entity.addScript(FleetAdvanceScript(defenders))
                            }
                            memory.expire(
                                "\$defenderFleet",
                                10f
                            ) // defenders may have gotten damaged; persist them for a bit
                        }
                        dialog.dismiss()
                    }
                } else {
                    dialog.dismiss()
                }
            }

            override fun battleContextCreated(dialog: InteractionDialogAPI, bcc: BattleCreationContext) {
                bcc.aiRetreatAllowed = false
                bcc.objectivesAllowed = true
                bcc.enemyDeployAll = true
            }

            override fun postPlayerSalvageGeneration(
                dialog: InteractionDialogAPI,
                context: FleetEncounterContext,
                salvage: CargoAPI
            ) {
                val winner = context.winnerData
                val loser = context.loserData

                if (winner == null || loser == null) return

                val playerContribMult = context.computePlayerContribFraction()

                val dropRandom: MutableList<DropData> = ArrayList()
                val dropValue: List<DropData> = ArrayList()

                val valueMultFleet =
                    Global.getSector().playerFleet.stats.dynamic.getValue(Stats.BATTLE_SALVAGE_MULT_FLEET)
                val valueModShips = context.salvageValueModPlayerShips

                for (data in winner.enemyCasualties) {
                    // add at least one of each weapon that was present on the OMEGA ships, since these
                    // are hard to get; don't want them to be too RNG
                    if (data.member != null && context.battle != null) {
                        val fleet = context.battle.getSourceFleet(data.member)

                        if (fleet != null && fleet.faction.id == Factions.OMEGA) {
                            for (slotId in data.member.variant.nonBuiltInWeaponSlots) {
                                val weaponId = data.member.variant.getWeaponId(slotId) ?: continue
                                if (salvage.getNumWeapons(weaponId) <= 0) {
                                    val spec = Global.getSettings().getWeaponSpec(weaponId)
                                    if (spec.hasTag(Tags.NO_DROP)) continue

                                    salvage.addWeapons(weaponId, 1)
                                }
                            }
                        }

                        if (fleet != null &&
                            fleet.faction.getCustomBoolean(Factions.CUSTOM_NO_AI_CORES_FROM_AUTOMATED_DEFENSES)
                        ) {
                            continue
                        }
                    }
                    if (config.salvageRandom.nextFloat() < playerContribMult) {
                        val drop = DropData()
                        drop.chances = 1
                        drop.value = -1
                        when (data.member.hullSpec.hullSize) {
                            HullSize.CAPITAL_SHIP -> {
                                drop.group = Drops.AI_CORES3
                                drop.chances = 2
                            }

                            HullSize.CRUISER -> drop.group = Drops.AI_CORES3
                            HullSize.DESTROYER -> drop.group = Drops.AI_CORES2
                            HullSize.FIGHTER, HullSize.FRIGATE -> drop.group = Drops.AI_CORES1
                            HullSize.DEFAULT -> {
                                return
                            }
                        }
                        if (drop.group != null) {
                            dropRandom.add(drop)
                        }
                    }
                }

                val fuelMult =
                    Global.getSector().playerFleet.stats.dynamic.getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET)

                //float fuel = salvage.getFuel();
                //salvage.addFuel((int) Math.round(fuel * fuelMult));
                val extra = SalvageEntity.generateSalvage(
                    config.salvageRandom,
                    valueMultFleet + valueModShips,
                    1f,
                    1f,
                    fuelMult,
                    dropValue,
                    dropRandom
                )
                for (stack in extra.stacksCopy) {
                    if (stack.isFuelStack) {
                        stack.size = (stack.size * fuelMult).toInt().toFloat()
                    }
                    salvage.addFromStack(stack)
                }
            }
        }


        dialog.plugin = plugin
        plugin.init(dialog)

        return true
    }


}