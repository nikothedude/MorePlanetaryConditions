package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.sun.org.apache.xpath.internal.operations.Bool

abstract class baseNikoIndustry: BaseIndustry() {
    var reapplying = false

    open fun delete() {
        market.removeIndustry(this.id, null, false)
    }

    override fun reapply() {
        reapplying = true
        super.reapply()
        reapplying = false
    }

}
