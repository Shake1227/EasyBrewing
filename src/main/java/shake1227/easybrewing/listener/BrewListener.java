package shake1227.easybrewing.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
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

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (!(event.getClickedInventory() instanceof PlayerInventory)) {
                startBrewingProcess(inventory);
                return;
            }

            ItemStack sourceItem = event.getCurrentItem();
            if (sourceItem == null || sourceItem.getType().isAir()) return;

            if (recipeManager.isCustomIngredient(sourceItem)) {
                ItemStack currentIngredient = inventory.getIngredient();
                if (currentIngredient == null || currentIngredient.getType() == Material.AIR) {
                    event.setCancelled(true);
                    ItemStack toPlace = sourceItem.clone();
                    toPlace.setAmount(1);
                    inventory.setIngredient(toPlace);
                    sourceItem.setAmount(sourceItem.getAmount() - 1);
                    startBrewingProcess(inventory);
                    return;
                } else if (currentIngredient.isSimilar(sourceItem) && currentIngredient.getAmount() < currentIngredient.getMaxStackSize()) {
                    event.setCancelled(true);
                    int canAdd = currentIngredient.getMaxStackSize() - currentIngredient.getAmount();
                    int moveAmount = Math.min(sourceItem.getAmount(), canAdd);
                    currentIngredient.setAmount(currentIngredient.getAmount() + moveAmount);
                    inventory.setIngredient(currentIngredient);
                    sourceItem.setAmount(sourceItem.getAmount() - moveAmount);
                    startBrewingProcess(inventory);
                    return;
                }
            }
            else if (recipeManager.isCustomInput(sourceItem) || isPotion(sourceItem)) {
                event.setCancelled(true);
                scheduleInputPlaceAndBrewCheck(inventory, sourceItem);
                return;
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BREWING) {
            int slot = event.getRawSlot();
            ItemStack cursorItem = event.getCursor();
            ItemStack currentItem = event.getCurrentItem();

            if (slot == 3) {
                event.setCancelled(true);
                if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                    inventory.setIngredient(null);
                    event.getView().setCursor(currentItem);
                }
                else if ((currentItem == null || currentItem.getType() == Material.AIR) && recipeManager.isCustomIngredient(cursorItem)) {
                    ItemStack toPlace = cursorItem.clone();
                    inventory.setIngredient(toPlace);
                    event.getView().setCursor(null);
                }
                else if (currentItem != null && cursorItem != null && !cursorItem.getType().isAir()){
                    if (currentItem.isSimilar(cursorItem) && recipeManager.isCustomIngredient(cursorItem)) {
                        int maxStack = currentItem.getMaxStackSize();
                        int total = currentItem.getAmount() + cursorItem.getAmount();
                        if (total <= maxStack) {
                            currentItem.setAmount(total);
                            inventory.setIngredient(currentItem);
                            event.getView().setCursor(null);
                        } else {
                            currentItem.setAmount(maxStack);
                            inventory.setIngredient(currentItem);
                            cursorItem.setAmount(total - maxStack);
                            event.getView().setCursor(cursorItem);
                        }
                    }
                    else if (recipeManager.isCustomIngredient(cursorItem)) {
                        inventory.setIngredient(cursorItem.clone());
                        event.getView().setCursor(currentItem.clone());
                    }
                }
                startBrewingProcess(inventory);
                return;
            }
            else if (slot >= 0 && slot <= 2) {
                event.setCancelled(true);

                boolean canPlaceCursor = (cursorItem != null && !cursorItem.getType().isAir() &&
                        (recipeManager.isCustomInput(cursorItem) || isPotion(cursorItem)));

                if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                    inventory.setItem(slot, null);
                    event.getView().setCursor(currentItem);
                }
                else if (currentItem == null || currentItem.getType() == Material.AIR) {
                    if (canPlaceCursor) {
                        ItemStack toPlace = cursorItem.clone();
                        toPlace.setAmount(1);
                        inventory.setItem(slot, toPlace);
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                        event.getView().setCursor(cursorItem.getAmount() > 0 ? cursorItem : null);
                    }
                }
                else {
                    if (canPlaceCursor) {
                        if (!cursorItem.isSimilar(currentItem) || currentItem.getAmount() >= 1 ) {
                            ItemStack toPlace = cursorItem.clone();
                            toPlace.setAmount(1);
                            inventory.setItem(slot, toPlace);
                            cursorItem.setAmount(cursorItem.getAmount() - 1);
                            Player player = (Player) event.getWhoClicked();

                            if (cursorItem.getAmount() > 0 && currentItem.isSimilar(cursorItem)) {
                                int canAdd = cursorItem.getMaxStackSize() - cursorItem.getAmount();
                                int addAmount = Math.min(currentItem.getAmount(), canAdd);
                                cursorItem.setAmount(cursorItem.getAmount() + addAmount);
                            } else if (cursorItem.getAmount() <= 0) {
                                cursorItem = currentItem.clone();
                            } else {
                                player.getInventory().addItem(currentItem.clone());
                            }
                            event.getView().setCursor(cursorItem != null && cursorItem.getAmount() > 0 ? cursorItem : null);
                        }
                    }
                }
                startBrewingProcess(inventory);
                return;
            }
        }

        startBrewingProcess(inventory);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.BREWING) {
            return;
        }

        Location location = event.getInventory().getLocation();
        if (location != null && brewingManager.isBrewing(location)) {
            event.setCancelled(true);
            return;
        }

        boolean affectsBrewingInv = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot <= 3) {
                if (slot <= 2) {
                    ItemStack newItem = event.getNewItems().get(slot);
                    if (newItem != null && !newItem.getType().isAir()) {
                        if ((recipeManager.isCustomInput(newItem) || isPotion(newItem)) && newItem.getAmount() <= 1) {
                        } else {
                            event.setCancelled(true);
                            return;
                        }
                    }
                } else if (slot == 3) {
                    ItemStack newItem = event.getNewItems().get(slot);
                    if (newItem != null && !newItem.getType().isAir()) {
                        if (!recipeManager.isCustomIngredient(newItem)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                affectsBrewingInv = true;
            }
        }

        if (affectsBrewingInv) {
            startBrewingProcess((BrewerInventory) event.getInventory());
        }
    }


    private void startBrewingProcess(BrewerInventory inventory) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (inventory.getHolder() == null) {
                return;
            }

            if (inventory.getHolder() instanceof BrewingStand) {
                BrewingStand stand = (BrewingStand) inventory.getHolder();

                if (brewingManager.isBrewing(stand.getLocation())) {
                    return;
                }

                if (stand.getFuelLevel() > 0) {
                    ItemStack ingredient = inventory.getIngredient();
                    if (ingredient != null && !ingredient.getType().isAir()) {

                        boolean hasValidRecipe = false;
                        for (int i = 0; i < 3; i++) {
                            ItemStack input = inventory.getItem(i);
                            if (recipeManager.getRecipe(input, ingredient) != null) {
                                hasValidRecipe = true;
                                break;
                            }
                        }

                        if (hasValidRecipe) {
                            brewingManager.startBrewing(stand);
                        }
                    }
                }
            }
        }, 1L);
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
                    startBrewingProcess(inventory);
                }
            }
        }.runTaskLater(plugin, 1L);
    }


    private boolean isPotion(ItemStack itemStack) {
        if (itemStack == null) return false;
        Material type = itemStack.getType();
        return type == Material.POTION || type == Material.SPLASH_POTION ||
                type == Material.LINGERING_POTION || type == Material.GLASS_BOTTLE;
    }
}