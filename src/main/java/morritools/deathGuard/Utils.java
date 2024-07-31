package morritools.deathGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class Utils {

    public static String serializeInventory(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();

        return JSONComponentSerializer.json().serialize(Component.text(Arrays.toString(items))); // serialInv
    }

    public static String simplifyReason(String reason) {
        return reason.replaceFirst("^.*? was ", "").replaceFirst(" by.*$", "");
    }

    public static String simplifyLocation(Player player) {
        Location location = player.getLocation();
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
