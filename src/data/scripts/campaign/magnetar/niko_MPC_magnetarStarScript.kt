package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_playerFirstVisitToMagnetar
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.scripts.utils.SotfMisc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import data.utilities.niko_MPC_settings
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import java.util.EnumSet
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class niko_MPC_magnetarStarScript(
    val magnetar: PlanetAPI
): niko_MPC_baseNikoScript(), CampaignEventListener {

    companion object {
        fun generateFieldLines(magnetar: SectorEntityToken?, script: niko_MPC_magnetarStarScript?): HashSet<DipoleFieldLine> {
            // 3 layers. 8, 8, 8

            val lines = HashSet<DipoleFieldLine>()

            var currZ = Z_LEVEL
            var currOffset = OFFSET_ITER + 500f
            var left = LINE_LAYERS
            while (left-- > 0) {
                var linesInLayerLeft = LINES_PER_LAYER
                var currAngle = 0f
                while (linesInLayerLeft-- > 0) {
                    lines += DipoleFieldLine(magnetar, script, currAngle, currOffset, currZ)

                    currAngle += (360f * (1f / LINES_PER_LAYER))
                }
                currOffset += OFFSET_ITER
                currZ += OFFSET_ITER
            }

            for (line in lines) {
                LunaCampaignRenderer.addRenderer(line)
            }

            return lines
        }

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

        const val MIN_DAYS_PER_PULSE = 4f
        const val MAX_DAYS_PER_PULSE = 4.3f

        const val BASE_X_COORD_FOR_SYSTEM = -30800f
        const val BASE_Y_COORD_FOR_SYSTEM = -57320f

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

        const val LINE_LAYERS = 3
        const val LINES_PER_LAYER = 16
        const val OFFSET_ITER = 3000f
        const val Z_LEVEL = 3000f
    }

    // if we are under, we must be under the star and the fleet and terrain
    // TODO - add a terrain effect if you fly to the intersection point
    // uh oh! major damage. break drive field(?)
    // squish determines how far off the line is from the star. e.g. its angle from the camera
    // higher squish means more/less (not sure which) parallax
    // no squish? it goes forward, then directly back onto the other pole
    // 50 squish? follows a circle around the star to the pole
    // using an under layer means parallax is reversed.
    class DipoleFieldLine(val star: SectorEntityToken?, val script: niko_MPC_magnetarStarScript?, projectionAngle: Float, val dist: Float, val z: Float): LunaCampaignRenderingPlugin {

        companion object {
            const val LINE_WIDTH = 10f
            const val LINE_SEGMENTS = 50
        }

        val projectionAngle = Misc.normalizeAngle(projectionAngle - 90f)

        var P0: Vector3f
        var P1: Vector3f
        var P1Invert: Vector3f
        var P2: Vector3f
        var P2Invert: Vector3f
        var P3: Vector3f

        var alphaScroll = 0f

        init {

            P0 = Vector3f(0f, 0f, 0f)
            P3 = Vector3f(dist, 0f, 0f)
            val P3F = Vector2f(P3.x, P3.y)
            VectorUtils.rotateAroundPivot(P3F, Misc.ZERO, this.projectionAngle)
            P3.set(P3F.x, P3F.y, P3.z)
            P1 = Vector3f(
                P0.x,
                P0.y,
                z
            )
            P2 = Vector3f(
                P3.x,
                P3.y,
                z
            )
            P1Invert = Vector3f(
                P1.x,
                P1.y,
                -P1.z
            )
            P2Invert = Vector3f(
                P2.x,
                P2.y,
                -P2.z
            )
        }

        override fun isExpired(): Boolean = false

        override fun advance(amount: Float) {
            alphaScroll += amount * 0.1f
            if (alphaScroll >= 50000f) alphaScroll -= 50000f
        }

        override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
            return EnumSet.of(CampaignEngineLayers.TERRAIN_9, CampaignEngineLayers.TERRAIN_1) // above, so below
        }

        override fun render(
            layer: CampaignEngineLayers,
            viewport: ViewportAPI
        ) {
            // TODO - desloppifying this func made the quad strips... well, strips. find a way to return the behavior where they face the camera?
            // well its cool in this way too
            if (star != null && !star.containingLocation.isCurrentLocation) return

            val viewportHeight = viewport.visibleHeight
            val viewportCenter = viewport.center

            val P1 = if (layer == CampaignEngineLayers.TERRAIN_9) P1 else P1Invert
            val P2 = if (layer == CampaignEngineLayers.TERRAIN_9) P2 else P2Invert

            val xArray = FloatArray(LINE_SEGMENTS + 1)
            val yArray = FloatArray(LINE_SEGMENTS + 1)
            val parallaxArray = FloatArray(LINE_SEGMENTS + 1)

            for (i in 0..LINE_SEGMENTS) {
                val t = (i / LINE_SEGMENTS.toFloat())
                // cubic bezier curve function components
                //          1                               2                                           3                                           4
                val nextX = ((1f - t).pow(3f) * P0.x) + (3f * (1f - t).pow(2f) * (t * P1.x)) + (3f * (1f - t)) * (t.pow(2f) * P2.x) + (t.pow(3f) * P3.x)
                val nextY = ((1f - t).pow(3f) * P0.y) + (3f * (1f - t).pow(2f) * (t * P1.y)) + (3f * (1f - t)) * (t.pow(2f) * P2.y) + (t.pow(3f) * P3.y)
                val nextZ = ((1f - t).pow(3f) * P0.z) + (3f * (1f - t).pow(2f) * (t * P1.z)) + (3f * (1f - t)) * (t.pow(2f) * P2.z) + (t.pow(3f) * P3.z)

                // the more z it has, the less it scales when we zoom. vice versa
                val distBetweenViewportAndZ = (viewportHeight - nextZ)
                val parallaxMult = viewportHeight / ((distBetweenViewportAndZ).coerceAtLeast(0f))

                // im gonna be real i dont fully understand this block even still
                val worldX = nextX - viewportCenter.x
                val worldY = nextY - viewportCenter.y
                xArray[i] = viewportCenter.x + (worldX * parallaxMult)
                yArray[i] = viewportCenter.y + (worldY * parallaxMult)
                parallaxArray[i] = parallaxMult
            }

            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            val color = niko_MPC_magnetarPulse.BASE_COLOR
            val ourColor = arrayOf((color.red / 255f), (color.green / 255f), (color.blue / 255f), (color.alpha / 255f))

            GL11.glBegin(GL11.GL_QUAD_STRIP)
            for (i in 0..LINE_SEGMENTS) {
                val t = (i / LINE_SEGMENTS.toFloat())
                if (t == 0f) continue

                val last = (i - 1).coerceAtLeast(0)
                val next = (i + 1).coerceAtMost(LINE_SEGMENTS)

                val nextX = xArray[next]
                val nextY = yArray[next]
                val parallaxMult = parallaxArray[i]

                val lastX = xArray[last]
                val lastY = yArray[last]

                var xTangent = nextX - lastX
                var yTangent = nextY - lastY
                // distance formula
                var distBetween = sqrt(xTangent.pow(2f) + yTangent.pow(2f))
                if (distBetween == 0f) distBetween = 1f
                // turn them into a unit vector
                xTangent /= distBetween
                yTangent /= distBetween

                val widthHalf = LINE_WIDTH * 0.5f
                val widthMult = widthHalf * parallaxMult
                val xWidth = -yTangent
                val yWidth = xTangent * widthMult

                val localT = if (layer == CampaignEngineLayers.TERRAIN_9) t else 1f - t
                val mult = 1f - getAlphaMult(localT)
                val fade = getColorFade(localT)
                var ourAlpha = 1f * (mult).coerceAtMost(1f)
                val bonus = script?.lineBonus ?: 0f
                ourAlpha += (bonus * -0.7f)
                val blueMult = 1f + (-0.1f * fade) + (bonus * 1.13f)
                val redMult = 1f + (-0.2f * fade) + (bonus * 12f)
                var greenMult = 1f + (-0.1f * fade)
                greenMult -= bonus * 4f
                val red = (ourColor[0] * redMult).coerceAtMost(1f)
                val green = (ourColor[1] * greenMult).coerceAtMost(1f)
                val blue = (ourColor[2] * blueMult).coerceAtMost(1f)

                val x = xArray[i]
                val y = yArray[i]

                GL11.glColor4f(red, green, blue, ourAlpha)
                GL11.glTexCoord2f(0f, 0f)
                GL11.glVertex2f(x + xWidth, y + yWidth)         // one edge of the ribbon
                //GL11.glColor4f(red, green, blue, ourAlpha)
                GL11.glTexCoord2f(1f, 1f)
                GL11.glVertex2f(x - xWidth, y - yWidth)
            }
            //GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnd()
        }

        private fun getAlphaMult(t: Float): Float {
            val a = 0.48f
            val sineFunc = ((a)*sin((t-alphaScroll)/0.156f))+(0.8f)
            return sineFunc.coerceAtMost(1f)
        }

        private fun getColorFade(t: Float): Float {
            val sineFunc = ((0.5f)*sin((t-alphaScroll)/0.152f))+0.5f
            return sineFunc
        }

    }

    var fieldLines = generateFieldLines(magnetar, this)

    val daysPerPulse = IntervalUtil(MIN_DAYS_PER_PULSE, MAX_DAYS_PER_PULSE)
    var generatedDefenders = false

    override fun startImpl() {
        magnetar.addScript(this)
        Global.getSector().addListener(this)
    }

    override fun stopImpl() {
        magnetar.removeScript(this)
        Global.getSector().removeListener(this)
    }

    override fun runWhilePaused(): Boolean = false

    var recoilLeft = 0f
    var lineBonus = 0f

    override fun advance(amount: Float) {

        if (fieldLines == null) fieldLines = generateFieldLines(magnetar, this)

        if (recoilLeft > 0f) {
            recoilLeft = (recoilLeft - amount).coerceAtLeast(0f)
        }

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
                researchStation.memoryWithoutUpdate["\$MPC_hasSalvageObjectives"] = true
            }

            for (probe in containingLocation.getEntitiesWithTag("MPC_omegaDerelict_probe")) {
                probe.memoryWithoutUpdate["\$defenderFleet"] = createOmegaProbeDefenders(probe)
                probe.memoryWithoutUpdate["\$hasDefenders"] = true
                probe.memoryWithoutUpdate["\$hasStation"] = false
                probe.memoryWithoutUpdate["\$hasNonStation"] = true
                probe.memoryWithoutUpdate["\$MPC_hasSalvageObjectives"] = true
            }
            for (surveyShip in containingLocation.getEntitiesWithTag("MPC_omegaDerelict_survey_ship")) {
                surveyShip.memoryWithoutUpdate["\$defenderFleet"] = createOmegaSurveyShipDefenders(surveyShip)
                surveyShip.memoryWithoutUpdate["\$hasDefenders"] = true
                surveyShip.memoryWithoutUpdate["\$hasStation"] = false
                surveyShip.memoryWithoutUpdate["\$hasNonStation"] = true
                surveyShip.memoryWithoutUpdate["\$MPC_hasSalvageObjectives"] = true
            }

            val omegaCaches = containingLocation.getEntitiesWithTag("MPC_omegaCache")
            for (cache in omegaCaches) {
                cache.memoryWithoutUpdate["\$defenderFleet"] = createOmegaCacheDefenders()
                cache.memoryWithoutUpdate["\$hasDefenders"] = true
                cache.memoryWithoutUpdate["\$hasStation"] = false
                cache.memoryWithoutUpdate["\$hasNonStation"] = true
                cache.memoryWithoutUpdate["\$MPC_hasSalvageObjectives"] = true
            }

            val planetThree = containingLocation.getEntityById("MPC_magnetarSystemPlanetThree")
            planetThree?.memoryWithoutUpdate?.set("\$defenderFleet", createManufactorumDefenders())
            planetThree?.memoryWithoutUpdate?.set("\$hasDefenders", true)
            planetThree?.memoryWithoutUpdate?.set("\$hasStation", false)
            planetThree?.memoryWithoutUpdate?.set("\$hasNonStation", true)
            planetThree?.memoryWithoutUpdate?.set("\$MPC_hasSalvageObjectives", true)

            val planetTwo = containingLocation.getEntityById("MPC_magnetarSystemPlanetTwo")
            planetTwo?.memoryWithoutUpdate?.set("\$defenderFleet", createOmegaZigDefenders())
            planetTwo?.memoryWithoutUpdate?.set("\$hasDefenders", true)
            planetTwo?.memoryWithoutUpdate?.set("\$hasStation", false)
            planetTwo?.memoryWithoutUpdate?.set("\$hasNonStation", true)
            planetTwo?.memoryWithoutUpdate?.set("\$MPC_hasSalvageObjectives", true)

            magnetar.starSystem.addScript(MPC_magnetarThreatFleetManager())

            generatedDefenders = true
        }

        val days = Misc.getDays(amount)
        daysPerPulse.advance(days)
        // we advance to make things a bit more unpredictable
        if (containingLocation != Global.getSector().playerFleet?.containingLocation) return
        // but we dont pulse, since that can cause overhead
        val left = (daysPerPulse.intervalDuration - daysPerPulse.elapsed)
        val leftMult = 1f - ((left / 0.9f)).coerceAtMost(1f)
        if (leftMult > 0f) {
            doLaunchEffects(((1f - leftMult)).coerceAtLeast(0f).coerceAtMost(1f), ((1f - leftMult) * 0.3f).coerceAtMost(1f).coerceAtLeast(0f))
            val local = (leftMult - 0.75f).coerceAtLeast(0f)
            lineBonus = (local * 0.6f).coerceAtLeast(0f).coerceAtMost(1f)
        } else if (recoilLeft > 0f) {
            doLaunchEffects(1f - (recoilLeft / 1.1f).coerceAtLeast(0f).coerceAtMost(1f), 1f - (recoilLeft / 5f).coerceAtLeast(0f).coerceAtMost(1f))
            lineBonus = (recoilLeft * 0.05f).coerceAtLeast(0f).coerceAtMost(1f)
        } else {
            lineBonus = 0f
        }
        if (daysPerPulse.intervalElapsed()) {
            doPulse()
            recoilLeft = 3f
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

    fun doLaunchEffects(mult: Float, soundMult: Float = mult) {
        val inverted = (1f - mult)
        val dist = MathUtils.getDistance(magnetar, Global.getSector().playerFleet).coerceAtLeast(0.1f)
        val distMult = (2000f / dist).coerceAtMost(1f).coerceAtLeast(0f)
        Global.getSector().campaignUI.suppressMusic(((inverted * 1.5f) * distMult).coerceAtLeast(0f).coerceAtMost(1f))

        magnetar.containingLocation.addHitParticle(
            magnetar.location,
            Misc.ZERO,
            (magnetar.radius * 3f) * (inverted),
            2f * inverted,
            0.1f,
            niko_MPC_magnetarPulse.BASE_COLOR
        )

        Global.getSoundPlayer().playLoop(
            "phase_beam_loop",
            magnetar,
            (1f - soundMult),
            (1f - soundMult),
            magnetar.location,
            Misc.ZERO,
        )
    }

    fun doPulse() {
        val params = niko_MPC_magnetarPulse.MagnetarPulseParams(
            18000f,
            magnetar.radius + 50f,
            magnetar.radius / 2f,
            magnetar,
            startingRange = magnetar.radius / 2f,
            explosionDamageMult = 0.7f,
            affectPlanets = true
        )
        params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        niko_MPC_magnetarPulse.createPulse(
            magnetar,
            params
        )
        magnetar.containingLocation.addHitParticle(
            magnetar.location,
            Misc.ZERO,
            magnetar.radius * 15f,
            1f,
            3f,
            niko_MPC_magnetarPulse.BASE_COLOR
        )
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
        val fleetPoints = 240f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 100f))

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
        variant.addPermaMod(HullMods.EXTENDED_SHIELDS, true)

        variant.source = VariantSource.REFIT
        guardian.setVariant(variant, false, true)

        //defenderFleet.addTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)
        defenderFleet.fleetData.sort()

        return defenderFleet
    }

    private fun createOmegaZigDefenders(): CampaignFleetAPI {
        val fleetPoints = 120f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 100f))

        val zigg = defenderFleet.fleetData.addFleetMember("MPC_omegurat_abomination")
        zigg.setVariant(zigg.variant.clone(), false, false)
        zigg.variant.originalVariant = null
        zigg.variant.source = VariantSource.REFIT
        zigg.variant.addTag(Tags.SHIP_LIMITED_TOOLTIP)
        if (niko_MPC_settings.DISABLE_MAGNETAR_CORE_DROPS) {
            zigg.variant.addTag(Tags.VARIANT_DO_NOT_DROP_AI_CORE_FROM_CAPTAIN)
        }
        zigg.repairTracker.cr = zigg.repairTracker.maxCR
        zigg.captain = AICoreOfficerPluginImpl().createPerson(Commodities.OMEGA_CORE, niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, MathUtils.getRandom())
        integrateAndAdaptCoreForAIFleet(zigg)

        val variant = zigg.variant.clone()

        variant.source = VariantSource.REFIT
        zigg.setVariant(variant, false, true)

        //defenderFleet.addTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)
        defenderFleet.fleetData.sort()

        return defenderFleet
    }

    fun createOmegaMothershipDefenders(): CampaignFleetAPI {
        val fleetPoints = 410f // the mothership is very powerful, so add like 50 dp to this mentally
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

    fun getSecsTilNextPulse(): Float {
        return Global.getSector().clock.convertToSeconds(daysPerPulse.intervalDuration - daysPerPulse.elapsed)
    }

    fun getSecsTilNextPulseString(): String {
        return "${getSecsTilNextPulse().roundNumTo(1)} second(s) until next pulse"
    }
}