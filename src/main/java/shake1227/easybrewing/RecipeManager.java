package shake1227.easybrewing;

// import org.bukkit.Material; // 未使用なので削除
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class RecipeManager {

    private final EasyBrewing plugin;
    private File recipesFile;
    private FileConfiguration recipesConfig;
    private final Map<String, CustomRecipe> recipeMap = new HashMap<>();

    public RecipeManager(EasyBrewing plugin) {
        this.plugin = plugin;
        // loadRecipes(); // mainクラスのonEnableで呼び出す
    }

    public void loadRecipes() {
        recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            try {
                recipesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create recipes.yml!", e); // エラーログ改善
            }
        }
        recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);

        recipeMap.clear();
        ConfigurationSection recipesSection = recipesConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return;
        }

        plugin.getLogger().info("Loading custom brewing recipes..."); // ロード開始ログ
        for (String recipeName : recipesSection.getKeys(false)) {
            ConfigurationSection current = recipesSection.getConfigurationSection(recipeName);
            if (current != null) {
                ItemStack input = current.getItemStack("input");
                ItemStack ingredient = current.getItemStack("ingredient");
                ItemStack output = current.getItemStack("output");

                if (input != null && ingredient != null && output != null) {
                    recipeMap.put(recipeName.toLowerCase(), new CustomRecipe(recipeName, input, ingredient, output));
                    plugin.getLogger().info(" - Loaded recipe: " + recipeName); // 個別ログ
                } else {
                    plugin.getLogger().warning(" - Failed to load recipe '" + recipeName + "': Missing item definition (input/ingredient/output).");
                }
            }
        }
        plugin.getLogger().info("Finished loading " + recipeMap.size() + " custom recipes."); // ロード完了ログ
    }

    public void saveRecipe(String recipeName, ItemStack input, ItemStack ingredient, ItemStack output) {
        String configKey = "recipes." + recipeName;
        recipesConfig.set(configKey + ".input", input);
        recipesConfig.set(configKey + ".ingredient", ingredient);
        recipesConfig.set(configKey + ".output", output);
        saveConfig();

        recipeMap.put(recipeName.toLowerCase(), new CustomRecipe(recipeName, input, ingredient, output));
    }

    public void deleteRecipe(String recipeName) {
        recipesConfig.set("recipes." + recipeName, null);
        saveConfig();
        recipeMap.remove(recipeName.toLowerCase());
    }

    public void saveConfig() {
        try {
            recipesConfig.save(recipesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml!", e); // エラーログ改善
        }
    }

    public CustomRecipe getRecipe(ItemStack input, ItemStack ingredient) {
        plugin.getLogger().info("[RecipeManager] getRecipe called:");
        plugin.getLogger().info("  - Input: " + itemStackToString(input));
        plugin.getLogger().info("  - Ingredient: " + itemStackToString(ingredient));

        if (input == null || ingredient == null) {
            plugin.getLogger().info("  - Result: null (Input or Ingredient is null)");
            return null;
        }

        for (CustomRecipe recipe : recipeMap.values()) {
            boolean inputSimilar = recipe.getInput().isSimilar(input);
            boolean ingredientSimilar = recipe.getIngredient().isSimilar(ingredient);
            plugin.getLogger().fine("  - Comparing with recipe '" + recipe.getRecipeName() + "':");
            plugin.getLogger().fine("    - Recipe Input: " + itemStackToString(recipe.getInput()) + " (Similar: " + inputSimilar + ")");
            plugin.getLogger().fine("    - Recipe Ingredient: " + itemStackToString(recipe.getIngredient()) + " (Similar: " + ingredientSimilar + ")");

            if (inputSimilar && ingredientSimilar) {
                plugin.getLogger().info("  - Result: Found matching recipe '" + recipe.getRecipeName() + "'");
                return recipe;
            }
        }
        plugin.getLogger().info("  - Result: null (No matching recipe found)");
        return null;
    }

    public CustomRecipe getRecipeByName(String name) {
        return recipeMap.get(name.toLowerCase());
    }

    public Collection<CustomRecipe> getAllRecipes() {
        return recipeMap.values();
    }

    public Set<String> getRecipeNames() {
        // レシピ名（小文字）のセットを返す
        return recipeMap.keySet();
    }

    public boolean isCustomInput(ItemStack item) {
        if (item == null) return false;
        for (CustomRecipe recipe : recipeMap.values()) {
            if (recipe.getInput().isSimilar(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCustomIngredient(ItemStack item) {
        if (item == null) return false;
        for (CustomRecipe recipe : recipeMap.values()) {
            if (recipe.getIngredient().isSimilar(item)) {
                return true;
            }
        }
        return false;
    }

    private String itemStackToString(ItemStack item) {
        if (item == null) return "null";
        String metaInfo = "";
        if (item.hasItemMeta()) {
            metaInfo = " (meta: " + item.getItemMeta().getClass().getSimpleName();
            if (item.getItemMeta().hasDisplayName()) {
                metaInfo += ", name:\"" + item.getItemMeta().getDisplayName() + "\"";
            }
            if (item.getItemMeta().hasLore()) {
                metaInfo += ", hasLore";
            }
            metaInfo += ")";
        }
        return item.getType() + " x" + item.getAmount() + metaInfo;
    }
}