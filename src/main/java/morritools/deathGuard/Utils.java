package morritools.deathGuard;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class Utils {

    public static String dataSplitter(String input, String part) {
        String[] parts = input.split("\\.");
        return switch (part) {
            case "ID" -> parts[0];
            case "INV" -> parts[1];
            case "TIME" -> parts[2];
            case "TYPE" -> parts[3];
            case "LOC" -> parts[4];
            case "WORLD" -> parts[5];
            default -> throw new IllegalStateException("Unexpected value: " + part);
        };
    }

    public static String serializeInventory(Player player) {
        ItemStack[] inventoryContents = player.getInventory().getContents();
        String[] serializedInventory = new String[inventoryContents.length];

        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];
            if (item != null) {
                Map<String, Object> itemMap = item.serialize();
                YamlConfiguration config = new YamlConfiguration();
                config.set("item", itemMap);
                serializedInventory[i] = config.saveToString();
            } else {
                serializedInventory[i] = "null";
            }
        }
        return String.join(";;;", serializedInventory);
    }

    public static ItemStack[] deserializeInventory(String serializedInventory) {
        String[] itemStrings = serializedInventory.split(";;;");
        ItemStack[] inventoryContents = new ItemStack[itemStrings.length];

        for (int i = 0; i < itemStrings.length; i++) {
            if (!itemStrings[i].equals("null")) {
                YamlConfiguration config = new YamlConfiguration();
                try {
                    config.loadFromString(itemStrings[i]);
                    Map<String, Object> itemMap = config.getConfigurationSection("item").getValues(false);
                    inventoryContents[i] = ItemStack.deserialize(itemMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return inventoryContents;
    }

    public static String simplifyReason(String reason, String playerName) {
        return reason.replaceFirst("^.*? was ", "").replaceFirst(" by.*$", "").replaceFirst(playerName + " ", "");
    }

    public static String simplifyTime(String time) {
        long timestamp = Long.parseLong(time);
        Instant now = Instant.now();
        Instant then = Instant.ofEpochMilli(timestamp);
        Duration duration = Duration.between(then, now);

        long days = duration.toDays();
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        }

        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        }

        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        }

        return "just now";
    }

    public static String simplifyLocation(Player player) {
        Location location = player.getLastDeathLocation();
        return location.getBlockX() + "x" + location.getBlockY() + "y" + location.getBlockZ() + "z";
    }

    public static String simplifyWorld(Player player) {
        World world = player.getWorld();
        switch (world.getEnvironment()) {
            case NORMAL:
                return "Overworld";
            case NETHER:
                return "Nether";
            case THE_END:
                return "End";
            default:
                return "Unknown";
        }
    }
}
