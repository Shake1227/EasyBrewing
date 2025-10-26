package shake1227.easybrewing;

// import org.bukkit.block.Block; // 不要
import org.bukkit.plugin.java.JavaPlugin;
import shake1227.easybrewing.listener.GUIListener;
import shake1227.easybrewing.listener.BrewListener;

import java.util.HashMap;
// import java.util.Map; // 不要
import java.util.UUID;

public final class EasyBrewing extends JavaPlugin {

    private static EasyBrewing instance;
    private RecipeManager recipeManager;
    private MessageManager messageManager;
    private GUIListener guiListener;
    private BrewingManager brewingManager; // BrewingManager を追加

    private final HashMap<UUID, String> recipeCreators = new HashMap<>();
    // private final Map<Block, Integer> customBrewTimes = new HashMap<>(); // 削除
    // private BrewingTicker brewingTicker; // 削除

    @Override
    public void onEnable() {
        instance = this;

        this.messageManager = new MessageManager(this);
        messageManager.loadMessages();

        this.recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();

        this.guiListener = new GUIListener(this, recipeManager, messageManager, recipeCreators);
        this.brewingManager = new BrewingManager(this, recipeManager); // BrewingManager を初期化

        getServer().getPluginManager().registerEvents(guiListener, this);
        // BrewListener に brewingManager を渡す
        getServer().getPluginManager().registerEvents(new BrewListener(this, recipeManager, brewingManager), this);

        getCommand("easybrewing").setExecutor(new MainCommand(this, messageManager, guiListener));

        // BrewingTicker の登録を削除
        // this.brewingTicker = new BrewingTicker(this, recipeManager, customBrewTimes);
        // this.brewingTicker.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        if (guiListener != null) {
            guiListener.handlePluginDisable();
        }
        // BrewingTicker のキャンセルを削除
        // if (brewingTicker != null) {
        //     brewingTicker.cancel();
        // }
        // customBrewTimes.clear(); // 削除

        // すべての醸造タスクを停止
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

    // BrewingManager の getter を追加
    public BrewingManager getBrewingManager() {
        return brewingManager;
    }

    public HashMap<UUID, String> getRecipeCreators() {
        return recipeCreators;
    }
}