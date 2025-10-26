package shake1227.easybrewing;

import org.bukkit.plugin.java.JavaPlugin;
import shake1227.easybrewing.listener.GUIListener;
import shake1227.easybrewing.listener.BrewListener;

import java.util.HashMap;
import java.util.UUID;

public final class EasyBrewing extends JavaPlugin {

    private static EasyBrewing instance;
    private RecipeManager recipeManager;
    private MessageManager messageManager;
    private GUIListener guiListener;
    private BrewingManager brewingManager;

    private final HashMap<UUID, String> recipeCreators = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        this.messageManager = new MessageManager(this);
        messageManager.loadMessages();

        this.recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();

        this.guiListener = new GUIListener(this, recipeManager, messageManager, recipeCreators);
        this.brewingManager = new BrewingManager(this, recipeManager);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new BrewListener(this, recipeManager, brewingManager), this);

        getCommand("easybrewing").setExecutor(new MainCommand(this, messageManager, guiListener));

    }

    @Override
    public void onDisable() {
        if (guiListener != null) {
            guiListener.handlePluginDisable();
        }
        if (brewingManager != null) {
            brewingManager.stopAllBrews();
        }
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

    public BrewingManager getBrewingManager() {
        return brewingManager;
    }

    public HashMap<UUID, String> getRecipeCreators() {
        return recipeCreators;
    }
}