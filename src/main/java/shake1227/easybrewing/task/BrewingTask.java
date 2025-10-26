package shake1227.easybrewing.task;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import shake1227.easybrewing.BrewingManager;
import shake1227.easybrewing.CustomRecipe;
import shake1227.easybrewing.EasyBrewing;
import shake1227.easybrewing.RecipeManager;

import java.util.Arrays; // 追加

public class BrewingTask extends BukkitRunnable {

    private final BrewingManager manager;
    private final BrewingStand brewingStand;
    private final RecipeManager recipeManager;
    private final Location location;
    private int brewTime = 400;
    private final ItemStack initialIngredient;
    private final EasyBrewing plugin;

    // --- ★★★ 監視フェーズ用フィールド ★★★ ---
    private boolean isMonitoring = false; // 監視フェーズかどうかのフラグ
    private long monitorEndTimeMillis = 0; // 監視終了時刻
    private ItemStack[] expectedOutputs = new ItemStack[3]; // 期待される出力 (スロット0, 1, 2)

    public BrewingTask(BrewingManager manager, BrewingStand brewingStand, RecipeManager recipeManager) {
        this.manager = manager;
        this.brewingStand = brewingStand;
        this.recipeManager = recipeManager;
        this.location = brewingStand.getLocation();
        this.plugin = EasyBrewing.getInstance();
        ItemStack ingredientInSlot = brewingStand.getInventory().getIngredient();
        this.initialIngredient = (ingredientInSlot != null && ingredientInSlot.getType() != Material.AIR) ?
                ingredientInSlot.clone() : null;

        if (this.initialIngredient == null) {
            plugin.getLogger().warning("[BrewingTask] Task created but initial ingredient was null or AIR at " + locationToString(location) + ". Cancelling immediately.");
            brewTime = -1; // 即時停止マーク
        } else {
            plugin.getLogger().info("[BrewingTask] Started at " + locationToString(location) + " with ingredient: " + itemStackToString(initialIngredient));
        }
    }

    @Override
    public void run() {
        // 即時停止マーク
        if (brewTime < 0) {
            manager.stopBrewing(location); // Mapから削除
            cancel(); // Runnable停止
            return;
        }

        // --- ★★★ 監視フェーズの処理 ★★★ ---
        if (isMonitoring) {
            runMonitoringPhase();
            return; // 監視フェーズ中は以下の醸造処理を行わない
        }

        // --- ↓↓↓ 通常の醸造フェーズの処理 ↓↓↓ ---

        // 醸造台の状態確認
        Block block = location.getBlock();
        if (!brewingStand.getChunk().isLoaded() || !(block.getState() instanceof BrewingStand)) {
            plugin.getLogger().info("[BrewingTask] Brewing stand state invalid/unloaded during brewing phase at " + locationToString(location) + ". Stopping task.");
            manager.stopBrewing(location);
            cancel();
            return;
        }
        BrewingStand stand = (BrewingStand) block.getState(); // 最新の状態を使う

        BrewerInventory inventory = stand.getInventory();
        ItemStack currentIngredient = inventory.getIngredient();

        // 燃料切れ or 素材不一致
        if (stand.getFuelLevel() <= 0 || currentIngredient == null || currentIngredient.getType() == Material.AIR || !currentIngredient.isSimilar(initialIngredient)) {
            plugin.getLogger().info("[BrewingTask] Fuel empty, ingredient missing/changed during brewing phase at " + locationToString(location) + ". Stopping task.");
            stand.setBrewingTime(0);
            stand.update();
            manager.stopBrewing(location);
            cancel();
            return;
        }

        // 見た目の進行度バーを更新
        stand.setBrewingTime(Math.max(0, brewTime));
        // stand.update(); // 毎ティック不要

        // --- 醸造完了 → 監視フェーズへ移行 ---
        if (brewTime == 0) {
            plugin.getLogger().info("[BrewingTask] Brew time finished at " + locationToString(location) + ". Performing initial replacement and starting monitoring...");
            // 1. 即時置き換え処理を実行し、期待される結果を取得
            ItemStack[] initialExpected = handleBrewingFinishImmediate(stand, inventory, initialIngredient);
            location.getWorld().playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);

            // 2. 監視フェーズに移行
            if (initialExpected != null) { // 変換が行われた場合のみ監視開始
                this.isMonitoring = true;
                this.monitorEndTimeMillis = System.currentTimeMillis() + (60 * 1000); // 1分後
                this.expectedOutputs = initialExpected; // 期待される結果を保存
                plugin.getLogger().info("[BrewingTask] Monitoring phase started for 1 minute at " + locationToString(location));
                plugin.getLogger().info("  - Expected state: " + Arrays.toString(expectedOutputs));
                // ★★★ 注意: manager.stopBrewing() と cancel() はここでは呼ばない ★★★
                // タスクは監視フェーズとして継続する
            } else {
                // 変換が行われなかった場合は、タスクを通常通り終了
                plugin.getLogger().info("[BrewingTask] No conversion occurred. Task finishing normally at " + locationToString(location));
                manager.stopBrewing(location); // Map から削除
                cancel(); // Runnable 停止
            }
            return; // 醸造フェーズの処理はここまで
        }

