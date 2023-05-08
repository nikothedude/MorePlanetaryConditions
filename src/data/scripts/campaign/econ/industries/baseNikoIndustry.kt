package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.sun.org.apache.xpath.internal.operations.Bool

abstract class baseNikoIndustry: BaseIndustry() {

    /** FOR SOME REASON WE CANT FUCKIN DEPEND ON THIS
     * SINCE REAPPLYINDUSTRIES DOESNT CALL REAPPLY AHAHAH kil lme */
    @Transient
    var reapplying: Boolean = false

    open fun delete() {
        market.removeIndustry(this.id, null, false)
    }

    override fun reapply() {
        reapplying = true
        super.reapply()
        reapplying = false
    }

}
