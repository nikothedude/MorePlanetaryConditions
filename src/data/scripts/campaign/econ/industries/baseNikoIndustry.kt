package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.sun.org.apache.xpath.internal.operations.Bool

abstract class baseNikoIndustry: BaseIndustry() {

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
