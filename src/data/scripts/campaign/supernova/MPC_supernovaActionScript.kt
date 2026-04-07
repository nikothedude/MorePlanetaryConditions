package data.scripts.campaign.supernova

import com.fs.graphics.particle.GenericTextureParticle
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain.TileParams
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.campaign.ParticleController
import com.fs.state.AppDriver
import data.scripts.campaign.supernova.entities.MPC_supernovaExplosion
import data.scripts.campaign.supernova.renderers.MPC_supernovaShader
import data.scripts.campaign.supernova.terrain.SupernovaNebulaHandler
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids.SUPERNOVA_NEBULA_ONE_MEMID
import data.utilities.niko_MPC_miscUtils.changeTypeManual
import data.utilities.niko_MPC_miscUtils.playSoundFar
import data.utilities.niko_MPC_miscUtils.setArc
import data.utilities.niko_MPC_reflectionUtils
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

class MPC_supernovaActionScript(
    val star: PlanetAPI
): niko_MPC_baseNikoScript() {

    companion object {
        fun getCurrStage(): Stage? {
            return (Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"]) as? Stage
        }


        fun addNebulaFromPNG(
            image: String?, centerX: Float, centerY: Float, location: LocationAPI,
            category: String?, key: String?, tilesWide: Int, tilesHigh: Int,
            terrainType: String?, age: StarAge?, chunkSize: Int = 10000, tileSize: Float = NebulaTerrainPlugin.TILE_SIZE
        ): SectorEntityToken? {
            try {
                var img: BufferedImage? = null
                //img = ImageIO.read(new File("../starfarer.res/res/data/campaign/terrain/nebula_test.png"));
                img = ImageIO.read(Global.getSettings().openStream(image))

                //val chunkSize = 10000
                val w = img.width
                val h = img.height
                val data = img.getData()
                var i = 0
                while (i < w) {
                    var j = 0
                    while (j < h) {
                        var chunkWidth = chunkSize
                        if (i + chunkSize > w) chunkWidth = w - i
                        var chunkHeight = chunkSize
                        if (j + chunkSize > h) chunkHeight = h - i


//		    		boolean hasAny = false;
//		    		for (int x = i; x < i + chunkWidth; x++) {
//		    			for (int y = j; y < j + chunkHeight; y++) {
//		    				int [] pixel = data.getPixel(i, h - j - 1, (int []) null);
//		    				int total = pixel[0] + pixel[1] + pixel[2];
//		    				if (total > 0) {
//		    					hasAny = true;
//		    					break;
//		    				}
//		    			}
//		    		}
//		    		if (!hasAny) continue;
                        val string = StringBuilder()
                        for (y in j + chunkHeight - 1 downTo j) {
                            for (x in i..<i + chunkWidth) {
                                val pixel = data.getPixel(x, h - y - 1, null as IntArray?)
                                val total = pixel[0] + pixel[1] + pixel[2]
                                if (total > 0) {
                                    string.append("x")
                                } else {
                                    string.append(" ")
                                }
                            }
                        }

                        val x = centerX - tileSize * w.toFloat() / 2f + i.toFloat() * tileSize + chunkWidth / 2f * tileSize
                        val y = centerY - tileSize * h.toFloat() / 2f + j.toFloat() * tileSize + chunkHeight / 2f * tileSize

                        val curr = location.addTerrain(
                            terrainType, TileParams(
                                string.toString(),
                                chunkWidth, chunkHeight,
                                category, key, tilesWide, tilesHigh, null
                            )
                        )
                        curr.location.set(x, y)

                        if (location is StarSystemAPI) {
                            val system = location

                            system.age = age
                            system.setHasSystemwideNebula(true)
                        }

                        return curr
                        j += chunkSize
                    }
                    i += chunkSize
                }
                return null
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        const val PREPARE_PHASE_LENGTH = 9f // seconds
        const val DURING_PHASE_LENGTH = 30f
        const val ENDING_PHASE_LENGTH = 10f

        const val MIN_STAR_SIZE = 125f
        const val MIN_CORONA_BAND = 25f

        // the absolute max. we will make the edges more fuzzy...
        const val MAX_NEBULA_RADIUS = 4000f
        const val SHIELD_BUBBLE_ACTIVATE_DIST = 13500f
        const val SHIELD_BUBBLE_DIST = 30000f
    }

    enum class Stage {
        BEFORE,
        DURING,
        ENDING;

        fun apply() {}
    }

    var coronaRad = 0f
    var coronaBand = 0f
    var baseRadius = 0f
    var baseColor = star.lightColor
    var screenshake: MPC_supernovaCameraShake? = null
    var supernovaGlow: ParticleControllerAPI? = null
    var supernovaParticle: Any? = null
    var shockwaveColor = Color(star.spec.atmosphereColor.red, star.spec.atmosphereColor.blue, star.spec.atmosphereColor.green, star.spec.atmosphereColor.alpha)
        get() {
            if (field == null) field = Color(star.spec.atmosphereColor.red, star.spec.atmosphereColor.blue, star.spec.atmosphereColor.green, star.spec.atmosphereColor.alpha)
            return field
        }
    var shockwave: MPC_supernovaExplosion? = null
    init {
        Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.BEFORE
        playSoundFar("MPC_supernovaInit", star.containingLocation, star.location)
        val corona = Misc.getCoronaFor(star)
        coronaRad = corona.params.middleRadius
        coronaBand = corona.params.bandWidthInEngine
        baseRadius = star.radius

        LunaCampaignRenderer.addRenderer(MPC_supernovaShader(this))
    }

    override fun startImpl() {
        star.addScript(this)
    }

    override fun stopImpl() {
        star.removeScript(this)
        Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    val interval = IntervalUtil(PREPARE_PHASE_LENGTH, PREPARE_PHASE_LENGTH) // will be overridden by stages
    val initRadius = star.radius
    override fun advance(amount: Float) {
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            when (getCurrStage()!!) {
                Stage.BEFORE -> {
                    playSoundFar("MPC_supernova", star.containingLocation, star.location)
                    playSoundFar("MPC_supernovaThunder", star.containingLocation, star.location)
                    if (star.containingLocation.isCurrentLocation) {
                        playSoundFar("MPC_supernovaTwo", star.containingLocation, star.location)
                    } else {
                        Global.getSoundPlayer().playSound("MPC_supernovaTwo", 1f, 1f, Global.getSector().playerFleet.location, Misc.ZERO)
                    }
                    playSoundFar("MPC_supernovaUnder", star.containingLocation, star.location)
                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.DURING

                    val containing = star.containingLocation
                    val explParams = ExplosionEntityPlugin.ExplosionParams(
                        shockwaveColor,
                        containing,
                        Vector2f(star.location),
                        star.radius * 1.1f,
                        12f
                    )
                    explParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.EXTREME
                    val expl = containing.addCustomEntity(
                        "MPC_supernovaExplosionInit",
                        null,
                        "MPC_supernovaExplosion",
                        Factions.NEUTRAL,
                        explParams
                    )
                    shockwave = expl.customPlugin as MPC_supernovaExplosion
                    supernovaGlow = star.containingLocation.addParticle(
                        Vector2f(star.location),
                        Misc.ZERO,
                        100000f,
                        1f,
                        0f,
                        10000f,
                        shockwaveColor
                    )
                    if (supernovaGlow != null) {
                        supernovaParticle = niko_MPC_reflectionUtils.get("p", supernovaGlow!!, ParticleController::class.java)
                    }
                    screenshake = MPC_supernovaCameraShake(this)
                    screenshake?.start()

                    star.changeTypeManual(
                        StarTypes.WHITE_DWARF,
                        Color.WHITE,
                        Color.WHITE,
                        MathUtils.getRandom()
                    )

                    interval.setInterval(DURING_PHASE_LENGTH, DURING_PHASE_LENGTH)

                    Global.getSector().campaignUI.addMessage("WARNING:::MASSIVE SPATIAL DISRUPTION DETECTED", Misc.getNegativeHighlightColor())

                    val nebula = addNebulaFromPNG("data/campaign/terrain/generic_system_nebula.png",
                        0f, 0f, star.containingLocation, "terrain", "nebula", 2, 2, "MPC_supernovaRemnantNebula", StarAge.YOUNG
                    ) as? CampaignTerrainAPI ?: return
                    star.containingLocation.memoryWithoutUpdate[SUPERNOVA_NEBULA_ONE_MEMID] = nebula.plugin
                    addNebulaFromPNG("data/campaign/terrain/MPC_superheatedNebula.png",
                        0f, 0f, star.containingLocation, "terrain", "nebula_amber", 2, 2, "MPC_superheatedNebula", StarAge.YOUNG)
                    getJumpPoints().forEach { it.memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] = true }

                }
                Stage.DURING -> {
                    supernovaGlow?.maxAge = 500f
                    screenshake?.stop()
                    screenshake = null

                    star.containingLocation.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "MPC_supernovaAmbience"
                    /*val nebulaTwo = Misc.addNebulaFromPNG("data/campaign/terrain/generic_system_nebula.png",
                        0f, 0f, star.containingLocation, "terrain", "nebula_amber", 2, 2, star.starSystem.age) as? CampaignTerrainAPI ?: return
                    star.containingLocation.memoryWithoutUpdate[SUPERNOVA_NEBULA_TWO_MEMID] = nebulaTwo.plugin*/

                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.ENDING
                    interval.setInterval(ENDING_PHASE_LENGTH, ENDING_PHASE_LENGTH)
                }
                Stage.ENDING -> {
                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = null
                    supernovaGlow?.maxAge = 0f
                    getJumpPoints().forEach { it.memoryWithoutUpdate.unset(JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY) }
                    supernovaFinalized()
                    failsafe()
                    delete()
                    return
                }
            }
        }

        val progress = getStageProgress()
        val corona = Misc.getCoronaFor(star) ?: return
        when (getCurrStage()!!) {
            Stage.BEFORE -> {
                val dist = (initRadius - MIN_STAR_SIZE)
                val remainder = (dist / initRadius)
                val inverted = (1 - (progress * remainder))
                star.radius = (initRadius * (inverted)).coerceAtLeast(MIN_STAR_SIZE)
                val diff = initRadius - star.radius
                //corona.params.bandWidthInEngine = (coronaBand * inverted).coerceAtLeast(MIN_CORONA_BAND)
                corona.params.middleRadius = (coronaRad * inverted).coerceAtLeast(MIN_STAR_SIZE)

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(progress)
                }

                //star.lightColorOverrideIfStar = Color(r, g, b, 255)

                star.containingLocation.addHitParticle(
                    star.location,
                    Misc.ZERO,
                    star.radius + (diff * 0.5f),
                    progress * 10f,
                    amount,
                    star.spec.glowColor
                )
            }
            Stage.DURING -> {
                val shockwaveDist = shockwave!!.getProgress() * 5f
                if (supernovaParticle is GenericTextureParticle) {
                    val casted = supernovaParticle as GenericTextureParticle
                    casted.setSize(shockwaveDist, shockwaveDist)
                }

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(1f)
                }
            }
            Stage.ENDING -> {
                val shockwaveDist = shockwave!!.getProgress() * 5f
                if (supernovaParticle is GenericTextureParticle) {
                    val casted = supernovaParticle as GenericTextureParticle
                    val inverted = 1f - (interval.elapsed / interval.intervalDuration)
                    casted.setSize(shockwaveDist * inverted, shockwaveDist * inverted)
                }

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(1f - progress)
                }
            }
        }
    }

    private fun supernovaFinalized() {
        MPC_supernovaMoteScript(star).start()
    }

    // finishes generation in case our supernova didnt actually do it all
    private fun failsafe() {
        val editors = SupernovaNebulaHandler.getEditors() ?: return
        val rad = star.radius * 3f
        for (editor in editors.values) {
            editor.setArc(
                100,
                0f,
                0f,
                rad,
                MAX_NEBULA_RADIUS,
                0f,
                360f
            )
        }


    }

    fun getJumpPoints(): MutableSet<JumpPointAPI> {
        val points = HashSet<JumpPointAPI>()

        points.addAll(star.starSystem.jumpPoints as Collection<out JumpPointAPI>)
        for (point in Global.getSector().hyperspace.jumpPoints) {
            val casted = point as? JumpPointAPI ?: continue
            if (casted.destinationStarSystem == star.starSystem) points += casted
        }

        return points
    }

    fun getStageProgress(): Float {
        val curr = interval.elapsed
        val dur = interval.intervalDuration

        return (curr / dur)
    }

    fun detonate() {
        doExplodeAlwaysEffects()
    }

    private fun doExplodeAlwaysEffects() {
        Global.getSoundPlayer().playUISound( // TODO add this sound. make it mono too
            "MPC_supernovaDistant",
            1f,
            1f
        ) // boom.
        Global.getSector().campaignUI.addMessage(
            "WARNING::::MASSIVE SPATIAL DISTORTION DETECTED",
            Misc.getNegativeHighlightColor()
        )
    }
}