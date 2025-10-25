package shake1227.easybrewing;

import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BrewingTicker extends BukkitRunnable {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final Map<Block, Integer> customBrewTimes;

    public BrewingTicker(EasyBrewing plugin, RecipeManager recipeManager, Map<Block, Integer> customBrewTimes) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.customBrewTimes = customBrewTimes;
    }

    @Override
    public void run() {
        if (customBrewTimes.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Block, Integer>> iterator = customBrewTimes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Block, Integer> entry = iterator.next();
            Block block = entry.getKey();
            int remainingTicks = entry.getValue();

            if (!(block.getState() instanceof BrewingStand)) {
                iterator.remove();
                continue;
            }

            BrewingStand stand = (BrewingStand) block.getState();
            BrewerInventory inventory = stand.getInventory();
            ItemStack ingredient = inventory.getIngredient();

            if (stand.getFuelLevel() <= 0 || ingredient == null || ingredient.getType().isAir()) {
                stand.setBrewingTime(0);
                stand.update();
                iterator.remove();
                continue;
            }

            Map<Integer, CustomRecipe> validRecipes = new HashMap<>();
            for (int i = 0; i < 3; i++) {
                ItemStack input = inventory.getItem(i);
                if (input != null) {
                    CustomRecipe recipe = recipeManager.getRecipe(input, ingredient);
                    if (recipe != null) {
                        validRecipes.put(i, recipe);
                    }
                }
            }

            if (validRecipes.isEmpty()) {
                stand.setBrewingTime(0);
                stand.update();
                iterator.remove();
                continue;
            }

            remainingTicks--;

            if (remainingTicks <= 1) {
                stand.setFuelLevel(stand.getFuelLevel() - 1);

                ItemStack newIngredient = ingredient.clone();
                newIngredient.setAmount(ingredient.getAmount() - 1);
                inventory.setIngredient(newIngredient);

                for (Map.Entry<Integer, CustomRecipe> recipeEntry : validRecipes.entrySet()) {
                    inventory.setItem(recipeEntry.getKey(), recipeEntry.getValue().getOutput().clone());
                }

                stand.setBrewingTime(0);
                stand.update();
                iterator.remove();
            } else {
                stand.setBrewingTime(remainingTicks);
                stand.update();
                customBrewTimes.put(block, remainingTicks);
            }
        }
    }
}