package shake1227.easybrewing.task;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import shake1227.easybrewing.BrewingManager;
import shake1227.easybrewing.CustomRecipe;
import shake1227.easybrewing.EasyBrewing;
import shake1227.easybrewing.RecipeManager;

public class BrewingTask extends BukkitRunnable {

    private final BrewingManager manager;
    private final BrewingStand brewingStand;
    private final Location location;
    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private int brewTime = 400;

    public BrewingTask(BrewingManager manager, BrewingStand brewingStand, RecipeManager recipeManager) {
        this.manager = manager;
        this.brewingStand = brewingStand;
        this.location = brewingStand.getLocation();
        this.plugin = EasyBrewing.getInstance();
        this.recipeManager = recipeManager;
    }

    @Override
    public void run() {
        if (!(location.getBlock().getState() instanceof BrewingStand)) {
            manager.stopBrewing(location);
            return;
        }

        BrewingStand stand = (BrewingStand) location.getBlock().getState();
        BrewerInventory inventory = stand.getInventory();
        ItemStack ingredient = inventory.getIngredient();

        if (ingredient == null || ingredient.getType() == Material.AIR) {
            stand.setBrewingTime(0);
            stand.update();
            manager.stopBrewing(location);
            return;
        }

        if (stand.getFuelLevel() <= 0) {
            stand.setBrewingTime(0);
            stand.update();
            manager.stopBrewing(location);
            return;
        }

        stand.setBrewingTime(brewTime);
        stand.update();
        brewTime--;

        if (brewTime <= 0) {
            handleBrewingFinish(stand, inventory, ingredient);
            manager.stopBrewing(location);
        }
    }

    private void handleBrewingFinish(BrewingStand stand, BrewerInventory inventory, ItemStack ingredient) {
        RecipeOutcome outcome0 = determineRecipe(inventory.getItem(0), ingredient);
        RecipeOutcome outcome1 = determineRecipe(inventory.getItem(1), ingredient);
        RecipeOutcome outcome2 = determineRecipe(inventory.getItem(2), ingredient);

        boolean brewingOccurred = false;
        if (outcome0.isSuccess()) {
            inventory.setItem(0, outcome0.getResult());
            brewingOccurred = true;
        }
        if (outcome1.isSuccess()) {
            inventory.setItem(1, outcome1.getResult());
            brewingOccurred = true;
        }
        if (outcome2.isSuccess()) {
            inventory.setItem(2, outcome2.getResult());
            brewingOccurred = true;
        }

        if (brewingOccurred) {
            World world = location.getWorld();
            if (world != null) {
                world.playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
            }

            ingredient.setAmount(ingredient.getAmount() - 1);
            inventory.setIngredient(ingredient);

            stand.setFuelLevel(stand.getFuelLevel() - 1);
        }
    }

    private RecipeOutcome determineRecipe(ItemStack input, ItemStack ingredient) {
        if (input == null || ingredient == null) {
            return RecipeOutcome.FAILURE;
        }

        CustomRecipe recipe = recipeManager.getRecipe(input, ingredient);
        if (recipe != null) {
            return new RecipeOutcome(recipe.getOutput().clone());
        }

        return RecipeOutcome.FAILURE;
    }

    private static class RecipeOutcome {
        static final RecipeOutcome FAILURE = new RecipeOutcome(null, true);
        private final ItemStack result;
        private final boolean failure;

        RecipeOutcome(ItemStack result, boolean failure) {
            this.result = result;
            this.failure = failure;
        }

        RecipeOutcome(ItemStack result) {
            this(result, false);
        }

        boolean isSuccess() {
            return !failure;
        }

        boolean isFailure() {
            return failure;
        }

        ItemStack getResult() {
            return result;
        }
    }
}