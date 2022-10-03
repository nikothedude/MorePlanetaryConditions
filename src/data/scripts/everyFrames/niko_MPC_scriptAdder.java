package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.utilities.niko_MPC_scriptUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;

public class niko_MPC_scriptAdder implements EveryFrameScript {

    private static final Logger log = Global.getLogger(niko_MPC_scriptAdder.class);

    static {
        log.setLevel(Level.ALL);
    }

    public boolean done = false;
    public List<EveryFrameScript> scriptsToAdd;
    public SectorEntityToken entityToAddScriptsTo;

    public niko_MPC_scriptAdder(List<EveryFrameScript> scriptsToAdd, SectorEntityToken entityToAddScriptsTo) {
        this.scriptsToAdd = scriptsToAdd;
        this.entityToAddScriptsTo = entityToAddScriptsTo;
        log.debug("delayed script adder created. entity: " + entityToAddScriptsTo);

        List<niko_MPC_scriptAdder> scriptAdders = niko_MPC_scriptUtils.getEntityScriptAdderList(entityToAddScriptsTo);
        if (scriptAdders != null) {
            scriptAdders.add(this);
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (entityToAddScriptsTo == null) {
            prepareForGarbageCollection();
            return;
        }
        if (niko_MPC_scriptUtils.isValidTimeToAddScripts(entityToAddScriptsTo)) {
            addScripts();
        }
    }

    public void addScripts() {
        for (EveryFrameScript script : scriptsToAdd) {
            if (!entityToAddScriptsTo.hasScriptOfClass(script.getClass())) {
                entityToAddScriptsTo.addScript(script);
            }
        }
        prepareForGarbageCollection();
    }

    private void prepareForGarbageCollection() {
        List<niko_MPC_scriptAdder> scriptAdders = niko_MPC_scriptUtils.getEntityScriptAdderList(entityToAddScriptsTo);
        if (scriptAdders != null) {
            scriptAdders.remove(this);
        }
        Global.getSector().removeScript(this);
        entityToAddScriptsTo = null;
        scriptsToAdd.clear();
        done = true;
    }
}
