package shake1227.easybrewing.task;

import org.bukkit.Location;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import shake1227.easybrewing.BrewingManager;
import shake1227.easybrewing.EasyBrewing;

import java.util.Arrays;
import java.util.Objects;

public class BrewCompletionWatcherTask extends BukkitRunnable {

    private final BrewingManager brewingManager;
    private final Location location;
    private final ItemStack[] expectedOutputs;
    private final long endTimeMillis;
    private final EasyBrewing plugin;

    private static final long CHECK_INTERVAL = 100L;

    public BrewCompletionWatcherTask(BrewingManager brewingManager, Location location, ItemStack[] expectedOutputs, long durationMillis) {
        this.brewingManager = brewingManager;
        this.location = location;
        this.expectedOutputs = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            this.expectedOutputs[i] = (expectedOutputs[i] != null) ? expectedOutputs[i].clone() : null;
        }
        this.endTimeMillis = System.currentTimeMillis() + durationMillis;
        this.plugin = EasyBrewing.getInstance();

        plugin.getLogger().info("[WatcherTask] Started for " + locationToString(location) + " until " + endTimeMillis);
        plugin.getLogger().info("  - Expected outputs: " + Arrays.toString(this.expectedOutputs));

        this.runTaskTimer(plugin, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() > endTimeMillis) {
            plugin.getLogger().info("[WatcherTask] Watch duration ended for " + locationToString(location) + ". Stopping.");
            cancel();
            return;
        }
        BrewingStand brewingStand = null;
        try {
            if (!location.getChunk().isLoaded() || !(location.getBlock().getState() instanceof BrewingStand)) {
                plugin.getLogger().info("[WatcherTask] Brewing stand state invalid or unloaded at " + locationToString(location) + ". Stopping.");
                cancel();
                return;
            }
            brewingStand = (BrewingStand) location.getBlock().getState();
        } catch (Exception e) {

            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[WatcherTask] Error getting brewing stand state at " + locationToString(location) + ". Stopping.", e);
            cancel();
            return;
        }

        if (brewingManager.isBrewing(location)) {
            plugin.getLogger().info("[WatcherTask] New brewing process started at " + locationToString(location) + ". Stopping watcher.");
            cancel();
            return;
        }

        plugin.getLogger().fine("[WatcherTask] Checking inventory at " + locationToString(location));
        BrewerInventory inventory = brewingStand.getInventory();
        boolean changed = false;

        for (int i = 0; i < 3; i++) {
            ItemStack currentItem = inventory.getItem(i);
            ItemStack expectedItem = expectedOutputs[i];

            plugin.getLogger().info("  - Monitoring slot " + i + ": Found=" + itemStackToString(currentItem));

            boolean isCorrect = (currentItem == null && expectedItem == null) ||
                    (currentItem != null && expectedItem != null && currentItem.isSimilar(expectedItem));

            plugin.getLogger().info("    - Force setting slot " + i + " to expected: " + itemStackToString(expectedItem));
            inventory.setItem(i, expectedItem != null ? expectedItem.clone() : null);

            ItemStack itemAfterSet = inventory.getItem(i);
            boolean actuallyCorrectNow = (itemAfterSet == null && expectedItem == null) ||
                    (itemAfterSet != null && expectedItem != null && itemAfterSet.isSimilar(expectedItem));
            plugin.getLogger().fine("      - State after setItem: " + itemStackToString(itemAfterSet) + " (Matches expected: " + actuallyCorrectNow + ")");

            if (!isCorrect) {
                changed = true;
            }
        }

        if (changed) {
            plugin.getLogger().info("  - Applying forced update to brewing stand state (change detected or forced set).");
            brewingStand.update(true);
        } else {
            plugin.getLogger().fine("  - No change detected and force set resulted in same state. Skipping update.");
        }
    }

    private String locationToString(Location loc) { return loc != null ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" : "null"; }
    private String itemStackToString(ItemStack item) { return item != null ? item.getType() + " x" + item.getAmount() + (item.hasItemMeta() ? "(M)" : "") : "null"; }

}