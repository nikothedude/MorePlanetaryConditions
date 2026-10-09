package data.scripts.campaign.magnetar.crisis.intel.church

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.StarSystem
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.MPC_churchOverextensionCondition
import data.scripts.campaign.magnetar.crisis.MPC_knightsDownedCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_indieContributionIntel
import data.scripts.campaign.rulecmd.MPC_IAIICChurchCMD
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.addDebrisField
import org.magiclib.kotlin.getFactionMarkets
import org.magiclib.kotlin.isMilitary
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color

open class MPC_churchContributionIntel: BaseIntelPlugin(), FleetEventListener, ColonyPlayerHostileActListener {

    companion object {
        fun get(withUpdate: Boolean = false, noUpdate: Boolean = false, text: TextPanelAPI? = null): MPC_churchContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_churchContributionIntel()
                    Global.getSector().intelManager.addIntel(intel, noUpdate, text)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_churchContributionIntel
        }
        fun getMarket(id: String): MarketAPI? = Global.getSector().economy.getMarket(id)
        fun destroyKnights() {
            MPC_knightsDownedCondition.MPC_knightsDownedScript().start()
            for (market in Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getFactionMarkets()) {
                for (iterInd in market.industries.filter { it.spec.hasTag(Industries.TAG_MILITARY) }) {
                    iterInd.setDisrupted(MPC_knightsDownedCondition.MPC_knightsDownedScript.DURATION)
                }
            }
        }

        private fun getTrapPlanet(): PlanetAPI {

        }

        private fun getHideoutSystem(): StarSystemAPI {

        }

        private fun getHideoutPlanet(): PlanetAPI {

        }

        private fun getTransmissionSystem(): StarSystemAPI {

        }

        private fun getTrapSystem(): StarSystemAPI {
            TODO("Not yet implemented")
        }

        private fun getKnightLeaderLocation(): StarSystemAPI {

        }

