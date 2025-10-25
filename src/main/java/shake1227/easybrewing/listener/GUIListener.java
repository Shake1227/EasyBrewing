package shake1227.easybrewing.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import shake1227.easybrewing.CustomRecipe;
import shake1227.easybrewing.EasyBrewing;
import shake1227.easybrewing.MessageManager;
import shake1227.easybrewing.RecipeManager;

import java.util.*;

public class GUIListener implements Listener {

    private final EasyBrewing plugin;
    private final RecipeManager recipeManager;
    private final MessageManager messages;
    private final Map<UUID, String> recipeCreators;
    private final Map<UUID, String> openGUIs = new HashMap<>();
    private final Map<UUID, Integer> openPages = new HashMap<>();
    private final Map<UUID, String> editingRecipe = new HashMap<>();

    private static final int GUI_SIZE = 54;
    private static final int RECIPES_PER_PAGE = 45;

    private static final int SLOT_INPUT = 11;
    private static final int SLOT_INGREDIENT = 13;
    private static final int SLOT_OUTPUT = 15;

    private static final int SLOT_INPUT_ICON = 2;
    private static final int SLOT_INGREDIENT_ICON = 4;
    private static final int SLOT_OUTPUT_ICON = 6;

    private static final int SLOT_SAVE = 25;
    private static final int SLOT_CANCEL = 19;
    private static final int SLOT_INFO = 22;

