package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class niko_MPC_scriptUtils {

    public static void addScriptIfScriptIsUnique(SectorEntityToken entity, EveryFrameScript script) { //todo: maybe make an alternate type of script that has an "id" var
        if (!(entity.hasScriptOfClass(script.getClass()))) { //todo: might be able to implement this better by passing a class instead
            entity.addScript(script);
        }
    }
}
