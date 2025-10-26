package shake1227.easybrewing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import shake1227.easybrewing.listener.GUIListener;

public class MainCommand implements CommandExecutor {

    private final EasyBrewing plugin;
    private final MessageManager messages;
    private final GUIListener guiListener;

    public MainCommand(EasyBrewing plugin, MessageManager messages, GUIListener guiListener) {
        this.plugin = plugin;
        this.messages = messages;
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.getMessageManager().loadMessages();
                plugin.getRecipeManager().loadRecipes();
                messages.sendRaw(sender, messages.get("prefix") + messages.get("reload"));
                return true;
            }
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("easybrewing.admin")) {
            messages.send(player, "no-permission");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getMessageManager().loadMessages();
            plugin.getRecipeManager().loadRecipes();
            messages.send(player, "reload");
            return true;
        }

        guiListener.openMainGUI(player, 0);
        return true;
    }
}