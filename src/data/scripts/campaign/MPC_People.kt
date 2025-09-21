package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.FullName.Gender
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.ids.Voices
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.adjustReputationWithPlayer
import org.magiclib.kotlin.makeImportant

object MPC_People {
    const val KANTA_GOON_ONE = "MPC_kanta_goon_one"
    const val KANTA_GOON_TWO = "MPC_kanta_goon_two"

    const val VIOLENCE_GUARD = "MPC_violenceGuard"
    const val INTELLIGENCE_GUARD = "MPC_intelligenceGuard"
    const val MORALITY_GUARD = "MPC_moralityGuard"

    const val PLAYER_FACTION_INTSEC_SQUAD_CHIEF = "MPC_playerFacIntsecSquadChief"
    const val HEGEMONY_SPY = "MPC_hegemonySpy"

    const val IAIIC_LEADER = "MPC_IAIIC_Leader"
    const val IAIIC_MERC_COMMANDER = "MPC_IAIIC_Merc_Commander"

    const val ARROW_PATHER = "MPC_arrowPather"
    const val ARROW_PATHER_REP = "MPC_arrowPatherRep"

    const val DONN_PIRATE = "MPC_donnPirate"

    const val ARRESTING_KNIGHT_OFFICER = "MPC_arrestingKnightOfficer"

    const val TRITACH_BRUISER_ONE = "MPC_triTachBruiserOne"
    const val TRITACH_BRUISER_TWO = "MPC_triTachBruiserTwo"

    const val UMBRA_INFILTRATOR = "MPC_umbraInfiltrator"

    const val HAMMER_REP = "MPC_hammerRep"
    const val TACTISTAR_REP = "MPC_tactistarRep"
    const val BLACKKNIFE_REP = "MPC_blackknifeRep"
    const val BLACKKNIFE_BAR_GUY = "MPC_blackknifeBarGuy"
    const val BLACKKNIFE_TARGET = "MPC_blackknifeTarget"
    const val MMMC_REP = "MPC_MMMCRep"
    const val VOIDSUN_REP = "MPC_voidsunRep"

    const val HEGE_ARISTO_DEFECTOR = "MPC_hegeAristoDefector"

    const val HEGE_MILITARIST_ARISTO_REP = "MPC_hegeAristoMilitaryRep"
    const val HEGE_OPPORTUNISTIC_ARISTO_REP = "MPC_hegeAristoOpportunistRep"
    const val HEGE_MORALIST_ARISTO_REP = "MPC_hegeAristoMoralistRep"
    const val HEGE_MORALIST_ARISTO_REP_TWO = "MPC_hegeAristoMoralistRepTwo"
    const val HEGE_MORALIST_RECEPTIONIST = "MPC_hegeMoralistReceptionist"
    const val HEGE_MORALIST_COMSEC_DUELIST = "MPC_hegeAristoMoralistComsecDuelist"

    const val HEGE_ALOOF_SISTER = "MPC_hegeAristoSister"
    const val HEGE_ALOOF_BROTHER = "MPC_hegeAloofBrother"

    const val HEGE_HON_TRAITOR = "MPC_hegeHonTraitor"

    const val PATHER_BROKER = "MPC_patherBroker"
    const val CHURCH_ALOOF_MILITANT = "MPC_aloofMilitant"

    const val HEGE_INTSEC_GOON = "MPC_hegeIntsecGoon"

