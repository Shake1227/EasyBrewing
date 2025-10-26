package shake1227.easybrewing;

import org.bukkit.Location;
import org.bukkit.block.BrewingStand;
import shake1227.easybrewing.task.BrewingTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrewingManager {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final Map<Location, BrewingTask> activeBrews = new ConcurrentHashMap<>();

    public BrewingManager(EasyBrewing plugin, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
    }

    public void startBrewing(BrewingStand brewingStand) {
        Location location = brewingStand.getLocation();
        if (activeBrews.containsKey(location)) {
            return;
        }

        BrewingTask task = new BrewingTask(this, brewingStand, recipeManager);
        activeBrews.put(location, task);
        task.runTaskTimer(plugin, 0L, 1L);
    }

    public void stopBrewing(Location location) {
        BrewingTask task = activeBrews.remove(location);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean isBrewing(Location location) {
        return activeBrews.containsKey(location);
    }

    public void stopAllBrews() {
        for (Location loc : activeBrews.keySet()) {
            stopBrewing(loc);
        }
        activeBrews.clear();
    }
}