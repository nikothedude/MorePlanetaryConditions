package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SoundAPI
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignPlanet
import com.fs.starfarer.combat.entities.terrain.Planet
import com.fs.starfarer.loading.SpecStore
import com.fs.starfarer.loading.specs.PlanetSpec
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.computeNumFighterBays
import java.awt.Color
import java.util.*
import kotlin.math.sign

object niko_MPC_miscUtils {
    @JvmStatic
    fun SectorEntityToken.isOrbitalStation(): Boolean {
        return (hasTag(Tags.STATION))
    }

    @JvmStatic
    fun SectorEntityToken.getStationFleet(): CampaignFleetAPI? {
        // it seems that if the station fleet is destroyed, the station base fleet takes its place
        return memoryWithoutUpdate.getFleet(MemFlags.STATION_FLEET) ?: memoryWithoutUpdate.getFleet(MemFlags.STATION_BASE_FLEET)
    }

    @JvmStatic
    fun SectorEntityToken.getStationMarket(): MarketAPI? {
        if (market != null) return market
        val stationFleet = getStationFleet()
        if (stationFleet != null) {
            return stationFleet.getStationFleetMarket()
        }
        return null
    }

    @JvmStatic
    fun SectorEntityToken.getStationEntity(): SectorEntityToken? {
        val marketEntity = market?.primaryEntity
        if (marketEntity != null) {
            if (marketEntity == this) niko_MPC_debugUtils.log.info("found self-referencing station entity on $this, ${this.market?.name}")
            return marketEntity
        }
        val fleetEntity = getStationFleet()?.getStationFleetMarket()?.primaryEntity
        if (fleetEntity != null) {
            if (fleetEntity !== orbitFocus) {
                niko_MPC_debugUtils.log.info("$this had a fleet entity of $fleetEntity, and a orbitfocus of $orbitFocus")
            }
            return fleetEntity
        }
        return orbitFocus
    }

    @JvmStatic
    fun CampaignFleetAPI.getStationFleetMarket(): MarketAPI? {
        return memoryWithoutUpdate[MemFlags.STATION_MARKET] as? MarketAPI
    }

    @JvmStatic
    fun CampaignFleetAPI.isStationFleet(): Boolean {
        return (isStationMode || memoryWithoutUpdate.contains(MemFlags.STATION_MARKET))
    }

    @JvmStatic
    fun MarketAPI.getStationsInOrbit(): MutableList<SectorEntityToken> {
        val stations = ArrayList<SectorEntityToken>()
        for (connectedEntity: SectorEntityToken in connectedEntities) {
            if (connectedEntity.isOrbitalStation()){
                stations.add(connectedEntity)
            }
        }
        return stations
    }

    @JvmStatic
    fun SectorEntityToken.isDespawning(): Boolean {
        return (!isAlive || hasTag(Tags.FADING_OUT_AND_EXPIRING))
    }

    fun formatStringsToLines(data: Collection<String>): String {
        var addNewLine = false
        var finalString = ""

        for (entry in data) {
            var string = entry
            if (addNewLine) {
                string = "\n" + string
            }
            addNewLine = true // the first string gets no newline
            finalString += string
        }
        if (finalString.isEmpty()) finalString = "None"
        return finalString

    }

    fun NebulaEditor.setArc(
        value: Int,
        x: Float,
        y: Float,
        innerRadius: Float,
        outerRadius: Float,
        startAngle: Float,
        endAngle: Float,
        endRadiusMult: Float = 1f,
        noiseThresholdToClear: Float = 0f
    ) {
        val circumference = Math.PI.toFloat() * 2f * outerRadius
        val ts: Float = niko_MPC_reflectionUtils.get("ts", this) as Float
        val degreesPerIteration: Float = 360f / (circumference / (ts * 0.5f))
        var angle = startAngle
        while (angle < endAngle) {
            val dir = Misc.getUnitVectorAtDegreeAngle(angle)
            var distMult = 1f
            if (endAngle > startAngle) {
                val p = (angle - startAngle) / (endAngle - startAngle)
                distMult = 1f + (endRadiusMult - 1f) * p
            }

            //for (float dist = innerRadius; dist <= outerRadius; dist += ts * 0.5f) {
            var dist = innerRadius * distMult
            while (dist <= innerRadius * distMult + (outerRadius - innerRadius)) {
                val curr = Vector2f(dir)
                //curr.scale(dist * distMult);
                curr.scale(dist)
                curr.x += x
                curr.y += y
                setTileAt(curr.x, curr.y, value, noiseThresholdToClear, isSetToOrigInsteadOfClear)
                dist += ts * 0.5f
            }
            angle += degreesPerIteration
        }
    }

