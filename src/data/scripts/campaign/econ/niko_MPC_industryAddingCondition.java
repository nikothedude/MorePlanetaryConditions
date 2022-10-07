package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;

public abstract class niko_MPC_industryAddingCondition extends BaseHazardCondition {
    public String industryId;

    @Override
    public void apply(String id) {
        super.apply(id);

        if (wantToApplyIndustry()) {
            applyIndustry();
        }
    }

    protected abstract boolean wantToApplyIndustry();

    protected abstract void applyIndustry();
}
