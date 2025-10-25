package shake1227.easybrewing;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class MessageManager {

    private final EasyBrewing plugin;
    private FileConfiguration messagesConfig;
    private String prefix;

    public MessageManager(EasyBrewing plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        this.prefix = color(messagesConfig.getString("prefix", "&8[&eEasyBrewing&8] &r"));
    }

    public String get(String path) {
        return color(messagesConfig.getString(path, ""));
    }

    public String get(String path, String... placeholders) {
        String message = get(path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }

    public List<String> getList(String path, String... placeholders) {
        return messagesConfig.getStringList(path).stream()
                .map(line -> {
                    for (int i = 0; i < placeholders.length; i += 2) {
                        if (i + 1 < placeholders.length) {
                            line = line.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
                        }
                    }
                    return color(line);
                })
                .collect(Collectors.toList());
    }

    public void send(CommandSender sender, String path, String... placeholders) {
        if (sender == null) return;
        String message = get(path, placeholders);
        if (message.isEmpty()) return;
        sender.sendMessage(prefix + message);
    }

    public void sendRaw(CommandSender sender, String message) {
        if (sender == null) return;
        sender.sendMessage(color(message));
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}