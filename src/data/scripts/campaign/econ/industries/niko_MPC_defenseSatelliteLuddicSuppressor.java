package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class niko_MPC_defenseSatelliteLuddicSuppressor extends BaseIndustry {

    /**
     * Must be negative.
     */
    private final float patherSuppressionAmount = -3f;

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
        return patherSuppressionAmount;
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
