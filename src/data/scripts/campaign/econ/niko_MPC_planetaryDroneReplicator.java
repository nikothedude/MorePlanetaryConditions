package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class niko_MPC_planetaryDroneReplicator extends BaseHazardCondition {

    private static final Logger log = Global.getLogger(niko_MPC_planetaryDroneReplicator.class);

    static {
        log.setLevel(Level.ALL);
    }

}
