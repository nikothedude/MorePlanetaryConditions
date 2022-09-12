package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.bounty.MagicBountyCampaignPlugin;
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener;
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin;

public class niko_MPC_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws RuntimeException {
        boolean isMagicLibEnabled = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!isMagicLibEnabled) {
            throw(new RuntimeException("MagicLib is required for more planetary conditions!"));
        }
        boolean isLazyLibEnabled = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!isLazyLibEnabled) {
            throw(new RuntimeException("LazyLib is required for more planetary conditions!"));
        }
    }


    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        Global.getSector().addTransientListener(new niko_MPC_satelliteEventListener(false));

        Global.getSector().registerPlugin(new niko_MPC_campaignPlugin());
    }
}

