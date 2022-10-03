package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import data.utilities.niko_MPC_settings;

public abstract class niko_MPC_xenoIndustry extends BaseIndustry {

    @Override
    public boolean isAvailableToBuild() {
        if (!niko_MPC_settings.XENOLIFE_ENABLED || !niko_MPC_conditionUtils.marketHasXenoLife()) {
            return false;
        }

        return super.isAvailableToBuild();
    }

    @Override
    public boolean showWhenUnavailable() {
        if (!niko_MPC_settings.XENOLIFE_ENABLED) {
            return false;
        }

        return super.showWhenUnavailable();
    }
}
