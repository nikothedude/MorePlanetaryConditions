package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

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

    //@Override
    //public void onGameLoad(boolean newGame) {
       // addSatelliteCleanupListenerIfNonePresent();
}

