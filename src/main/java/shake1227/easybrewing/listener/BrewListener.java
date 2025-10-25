package shake1227.easybrewing.listener;

import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import shake1227.easybrewing.EasyBrewing;
import shake1227.easybrewing.RecipeManager;

import java.util.Map;

public class BrewListener implements Listener {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final Map<Block, Integer> customBrewTimes;

    public BrewListener(EasyBrewing plugin, RecipeManager recipeManager, Map<Block, Integer> customBrewTimes) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.customBrewTimes = customBrewTimes;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (customBrewTimes.containsKey(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.BREWING) {
            return;
        }

        if (event.isShiftClick() && event.getView().getTopInventory().getType() == InventoryType.BREWING) {

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) {

            } else {
                BrewerInventory brewingInventory = (BrewerInventory) event.getView().getTopInventory();

                if (recipeManager.isCustomIngredient(clickedItem)) {
                    if (brewingInventory.getIngredient() == null || brewingInventory.getIngredient().getType().isAir()) {

                        ItemStack toMove = clickedItem.clone();
                        toMove.setAmount(1);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                brewingInventory.setIngredient(toMove);
                                clickedItem.setAmount(clickedItem.getAmount() - 1);
                                startBrewCheck(brewingInventory);
                            }
                        }.runTaskLater(plugin, 1L);

                        event.setCancelled(true);
                        return;
                    }
                }
                else if (recipeManager.isCustomInput(clickedItem)) {

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            int amountLeft = clickedItem.getAmount();
                            for (int i = 0; i <= 2; i++) {
                                if (amountLeft <= 0) break;

                                ItemStack slotItem = brewingInventory.getItem(i);
                                if (slotItem == null || slotItem.getType().isAir()) {
                                    ItemStack toMove = clickedItem.clone();
                                    toMove.setAmount(1);
                                    brewingInventory.setItem(i, toMove);
                                    amountLeft--;
                                } else if (slotItem.isSimilar(clickedItem) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                                    brewingInventory.setItem(i, slotItem);
                                    amountLeft--;
                                }
                            }
                            clickedItem.setAmount(amountLeft);
                            startBrewCheck(brewingInventory);
                        }
                    }.runTaskLater(plugin, 1L);

                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BREWING) {
            ItemStack cursorItem = event.getCursor();

            if (cursorItem != null && !cursorItem.getType().isAir()) {
                int slot = event.getSlot();
                if (slot == 3) {
                    if (recipeManager.isCustomIngredient(cursorItem)) {
                        event.setResult(Event.Result.ALLOW);
                    }
                } else if (slot >= 0 && slot <= 2) {
                    if (recipeManager.isCustomInput(cursorItem)) {
                        event.setResult(Event.Result.ALLOW);
                    }
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                startBrewCheck((BrewerInventory) event.getInventory());
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.BREWING) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                startBrewCheck((BrewerInventory) event.getInventory());
            }
        }.runTaskLater(plugin, 1L);
    }

    private void startBrewCheck(BrewerInventory inventory) {
        if (inventory.getHolder() == null) return;

        Block block = inventory.getHolder().getBlock();
        if (!(block.getState() instanceof BrewingStand)) return;

        if (customBrewTimes.containsKey(block)) {
            return;
        }

        BrewingStand stand = (BrewingStand) block.getState();
        if (stand.getFuelLevel() <= 0) {
            return;
        }

        ItemStack ingredient = inventory.getIngredient();
        if (ingredient == null || ingredient.getType().isAir()) {
            return;
        }

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
            customBrewTimes.put(block, 400);
        }
    }
}