package data.scripts.campaign.econ;

import data.utilities.niko_MPC_industryIds;

public class niko_MPC_overgrownNanoforge extends niko_MPC_industryAddingCondition {

    public niko_MPC_overgrownNanoforge() {
        this.industryId = niko_MPC_industryIds.overgrownNanoforgeIndustryId;
    }

    @Override
    public void apply(String id) {
        this.industryId = niko_MPC_industryIds.overgrownNanoforgeIndustryId; //todo revisit and see if constructor works
        super.apply(id);
    }

    @Override
    protected boolean wantToApplyIndustry() {
        return (!market.hasIndustry(industryId));
    }

    @Override
    protected void applyIndustry() {
        market.addIndustry(industryId);
    }
}
