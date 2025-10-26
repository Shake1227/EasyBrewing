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

    private final BrewingManager brewingManager; // Managerへの参照は保持 (isBrewing確認のため)
    private final Location location;
    private final ItemStack[] expectedOutputs;
    private final long endTimeMillis;
    private final EasyBrewing plugin;

    private static final long CHECK_INTERVAL = 100L; // 5秒ごと

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
        plugin.getLogger().info("  - Expected outputs: " + Arrays.toString(this.expectedOutputs)); // this. を追加

        this.runTaskTimer(plugin, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    @Override
    public void run() {
        // --- 終了条件チェック ---
        if (System.currentTimeMillis() > endTimeMillis) {
            plugin.getLogger().info("[WatcherTask] Watch duration ended for " + locationToString(location) + ". Stopping.");
            // brewingManager.stopWatcher(location); // 削除
            cancel(); // 自身をキャンセル
            return;
        }
        BrewingStand brewingStand = null; // try-catch の外で宣言
        try {
            // ブロックの状態取得とキャストを安全に行う
            if (!location.getChunk().isLoaded() || !(location.getBlock().getState() instanceof BrewingStand)) {
                plugin.getLogger().info("[WatcherTask] Brewing stand state invalid or unloaded at " + locationToString(location) + ". Stopping.");
                // brewingManager.stopWatcher(location); // 削除
                cancel(); // 自身をキャンセル
                return;
            }
            brewingStand = (BrewingStand) location.getBlock().getState(); // キャストして変数に格納
        } catch (Exception e) {
            // 状態取得中にエラーが発生した場合 (例: ワールドがアンロードされた直後など)
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[WatcherTask] Error getting brewing stand state at " + locationToString(location) + ". Stopping.", e);
            // brewingManager.stopWatcher(location); // 削除
            cancel(); // 自身をキャンセル
            return;
        }

        // 醸造が再開されたら停止
        if (brewingManager.isBrewing(location)) {
            plugin.getLogger().info("[WatcherTask] New brewing process started at " + locationToString(location) + ". Stopping watcher.");
            // brewingManager.stopWatcher(location); // 削除
            cancel(); // 自身をキャンセル
            return;
        }


        // --- アイテムチェックと強制修正 ---
        plugin.getLogger().fine("[WatcherTask] Checking inventory at " + locationToString(location));
        BrewerInventory inventory = brewingStand.getInventory();
        boolean changed = false;

        for (int i = 0; i < 3; i++) {
            ItemStack currentItem = inventory.getItem(i);
            ItemStack expectedItem = expectedOutputs[i];

            plugin.getLogger().info("  - Monitoring slot " + i + ": Found=" + itemStackToString(currentItem));

            boolean isCorrect = (currentItem == null && expectedItem == null) ||
                    (currentItem != null && expectedItem != null && currentItem.isSimilar(expectedItem));

            // 常に強制セット
            plugin.getLogger().info("    - Force setting slot " + i + " to expected: " + itemStackToString(expectedItem));
            inventory.setItem(i, expectedItem != null ? expectedItem.clone() : null);
            // setItem後に isCorrect を再評価（ログ用）
            ItemStack itemAfterSet = inventory.getItem(i); // 再取得
            boolean actuallyCorrectNow = (itemAfterSet == null && expectedItem == null) ||
                    (itemAfterSet != null && expectedItem != null && itemAfterSet.isSimilar(expectedItem));
            plugin.getLogger().fine("      - State after setItem: " + itemStackToString(itemAfterSet) + " (Matches expected: " + actuallyCorrectNow + ")");

            // 変更前の状態が期待と異なっていた場合に changed フラグを立てる
            if (!isCorrect) {
                changed = true;
            }
        }

        // 強制セットによる変更があった場合、または変更前の状態が期待と異なっていた場合に update
        if (changed) {
            plugin.getLogger().info("  - Applying forced update to brewing stand state (change detected or forced set).");
            brewingStand.update(true);
        } else {
            plugin.getLogger().fine("  - No change detected and force set resulted in same state. Skipping update.");
        }
    }

    // --- ログ用ヘルパー ---
    private String locationToString(Location loc) { return loc != null ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" : "null"; }
    private String itemStackToString(ItemStack item) { return item != null ? item.getType() + " x" + item.getAmount() + (item.hasItemMeta() ? "(M)" : "") : "null"; }

}