    fun SectorEntityToken.getApproximateHyperspaceLoc(): Vector2f {
        if (isInHyperspace) return location
        if (containingLocation !is StarSystemAPI) return Vector2f()
        val starSystem = containingLocation as StarSystemAPI

        val offset = Vector2f.sub(location, starSystem.center.location, Vector2f()) // taken from transverse jump
        val maxInSystem = 20000f
        val maxInHyper = 2000f
        var f = offset.length() / maxInSystem
        if (f > 0.5f) f = 0.5f

        val angle = Misc.getAngleInDegreesStrict(offset)

        val destOffset = Misc.getUnitVectorAtDegreeAngle(angle)
        destOffset.scale(f * maxInHyper)

        Vector2f.add(starSystem.location, destOffset, destOffset)

        return destOffset
    }

    fun PulsarBeamTerrainPlugin.getApproximateOrbitDays(): Float {
        val rotationSpeed = niko_MPC_reflectionUtils.get("pulsarRotation", this) as Float
        if (rotationSpeed == 0f) return 0f
        return -(365/rotationSpeed)
    }

    fun refreshCoronaDefenderFleetVariables(fleet: CampaignFleetAPI) {
        val flagship = fleet.flagship

        if (flagship.shipName == niko_MPC_ids.SKULIODA_SHIP_NAME) {
            val newVariant: ShipVariantAPI
            if (niko_MPC_settings.MCTE_loaded && MCTE_settings.PULSAR_EFFECT_ENABLED) {
                newVariant = Global.getSettings().getVariant("legion_xiv_skulioda").clone()
                flagship.captain?.setPersonality(Personalities.RECKLESS)
            } else {
                newVariant = Global.getSettings().getVariant("legion_xiv_Elite").clone()
                flagship.captain?.setPersonality(Personalities.AGGRESSIVE)
            }
            // probably safe, i mean, when the hell will the corona resist fleet get another skuliodas prize
            newVariant.originalVariant = null
            flagship.setVariant(newVariant, true, false)

            flagship.variant.addPermaMod(HullMods.SOLAR_SHIELDING, true)
            flagship.variant.addPermaMod(HullMods.HEAVYARMOR, true)
            flagship.variant.addPermaMod("niko_MPC_fighterSolarShielding", true)
        }
        flagship.repairTracker.cr = flagship.repairTracker.maxCR
        flagship.variant.tags += Tags.VARIANT_ALWAYS_RECOVERABLE

        for (ship in fleet.fleetData.membersListCopy - flagship) {
            ship.variant.addPermaMod(HullMods.SOLAR_SHIELDING, true)
            if (ship.variant.computeNumFighterBays() > 0) ship.variant.addPermaMod("niko_MPC_fighterSolarShielding", true)
            ship.repairTracker.cr = ship.repairTracker.maxCR
        }
    }

