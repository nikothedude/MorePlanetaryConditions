package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.ids.Tags.HULLMOD_DMOD
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import lunalib.lunaExtensions.getMarketsCopy
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.computeNumFighterBays

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

    fun SectorEntityToken.getApproximateHyperspaceLoc(

    ): Vector2f {
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

    fun refreshCoronaDefenderFleetSmods(fleet: CampaignFleetAPI) {
        val flagship = fleet.flagship
        flagship.variant.addPermaMod(HullMods.SOLAR_SHIELDING, true)
        flagship.variant.addPermaMod(HullMods.HEAVYARMOR, true)
        flagship.variant.addPermaMod("niko_MPC_fighterSolarShielding", true)

        for (ship in fleet.fleetData.membersListCopy - flagship) {
            ship.variant.addPermaMod(HullMods.SOLAR_SHIELDING, true)
            if (ship.variant.computeNumFighterBays() > 0) ship.variant.addPermaMod("niko_MPC_fighterSolarShielding", true)
        }
    }


}
