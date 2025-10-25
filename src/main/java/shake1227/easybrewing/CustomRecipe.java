package shake1227.easybrewing;

import org.bukkit.inventory.ItemStack;

public class CustomRecipe {
    private final String recipeName;
    private final ItemStack input;
    private final ItemStack ingredient;
    private final ItemStack output;

    public CustomRecipe(String recipeName, ItemStack input, ItemStack ingredient, ItemStack output) {
        this.recipeName = recipeName;
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return output;
    }
}