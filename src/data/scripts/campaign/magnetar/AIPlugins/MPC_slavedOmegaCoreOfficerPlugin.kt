package data.scripts.campaign.magnetar.AIPlugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.utilities.niko_MPC_ids
import java.util.*

class MPC_slavedOmegaCoreOfficerPlugin: BaseAICoreOfficerPluginImpl() {

    var OMEGA_MULT = 5f

    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI) {
        val opad = 10f
        val text = person.faction.baseUIColor
        val bg = person.faction.darkUIColor
        val spec = Global.getSettings().getCommoditySpec(person.aiCoreId)
        tooltip.addSectionHeading("Personality: tactical", text, bg, Alignment.MID, 20f)
        tooltip.addPara(
            "In combat, the ${spec.name} meticulously calculates every possible action, then takes the most advantageous. " +
                "In a human captain, its traits might be considered steady. In a machine, they're awe-inspiring.",
            opad
        )
    }

    override fun createPerson(aiCoreId: String?, factionId: String?, random: Random?): PersonAPI {

        val person = Global.getFactory().createPerson()
        person.setFaction(factionId)
        person.aiCoreId = aiCoreId

        person.stats.isSkipRefresh = true

        val spec = Global.getSettings().getCommoditySpec(aiCoreId)
        person.name = FullName(spec.name, "", FullName.Gender.ANY)

        // assume it's not going to be integrated, no reason to do it - same as assuming it's always integrated
        person.portraitSprite = "graphics/portraits/characters/omega.png"
        person.stats.level = 10
        person.stats.setSkillLevel(Skills.HELMSMANSHIP, 2f)
        person.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f)
       // person.stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
        person.stats.setSkillLevel(Skills.BALLISTIC_MASTERY, 2f)
        //person.getStats().setSkillLevel(Skills.SHIELD_MODULATION, 2);
        //person.getStats().setSkillLevel(Skills.SHIELD_MODULATION, 2);
        person.stats.setSkillLevel(Skills.FIELD_MODULATION, 2f)
        //person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        //person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        person.stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f)
        //person.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 2);
        //person.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 2);
        person.stats.setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2f)
        person.stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)
        person.stats.setSkillLevel(Skills.POINT_DEFENSE, 2f)
        person.stats.setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2f)
        person.stats.setSkillLevel(niko_MPC_ids.FRACTAL_OPTIMIZATIONS_SKILL_ID, 2f)

        /*if (points != 0) {
            person.memoryWithoutUpdate[AICoreOfficerPlugin.AUTOMATED_POINTS_VALUE] = points
        }*/
        person.memoryWithoutUpdate[AICoreOfficerPlugin.AUTOMATED_POINTS_MULT] = OMEGA_MULT

        person.setPersonality(Personalities.STEADY)
        person.rankId = Ranks.SPACE_CAPTAIN
        person.postId = null

        person.stats.isSkipRefresh = false

        return person
    }
}