    fun getImportantPeople(): HashMap<String, PersonAPI> {
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IMPORTANT_PEOPLE] == null) {
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IMPORTANT_PEOPLE] = HashMap<String, PersonAPI>()
        }
        return Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IMPORTANT_PEOPLE] as HashMap<String, PersonAPI>
    }

    fun createCharacters() {
        val importantPeople = Global.getSector().importantPeople

        // Pirate bar encounter after delivering loke, points you to the magnetar quest
        val MPC_importantPeople = getImportantPeople()
        if (MPC_importantPeople[KANTA_GOON_ONE] == null) {
            val goonOne = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(StarSystemGenerator.random)
            goonOne.id = KANTA_GOON_ONE
            goonOne.rankId = Ranks.SPECIAL_AGENT
            goonOne.postId = Ranks.SPECIAL_AGENT
            goonOne.importance = PersonImportance.HIGH
            goonOne.portraitSprite = "graphics/portraits/portrait_mercenary01.png"
            goonOne.name = FullName("Jamwell", "Pourus", Gender.MALE)
            goonOne.gender = Gender.MALE
            goonOne.voice = Voices.VILLAIN
            importantPeople.addPerson(goonOne)
            MPC_importantPeople[KANTA_GOON_ONE] = goonOne
        }

        if (MPC_importantPeople[KANTA_GOON_TWO] == null) {
            val goonTwo = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(StarSystemGenerator.random)
            goonTwo.id = KANTA_GOON_TWO
            goonTwo.rankId = Ranks.SPECIAL_AGENT
            goonTwo.postId = Ranks.SPECIAL_AGENT
            goonTwo.importance = PersonImportance.HIGH
            goonTwo.portraitSprite = "graphics/portraits/portrait_pirate14.png"
            goonTwo.name = FullName("Bob", "Joe", Gender.MALE)
            goonTwo.gender = Gender.MALE
            goonTwo.voice = Voices.VILLAIN
            importantPeople.addPerson(goonTwo)
            MPC_importantPeople[KANTA_GOON_TWO] = goonTwo
        }

        if (MPC_importantPeople[VIOLENCE_GUARD] == null) {
            val violenceGuard = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(StarSystemGenerator.random)
            violenceGuard.id = VIOLENCE_GUARD
            violenceGuard.rankId = Ranks.AGENT
            violenceGuard.postId = Ranks.AGENT
            violenceGuard.importance = PersonImportance.HIGH
            violenceGuard.portraitSprite = "graphics/portraits/MPC_violenceGuard.png"
            violenceGuard.name = FullName("Pontus", "Excalatus", Gender.MALE)
            violenceGuard.gender = Gender.MALE
            violenceGuard.voice = Voices.VILLAIN
            importantPeople.addPerson(violenceGuard)
            MPC_importantPeople[VIOLENCE_GUARD] = violenceGuard
        }

        if (MPC_importantPeople[INTELLIGENCE_GUARD] == null) {
            val intelligenceGuard = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(StarSystemGenerator.random)
            intelligenceGuard.id = INTELLIGENCE_GUARD
            intelligenceGuard.rankId = Ranks.AGENT
            intelligenceGuard.postId = Ranks.AGENT
            intelligenceGuard.importance = PersonImportance.HIGH
            intelligenceGuard.portraitSprite = "graphics/portraits/MPC_intelligenceGuard.png"
            intelligenceGuard.name = FullName("Erasmus", "XII", Gender.MALE)
            intelligenceGuard.gender = Gender.MALE
            intelligenceGuard.voice = Voices.ARISTO
            importantPeople.addPerson(intelligenceGuard)
            MPC_importantPeople[INTELLIGENCE_GUARD] = intelligenceGuard
        }

        if (MPC_importantPeople[MORALITY_GUARD] == null) {
            val moralityGuard = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(StarSystemGenerator.random)
            moralityGuard.id = MORALITY_GUARD
            moralityGuard.rankId = Ranks.AGENT
            moralityGuard.postId = Ranks.AGENT
            moralityGuard.importance = PersonImportance.HIGH
            moralityGuard.portraitSprite = "graphics/portraits/MPC_moralityGuard.png"
            moralityGuard.name = FullName("Simon", "Geria", Gender.MALE)
            moralityGuard.gender = Gender.MALE
            moralityGuard.voice = Voices.FAITHFUL
            importantPeople.addPerson(moralityGuard)
            MPC_importantPeople[MORALITY_GUARD] = moralityGuard
        }

        if (MPC_importantPeople[PLAYER_FACTION_INTSEC_SQUAD_CHIEF] == null) {
            val intsecChief = Global.getSector().getFaction(Factions.PLAYER).createRandomPerson(StarSystemGenerator.random)
            intsecChief.id = PLAYER_FACTION_INTSEC_SQUAD_CHIEF
            intsecChief.rankId = Ranks.SPECIAL_AGENT
            intsecChief.postId = Ranks.SPECIAL_AGENT
            intsecChief.importance = PersonImportance.VERY_HIGH
            //moralityGuard.portraitSprite = "graphics/portraits/MPC_moralityGuard.png"
            //moralityGuard.name = FullName("Simon", "Geria", FullName.Gender.MALE)
            //moralityGuard.gender = FullName.Gender.MALE
            //moralityGuard.voice = Voices.FAITHFUL
            importantPeople.addPerson(intsecChief)
            MPC_importantPeople[PLAYER_FACTION_INTSEC_SQUAD_CHIEF] = intsecChief
        }

        if (MPC_importantPeople[HEGEMONY_SPY] == null) {
            val hegemonySpy = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson()
            hegemonySpy.id = HEGEMONY_SPY
            hegemonySpy.rankId = Ranks.CITIZEN
            hegemonySpy.postId = Ranks.CITIZEN
            hegemonySpy.importance = PersonImportance.MEDIUM
            hegemonySpy.portraitSprite = "graphics/portraits/portrait23.png"
            hegemonySpy.name = FullName("Bodewell", "Calus", Gender.MALE)
            hegemonySpy.gender = Gender.MALE
            //moralityGuard.voice = Voices.FAITHFUL
            importantPeople.addPerson(hegemonySpy)
            MPC_importantPeople[HEGEMONY_SPY] = hegemonySpy
        }

        if (MPC_importantPeople[IAIIC_LEADER] == null) {
            val IAIICLeader = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).createRandomPerson()
            IAIICLeader.id = IAIIC_LEADER
            IAIICLeader.rankId = Ranks.FACTION_LEADER
            IAIICLeader.postId = Ranks.POST_FACTION_LEADER
            IAIICLeader.importance = PersonImportance.VERY_HIGH
            IAIICLeader.portraitSprite = "graphics/portraits/characters/imoinu_kato.png" // TODO: this will likely be used in a later SS update
            IAIICLeader.name = FullName("Jill", "Mirthson", Gender.MALE)
            IAIICLeader.gender = Gender.FEMALE
            IAIICLeader.voice = Voices.SOLDIER

            IAIICLeader.stats.level = 10
            IAIICLeader.stats.setSkillLevel("MPC_fleet_logistics", 1f)
            IAIICLeader.stats.setSkillLevel("MPC_space_operations", 1f)
            IAIICLeader.stats.setSkillLevel("MPC_planetary_operations", 1f)

            importantPeople.addPerson(IAIICLeader)
            MPC_importantPeople[IAIIC_LEADER] = IAIICLeader
        }

        if (MPC_importantPeople[IAIIC_MERC_COMMANDER] == null) {
            val IAIICMercCommander = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).createRandomPerson()
            IAIICMercCommander.id = IAIIC_MERC_COMMANDER
            IAIICMercCommander.rankId = Ranks.SPACE_COMMANDER
            IAIICMercCommander.postId = Ranks.POST_SPACER
            IAIICMercCommander.importance = PersonImportance.HIGH
            IAIICMercCommander.portraitSprite = "graphics/portraits/characters/fenius.png" // TODO: this will likely be used in a later SS update
            IAIICMercCommander.name = FullName("Jishino", "Mentsu", Gender.MALE)
            IAIICMercCommander.gender = Gender.MALE
            IAIICMercCommander.voice = Voices.SPACER

            importantPeople.addPerson(IAIICMercCommander)
            MPC_importantPeople[IAIIC_MERC_COMMANDER] = IAIICMercCommander
        }

        if (MPC_importantPeople[ARROW_PATHER] == null) {
            val arrowPather = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson(Gender.MALE)

            arrowPather.id = ARROW_PATHER
            arrowPather.rankId = Ranks.CITIZEN
            arrowPather.postId = Ranks.CITIZEN
            arrowPather.importance = PersonImportance.MEDIUM
            arrowPather.voice = Voices.PATHER

            importantPeople.addPerson(arrowPather)
            MPC_importantPeople[ARROW_PATHER] = arrowPather
        }

        if (MPC_importantPeople[ARROW_PATHER_REP] == null) {
            val arrowPatherRep = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson()

            arrowPatherRep.id = ARROW_PATHER_REP
            arrowPatherRep.rankId = Ranks.GROUND_MAJOR
            arrowPatherRep.postId = Ranks.POST_BASE_COMMANDER
            arrowPatherRep.importance = PersonImportance.VERY_HIGH
            arrowPatherRep.voice = Voices.PATHER

            importantPeople.addPerson(arrowPatherRep)
            MPC_importantPeople[ARROW_PATHER_REP] = arrowPatherRep
        }

        if (MPC_importantPeople[DONN_PIRATE] == null) {
            val pirate = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson()

            pirate.id = DONN_PIRATE
            pirate.rankId = Ranks.SPACE_COMMANDER
            pirate.postId = Ranks.POST_SPACER
            pirate.importance = PersonImportance.HIGH
            pirate.voice = Voices.VILLAIN

            importantPeople.addPerson(pirate)
            MPC_importantPeople[DONN_PIRATE] = pirate
        }

        if (MPC_importantPeople[ARRESTING_KNIGHT_OFFICER] == null) {
            val knight = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.MALE)

            knight.id = ARRESTING_KNIGHT_OFFICER
            knight.rankId = Ranks.KNIGHT_CAPTAIN
            knight.postId = Ranks.POST_BASE_COMMANDER
            knight.importance = PersonImportance.HIGH
            knight.voice = Voices.SOLDIER
            knight.name = FullName("Jefferson", "Kelk", Gender.MALE)
            knight.gender = Gender.MALE

            importantPeople.addPerson(knight)
            MPC_importantPeople[ARRESTING_KNIGHT_OFFICER] = knight
        }

        if (MPC_importantPeople[TRITACH_BRUISER_ONE] == null) {
            val bruiserOne = Global.getSector().getFaction(Factions.TRITACHYON).createRandomPerson(Gender.MALE)

            bruiserOne.id = TRITACH_BRUISER_ONE
            bruiserOne.rankId = Ranks.AGENT
            bruiserOne.postId = Ranks.POST_AGENT

            bruiserOne.importance = PersonImportance.MEDIUM
            bruiserOne.voice = Voices.SOLDIER

            importantPeople.addPerson(bruiserOne)
            MPC_importantPeople[TRITACH_BRUISER_ONE] = bruiserOne
        }

        if (MPC_importantPeople[TRITACH_BRUISER_TWO] == null) {
            val bruiserTwo = Global.getSector().getFaction(Factions.TRITACHYON).createRandomPerson(Gender.MALE)

            bruiserTwo.id = TRITACH_BRUISER_TWO
            bruiserTwo.rankId = Ranks.AGENT
            bruiserTwo.postId = Ranks.POST_AGENT

            bruiserTwo.importance = PersonImportance.MEDIUM
            bruiserTwo.voice = Voices.SOLDIER

            importantPeople.addPerson(bruiserTwo)
            MPC_importantPeople[TRITACH_BRUISER_TWO] = bruiserTwo
        }

        if (MPC_importantPeople[UMBRA_INFILTRATOR] == null) {
            val infiltrator = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.FEMALE)

            infiltrator.id = UMBRA_INFILTRATOR
            infiltrator.rankId = Ranks.SPECIAL_AGENT
            infiltrator.postId = Ranks.POST_SPECIAL_AGENT
            infiltrator.setFaction(Factions.DIKTAT)

            infiltrator.importance = PersonImportance.MEDIUM
            infiltrator.voice = Voices.SOLDIER

            infiltrator.name = FullName("Hera", "Calibri", Gender.FEMALE)

            importantPeople.addPerson(infiltrator)
            MPC_importantPeople[UMBRA_INFILTRATOR] = infiltrator
        }

        if (MPC_importantPeople[HAMMER_REP] == null) {
            val rep = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.MALE)
            rep.id = HAMMER_REP

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_MERCENARY
            rep.setFaction(Factions.LUDDIC_CHURCH)

            rep.importance = PersonImportance.MEDIUM
            rep.voice = Voices.FAITHFUL

            rep.name = FullName("Jeron", "Blast", Gender.MALE)

            rep.relToPlayer.rel = -0.5f

            importantPeople.addPerson(rep)
            MPC_importantPeople[HAMMER_REP] = rep
        }
        if (MPC_importantPeople[TACTISTAR_REP] == null) {
            val rep = Global.getSector().getFaction(Factions.MERCENARY).createRandomPerson(Gender.MALE)
            rep.id = TACTISTAR_REP

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_MERCENARY

            rep.importance = PersonImportance.MEDIUM
            rep.voice = Voices.SOLDIER

            rep.name = FullName("Jensen", "DC", Gender.MALE)

            rep.relToPlayer.rel = -0.1f

            importantPeople.addPerson(rep)
            MPC_importantPeople[TACTISTAR_REP] = rep

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }
        if (MPC_importantPeople[BLACKKNIFE_REP] == null) {
            val rep = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(Gender.FEMALE)
            rep.id = BLACKKNIFE_REP

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_MERCENARY

            rep.importance = PersonImportance.MEDIUM
            rep.voice = Voices.VILLAIN

            rep.name = FullName("Jango", "Retrina", Gender.MALE)

            importantPeople.addPerson(rep)
            MPC_importantPeople[BLACKKNIFE_REP] = rep

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }
        if (MPC_importantPeople[BLACKKNIFE_BAR_GUY] == null) {
            val rep = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(Gender.MALE)
            rep.id = BLACKKNIFE_BAR_GUY

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_MERCENARY

            rep.importance = PersonImportance.LOW
            rep.voice = Voices.SOLDIER

            importantPeople.addPerson(rep)
            MPC_importantPeople[BLACKKNIFE_BAR_GUY] = rep

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }
        if (MPC_importantPeople[BLACKKNIFE_TARGET] == null) {
            val rep = Global.getSector().getFaction(Factions.PERSEAN).createRandomPerson(Gender.MALE)
            rep.id = BLACKKNIFE_TARGET

            rep.rankId = Ranks.POST_AGENT
            rep.postId = Ranks.POST_PATROL_COMMANDER

            rep.importance = PersonImportance.LOW
            rep.voice = Voices.SOLDIER

            rep.name = FullName("Jenius", "Grapetrain", Gender.MALE)

            importantPeople.addPerson(rep)
            MPC_importantPeople[BLACKKNIFE_TARGET] = rep

            rep.stats.level = 7
            rep.stats.setSkillLevel(Skills.CREW_TRAINING, 1f)
            rep.stats.setSkillLevel(Skills.CARRIER_GROUP, 1f)
            rep.stats.setSkillLevel(Skills.FIGHTER_UPLINK, 1f)
            rep.stats.setSkillLevel(Skills.HELMSMANSHIP, 1f)
            rep.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 1f)
            rep.stats.setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1f)
            rep.stats.setSkillLevel(Skills.ORDNANCE_EXPERTISE, 1f)

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        if (MPC_importantPeople[MMMC_REP] == null) {
            val rep = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.MALE)
            rep.id = MMMC_REP

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_CITIZEN

            rep.importance = PersonImportance.HIGH
            rep.voice = Voices.SPACER

            rep.name = FullName("Selius", "Mikhael", Gender.MALE)

            importantPeople.addPerson(rep)
            MPC_importantPeople[MMMC_REP] = rep

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        if (MPC_importantPeople[VOIDSUN_REP] == null) {
            val rep = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.MALE)
            rep.id = VOIDSUN_REP

            rep.rankId = Ranks.CITIZEN
            rep.postId = Ranks.POST_MERCENARY

            rep.importance = PersonImportance.HIGH
            rep.voice = Voices.SPACER

            rep.name = FullName("Mike", "Jackdaw", Gender.MALE)

            importantPeople.addPerson(rep)
            MPC_importantPeople[VOIDSUN_REP] = rep

            rep.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        if (MPC_importantPeople[HEGE_ARISTO_DEFECTOR] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.FEMALE)

            aristo.id = HEGE_ARISTO_DEFECTOR

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = "MPC_aloofAristo"

            aristo.importance = PersonImportance.MEDIUM
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Aleratus", "Youn", Gender.FEMALE)

            aristo.portraitSprite = "graphics/portraits/portrait_hegemony10.png"

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_ARISTO_DEFECTOR] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_ALOOF_SISTER] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.FEMALE)

            aristo.id = HEGE_ALOOF_SISTER

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = Ranks.POST_CITIZEN

            aristo.importance = PersonImportance.MEDIUM
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Alnessa", "Youn", Gender.FEMALE)
            aristo.adjustReputationWithPlayer(-0.4f, null, null)

            //aristo.portraitSprite = "graphics/portraits/portrait_hegemony10.png"

            importantPeople.addPerson(aristo)
            MPC_importantPeople[aristo.id] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_ALOOF_BROTHER] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            aristo.id = HEGE_ALOOF_BROTHER

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = Ranks.POST_CITIZEN

            aristo.importance = PersonImportance.LOW
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Orthus", "Youn", Gender.FEMALE)
            aristo.adjustReputationWithPlayer(-0.9f, null, null)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[aristo.id] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_MILITARIST_ARISTO_REP] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.FEMALE)

            aristo.id = HEGE_MILITARIST_ARISTO_REP

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = "MPC_militaristicAristo"

            aristo.importance = PersonImportance.VERY_HIGH
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Allison", "Mellour", Gender.FEMALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_MILITARIST_ARISTO_REP] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_OPPORTUNISTIC_ARISTO_REP] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            aristo.id = HEGE_OPPORTUNISTIC_ARISTO_REP

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = "MPC_pacifistAristo"

            aristo.importance = PersonImportance.VERY_HIGH
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Mikael", "Lindunberg", Gender.MALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_OPPORTUNISTIC_ARISTO_REP] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_MORALIST_ARISTO_REP] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            aristo.id = HEGE_MORALIST_ARISTO_REP

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = "MPC_honorableAristo"

            aristo.importance = PersonImportance.HIGH
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Jerus", "Alotera", Gender.MALE)

            val stats = aristo.stats
            stats.setSkillLevel(Skills.HELMSMANSHIP, 1f)
            stats.setSkillLevel(Skills.TARGET_ANALYSIS, 1f)
            stats.setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1f)
            stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)
            stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
            stats.setSkillLevel(Skills.FIELD_MODULATION, 2f)

            stats.setSkillLevel(Skills.CREW_TRAINING, 1f)
            stats.setSkillLevel(Skills.PHASE_CORPS, 1f)
            stats.setSkillLevel(Skills.BEST_OF_THE_BEST, 1f)

            stats.level = 10

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_MORALIST_ARISTO_REP] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_MORALIST_ARISTO_REP_TWO] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            aristo.id = HEGE_MORALIST_ARISTO_REP_TWO

            aristo.rankId = Ranks.ARISTOCRAT
            aristo.postId = "MPC_honorableAristo"

            aristo.importance = PersonImportance.HIGH
            aristo.voice = Voices.ARISTO

            aristo.name = FullName("Halatius", "Alotera", Gender.MALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_MORALIST_ARISTO_REP_TWO] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_MORALIST_COMSEC_DUELIST] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            aristo.id = HEGE_MORALIST_COMSEC_DUELIST

            aristo.rankId = Ranks.SPECIAL_AGENT
            aristo.postId = Ranks.POST_SPECIAL_AGENT

            aristo.importance = PersonImportance.HIGH
            aristo.voice = Voices.SOLDIER

            aristo.name = FullName("Jamaniah", "Glowfest", Gender.MALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_MORALIST_COMSEC_DUELIST] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_MORALIST_RECEPTIONIST] == null) {
            val aristo = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.FEMALE)

            aristo.id = HEGE_MORALIST_RECEPTIONIST

            aristo.rankId = Ranks.CITIZEN
            aristo.postId = Ranks.POST_AGENT

            aristo.importance = PersonImportance.LOW
            aristo.voice = Voices.OFFICIAL

            //aristo.name = FullName("Jerus", "Alotera", Gender.MALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_MORALIST_RECEPTIONIST] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_HON_TRAITOR] == null) {
            val aristo = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.FEMALE)
            aristo.name = FullName("Jane", "Blackwell", Gender.FEMALE)

            aristo.id = HEGE_HON_TRAITOR

            aristo.rankId = Ranks.KNIGHT_CAPTAIN
            aristo.postId = Ranks.POST_PATROL_COMMANDER

            aristo.importance = PersonImportance.HIGH
            aristo.voice = Voices.OFFICIAL
            aristo.portraitSprite = "graphics/portraits/portrait_luddic07.png"

            //aristo.name = FullName("Jerus", "Alotera", Gender.MALE)

            importantPeople.addPerson(aristo)
            MPC_importantPeople[HEGE_HON_TRAITOR] = aristo

            aristo.makeImportant("MPC_hegeAristo")
        }
        if (MPC_importantPeople[HEGE_INTSEC_GOON] == null) {
            val goon = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.MALE)

            goon.id = HEGE_INTSEC_GOON

            goon.rankId = Ranks.SPECIAL_AGENT
            goon.postId = Ranks.POST_SPECIAL_AGENT

            goon.importance = PersonImportance.MEDIUM
            goon.voice = Voices.SOLDIER

            goon.name = FullName("Simon", "Fellspar", Gender.FEMALE)

            importantPeople.addPerson(goon)
            MPC_importantPeople[HEGE_INTSEC_GOON] = goon

            goon.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        // LUDDIC CHURCH SHIT
        if (MPC_importantPeople[CHURCH_ALOOF_MILITANT] == null) {
            val militant = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.FEMALE)
            militant.id = CHURCH_ALOOF_MILITANT

            militant.rankId = Ranks.CITIZEN
            militant.postId = Ranks.POST_TERRORIST

            militant.importance = PersonImportance.VERY_HIGH
            militant.voice = Voices.FAITHFUL

            //broker.portraitSprite = "graphics/portraits/MPC_luddicBroker.png"

            militant.name = FullName("Alice", "Semblemind", Gender.FEMALE)

            importantPeople.addPerson(militant)
            MPC_importantPeople[CHURCH_ALOOF_MILITANT] = militant

            militant.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        if (MPC_importantPeople[PATHER_BROKER] == null) {
            val broker = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.ANY)
            broker.id = PATHER_BROKER

            broker.rankId = Ranks.CITIZEN
            broker.postId = Ranks.POST_HERMIT

            broker.importance = PersonImportance.VERY_HIGH
            broker.voice = Voices.PATHER

            broker.portraitSprite = "graphics/portraits/MPC_luddicBroker.png"

            broker.name = FullName("Broker", "", Gender.ANY)

            importantPeople.addPerson(broker)
            MPC_importantPeople[PATHER_BROKER] = broker

            broker.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        }

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.GENERATED_PEOPLE] = true
    }
}