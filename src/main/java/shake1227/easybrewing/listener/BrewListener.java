package shake1227.easybrewing.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent; // BrewEvent を再度インポート
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import shake1227.easybrewing.BrewingManager;
import shake1227.easybrewing.EasyBrewing;
import shake1227.easybrewing.RecipeManager;

public class BrewListener implements Listener {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final BrewingManager brewingManager;

    public BrewListener(EasyBrewing plugin, RecipeManager recipeManager, BrewingManager brewingManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.brewingManager = brewingManager;
    }

    // ★★★ BrewEvent を常にキャンセルするリスナーを追加 ★★★
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaBrew(BrewEvent event) {
        // EasyBrewing のカスタムレシピかどうかに関わらず、
        // BrewingTask で処理するためバニラの醸造処理はキャンセルする
        Location loc = event.getBlock().getLocation();
        // 念のため、現在このプラグインのタスクが動いているかも確認
        if (brewingManager.isBrewing(loc)) {
            plugin.getLogger().fine("[BrewListener] Cancelling vanilla BrewEvent because custom task is running at " + loc);
            event.setCancelled(true);
        } else {
            // カスタムタスクが動いていない場合でも、カスタムレシピの可能性があるならキャンセルすべきか？
            // -> startBrewCheck が呼ばれるので、もしカスタムレシピならタスクが開始される。
            //    バニラレシピならタスクは開始されず、ここでキャンセルするとバニラ醸造も動かなくなる。
            //    よって、isBrewing 中のみキャンセルするのが良さそう。
            plugin.getLogger().finest("[BrewListener] Letting vanilla BrewEvent pass at " + loc + " as no custom task is running.");
        }

    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.BREWING) {
            return;
        }

        BrewerInventory inventory = (BrewerInventory) event.getView().getTopInventory();
        Location location = inventory.getLocation();

        if (location != null && brewingManager.isBrewing(location)) {
            event.setCancelled(true);
            return;
        }

        // --- シフトクリック処理 ---
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (!(event.getClickedInventory() instanceof PlayerInventory)) return;
            ItemStack sourceItem = event.getCurrentItem();
            if (sourceItem == null) return;

            if (recipeManager.isCustomIngredient(sourceItem) &&
                    (inventory.getIngredient() == null || inventory.getIngredient().getType() == Material.AIR))
            {
                event.setCancelled(true);
                ItemStack toPlace = sourceItem.clone();
                toPlace.setAmount(1);
                inventory.setIngredient(toPlace);
                sourceItem.setAmount(sourceItem.getAmount() - 1);
                scheduleBrewCheck(inventory);
                return;
            }
            else if (recipeManager.isCustomInput(sourceItem)) {
                event.setCancelled(true);
                scheduleInputPlaceAndBrewCheck(inventory, sourceItem);
                return;
            }
        }

        // --- 通常クリック処理 ---
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BREWING) {
            int slot = event.getRawSlot();
            ItemStack cursorItem = event.getCursor();

            if (slot == 3 && cursorItem != null && !cursorItem.getType().isAir()) {
                if (recipeManager.isCustomIngredient(cursorItem)) {
                    if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                        event.setCancelled(true);
                        ItemStack toPlace = cursorItem.clone();
                        toPlace.setAmount(1);
                        inventory.setIngredient(toPlace);
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                        event.getView().setCursor(cursorItem.getAmount() > 0 ? cursorItem : null);
                        scheduleBrewCheck(inventory);
                        return;
                    }
                }
            }
            else if (slot >= 0 && slot <= 2 && cursorItem != null && !cursorItem.getType().isAir()) {
                if (recipeManager.isCustomInput(cursorItem)) {
                    event.setResult(Event.Result.ALLOW);
                    scheduleBrewCheck(inventory);
                    return;
                }
            }
        }

        scheduleBrewCheck(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.BREWING) {
            return;
        }
        boolean affectsBrewingInv = false;
        for (int slot : event.getRawSlots()) {
            if (event.getView().getInventory(slot) != null && event.getView().getInventory(slot).getType() == InventoryType.BREWING) {
                affectsBrewingInv = true;
                break;
            }
        }
        if (affectsBrewingInv) {
            scheduleBrewCheck((BrewerInventory) event.getInventory());
        }
    }

    private void scheduleBrewCheck(BrewerInventory inventory) {
        new BukkitRunnable() {
            @Override
            public void run() {
                startBrewCheck(inventory);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void scheduleInputPlaceAndBrewCheck(BrewerInventory inventory, ItemStack sourceItem) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int amountLeft = sourceItem.getAmount();
                boolean changed = false;
                for (int i = 0; i <= 2; i++) {
                    if (amountLeft <= 0) break;
                    ItemStack slotItem = inventory.getItem(i);
                    if (slotItem == null || slotItem.getType().isAir()) {
                        ItemStack toMove = sourceItem.clone();
                        toMove.setAmount(1);
                        inventory.setItem(i, toMove);
                        amountLeft--;
                        changed = true;
                    }
                }
                sourceItem.setAmount(amountLeft);
                if (changed) {
                    startBrewCheck(inventory);
                }
            }
        }.runTaskLater(plugin, 1L);
    }


    private void startBrewCheck(BrewerInventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof BrewingStand stand)) {
            return;
        }

        Location location = stand.getLocation();

        if (brewingManager.isBrewing(location)) return;
        if (stand.getFuelLevel() <= 0) return;

        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType().isAir()) return;

        boolean hasValidRecipe = false;
        for (int i = 0; i < 3; i++) {
            ItemStack input = inventory.getItem(i);
            if (input != null && !input.getType().isAir()) {
                if (recipeManager.getRecipe(input, ingredient) != null) {
                    hasValidRecipe = true;
                    break;
                }
            }
        }

        if (hasValidRecipe) {
            plugin.getLogger().info("[BrewListener] Valid custom recipe detected at " + location + ". Starting BrewingTask.");
            brewingManager.startBrewing(stand);
        } else {
            plugin.getLogger().fine("[BrewListener] No valid custom recipe detected at " + location + ". Letting vanilla handle it (if applicable).");
        }
    }
}