        const val STAGE_ONE_ACTIONS_NEEDED = 3
        const val STAGE_TWO_ACTIONS_NEEDED = 3
        const val CREDITS_NEEDED = 5000f // they cant pay much lmao
        const val TRANSMISSION_STATION_DET_DIST = 500f
        const val KEY = "\$MPC_churchContributionIntel"
    }

    var stageOneCompletion = 0
        set(value) {
            if (state == State.REMOVE_TRUST && value >= STAGE_ONE_ACTIONS_NEEDED) {
                state = State.INTERMISSION // so we can explain what we've done, what to do next
                sendUpdateIfPlayerHasIntel(State.INTERMISSION, false)
            }
            field = value
        }
    var stageTwoCompletion = 0
        set(value) {
            if (state == State.EMPOWER_POPULACE && value >= STAGE_TWO_ACTIONS_NEEDED) {
                state = State.INTERMISSION_TWO
                sendUpdateIfPlayerHasIntel(State.INTERMISSION_TWO, false)
            }
            field = value
        }

    var leadersKilled = 0f
        set(value) {
            if (!leaderThreshMet && value >= 3f) {
                stageTwoCompletion++
                leaderThreshMet = true
            }
            field = value
        }
    var leaderThreshMet = false
    val marketsArmed = ArrayList<String>()

    var creditsGainedFromArmsTrade = CREDITS_NEEDED
        set(value) {
            if (value >= CREDITS_NEEDED) {
                armsGivenState = ArmsGivenState.DONE
                sendUpdateIfPlayerHasIntel(ArmsGivenState.DONE, false)
            }
            field = value
        }
    var armsGivenState: ArmsGivenState = ArmsGivenState.NOT_STARTED

    enum class ArmsGivenState {
        NOT_STARTED,
        MET,
        DONE,
        DEPLOYED;
    }

    enum class State {
        REMOVE_TRUST {
            override fun apply() {
                Global.getSector().economy.getMarket("asher")?.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                // do the bit with the weapons here
                // transmit to comm relay in major sys
                // ambuhed by fleet on the way

                // some piss easy expedition they take for formalities
                // ambush the fleet and destroy it?

                // after this unrest becomes serious across church worlds, reducing stability by 1 or so
            }
            override fun unapply() {
                Global.getSector().economy.getMarket("asher")?.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
        },
        INTERMISSION {
            override fun apply() {
                super.apply()

                class IntermissionOneScript(): MPC_delayedExecutionNonLambda(IntervalUtil(0f, 0f)) {
                    override fun executeImpl() {
                        Global.getSector().campaignUI.showInteractionDialog(
                            RuleBasedInteractionDialogPluginImpl("MPC_IAIICCHIntermissionOneDialog"),
                            Global.getSector().playerFleet
                        )
                        MPC_churchContributionIntel.get()!!.state = MPC_churchContributionIntel.State.EMPOWER_POPULACE
                    }
                }
                IntermissionOneScript().start()

            }
        },
        EMPOWER_POPULACE {
            override fun apply() {
                super.apply()
                get()!!.unrestActive = true
                spawnLeaderFleet()
                Global.getSector().getEntityById("ceyx")?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            private fun spawnLeaderFleet() {
                TODO("Not yet implemented")
            }

            override fun unapply() {
                super.unapply()
                Global.getSector().getEntityById("ceyx")?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)

            }
            // also have to weaken knights here
            // assassinate a few leaders - see the church part for ideas
            // raid oryx or whatever its called

            // bust into a knights of ludd prison and free some dangerous prisoners
            // make it look like it was some guards being stupid

            // start distributing weapons to church worlds

            // get into contact with some militant groups and do some favors for each

            // tacbombing military worlds, too
        },
        INTERMISSION_TWO {
            override fun apply() {
                super.apply()
                class IntermissionTwoScript(): MPC_delayedExecutionNonLambda(IntervalUtil(35f, 39f)) {
                    override fun executeImpl() {
                        Global.getSector().campaignUI.showInteractionDialog(
                            RuleBasedInteractionDialogPluginImpl("MPC_IAIICCHIntermissionTwoDialog"),
                            Global.getSector().playerFleet
                        )
                        MPC_churchContributionIntel.get()!!.state = MPC_churchContributionIntel.State.NEGOTIATE_WITH_LEADER
                    }
                }
                IntermissionTwoScript().start()

            }
            // complication - the knights actually close their hand on the church and enact a form of martial law
            // a ldudic leader is responsible for this
            // negoatite with them
        },
        NEGOTIATE_WITH_LEADER {
            override fun apply() {
                super.apply()
                get()!!.martialLawEnacted = true
                val tart = Global.getSector().economy.getMarket("tartessus")
                val person = MPC_People.getImportantPeople()[MPC_People.CHURCH_LEADER]
                person?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                tart.commDirectory.addPerson(person)
                tart?.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }
            override fun unapply() {
                super.unapply()
                val tart = Global.getSector().economy.getMarket("tartessus")
                val person = MPC_People.getImportantPeople()[MPC_People.CHURCH_LEADER]
                person?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
                tart?.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
            // turns out the leader doesnt like the knights but knows this may destroy the church
            // would love to remove the knights but cant, not yet
            // recognizes that you can solve this problem
            // asks that you recover something to hold the glue togerther while knights are removed

            // this is where we find broker
            // turns out broker is actually some prophet from the olden days
            // extremely influential, but vanished
            // if we can convince him... the leader will release the hold

            // this time, he left because he realized his time had come, and he had to move on
            // used the path to cover his tracks
            // he was always mute
            // he recognizes that he needs to come back, etc, etc

            // WHY CANT WE JUST KILL THE LEADER?
            // then martial law will never end

            // effects - +2 stab, more ground def and fleet size, reduced ind output, reduced income
            // lasts 2 cycles after crisis ends if not resolved
        },
        GO_TO_COORDINATES {
            override fun apply() {
                super.apply()

                val planet = getTrapPlanet()
                planet.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                super.unapply()

                val planet = getTrapPlanet()
                planet.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
        },

        TRACE_TRANSMISSION {
            override fun apply() {
                super.apply()

                val sys = getTrapSystem()
                val loc = sys.addCustomEntity(
                    "MPC_IAIICChurchTransmissionLoc",
                    "Transmission Target",
                    Entities.MISSION_LOCATION,
                    Factions.NEUTRAL
                )
                loc.setCircularOrbit(sys.star, 56f, 10000f, 900f)
                loc.memoryWithoutUpdate["\$MPC_IAIICChurchTransmissionLoc"] = true
                loc.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                super.unapply()

                val sys = getTrapSystem()
                val loc = sys.getEntityById("MPC_IAIICChurchTransmissionLoc")
                sys.removeEntity(loc)
            }
          // find a comms buoy
        },
        TRACE_TRANSMISSION_2 {
            override fun apply() {
                super.apply()

                val sys = getTransmissionSystem()
                val loc = sys.addCustomEntity(
                    "MPC_IAIICChurchTransmissionLocTwo",
                    "Transmission Target",
                    Entities.MISSION_LOCATION,
                    Factions.NEUTRAL
                )
                loc.setCircularOrbit(sys.star, MathUtils.getRandomNumberInRange(0f, 360f), 7500f, 534f)
                loc.memoryWithoutUpdate["\$MPC_IAIICChurchTransmissionLocTwo"] = true

                class ExplosionScript(val loc: SectorEntityToken): niko_MPC_baseNikoScript() {
                    override fun startImpl() {
                        loc.addScript(this)
                    }

                    override fun stopImpl() {
                        loc.removeScript(this)
                    }

                    override fun runWhilePaused(): Boolean = false

                    override fun advance(amount: Float) {
                        if (!loc.starSystem.isCurrentLocation) return
                        val dist = MathUtils.getDistance(loc, Global.getSector().playerFleet)
                        if (dist <= TRANSMISSION_STATION_DET_DIST) {
                            explode()
                            delete()
                        }
                    }

                    fun explode() {
                        val params = ExplosionEntityPlugin.ExplosionParams(
                            Color(255, 90, 60),
                            loc.starSystem,
                            loc.location,
                            100f,
                            1f
                        )
                        val explosion = loc.starSystem.addCustomEntity(
                            null,
                            null,
                            Entities.EXPLOSION,
                            Factions.NEUTRAL,
                            params
                        )
                        explosion.setLocation(loc.location.x, loc.location.y)
                        loc.starSystem.removeEntity(loc)

                        val debrisParams = DebrisFieldTerrainPlugin.DebrisFieldParams(
                            100f,
                            2f,
                            Float.MAX_VALUE,
                            0f
                        )
                        val field = loc.starSystem.addDebrisField(
                            debrisParams,
                            MathUtils.getRandom()
                        )
                        field.setLocation(loc.location.x, loc.location.y)
                        field.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                        field.memoryWithoutUpdate["\$MPC_IAIICChurchTransmissionStationRemnants"] = true
                    }
                }

                ExplosionScript(loc).start()

                loc.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                super.unapply()
            }
            // go to far away system
            // find pather base that self destructs (may damage you)
            // pick up single survivor in the ruins
        },

        GO_TO_CHALCELEDON {
            override fun apply() {
                super.apply()

                Global.getSector().economy.getMarket("chalcedon")?.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                super.unapply()

                Global.getSector().economy.getMarket("chalcedon")?.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }

            // bar has no lead
            // admin can be bribed or intimidated
            // go to location to find broker
            // is ambush
            // admin is sketchy
            // threaten them more - they relent and gives you the commskey to jereth
            // leaves. no admin for a few days
        },

        TALK_TO_JERETH {
            override fun apply() {
                super.apply()

                val chal = Global.getSector().economy.getMarket("chalcedon") ?: return
                val jereth = MPC_People.getImportantPeople()[MPC_People.FIRST_BROKER_DEFECTOR]
                jereth?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                chal.commDirectory.addPerson(jereth)
            }

            override fun unapply() {
                super.unapply()
                val chal = Global.getSector().economy.getMarket("chalcedon") ?: return
                val jereth = MPC_People.getImportantPeople()[MPC_People.FIRST_BROKER_DEFECTOR]
                chal.commDirectory.removePerson(jereth)
            }
            // he tells you that he doesnt know the planet, just spatial coordinates
        },

        FIND_INFO_ON_BROKER {
            override fun apply() {
                super.apply()

                val secondDefector = MPC_People.getImportantPeople()[MPC_People.SECOND_BROKER_DEFECTOR]
                secondDefector?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                Global.getSector().economy.getMarket("baetis")?.commDirectory?.addPerson(secondDefector)
                val thirdDefector = MPC_People.getImportantPeople()[MPC_People.THIRD_BROKER_DEFECTOR]
                thirdDefector?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
                Global.getSector().economy.getMarket("baetis")?.commDirectory?.addPerson(thirdDefector)
            }
            // diverges here

            // comm relay bit AND finding the other two coordinates
        },

        FIND_BROKER {
            override fun apply() {
                super.apply()
                getHideoutPlanet().makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                getHideoutPlanet().makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
                super.unapply()
            }
            // this is where you finally have the planet
        },
        FINAL_PUSH {
            override fun apply() {
                super.apply()
                Global.getSector().economy.getMarket("gilead")?.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }

            override fun unapply() {
                super.unapply()
                Global.getSector().economy.getMarket("gilead")?.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
            // go to gilead and assist with the final removal of the knights
            // youev transported broker here
            // he unmasks...? (maybe not)
            // and through his actions, the message becomes clear
            // at this point the knights give up without much of a fight
            // lots of celebration
            // a new "golden" age for the church
        },
        DONE {
            override fun apply() {
                MPC_churchOverextensionCondition.MPC_churchOverextensionScript.getScript()?.delete()
                destroyKnights()
            }
        },
        FAILED;

        open fun apply() {}
        open fun unapply() {}
    }
    var state: State = State.REMOVE_TRUST
        set(value) {
            field.unapply()
            field = value
            field.apply()
        }
    var unrestActive = false
    var martialLawEnacted = false

    init {
        Global.getSector().listenerManager.addListener(this, false)
    }

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH).crest
    }

    override fun getName(): String = "Church involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + MPC_indieContributionIntel.getBaseContributionTags() + Factions.LUDDIC_CHURCH).toMutableSet()
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
                State.REMOVE_TRUST -> {
                    info.addPara(
                        "Bring the anti-knight conflict to a boil", initPad
                    )
                }
                State.FIND_ASHER_CONTACT -> {
                    info.addPara(
                        "Find the %s on %s",
                        initPad,
                        Misc.getHighlightColor(),
                        "fence", getMarket("asher")?.name
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        getMarket("asher")!!.faction.baseUIColor
                    )
                }
                State.GO_TO_ASHER_NANOFORGE -> {
                    info.addPara(
                        "Visit the %s on %s",
                        initPad,
                        Misc.getHighlightColor(),
                        "Knights", getMarket("asher")?.name
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        getMarket("asher")!!.faction.baseUIColor
                    )
                }
                State.FAILED -> {
                    info.addPara("Failed", initPad)
                }
                State.DONE -> {
                    info.addPara(
                        "Success", initPad
                    )
                }
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the Luddic Church - primarily, the Knights Of Ludd - may be involved in the IAIIC.",
            5f
        )

        info.addPara(
            "Given the deep-seated nature of the Knights of Ludd within the Luddic Church, outright targeted assaults are unlikely to work. However, " +
            "your intelligence division has offered a suitable replacement - there exists a deep-rooted religious schism between the civilian and military " +
            "governments. %s this division could, in theory, %s one while %s the other.",
            5f,
            Misc.getHighlightColor(),
            "exploiting", "severely weaken", "severely strengthening"
        ).setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getPositiveHighlightColor()
        )

        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICCHKnowsBroker")) {
            info.addPara(
                "You've been issued an ultimatum by a head of the %s government - locate a lost Luddic Prophet, known only as %s, " +
                "or the martial law will never end. Allegedly, without the guidance of a powerful historical figure, the Luddic Church " +
                "would tear itself apart from this conflict.",
                5f,
                Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                "Tartessus", "Ishmael"
            )
        }

        when (state) {
            State.REMOVE_TRUST -> {
                info.addPara(
                    "The conflict is simmering, but ready to boil. You have a number of %s available to you for that purpose.",
                    5f,
                    Misc.getHighlightColor(),
                    "options"
                )

                info.addPara(
                    "IntSec suggests you must complete %s more action(s) before the conflict escalates.",
                    5f,
                    Misc.getHighlightColor(),
                    "${STAGE_ONE_ACTIONS_NEEDED - stageOneCompletion}"
                )

                info.setBulletedListMode(BaseIntelPlugin.BULLET)
                // this one can probaly give you two(?)
                when (armsGivenState) {
                    ArmsGivenState.NOT_STARTED -> {
                        info.addPara(
                            "The knights on %s are particularly %s - they'd happily accept some %s",
                            5f,
                            Misc.getHighlightColor(),
                            "unscrupulous", "Asher", "heretical artifacts"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Global.getSector().economy.getMarket("asher").faction.baseUIColor,
                            Misc.getHighlightColor()
                        )
                    }
                    ArmsGivenState.MET -> {
                        info.addPara(
                            "You've met with one %s on %s, who is (begrudgingly) accepting %s for his industrial needs",
                            5f,
                            Misc.getHighlightColor(),
                            "Jao'Ho Imineu", "heretical artifacts"
                        ).setHighlightColors(
                            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                            Misc.getHighlightColor()
                        )

                        info.addPara(
                            "You estimate your propaganda officers will be able to create something damning after you receive %s more credits from your illicit trades",
                            5f,
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(CREDITS_NEEDED - creditsGainedFromArmsTrade)
                        )
                    }

                    ArmsGivenState.DONE -> {
                        info.addPara(
                            "Your propaganda officers have created a package expertly crafted to paint %s and all of the knights " +
                            "as a terrible Moloch-ridden beast, excluding your involvement and making it seem they did this for " +
                            "their own corrupted gain. You must now %s to a %s in a system with a %s world of %s or higher",
                            5f,
                            Misc.getHighlightColor(),
                            "Jao'Ho Imineu", "deploy the package", "comm relay", "Luddic Church", "size 5"
                        ).setHighlightColors(
                            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                            Misc.getHighlightColor()
                            )
                    }

                    ArmsGivenState.DEPLOYED -> {
                        info.addPara(
                            "You've, through illicit dealings with one %s on %s, convinced the general populace the Knights are corrupted by " +
                            "technology and Mammon",
                            5f,
                            Misc.getHighlightColor(),
                            "Jao'Ho Imineu", "Asher"
                        ).setHighlightColors(
                            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                            Global.getSector().economy.getMarket("asher").faction.baseUIColor,
                        )
                    }
                }

                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICCHDestroyedDefenseArmada")) {
                    info.addPara(
                        "You've destroyed the Knight's pride, the Armada of the Ecumene - that has the populace second guessing their competency",
                        5f
                    )
                } else {
                    info.addPara(
                        "The %s, often seen parading and patrolling Luddic space, is often seen as invincible. Destroying it would send chills down the Church's spines",
                        5f,
                        Misc.getHighlightColor(),
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                        "Armada of the Ecumene"
                    )
                }
                // bars, 2 sp option
                // daedalon - theyre actually studying it
                    // starts a explo breadcrumb? end up finding a secret knight lab in the fringes
                    // theyre doing some sketchy shit here.
                // gates - you can fight a knight fleet or something. theyre studying it
                info.addPara(
                    "Simply %s Luddic space and areas could divine new opportunities",
                    5f,
                    Misc.getHighlightColor(),
                    "exploring"
                )

                info.setBulletedListMode(null)
            }
            State.INTERMISSION -> {}
            State.EMPOWER_POPULACE -> {
                info.addPara(
                    "The conflict has boiled over. Protests have erupted in the streets of many church worlds, and " +
                    "all that's left to do is add fuel to the fire.",
                    5f,
                    Misc.getHighlightColor(),
                    "options"
                )

                info.addPara(
                    "IntSec suggests you must complete %s more action(s) before the conflict escalates.",
                    5f,
                    Misc.getHighlightColor(),
                    "${STAGE_TWO_ACTIONS_NEEDED - stageTwoCompletion}"
                )

                info.setBulletedListMode(BaseIntelPlugin.BULLET)
                info.addPara(
                    "%s may be armed through %s of %s, %s and %s",
                    5f,
                    Misc.getHighlightColor(),
                    "Luddic militants"
                )
                if (marketsArmed.isNotEmpty()) {
                    info.setBulletedListMode("  ${BaseIntelPlugin.BULLET}")
                    for (entry in marketsArmed) {
                        val market = Global.getSector().economy.getMarket(entry) ?: continue
                        info.addPara("%s", 0f, market.faction.baseUIColor, market.name)
                    }
                    info.setBulletedListMode(BaseIntelPlugin.BULLET)
                }
                info.setBulletedListMode(null)

                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICCHCeyxPrisonerFreed")) {
                    info.addPara(
                        "A dangerous %s is being held on %s in %s, having once led a %s",
                        5f,
                        Misc.getHighlightColor(),
                        "dissident prisoner", "Ceyx", "Eos Exodus", "popular uprising"
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                        Misc.getHighlightColor()
                    )
                }

                info.addPara(
                    "An important %s recently went on an expedition to %s",
                    5f,
                    Misc.getHighlightColor(),
                    "Knight leader", "${getKnightLeaderLocation().name}"
                )

                info.addPara(
                    "%s military colonies would severely weaken the Knights",
                    5f,
                    Misc.getHighlightColor(),
                    "Tactically bombarding"
                )
                info.setBulletedListMode(null)
            }

            State.INTERMISSION_TWO -> {
                info.addPara(
                    "The uprising has fully begun. Protests, riots, and general unrest now pervades the church. " +
                    "Preachers shout from the rooftop, families huddle in fear, and looters raid their neighbors in the chaos." +
                    "Only time will tell what happens next.",
                    5f
                )
            }

            State.NEGOTIATE_WITH_LEADER -> {
                val tart = Global.getSector().economy.getMarket("tartessus")
                info.addPara(
                    "The uprising has come to a screeching halt, %s being invoked from the top of the civilian government. " +
                    "Knights and militias now stalk the streets, striking down dissidence wherever it may be. Unless you find a way to " +
                    "deal with this rogue government, your uprising will entirely fail.",
                    5f,
                    Misc.getHighlightColor(),
                    "martial law"
                )

                info.addPara(
                    "Go to %s and negotiate with the civilian leadership",
                    5f,
                    tart.faction.baseUIColor,
                    "Tartessus"
                )
            }

            State.GO_TO_COORDINATES -> {
                info.addPara(
                    "%s's last known location was on a pilgrimage to %s in %s, with a warning for no-one to follow.",
                    5f,
                    Misc.getHighlightColor(),
                    "Ishmael", "${getTrapPlanet().name}", "${getTrapSystem().name}"
                )
            }

            State.TRACE_TRANSMISSION -> {
                info.addPara(
                    "The planet was a trap. An interdiction mine disabled your drive field and a hyperwave transmission was sent - surely for a nearby fleet. " +
                    "However, a more precise signal was detected going far into the outskirts of the system - trace it.",
                    5f
                )
            }

            State.TRACE_TRANSMISSION_2 -> {
                info.addPara(
                    "The comms buoy you found had relayed the transmission far away, into the %s system.",
                    5f,
                    Misc.getBasePlayerColor(),
                    "${getTransmissionSystem().name}"
                )
            }

            State.GO_TO_CHALCELEDON -> {
                info.addPara(
                    "After being trapped, you tracked down a cross-system transmission to a Luddic Path base - which detonated before " +
                    "you could investigate. Against all odds, you found a sole survivor, who gave you the name and location of someone: %s, %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "Jereth Amblast", "Chalcedon"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().economy.getMarket("chalcedon").faction.baseUIColor
                )
            }

            State.TALK_TO_JERETH -> {
                info.addPara(
                    "Talk to Jereth on Chalcedon.",
                    5f
                )
            }

            State.FIND_INFO_ON_BROKER -> {
                info.addPara(
                    "%s now rests in your hold, after the misdirection and ambush from the %s administrator - who is now in hiding. " +
                    "Under threat of death, %s has promised to cooperate, and has already gifted you a spatial coordinate - %s, presumably " +
                    "the X coordinate of Ishmael's hideout on whatever planet he is hidden within.",
                    5f,
                    Misc.getHighlightColor(),
                    "Jereth Amblast", "Chalcedon", "Jereth", "5915:895115"
                ).setHighlightColors(
                    Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                    Global.getSector().economy.getMarket("chalcedon").faction.baseUIColor,
                    Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                    Misc.getHighlightColor()
                )

                info.addPara(
                    "The first %s, %s, should be located on %s in %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "keyholder", "UNFINISHED", "Baetis", "Eos Exodus"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                    Global.getSector().economy.getMarket("baetis").faction.baseUIColor,
                    Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor
                )

                when (firstCoordGuyState) {
                    FirstCoordGuyState.NOT_STARTED -> {
                    }
                    FirstCoordGuyState.RECEIVED_OFFER -> {
                        info.addPara(
                            "You've met with %s, and received his offer: %s, and %s. He seens rather unloyal.",
                            5f,
                            Misc.getHighlightColor(),
                            "UNFINISHED", "a Hyperion-class frigate", "${Misc.getDGSCredits(FIRST_KEYHOLDER_CREDITS)}"
                        ).setHighlightColors(
                            Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor()
                        )
                    }
                    FirstCoordGuyState.RECEIVED_COORDINATES -> {
                        info.addPara(
                            "You've, through one way or another, obtained the %s: %s.",
                            5f,
                            Misc.getHighlightColor(),
                            "Y coordinate", "91154:1425"
                        )
                    }
                }

                info.addPara(
                    "The second %s, %s, should be located on %s in %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "keyholder", "UNFINISHED", "Epiphany", "Al Gebbar"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                    Global.getSector().economy.getMarket("epiphany").faction.baseUIColor,
                    Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor
                )

                when (secondCoordGuyState) {
                    SecondCoordGuyState.NOT_STARTED -> {
                    }
                    SecondCoordGuyState.TALKED -> {
                        info.addPara(
                            "You've met with %s, a loyal ex-ex-Knight. He seeks to be sure you are not trying to hurt the church, nor the path, nor Ismael.",
                            5f,
                            Misc.getHighlightColor(),
                            "UNFINISHED"
                        ).setHighlightColors(
                            Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                        )
                    }
                    SecondCoordGuyState.RECEIVED_COORDINATES -> {
                        info.addPara(
                            "You've, through one way or another, obtained the %s: %s.",
                            5f,
                            Misc.getHighlightColor(),
                            "Z coordinate", "00042:99401"
                        )
                    }
                }

                if (commRelaysSearched < COMM_RELAYS_NEEDED) {
                    info.addPara(
                        "You must find the %s of which %s is hiding on - Jereth suggested %s, and sensors agreed.",
                        5f,
                        Misc.getHighlightColor(),
                        "planet", "Ishmael", "luddic-controlled comm relays"
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor
                    )
                } else {
                    info.addPara(
                        "You've found the planet %s is hiding on - %s, in %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "Ishmael", "${getHideoutPlanet().name}, ${getHideoutSystem().name}"
                    ).setHighlightColors(
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                        Misc.getBasePlayerColor(),
                        Misc.getBasePlayerColor()
                    )
                }
            }

            State.FIND_BROKER -> {
                info.addPara(
                    "You finally have everything you need to locate Ishmael. He resides on %s in %s, with the coordinates %s, %s, and %s. You hope this isn't a trap. Again.",
                    5f,
                    Misc.getHighlightColor(),
                    "${getHideoutPlanet().name}, ${getHideoutSystem().name}", "X: 5915:895115", "Y: 91154:1425", "Z: 00042:99401"
                )
            }

            State.FINAL_PUSH -> {
                info.addPara(
                    "Ishamel is aboard your fleet. Now, you must return to Gilead, where presumably, they will make their grand return and finalize your plan.",
                    5f
                )
            }

            State.FAILED -> {
                info.addPara("You have failed to drive a wedge between the IAIIC and the church.", 5f)
            }
            State.DONE -> {
                info.addPara("You have successfully dismantled the %s, and instilled a deep distrust for them within the populist church. " +
                        "Church vessels have ceased appearing in your space, and INTSEC suggests the %s is now lacking the military of the church.",
                5f,
                Misc.getHighlightColor(),
                "Luddic Knights", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }

            State.VISIT_HIDEOUT -> {
                info.addPara(
                    "You need to visit the hideout to link up with the rest of the militants. You've been given a set of coordinates, " +
                    "and a pass-phrase: '%s'.",
                    5f,
                    Misc.getHighlightColor(),
                    "In the light of darkness"
                )
            }
            State.FIND_ASHER_CONTACT -> {
                info.addFirstStepText()

                info.addPara(
                    "You are currently trying to meet with a %s on %s to secure contact with a 'technologically malleable' knight.",
                    5f,
                    Misc.getHighlightColor(),
                    "fence", getMarket("asher")?.name
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    getMarket("asher")!!.faction.baseUIColor
                )
            }
            State.GO_TO_ASHER_NANOFORGE -> {
                info.addFirstStepText()

                info.addPara(
                    "You've made contact with the fence and obtained coordinates and a brief window to bypass knight " +
                        "patrols at %s, a military/industrial stronghold for the Knights of Ludd on Asher. You'll find your target there.",
                    5f,
                    Misc.getHighlightColor(),
                    "Artebus Station"
                )
            }
            State.DELIVER_HERETICAL_TECH -> TODO()
        }
    }

    fun TooltipMakerAPI.addFirstStepText() {
        addPara(
            "You and the Militants have hatched a plan. Capitalizing on the latent civil unrest against the Knights' 'heresy', " +
            "you will record a deal with a scrupulous knight involving highly heretical technology and transmit it to luddic population centers.",
            5f
        )
    }

    private fun getAloofMilitant(): PersonAPI {
        return MPC_People.getImportantPeople()[MPC_People.CHURCH_ALOOF_MILITANT]!!
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        when (state) {
            State.VISIT_HIDEOUT -> {
                return MPC_IAIICChurchCMD.getHideout()
            }
            State.FIND_ASHER_CONTACT -> {
                return getMarket("asher")?.primaryEntity
            }
            else -> return null
        }
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        if (state != State.REMOVE_TRUST) return
        if (fleet == null) return
        if (battle == null) return
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICCHDestroyedDefenseArmada")) return
        if (!battle.isPlayerInvolved) return
        if (fleet.name != "Armada of the Ecumene") return
        if (!battle.wasFleetDefeated(fleet, primaryWinner)) return

        stageOneCompletion++
        Global.getSector().memoryWithoutUpdate["\$MPC_IAIICCHDestroyedDefenseArmada"] = true
        sendUpdateIfPlayerHasIntel("STAGE_ONE_INCR", false)
    }

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) {
        return
    }

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) {
        return
    }

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == null) return
        if (market.factionId != Factions.LUDDIC_CHURCH) return
        if (!market.isMilitary()) return

        stageTwoCompletion++
        sendUpdateIfPlayerHasIntel("STAGE_TWO_INCR", false)
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        return
    }
}