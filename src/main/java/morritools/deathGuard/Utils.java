package morritools.deathGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class Utils {

    public static String serializeInventory(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();

        return JSONComponentSerializer.json().serialize(Component.text(Arrays.toString(items)));
    }

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

    public static Inventory deSerializeInventory(String serializedInventory) {
        String deSerializedInventory = String.valueOf(JSONComponentSerializer.json().deserialize(String.valueOf(Component.text(serializedInventory))));
        serializedInventory = serializedInventory.substring(1, serializedInventory.length() - 1).trim();

        String[] elements = serializedInventory.split(",\\s*");

        Inventory inventory = Bukkit.createInventory(null, 54);

        for (int i = 0; i < elements.length; i++) {
            String element = elements[i];

            if ("null".equals(element)) {
                inventory.setItem(i, null);
            } else if (element.startsWith("ItemStack")) {
                ItemStack itemStack = parseItemStack(element);
                inventory.setItem(i, itemStack);
            } else {
                throw new IllegalArgumentException("Unexpected element format: " + element);
            }
        }

        return inventory;
    }

    private static ItemStack parseItemStack(String itemStackStr) {
        itemStackStr = itemStackStr.replace("ItemStack{", "").replace("}", "");
        String[] parts = itemStackStr.split(" x ");

        String materialName = parts[0].trim();
        int amount = Integer.parseInt(parts[1].trim());

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + materialName);
        }
        ItemStack itemStack = new ItemStack(material);
        itemStack.setAmount(amount);

        return itemStack;
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
