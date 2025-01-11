package data.scripts.campaign.magnetar.AIPlugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreAdminPlugin
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings
import exerelin.campaign.skills.NexSkills
import exerelin.utilities.StringHelper

class MPC_slavedOmegaCoreAdminPlugin: AICoreAdminPlugin {
    override fun createPerson(aiCoreId: String?, factionId: String?, seed: Long): PersonAPI {
        val person = Global.getFactory().createPerson()
        person.setFaction(factionId)
        person.aiCoreId = aiCoreId
        val commodityName = StringHelper.getCommodityName(aiCoreId)
        person.name = FullName(commodityName, "", FullName.Gender.ANY)
        Global.getSettings().loadTexture("graphics/portraits/MPC_fractalCore.png")
        person.portraitSprite = "graphics/portraits/MPC_fractalCore.png"

        person.rankId = null
        person.postId = Ranks.POST_ADMINISTRATOR

//		person.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 1);
//		person.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, 1);

//		person.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 1);
//		person.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, 1);
        /*if (niko_MPC_settings.nexLoaded) {
//		person.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 1);
//		person.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, 1);
            person.stats.setSkillLevel(NexSkills.AUXILIARY_SUPPORT_EX, 1f)
            person.stats.setSkillLevel(NexSkills.BULK_TRANSPORT_EX, 1f)
            person.stats.setSkillLevel(NexSkills.TACTICAL_DRILLS_EX, 1f)
            person.stats.setSkillLevel(NexSkills.MAKESHIFT_EQUIPMENT_EX, 1f)
        }*/
        person.stats.setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1f)
        person.stats.setSkillLevel(Skills.HYPERCOGNITION, 1f)
        person.stats.setSkillLevel(niko_MPC_ids.TRANSCENDANT_CONCIOUSNESS_SKILL_ID, 1f);
        if (Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.FRACTAL_CORE_UPGRADED)) {
            person.stats.setSkillLevel(niko_MPC_ids.ROUTING_OPTIMIZATION_SKILL_ID, 1f)
        }
        if (niko_MPC_settings.astralAscensionEnabled) {
            person.stats.setSkillLevel("acumenious_oracle", 1f)
            person.stats.setSkillLevel("delphic_optimiser", 1f)
            person.stats.setSkillLevel("ethereal_enlightenment", 1f)
        }

        return person
    }
}