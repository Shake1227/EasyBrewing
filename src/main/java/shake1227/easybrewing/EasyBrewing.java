package shake1227.easybrewing;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import shake1227.easybrewing.listener.GUIListener;
import shake1227.easybrewing.listener.BrewListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EasyBrewing extends JavaPlugin {

    private static EasyBrewing instance;
    private RecipeManager recipeManager;
    private MessageManager messageManager;
    private GUIListener guiListener;

    private final HashMap<UUID, String> recipeCreators = new HashMap<>();
    private final Map<Block, Integer> customBrewTimes = new HashMap<>();
    private BrewingTicker brewingTicker;

    @Override
    public void onEnable() {
        instance = this;

        this.messageManager = new MessageManager(this);
        messageManager.loadMessages();

        this.recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();

        this.guiListener = new GUIListener(this, recipeManager, messageManager, recipeCreators);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new BrewListener(this, recipeManager, customBrewTimes), this);

        getCommand("easybrewing").setExecutor(new MainCommand(this, messageManager, guiListener));

        this.brewingTicker = new BrewingTicker(this, recipeManager, customBrewTimes);
        this.brewingTicker.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        if (guiListener != null) {
            guiListener.handlePluginDisable();
        }
        if (brewingTicker != null) {
            brewingTicker.cancel();
        }
        customBrewTimes.clear();
    }

    public static EasyBrewing getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public HashMap<UUID, String> getRecipeCreators() {
        return recipeCreators;
    }
}