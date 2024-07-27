package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Voices
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import data.utilities.niko_MPC_ids

object MPC_People {
    const val KANTA_GOON_ONE = "MPC_kanta_goon_one"
    const val KANTA_GOON_TWO = "MPC_kanta_goon_two"

    fun createCharacters() {
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.GENERATED_PEOPLE] == true) return

        val importantPeople = Global.getSector().importantPeople

        // Pirate bar encounter after delivering loke, points you to the magnetar quest
        val goonOne = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(StarSystemGenerator.random)
        goonOne.id = KANTA_GOON_ONE
        goonOne.rankId = Ranks.SPECIAL_AGENT
        goonOne.postId = Ranks.SPECIAL_AGENT
        goonOne.importance = PersonImportance.HIGH
        goonOne.portraitSprite = "graphics/portraits/portrait_mercenary01.png"
        goonOne.name = FullName("Jamwell", "Pourus", FullName.Gender.MALE)
        goonOne.gender = FullName.Gender.MALE
        goonOne.voice = Voices.VILLAIN
        importantPeople.addPerson(goonOne)

        val goonTwo = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(StarSystemGenerator.random)
        goonTwo.id = KANTA_GOON_TWO
        goonTwo.rankId = Ranks.SPECIAL_AGENT
        goonTwo.postId = Ranks.SPECIAL_AGENT
        goonTwo.importance = PersonImportance.HIGH
        goonTwo.portraitSprite = "graphics/portraits/portrait_pirate14.png"
        goonOne.name = FullName("Bob", "Joe", FullName.Gender.MALE)
        goonOne.gender = FullName.Gender.MALE
        goonTwo.voice = Voices.VILLAIN
        importantPeople.addPerson(goonTwo)

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.GENERATED_PEOPLE] = true
    }
}