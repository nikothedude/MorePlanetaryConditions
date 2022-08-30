package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class niko_MPC_defenseSatelliteLuddicSuppressor extends BaseIndustry {

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public float getPatherInterest() {
        return -3f;
    }

    @Override
    public boolean isHidden() { //hidden structures dont block structure construction
        return true;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }
}
