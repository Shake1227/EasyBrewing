package shake1227.easybrewing;

import org.bukkit.Location;
import org.bukkit.block.BrewingStand;
// import shake1227.easybrewing.task.BrewCompletionWatcherTask; // 削除
import shake1227.easybrewing.task.BrewingTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrewingManager {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final Map<Location, BrewingTask> activeBrews = new ConcurrentHashMap<>();
    // private final Map<Location, BrewCompletionWatcherTask> activeWatchers = new ConcurrentHashMap<>(); // 削除

    public BrewingManager(EasyBrewing plugin, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
    }

    public void startBrewing(BrewingStand brewingStand) {
        Location location = brewingStand.getLocation();
        // 既に醸造中なら開始しない
        if (activeBrews.containsKey(location)) {
            plugin.getLogger().fine("[BrewingManager] Attempted to start brewing at " + location + " but a task is already active."); // fineレベルに変更
            return;
        }
        // stopWatcher(location); // 削除

        BrewingTask task = new BrewingTask(this, brewingStand, recipeManager);
        activeBrews.put(location, task);
        task.runTaskTimer(plugin, 0L, 1L); // 毎ティック実行
    }

    public void stopBrewing(Location location) {
        BrewingTask task = activeBrews.remove(location);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            plugin.getLogger().info("[BrewingManager] Brewing task stopped (or finished) at " + location);
        }
    }

    public boolean isBrewing(Location location) {
        // BrewingTaskが動いているか (監視フェーズも含む)
        return activeBrews.containsKey(location);
    }

    /* // Watcher関連メソッド削除
    public void startWatcher(Location location, org.bukkit.inventory.ItemStack[] expectedOutputs, long durationMillis) { ... }
    public void stopWatcher(Location location) { ... }
    */

    public void stopAllBrews() {
        // activeBrewsを停止
        for (Location loc : activeBrews.keySet()) {
            stopBrewing(loc);
        }
        activeBrews.clear();
        // Watcher関連削除
        // for (Location loc : activeWatchers.keySet()) { stopWatcher(loc); }
        // activeWatchers.clear();
    }
}