    public GUIListener(EasyBrewing plugin, RecipeManager recipeManager, MessageManager messages, Map<UUID, String> recipeCreators) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.messages = messages;
        this.recipeCreators = recipeCreators;
    }

    public void openMainGUI(Player player, int page) {
        List<CustomRecipe> recipes = new ArrayList<>(recipeManager.getAllRecipes());
        int maxPage = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, messages.get("gui-main-title"));

        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int index = (page * RECIPES_PER_PAGE) + i;
            if (index >= recipes.size()) break;

            CustomRecipe recipe = recipes.get(index);
            ItemStack displayItem = recipe.getOutput().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(messages.color("&f&l" + recipe.getRecipeName()));
                meta.setLore(messages.getList("gui-recipe-item-lore",
                        "input", getItemName(recipe.getInput()),
                        "ingredient", getItemName(recipe.getIngredient()),
                        "output", getItemName(recipe.getOutput())
                ));
                displayItem.setItemMeta(meta);
            }
            gui.setItem(i, displayItem);
        }

        if (page > 0) {
            gui.setItem(45, createDisplayItem(Material.ARROW, messages.get("gui-prev-page")));
        }

        gui.setItem(49, createDisplayItem(Material.ANVIL, messages.get("gui-create-item"), messages.getList("gui-create-lore")));

        if (page < maxPage - 1) {
            gui.setItem(53, createDisplayItem(Material.ARROW, messages.get("gui-next-page")));
        }

        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), "main");
        openPages.put(player.getUniqueId(), page);
    }

    public void openEditorGUI(Player player, String recipeName) {
        String title = messages.get("gui-editor-title", "recipe_name", recipeName);
        Inventory gui = Bukkit.createInventory(null, 27, title);

        CustomRecipe recipe = recipeManager.getRecipeByName(recipeName);

        gui.setItem(SLOT_INPUT_ICON, createDisplayItem(Material.GLASS_BOTTLE, messages.get("gui-editor-input-item"), messages.getList("gui-editor-input-lore")));
        gui.setItem(SLOT_INGREDIENT_ICON, createDisplayItem(Material.NETHER_WART, messages.get("gui-editor-ingredient-item"), messages.getList("gui-editor-ingredient-lore")));
        gui.setItem(SLOT_OUTPUT_ICON, createDisplayItem(Material.POTION, messages.get("gui-editor-output-item"), messages.getList("gui-editor-output-lore")));

        gui.setItem(SLOT_INFO, createDisplayItem(Material.PAPER, messages.get("gui-editor-info-item", "recipe_name", recipeName), messages.getList("gui-editor-info-lore")));

        gui.setItem(SLOT_CANCEL, createDisplayItem(Material.RED_WOOL, messages.get("gui-editor-cancel-item"), messages.getList("gui-editor-cancel-lore")));
        gui.setItem(SLOT_SAVE, createDisplayItem(Material.GREEN_WOOL, messages.get("gui-editor-save-item"), messages.getList("gui-editor-save-lore")));

        if (recipe != null) {
            gui.setItem(SLOT_INPUT, recipe.getInput());
            gui.setItem(SLOT_INGREDIENT, recipe.getIngredient());
            gui.setItem(SLOT_OUTPUT, recipe.getOutput());
        }

        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), "editor");
        editingRecipe.put(player.getUniqueId(), recipeName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!openGUIs.containsKey(uuid)) return;

        String guiType = openGUIs.get(uuid);

        if (guiType.equals("main")) {
            handleMainGUIClick(event, player, uuid);
        } else if (guiType.equals("editor")) {
            handleEditorGUIClick(event, player, uuid);
        }
    }

    private void handleMainGUIClick(InventoryClickEvent event, Player player, UUID uuid) {
        event.setCancelled(true);
        if (event.getClickedInventory() != player.getOpenInventory().getTopInventory()) return;

        int slot = event.getSlot();
        int currentPage = openPages.getOrDefault(uuid, 0);

        if (slot == 45 && currentPage > 0) {
            openMainGUI(player, currentPage - 1);
        } else if (slot == 53) {
            List<CustomRecipe> recipes = new ArrayList<>(recipeManager.getAllRecipes());
            int maxPage = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);
            if (currentPage < maxPage - 1) {
                openMainGUI(player, currentPage + 1);
            }
        } else if (slot == 49) {
            recipeCreators.put(uuid, "pending");
            player.closeInventory();
            messages.send(player, "recipe-name-prompt");
        } else if (slot < RECIPES_PER_PAGE) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR && clickedItem.hasItemMeta()) {
                String recipeName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                if (event.getClick() == ClickType.RIGHT) {
                    recipeManager.deleteRecipe(recipeName);
                    messages.send(player, "recipe-deleted", "recipe_name", recipeName);
                    openMainGUI(player, currentPage);
                } else {
                    openEditorGUI(player, recipeName);
                }
            }
        }
    }

    private void handleEditorGUIClick(InventoryClickEvent event, Player player, UUID uuid) {
        if (event.getClickedInventory() == player.getOpenInventory().getTopInventory()) {
            int slot = event.getSlot();
            if (slot == SLOT_INPUT || slot == SLOT_INGREDIENT || slot == SLOT_OUTPUT) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
                if (slot == SLOT_SAVE) {
                    saveRecipeFromGUI(player, event.getInventory());
                } else if (slot == SLOT_CANCEL) {
                    player.closeInventory();
                }
            }
        }
    }

    private void saveRecipeFromGUI(Player player, Inventory gui) {
        ItemStack input = gui.getItem(SLOT_INPUT);
        ItemStack ingredient = gui.getItem(SLOT_INGREDIENT);
        ItemStack output = gui.getItem(SLOT_OUTPUT);
        String recipeName = editingRecipe.get(player.getUniqueId());

        if (input == null || ingredient == null || output == null || input.getType() == Material.AIR || ingredient.getType() == Material.AIR || output.getType() == Material.AIR) {
            messages.send(player, "recipe-save-fail-items");
            return;
        }

        recipeManager.saveRecipe(recipeName, input.clone(), ingredient.clone(), output.clone());
        messages.send(player, "recipe-saved", "recipe_name", recipeName);

        gui.setItem(SLOT_INPUT, null);
        gui.setItem(SLOT_INGREDIENT, null);
        gui.setItem(SLOT_OUTPUT, null);

        player.closeInventory();
        openMainGUI(player, openPages.getOrDefault(player.getUniqueId(), 0));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!recipeCreators.containsKey(uuid)) return;

        event.setCancelled(true);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            recipeCreators.remove(uuid);
            messages.send(player, "recipe-name-cancelled");
            return;
        }

        if (message.contains(" ") || message.isEmpty()) {
            messages.send(player, "recipe-name-invalid");
            return;
        }

        String recipeName = message;
        recipeCreators.remove(uuid);

        Bukkit.getScheduler().runTask(plugin, () -> {
            openEditorGUI(player, recipeName);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        String guiType = openGUIs.remove(uuid);

        if ("editor".equals(guiType)) {
            String recipeName = editingRecipe.remove(uuid);
            Inventory gui = event.getInventory();

            ItemStack input = gui.getItem(SLOT_INPUT);
            ItemStack ingredient = gui.getItem(SLOT_INGREDIENT);
            ItemStack output = gui.getItem(SLOT_OUTPUT);

            boolean itemsReturned = false;
            if (input != null && input.getType() != Material.AIR) {
                player.getInventory().addItem(input);
                itemsReturned = true;
            }
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                player.getInventory().addItem(ingredient);
                itemsReturned = true;
            }
            if (output != null && output.getType() != Material.AIR) {
                player.getInventory().addItem(output);
                itemsReturned = true;
            }

            if (itemsReturned) {
                messages.send(player, "items-returned");
            }
        }

        if ("main".equals(guiType)) {
            openPages.remove(uuid);
        }
    }

    public void handlePluginDisable() {
        for (UUID uuid : new HashSet<>(openGUIs.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    private String getItemName(ItemStack item) {
        if (item == null) return "None";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().toString().replace("_", " ").toLowerCase();
    }

    private ItemStack createDisplayItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.color(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisplayItem(Material material, String name) {
        return createDisplayItem(material, name, new ArrayList<>());
    }
}