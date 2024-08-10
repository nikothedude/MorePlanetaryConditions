package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TerrainAIFlags
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTScanFactor
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_playerExposedToMagnetarCore
import data.scripts.campaign.terrain.niko_MPC_scannableTerrain
import data.scripts.everyFrames.niko_MPC_HTFactorTracker
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.BLIND_JUMPING
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class niko_MPC_magnetarField: MagneticFieldTerrainPlugin(), niko_MPC_scannableTerrain {
    companion object {
        const val HYPERSPACE_TOPOGRAPHY_POINTS = 20

        const val NO_BUBBLE_CR_MULT = 25f
        const val NO_BUBBLE_WIND_MULT = 16f

        const val DETECTED_MULT = 0.5f
        const val DETECTED_MULT_STORM = 0f

        const val SENSOR_RANGE_MULT = 0.9f
        const val SENSOR_RANGE_MULT_STORM = 0f // completely blinded
    }
    // ONLY APPLIES IF BUBBLE IS GONE
    var crLossMult = 3.5f
    var pullFactor = -0.16f

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)
        if (name == "Magnetic Field" || name == null) {
            name = "Magnetar Field"
        }
    }

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        /*val outerRadius = params.outerRadius
        val innerRadius = params.innerRadius
        val distFromMagnetar = (MathUtils.getDistance(entity, this.entity) - innerRadius).coerceAtLeast(0f)

        val percentToMagnetar = distFromMagnetar/(outerRadius) //TODO: this wont work that well, double check the math*/

        val fleet = entity as? CampaignFleetAPI ?: return

        if (flareManager.isInActiveFlareArc(fleet)) {

            fleet.stats.removeTemporaryMod(modId + "_7")
            fleet.stats.addTemporaryModMult(
                0.1f, modId + "_2",
                "Inside magnetar storm", SENSOR_RANGE_MULT_STORM,
                fleet.stats.sensorRangeMod
            )
            fleet.stats.removeTemporaryMod(modId + "_6")
            fleet.stats.addTemporaryModMult(
                0.1f, modId + "_5",
                "Inside magnetar storm", DETECTED_MULT_STORM,
                fleet.stats.detectedRangeMod
            )
        } else {
            fleet.stats.removeTemporaryMod(modId + "_2")
            fleet.stats.addTemporaryModMult(
                0.1f, modId + "_7",
                "Inside strong magnetar field", SENSOR_RANGE_MULT,
                fleet.stats.sensorRangeMod
            )
            fleet.stats.removeTemporaryMod(modId + "_5")
            fleet.stats.addTemporaryModMult(
                0.1f, modId + "_6",
                "Inside strong magnetar field", DETECTED_MULT,
                fleet.stats.detectedRangeMod
            )
        }

        var inFlare = false
        if (flareManager.isInActiveFlareArc(fleet)) {
            inFlare = true
        }

        val intensity: Float = getIntensityAtPoint(fleet.location)
        if (intensity <= 0) return

        val buffId = modId
        val buffDur = 0.1f

        var protectedFromCorona = false
        if (fleet.isInCurrentLocation &&
            Misc.getDistance(fleet, Global.getSector().playerFleet) < 500
        ) {
            for (curr in fleet.containingLocation.getCustomEntitiesWithTag(Tags.PROTECTS_FROM_CORONA_IN_BATTLE)) {
                val dist = Misc.getDistance(curr, fleet)
                if (dist < curr.radius + fleet.radius + 10f) {
                    protectedFromCorona = true
                    break
                }
            }
        }

        val bubbleGone = fleet.memoryWithoutUpdate[niko_MPC_ids.DRIVE_BUBBLE_DESTROYED] == true

        if (bubbleGone) {
            // CR loss and peak time reduction
            for (member in fleet.fleetData.membersListCopy) {
                val recoveryRate = member.stats.baseCRRecoveryRatePercentPerDay.modifiedValue
                val lossRate = member.stats.baseCRRecoveryRatePercentPerDay.baseValue
                var resistance = 1f // you cant actually resist this
                if (protectedFromCorona) resistance = 0f
                //if (inFlare) loss *= 2f;
                var lossMult = 1f
                if (inFlare) {
                    lossMult = 2f
                }
                val adjustedLossMult: Float =
                    0f + crLossMult * intensity * resistance * lossMult * StarCoronaTerrainPlugin.CR_LOSS_MULT_GLOBAL
                var loss = (-1f * recoveryRate + -1f * lossRate * adjustedLossMult) * days * 0.01f
                val curr = member.repairTracker.baseCR
                if (loss > curr) loss = curr
                if (resistance > 0) { // not actually resistance, the opposite
                    if (inFlare) {
                        member.repairTracker.applyCREvent(loss, "MPC_magnetarFlare", "Magnetar flare effect")
                    } else {
                        member.repairTracker.applyCREvent(loss, "MPC_magnetarField", "Magnetar corona effect")
                    }
                }

                // needs to be applied when resistance is 0 to immediately cancel out the debuffs (by setting them to 0)
                val peakFraction = 1f / Math.max(1.3333f, 1f + crLossMult * intensity)
                var peakLost = 1f - peakFraction
                peakLost *= resistance
                val degradationMult: Float = 1f + crLossMult * intensity * resistance / 2f
                member.buffManager.addBuffOnlyUpdateStat(PeakPerformanceBuff(buffId + "_1", 1f - peakLost, buffDur))
                member.buffManager.addBuffOnlyUpdateStat(CRLossPerSecondBuff(buffId + "_2", degradationMult, buffDur))
            }
        }

        // "wind" effect - adjust velocity
        val maxFleetBurn = fleet.fleetData.burnLevel
        val currFleetBurn = fleet.currBurnLevel

        var maxWindBurn: Float = pullFactor
        if (inFlare) {
            maxWindBurn *= 2f
        }
        if (bubbleGone) {
            maxWindBurn *= NO_BUBBLE_WIND_MULT
        }

        val currWindBurn = intensity * maxWindBurn
        val maxFleetBurnIntoWind = maxFleetBurn - Math.abs(currWindBurn)

        val angle = Misc.getAngleInDegreesStrict(this.entity.location, fleet.location)
        val windDir = Misc.getUnitVectorAtDegreeAngle(angle)
        if (currWindBurn < 0) {
            windDir.negate()
        }

        val velDir = Misc.normalise(Vector2f(fleet.velocity))
        velDir.scale(currFleetBurn)

        val fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir)

        var accelMult = 0.5f
        if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
            accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind)
        }
        val fleetAccelMult = fleet.stats.accelerationMult.modifiedValue
        if (fleetAccelMult > 0) { // && fleetAccelMult < 1) {
            accelMult /= fleetAccelMult
        }
        if (bubbleGone) {
            accelMult *= NO_BUBBLE_WIND_MULT
        } else if (fleet.getAbility(Abilities.SUSTAINED_BURN)?.isActive == true) {
            accelMult *= 0.25f
        }

        val seconds = days * Global.getSector().clock.secondsPerDay

        val vel = fleet.velocity
        val accel = fleet.acceleration
        windDir.scale(seconds * accel * accelMult)
        fleet.setVelocity(vel.x + windDir.x, vel.y + windDir.y)

        var glowColor = getAuroraColorForAngle(angle)
        val alpha = glowColor.alpha
        if (alpha < 75) {
            glowColor = Misc.setAlpha(glowColor, 75)
        }

        // visual effects - glow, tail
        val dist = Misc.getDistance(this.entity.location, fleet.location)
        var check = 100f
        if (params.relatedEntity != null) check = params.relatedEntity.radius * 0.5f
        if (dist > check) {
            val durIn = 1f
            val durOut = 10f
            Misc.normalise(windDir)
            val sizeNormal = 5f + 10f * intensity
            val sizeFlare = 10f + 15f * intensity
            for (view in fleet.views) {
                if (inFlare) {
                    view.windEffectDirX.shift(modId + "_flare", windDir.x * sizeFlare, durIn, durOut, 1f)
                    view.windEffectDirY.shift(modId + "_flare", windDir.y * sizeFlare, durIn, durOut, 1f)
                    view.windEffectColor.shift(modId + "_flare", glowColor, durIn, durOut, intensity)
                } else {
                    view.windEffectDirX.shift(modId, windDir.x * sizeNormal, durIn, durOut, 1f)
                    view.windEffectDirY.shift(modId, windDir.y * sizeNormal, durIn, durOut, 1f)
                    view.windEffectColor.shift(modId, glowColor, durIn, durOut, intensity)
                }
            }
        }

        // THE FUN SHIT
        // if you hit the middle of the magnetar, you get a prompt to transverse jump away or fucking die
        // doing so will drain CR of all ships to 0%, and set hull/armor integrity to 0 as well
        // fun stuff

        if (bubbleGone && intensity >= 1) {
            exposedToMagnetarCore(fleet)
        }
    }

    private fun exposedToMagnetarCore(fleet: CampaignFleetAPI) {
        if (fleet.isPlayerFleet) {
            playerExposedToMagnetarCore(fleet)
            return
        }
        for (member in fleet.fleetData.membersListCopy) {
            fleet.removeFleetMemberWithDestructionFlash(member)
        }
    }

    private fun playerExposedToMagnetarCore(fleet: CampaignFleetAPI) {
        if (fleet.memoryWithoutUpdate[BLIND_JUMPING] == true) {
            return
        }
        Global.getSector().campaignUI.showInteractionDialog(MPC_playerExposedToMagnetarCore(), entity.orbitFocus)
    }

    fun getIntensityAtPoint(point: Vector2f?): Float {
        val angle = Misc.getAngleInDegrees(params.relatedEntity.location, point)
        var maxDist = params.bandWidthInEngine
        if (flareManager.isInActiveFlareArc(angle)) {
            maxDist = computeRadiusWithFlare(flareManager.activeFlare)
        }
        val minDist = params.relatedEntity.radius
        val dist = Misc.getDistance(point, params.relatedEntity.location)
        if (dist > maxDist) return 0f
        var intensity = 1f
        if (minDist < maxDist) {
            intensity = 1f - (dist - minDist) / (maxDist - minDist)
            //intensity = 0.5f + intensity * 0.5f;
            if (intensity < 0) intensity = 0f
            if (intensity > 1) intensity = 1f
        }
        return intensity
    }

    fun computeRadiusWithFlare(flare: FlareManager.Flare): Float {
        //params.relatedEntity.getRadius() + 50f;
        //params.middleRadius + params.bandWidthInEngine * 0.75f;
        val inner = auroraInnerRadius
        val outer = params.middleRadius + params.bandWidthInEngine * 0.5f
        var thickness = outer - inner
        thickness *= flare.extraLengthMult
        thickness += flare.extraLengthFlat
        return inner + thickness
    }

    override fun getTerrainName(): String? {
        return if (flareManager.isInActiveFlareArc(Global.getSector().playerFleet)) {
            "Magnetar Storm"
        } else {
            return super.getTerrainName()
        }
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        val pad = 10f
        val small = 5f
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val fuel = Global.getSettings().getColor("progressBarFuelColor")
        val bad = Misc.getNegativeHighlightColor()

        tooltip.addTitle("Magnetar Field")
        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).text1, pad)

        val player = Global.getSector().playerFleet
        val bubbleGone = player.memoryWithoutUpdate[niko_MPC_ids.DRIVE_BUBBLE_DESTROYED] == true

        var nextPad = pad
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad)
            nextPad = small
        }

        val flare = (flareManager.isInActiveFlareArc(Global.getSector().playerFleet))
        var detectedMult = DETECTED_MULT
        var sensorMult = SENSOR_RANGE_MULT
        if (flare) {
            detectedMult = DETECTED_MULT_STORM
            sensorMult = SENSOR_RANGE_MULT_STORM
        }
        tooltip.addPara(
            "Reduces the range at which fleets inside can be detected by %s.", nextPad,
            highlight,
            "" + ((1f - detectedMult) * 100).toInt() + "%"
        )

        tooltip.addPara(
            "Reduces the range of a fleet's sensors by %s.", nextPad,
            highlight,
            "" + ((1f - sensorMult) * 100).toInt() + "%"
        )

        tooltip.addPara(
            "Reduces the combat readiness of all ships inside the field at a rather slow pace.",
            nextPad
        )
        tooltip.addPara(
            "The sheer magnetic force draws ferric objects, such as ships, closer to the magnetar.",
            nextPad
        )
        tooltip.addPara(
            "The drive field is responsible for shielding the fleet against the worst of the field. Should the fleet be " +
            "hit by an ionized pulse, the drive field would be shattered, severely amplifying the above negative effects.",
            nextPad
        )
        val bubbleStatusColor = if (bubbleGone) bad else Misc.getPositiveHighlightColor()
        val bubbleStatus = if (bubbleGone) "non-functional" else "intact"
        val protectionStatus = if (bubbleGone) "significantly worsening the field's effects" else "protecting you from the field"
        tooltip.addPara(
            "Your drive field is %s, $protectionStatus.",
            nextPad,
            bubbleStatusColor,
            bubbleStatus
        )

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad)
            tooltip.addPara("Combines the effect of a star corona and a magnetic field.", nextPad)
        }


    }

    override fun getRenderRange(): Float {
        return super.getRenderRange() + 5000f
    }

    override fun getNameColor(): Color? {
        val special = niko_MPC_magnetarPulse.BASE_COLOR
        val base = super.getNameColor()
        //bad = Color.red;
        return Misc.interpolateColor(base, special, Global.getSector().campaignUI.sharedFader.brightness * 1f)
    }

    override fun hasAIFlag(flag: Any): Boolean {
        return super.hasAIFlag(flag) ||
                //flag === TerrainAIFlags.CR_DRAIN || // sadly, this makes defender fleets act badly
                flag === TerrainAIFlags.BREAK_OTHER_ORBITS ||
                flag === TerrainAIFlags.MOVES_FLEETS
                //flag === TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE
    }

    override fun onScanned(
        factorTracker: niko_MPC_HTFactorTracker,
        playerFleet: CampaignFleetAPI,
        sensorBurstAbility: AbilityPlugin
    ) {
        if (!containsEntity(playerFleet)) return

        val id = entity.id

        if (factorTracker.scanned.contains(id)) {
            factorTracker.reportNoDataAcquired("Magnetar Field already scanned")
        } else {
            HyperspaceTopographyEventIntel.addFactorCreateIfNecessary(
                HTScanFactor("Magnetar Field scanned", HYPERSPACE_TOPOGRAPHY_POINTS), null
            )
            factorTracker.scanned.add(id)
        }
    }
}