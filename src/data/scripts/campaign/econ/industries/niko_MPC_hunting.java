package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import data.utilities.niko_MPC_settings;

public class niko_MPC_hunting extends niko_MPC_xenoIndustry {
    @Override
    public void apply() {

    }

    @Override
    public boolean isAvailableToBuild() {
        if (!niko_MPC_settings.XENO_HUNTING_IND_ENABLED) {
            return false;
        }
        return super.isAvailableToBuild();
    }

    @Override
    public boolean showWhenUnavailable() {
        if (!niko_MPC_settings.XENO_HUNTING_IND_ENABLED) {
            return false;
        }
        return super.showWhenUnavailable();
    }
}
