package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.everyFrames.niko_MPC_scriptAdder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class niko_MPC_scriptUtils {

    public static void addScriptsAtValidTime(EveryFrameScript script, SectorEntityToken entityToAddScriptsTo) {
        addScriptsAtValidTime(new ArrayList<>(Collections.singleton(script)), entityToAddScriptsTo);
    }

    /**
     * Required in apply() of conditions due to the fact that scripts variable on entities can be null during loading.
     * @param scriptsToAdd
     * @param entityToAddScriptsTo
     */
    public static void addScriptsAtValidTime(List<EveryFrameScript> scriptsToAdd, SectorEntityToken entityToAddScriptsTo) {
        if (entityToAddScriptsTo == null) return;

        if (isValidTimeToAddScripts(entityToAddScriptsTo)) {
            for (EveryFrameScript script : scriptsToAdd) {
                if (!entityToAddScriptsTo.hasScriptOfClass(script.getClass())) {
                    entityToAddScriptsTo.addScript(script);
                }
            }
        } else {
            List<niko_MPC_scriptAdder> scriptAdders = getEntityScriptAdderList(entityToAddScriptsTo);
            niko_MPC_scriptAdder scriptAdder = new niko_MPC_scriptAdder(scriptsToAdd, entityToAddScriptsTo);
            Global.getSector().addScript(scriptAdder);
            if (scriptAdders != null) {
                scriptAdders.add(scriptAdder);
            }
        }
    }

    public static boolean isValidTimeToAddScripts(@NotNull SectorEntityToken entity) {
        return (entity.getId() != null || Global.getCurrentState() != GameState.TITLE);
    }

    public static void forceScriptAdderToAddScriptsIfOneIsPresentAndIfIsValidTime(@NotNull SectorEntityToken primaryEntity) {
        if (niko_MPC_scriptUtils.isValidTimeToAddScripts(primaryEntity)) {
            List<niko_MPC_scriptAdder> scriptAdders = getEntityScriptAdderList(primaryEntity);

            if (scriptAdders != null && (!scriptAdders.isEmpty())) {
                for (niko_MPC_scriptAdder scriptAdder : scriptAdders) {
                    scriptAdder.addScripts();
                }
            }
        }
    }

    @Nullable
    public static List<niko_MPC_scriptAdder> getEntityScriptAdderList(@NotNull SectorEntityToken entity) {
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        if (entityMemory == null) return null;

        List<niko_MPC_scriptAdder> scriptAdders = (List<niko_MPC_scriptAdder>) entityMemory.get(niko_MPC_ids.scriptAdderId);
        if (!(scriptAdders instanceof ArrayList)) {
            entityMemory.set(niko_MPC_ids.scriptAdderId, new ArrayList<niko_MPC_scriptAdder>());
            scriptAdders = (List<niko_MPC_scriptAdder>) entityMemory.get(niko_MPC_ids.scriptAdderId);
        }

        return scriptAdders;
    }
}

