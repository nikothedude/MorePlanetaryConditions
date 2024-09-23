package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_playerFirstVisitToMagnetar
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.scripts.utils.SotfMisc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

class niko_MPC_magnetarStarScript(
    val magnetar: PlanetAPI
): niko_MPC_baseNikoScript(), CampaignEventListener {

    var generatedDefenders = false

    companion object {
        fun doBlindJump(fleet: CampaignFleetAPI) {
            val system = fleet.starSystem ?: return
            val approximateLoc = fleet.getApproximateHyperspaceLoc()

            val xOffset = MathUtils.getRandomNumberInRange(BLIND_JUMP_X_VARIATION_LOWER_BOUND, BLIND_JUMP_X_VARIATION_UPPER_BOUND)
            val yOffset = MathUtils.getRandomNumberInRange(BLIND_JUMP_Y_VARIATION_LOWER_BOUND, BLIND_JUMP_Y_VARIATION_UPPER_BOUND)

            approximateLoc.translate(xOffset, yOffset)
            val token = Global.getSector().hyperspace.createToken(approximateLoc.x, approximateLoc.y)

            val dest = JumpDestination(token, null)
            Global.getSector().doHyperspaceTransition(fleet, fleet, dest)
            fleet.memoryWithoutUpdate.set(niko_MPC_ids.BLIND_JUMPING, true, 2f)

            for (member in fleet.fleetData.membersListCopy) {
                member.status.applyDamage(9999999f) // very high, high enough to kill your fleet if you run into it twice
                member.repairTracker.cr = 0f
            }

            val crewNum = fleet.cargo.totalCrew
            val crewToLose = (crewNum * CREW_LOST_DURING_BLIND_JUMP_PERCENT).toInt().coerceAtMost(MAX_CREW_LOSS_DURING_BLIND_JUMP)
            fleet.cargo.removeCrew(crewToLose)
        }

        const val MIN_DAYS_PER_PULSE = 3f
        const val MAX_DAYS_PER_PULSE = 3.7f

        const val BASE_X_COORD_FOR_SYSTEM = -59800f
        const val BASE_Y_COORD_FOR_SYSTEM = -49320f

        const val X_COORD_VARIATION_LOWER_BOUND = -3600f
        const val X_COORD_VARIATION_UPPER_BOUND = 3900f
        const val Y_COORD_VARIATION_LOWER_BOUND = -100f
        const val Y_COORD_VARIATION_UPPER_BOUND = 3200f

        const val BLIND_JUMP_X_VARIATION_LOWER_BOUND = -3000f
        const val BLIND_JUMP_X_VARIATION_UPPER_BOUND = 3000f
        const val BLIND_JUMP_Y_VARIATION_LOWER_BOUND = -2000f
        const val BLIND_JUMP_Y_VARIATION_UPPER_BOUND = 3000f

        const val MAX_CREW_LOSS_DURING_BLIND_JUMP = 2000
        const val CREW_LOST_DURING_BLIND_JUMP_PERCENT = 0.1f
    }

    val daysPerPulse = IntervalUtil(MIN_DAYS_PER_PULSE, MAX_DAYS_PER_PULSE)

    override fun startImpl() {
        magnetar.addScript(this)
        Global.getSector().addListener(this)
    }

    override fun stopImpl() {
        magnetar.removeScript(this)
        Global.getSector().removeListener(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {

        val containingLocation = magnetar.containingLocation ?: return
        val playerFleet = Global.getSector().playerFleet
        if (!generatedDefenders && playerFleet?.containingLocation == containingLocation) {
            val mothership = containingLocation.getEntitiesWithTag("MPC_omegaDerelict_mothership").firstOrNull()
            if (mothership != null) {
                mothership.memoryWithoutUpdate["\$defenderFleet"] = createOmegaMothershipDefenders()
                mothership.memoryWithoutUpdate["\$hasDefenders"] = true
                mothership.memoryWithoutUpdate["\$hasStation"] = true
                mothership.memoryWithoutUpdate["\$hasNonStation"] = true
            }
            val researchStation = containingLocation.getEntitiesWithTag("MPC_station_researchMagnetarOne").firstOrNull()
            if (researchStation != null) {
                researchStation.memoryWithoutUpdate["\$defenderFleet"] = createDomainResearchStationDefenders()
                researchStation.memoryWithoutUpdate["\$hasDefenders"] = true
                researchStation.memoryWithoutUpdate["\$hasStation"] = false
                researchStation.memoryWithoutUpdate["\$hasNonStation"] = true
            }

            for (probe in containingLocation.getEntitiesWithTag("MPC_omegaDerelict_probe")) {
                probe.memoryWithoutUpdate["\$defenderFleet"] = createOmegaProbeDefenders(probe)
                probe.memoryWithoutUpdate["\$hasDefenders"] = true
                probe.memoryWithoutUpdate["\$hasStation"] = false
                probe.memoryWithoutUpdate["\$hasNonStation"] = true
            }
            for (surveyShip in containingLocation.getEntitiesWithTag("MPC_omegaDerelict_survey_ship")) {
                surveyShip.memoryWithoutUpdate["\$defenderFleet"] = createOmegaSurveyShipDefenders(surveyShip)
                surveyShip.memoryWithoutUpdate["\$hasDefenders"] = true
                surveyShip.memoryWithoutUpdate["\$hasStation"] = false
                surveyShip.memoryWithoutUpdate["\$hasNonStation"] = true
            }

            val omegaCaches = containingLocation.getEntitiesWithTag("MPC_omegaCache")
            for (cache in omegaCaches) {
                cache.memoryWithoutUpdate["\$defenderFleet"] = createOmegaCacheDefenders()
                cache.memoryWithoutUpdate["\$hasDefenders"] = true
                cache.memoryWithoutUpdate["\$hasStation"] = false
                cache.memoryWithoutUpdate["\$hasNonStation"] = true
            }

            val planetThree = containingLocation.getEntityById("MPC_magnetarSystemPlanetThree")
            planetThree?.memoryWithoutUpdate?.set("\$defenderFleet", createManufactorumDefenders())
            planetThree?.memoryWithoutUpdate?.set("\$hasDefenders", true)
            planetThree?.memoryWithoutUpdate?.set("\$hasStation", true)
            planetThree?.memoryWithoutUpdate?.set("\$hasNonStation", true)
            generatedDefenders = true
        }

        val days = Misc.getDays(amount)
        daysPerPulse.advance(days)
        // we advance to make things a bit more unpredictable
        if (containingLocation != Global.getSector().playerFleet?.containingLocation) return
        // but we dont pulse, since that can cause overhead
        if (daysPerPulse.intervalElapsed()) {
            doPulse()
        }
        // yeahhh this fucking sucks
        // this is best done in a listener but no listeners exist
        // also, salvage fields are completely contextless so i HAVE To do this blanket stuff
        if (!niko_MPC_settings.MAGNETAR_DROP_OMEGA_WEAPONS) {
            for (terrain in containingLocation.terrainCopy) {
                if (terrain.plugin.terrainId != Terrain.DEBRIS_FIELD || terrain.hasTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)) continue
                if (terrain.dropRandom.isEmpty()) continue
                val itemsToDrop = terrain.dropRandom[0]?.custom?.items ?: continue
                // prefix is _wpn
                val iterator = itemsToDrop.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val spec = entry.weaponSpec ?: continue
                    if (spec.hasTag("omega")) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun doPulse() {
        val color = niko_MPC_magnetarPulse.BASE_COLOR
        val params = niko_MPC_magnetarPulse.MPC_magnetarPulseParams(magnetar.containingLocation, magnetar.location, 500f, 2f, color = color)
        params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        val explosion = magnetar.containingLocation.addCustomEntity(
            Misc.genUID(), "Ionized Pulse",
            "MPC_magnetarPulse", Factions.NEUTRAL, params
        )
        explosion.setLocation(magnetar.location.x, magnetar.location.y)
    }

    override fun reportFleetJumped(fleet: CampaignFleetAPI?, from: SectorEntityToken?, to: JumpDestination?) {
        if (fleet == null || !fleet.isPlayerFleet || to?.destination?.containingLocation != magnetar.containingLocation) return

        val hasSierra = (niko_MPC_settings.SOTF_enabled && SotfMisc.playerHasSierra())
        if (hasSierra) {
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.SIERRA_SAW_MAGNETAR] = true
        }

        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_VISITED_MAGNETAR] == true) return

        Global.getSector().campaignUI.showInteractionDialog(MPC_playerFirstVisitToMagnetar(), magnetar)
    }


    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        return
    }

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        return
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        return
    }

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        return
    }

    override fun reportFleetDespawned(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        return
    }

    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        return
    }

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI?) {
        return
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        return
    }

    override fun reportPlayerReputationChange(person: PersonAPI?, delta: Float) {
        return
    }

    override fun reportPlayerActivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDeactivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDumpedCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportPlayerDidNotTakeCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportEconomyTick(iterIndex: Int) {
        return
    }

    override fun reportEconomyMonthEnd() {
        return
    }

    private fun createOmegaCacheDefenders(): CampaignFleetAPI {
        val fleetPoints = 160f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 100f))
        defenderFleet.addTag(niko_MPC_ids.BLOCKS_MAGNETAR_PULSE_TAG)

        return defenderFleet
    }

    private fun createManufactorumDefenders(): CampaignFleetAPI {
        val fleetPoints = 200f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null))

        val guardian = defenderFleet.fleetData.addFleetMember("MPC_omega_guardian_Standard")
        guardian.repairTracker.cr = guardian.repairTracker.maxCR
        guardian.captain = AICoreOfficerPluginImpl().createPerson(Commodities.OMEGA_CORE, niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, MathUtils.getRandom())
        integrateAndAdaptCoreForAIFleet(guardian)

        val variant = guardian.variant.clone()
        variant.addPermaMod("niko_MPC_subsumedIntelligence")
        variant.addPermaMod(HullMods.SAFETYOVERRIDES) // uh oh

        variant.addPermaMod(HullMods.HARDENED_SHIELDS, true)
        variant.addPermaMod(HullMods.HEAVYARMOR, true)
        variant.addPermaMod(HullMods.MAGAZINES, true)
        variant.addPermaMod(HullMods.UNSTABLE_INJECTOR, true)
        variant.addPermaMod(HullMods.REINFORCEDHULL, true)

        variant.source = VariantSource.REFIT
        guardian.setVariant(variant, false, true)

        //defenderFleet.addTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)
        defenderFleet.fleetData.sort()

        return defenderFleet
    }

    fun createOmegaMothershipDefenders(): CampaignFleetAPI {
        val fleetPoints = 210f // the mothership is very powerful, so add like 50 dp to this mentally
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 100f))
        val mothership = defenderFleet.fleetData.addFleetMember("MPC_omega_derelict_mothership_Standard")
        mothership.repairTracker.cr = mothership.repairTracker.maxCR
        mothership.captain = AICoreOfficerPluginImpl().createPerson(Commodities.OMEGA_CORE, niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, MathUtils.getRandom())
        integrateAndAdaptCoreForAIFleet(mothership)

        //defenderFleet.addTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)
        defenderFleet.fleetData.sort()

        return defenderFleet
    }

    fun createDomainResearchStationDefenders(): CampaignFleetAPI {
        val fleetPoints = 500f // TERROR
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 0f))

        return defenderFleet
    }

    fun createOmegaSurveyShipDefenders(surveyShip: SectorEntityToken): CampaignFleetAPI {
        val fleetPoints = 190f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null))

        return defenderFleet
    }

    fun createOmegaProbeDefenders(probe: SectorEntityToken): CampaignFleetAPI {
        val fleetPoints = 130f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null))

        return defenderFleet
    }
}