    class MPC_farAwaySoundPlayer(
        val containingLoc: LocationAPI,
        val source: Vector2f,
        val token: SectorEntityToken,
        val sound: SoundAPI
    ): niko_MPC_baseNikoScript() {
        var deleteTimer = IntervalUtil(30f, 30f)

        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            val days = Misc.getDays(amount)
            deleteTimer.advance(days)
            if (deleteTimer.intervalElapsed()) {
                delete()
                return
            }

            val playerFleet = Global.getSector().playerFleet
            if (!containingLoc.isCurrentLocation) {
                delete()
                return
            }

            val dir = VectorUtils.getAngle(playerFleet.location, source)
            val newLoc = MathUtils.getPointOnCircumference(playerFleet.location, (playerFleet.radius) * 1.1f, dir)
            token.setLocation(newLoc.x, newLoc.y)
            sound.setLocation(newLoc.x, newLoc.y)
        }

    }

    // this SHOULD maintain directional sound. i hope.
    fun playSoundEvenIfFar(soundId: String, location: LocationAPI, loc: Vector2f, volume: Float = 1f, pitch: Float = 1f, loop: Boolean = false, entity: SectorEntityToken? = null, outOfRangeVolumeMult: Float = 1f) {
        var entity = entity
        if (location.isCurrentLocation) {
            val viewport = Global.getSector().viewport
            var volume = volume

            var soundLoc = loc
            var sound: SoundAPI? = null
            var shouldUseScript = false

            if (!viewport.isNearViewport(loc, 10f)) {
                volume *= outOfRangeVolumeMult
                shouldUseScript = true
            }

            if (shouldUseScript) {
                entity = location.createToken(0f, 0f)
            }

            if (loop) {
                Global.getSoundPlayer().playLoop(soundId, entity, pitch, volume, soundLoc, Misc.ZERO)
            } else {
                sound = Global.getSoundPlayer().playSound(soundId, pitch, volume, Global.getSector().playerFleet.location, Misc.ZERO)
            }

            if (shouldUseScript && sound != null) {
                val script = MPC_farAwaySoundPlayer(
                    location,
                    soundLoc,
                    entity!!,
                    sound
                )
                script.start()
                script.advance(0.1f)
            }
        }
    }

    fun playSoundFar(soundId: String, location: LocationAPI, loc: Vector2f, volume: Float = 1f, pitch: Float = 1f) {
        if (location.isCurrentLocation) {
            val entity = location.createToken(0f, 0f)

            val sound = Global.getSoundPlayer().playSound(soundId, pitch, volume, Global.getSector().playerFleet.location, Misc.ZERO)

            val script = MPC_farAwaySoundPlayer(
                location,
                loc,
                entity,
                sound
            )
            script.start()
            script.advance(0.1f)
        }
    }

    fun PlanetAPI.changeTypeManual(newType: String, minColor: Color, maxColor: Color, random: Random = MathUtils.getRandom()) {
        (this as CampaignPlanet).changeTypeManual(newType, minColor, maxColor, random)
    }

    fun CampaignPlanet.changeTypeManual(newType: String, minColor: Color, maxColor: Color, random: Random = MathUtils.getRandom()) {
        val spec = Global.getSettings().allPlanetSpecs.firstOrNull { it.planetType == newType } ?: return

        niko_MPC_reflectionUtils.set("type", this, newType)
        niko_MPC_reflectionUtils.set("spec", this, spec)

        val var4 = CampaignPlanet.getColor(minColor, maxColor, random)
        this.spec.setPlanetColor(var4)
        if (this.spec.getAtmosphereThickness() > 0.0f) {
            var var5 = Misc.interpolateColor(this.spec.getAtmosphereColor(), var4, 0.25f)
            var5 = Misc.setAlpha(var5, this.spec.getAtmosphereColor().alpha)
            this.spec.setAtmosphereColor(var5)
            if (this.spec.getCloudTexture() != null) {
                val var6 = Misc.interpolateColor(this.spec.getCloudColor(), var4, 0.25f)
                Misc.setAlpha(var6, this.spec.getCloudColor().alpha)
                this.spec.setAtmosphereColor(var5)
            }
        }

        var var13 = this.spec.getTilt()
        var var15 = this.spec.getPitch()
        var var7: Float = sign(random.nextFloat() - 0.5f)
        var var8 = random.nextFloat().toDouble()
        if (var7 > 0.0f) {
            var13 = (var13.toDouble() + var8 * 45.0).toFloat()
        } else {
            var13 = (var13.toDouble() + var8 * -45.0).toFloat()
        }

        var7 = sign(random.nextFloat() - 0.5f)
        var8 = random.nextFloat().toDouble()
        if (var7 > 0.0f) {
            var15 = (var15.toDouble() + var8 * 45.0).toFloat()
        } else {
            var13 = (var13.toDouble() + var8 * -15.0).toFloat()
        }

        this.spec.setTilt(var13)
        this.spec.setPitch(var15)
        this.applySpecChanges()

        try {
            val newDiff = PlanetSpec.createDiff(newType, this.spec)
            niko_MPC_reflectionUtils.set("diff", this, newDiff)

        } catch (var11: Throwable) {
            throw RuntimeException(var11)
        }

        val newGraphics = Planet(newType, this.radius, 0.0f, Vector2f())
        niko_MPC_reflectionUtils.set("graphics", this, newGraphics)
        this.removeTag("star")
        this.removeTag("planet")
        this.removeTag("gas_giant")
        if (this.graphics.isStar) {
            this.addTag("star")
        } else {
            if (this.graphics.isGasGiant) {
                this.addTag("gas_giant")
            }

            this.addTag("planet")
        }
    }
}