        brewTime--; // 残り時間を減らす

        // 途中経過ログ
        if (brewTime > 0 && brewTime % 100 == 0) {
            plugin.getLogger().info("[BrewingTask] Brewing phase... " + brewTime + " ticks left at " + locationToString(location));
        }
    }

    /**
     * 監視フェーズの処理 (run()メソッドから呼ばれる)
     */
    private void runMonitoringPhase() {
        // --- 終了条件チェック ---
        if (System.currentTimeMillis() > monitorEndTimeMillis) {
            plugin.getLogger().info("[BrewingTask] Monitoring duration ended for " + locationToString(location) + ". Stopping task.");
            manager.stopBrewing(location); // Map から削除
            cancel(); // Runnable 停止
            return;
        }
        Block block = location.getBlock();
        if (!location.getChunk().isLoaded() || !(block.getState() instanceof BrewingStand)) {
            plugin.getLogger().info("[BrewingTask] Brewing stand state invalid/unloaded during monitoring phase at " + locationToString(location) + ". Stopping task.");
            manager.stopBrewing(location);
            cancel();
            return;
        }
        BrewingStand stand = (BrewingStand) block.getState(); // 最新の状態を取得

        // --- アイテムチェックと強制上書き ---
        plugin.getLogger().fine("[BrewingTask] Monitoring inventory at " + locationToString(location)); // fineレベル
        BrewerInventory inventory = stand.getInventory();
        boolean changed = false; // 強制変更が行われたか

        for (int i = 0; i < 3; i++) {
            ItemStack currentItem = inventory.getItem(i);
            ItemStack expectedItem = expectedOutputs[i];

            // ★★★ 現在のスロット内容をログ出力 ★★★
            plugin.getLogger().info("  - Monitoring slot " + i + ": Found=" + itemStackToString(currentItem));

            // isSimilar() で比較 (null 同士も true になるように)
            boolean isCorrect = (currentItem == null && expectedItem == null) ||
                    (currentItem != null && expectedItem != null && currentItem.isSimilar(expectedItem));

            // ★★★ 期待通り *でなくても* (常に) 強制的にセットする ★★★
            // if (!isCorrect) { // ← この条件を外す
            plugin.getLogger().info("    - Force setting slot " + i + " to expected: " + itemStackToString(expectedItem));
            // 強制的に期待されるアイテムをセット (null も可)
            inventory.setItem(i, expectedItem != null ? expectedItem.clone() : null);
            changed = true;
            // } else {
            //    plugin.getLogger().fine("    - Slot " + i + " matches expected state."); // fineレベル
            //}
        }

        // 強制変更が行われた場合のみ update
        if (changed) {
            plugin.getLogger().info("  - Applying forced update to brewing stand state.");
            stand.update(true); // 強制更新
        }
    }


    /**
     * 醸造完了時の処理 (即時更新 & 期待される結果を返す)
     * @return 変換が行われた場合は期待されるスロット0-2の状態、行われなかった場合は null
     */
    private ItemStack[] handleBrewingFinishImmediate(BrewingStand stand, BrewerInventory inventory, ItemStack ingredientToConsume) {
        plugin.getLogger().info("[BrewingTask] Executing handleBrewingFinishImmediate at " + locationToString(location));
        plugin.getLogger().info("  - Ingredient to consume: " + itemStackToString(ingredientToConsume));

        ItemStack currentIngredient = inventory.getIngredient();
        if(currentIngredient == null || !currentIngredient.isSimilar(ingredientToConsume)) {
            plugin.getLogger().warning("[BrewingTask] Ingredient changed/removed. Aborting conversion.");
            if(stand.getFuelLevel() > 0) stand.setFuelLevel(stand.getFuelLevel() - 1);
            stand.update(true);
            return null; // 変換なし -> 監視も開始しない
        }

        boolean consumed = false;
        ItemStack[] expectedOutputsResult = new ItemStack[3]; // このメソッドの戻り値用

        // --- 1. アイテム置き換え & 期待値設定 ---
        for (int i = 0; i < 3; i++) {
            ItemStack input = inventory.getItem(i);
            plugin.getLogger().info("  - Checking slot " + i + ": Input=" + itemStackToString(input));

            if (input != null && !input.getType().isAir()) {
                CustomRecipe recipe = recipeManager.getRecipe(input, ingredientToConsume);
                if (recipe != null) {
                    expectedOutputsResult[i] = recipe.getOutput().clone(); // 戻り値に保存
                    plugin.getLogger().info("    - Recipe found! Immediately replacing slot " + i + " with Output: " + itemStackToString(expectedOutputsResult[i]));
                    inventory.setItem(i, expectedOutputsResult[i].clone()); // 即座にセット
                    consumed = true;
                } else {
                    plugin.getLogger().info("    - No matching recipe. Slot " + i + " remains unchanged.");
                    expectedOutputsResult[i] = input.clone(); // 戻り値は元のアイテム
                }
            } else {
                expectedOutputsResult[i] = null; // 戻り値は空
            }
        }

        // --- 2. 消費処理 ---
        if (consumed) {
            plugin.getLogger().info("  - Consuming ingredient and fuel.");
            // 素材消費
            ItemStack ingredientInSlot = inventory.getIngredient(); // 最新取得
            if (ingredientInSlot != null && ingredientInSlot.isSimilar(ingredientToConsume)) {
                if (ingredientInSlot.getAmount() > 1) {
                    ingredientInSlot.setAmount(ingredientInSlot.getAmount() - 1);
                    inventory.setIngredient(ingredientInSlot);
                    plugin.getLogger().info("    - Ingredient amount reduced to " + ingredientInSlot.getAmount());
                } else {
                    inventory.setIngredient(null);
                    plugin.getLogger().info("    - Ingredient removed.");
                }
            } else {
                plugin.getLogger().warning("    - Ingredient changed during consumption phase!");
            }
            // 燃料消費
            if(stand.getFuelLevel() > 0) {
                stand.setFuelLevel(stand.getFuelLevel() - 1);
                plugin.getLogger().info("    - Fuel reduced to " + stand.getFuelLevel());
            }
        } else {
            plugin.getLogger().info("  - No items replaced, not consuming fuel or ingredient.");
            stand.update(true); // 変更なくても update
            plugin.getLogger().info("[BrewingTask] Finished handleBrewingFinishImmediate (no change) at " + locationToString(location));
            return null; // 変換なし -> 監視も開始しない
        }

        // --- 3. 最終更新 ---
        plugin.getLogger().info("  - Applying final update.");
        stand.update(true);

        plugin.getLogger().info("[BrewingTask] Finished handleBrewingFinishImmediate at " + locationToString(location));
        return expectedOutputsResult; // 変換後の期待値を返す
    }


    // --- ログ出力用のヘルパーメソッド ---
    private String locationToString(Location loc) { /* 省略 */ return loc != null ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" : "null"; }
    private String itemStackToString(ItemStack item) { /* 省略 */ return item != null ? item.getType() + " x" + item.getAmount() + (item.hasItemMeta() ? "(M)" : "") : "null"; }
}