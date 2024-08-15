package morritools.deathGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DGCommandExecutor implements CommandExecutor {
    private final DeathGuard plugin;
    private final Database database;
    private static final int maxPage = 7;
    private final Map<String, String> pendingConfirmations = new HashMap<>();

    public DGCommandExecutor(DeathGuard plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "lookup", "l":
                return handleLookup(sender, args);
            case "rollback":
                return handleRollback(sender, args);
            case "view":
                return handleView(sender, args);
            case "purge":
                return handlePurge(sender, args);
            case "purgeuser":
                return handlePurgeUser(sender, args);
            default:
                return false;
        }
    }

    private boolean handleLookup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /dg lookup <name> [page] [r:reason] [w:world]");
            return true;
        }

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            sender.sendMessage("Player not found. They are either offline or do not exist");
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            sender.sendMessage("No death data found for " + target.getName());
            return true;
        }

        int page = 1;
        StringBuilder reasonBuilder = new StringBuilder();
        StringBuilder worldBuilder = new StringBuilder();

        for (int i = 2; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.matches("\\d+")) {
                try {
                    page = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    if (reasonBuilder.length() > 0 || worldBuilder.length() > 0) {
                        reasonBuilder.append(" ").append(arg);
                    } else {
                        worldBuilder.append(arg);
                    }
                }
            } else if (arg.startsWith("r:")) {
                if (reasonBuilder.length() > 0) {
                    reasonBuilder.append(" ");
                }
                reasonBuilder.append(arg.substring(2));
            } else if (arg.startsWith("w:")) {
                if (worldBuilder.length() > 0) {
                    worldBuilder.append(" ");
                }
                worldBuilder.append(arg.substring(2));
            } else {
                if (reasonBuilder.length() > 0 || worldBuilder.length() > 0) {
                    reasonBuilder.append(" ").append(arg);
                } else {
                    worldBuilder.append(arg);
                }
            }
        }

        String reasonFilter = reasonBuilder.toString().trim();
        String worldFilter = worldBuilder.toString().trim();

        String[] entries = data.split("\\|");

        if (!reasonFilter.isEmpty()) {
            entries = Arrays.stream(entries)
                    .filter(entry -> Utils.dataSplitter(entry, "TYPE").equalsIgnoreCase(reasonFilter))
                    .toArray(String[]::new);
        }

        if (!worldFilter.isEmpty()) {
            entries = Arrays.stream(entries)
                    .filter(entry -> Utils.dataSplitter(entry, "WORLD").equalsIgnoreCase(worldFilter))
                    .toArray(String[]::new);
        }

        if (entries.length == 0) {
            String noEntriesMessage = "No death entries found";
            if (!reasonFilter.isEmpty()) {
                noEntriesMessage += " for reason: " + reasonFilter;
            }
            if (!worldFilter.isEmpty()) {
                noEntriesMessage += " in world: " + worldFilter;
            }
            sender.sendMessage(noEntriesMessage);
            return true;
        }

        Arrays.sort(entries, (a, b) -> {
            int idA = Integer.parseInt(Utils.dataSplitter(a, "ID"));
            int idB = Integer.parseInt(Utils.dataSplitter(b, "ID"));
            return Integer.compare(idB, idA);
        });

        int totalPages = (int) Math.ceil((double) entries.length / maxPage);
        if (page > totalPages || page < 1) {
            sender.sendMessage("Invalid page number. Please enter a number between 1 and " + totalPages);
            return true;
        }

        int startIndex = (page - 1) * maxPage;
        int endIndex = Math.min(startIndex + maxPage, entries.length);

        sender.sendMessage("----- " + ChatColor.DARK_PURPLE + "DeathGuard" + ChatColor.RESET + " ----- " + ChatColor.GRAY + "(" + target.getName() + ")");

        for (int i = startIndex; i < endIndex; i++) {
            String entry = entries[i];
            final TextComponent dataMessage = Component.text()
                    .content("#" + Utils.dataSplitter(entry, "ID"))
                    .append(Component.text(": " + Utils.dataSplitter(entry, "TYPE")))
                    .append(Component.text(", " + Utils.simplifyTime(Utils.dataSplitter(entry, "TIME"))))
                    .append(Component.text(" (" + Utils.dataSplitter(entry, "LOC") + ")").color(NamedTextColor.GRAY))
                    .build();
            sender.sendMessage(dataMessage);
        }

        sender.sendMessage("-----");
        sender.sendMessage(ChatColor.GRAY + "Page " + page + " of " + totalPages);

        return true;
    }

    private boolean handleRollback(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /dg rollback <name> <reason#>");
            return true;
        }
        Player player = (Player) sender;

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            player.sendMessage("Player not found. They are either offline or do not exist.");
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            player.sendMessage(target.getName() + " has no data to rollback.");
            return true;
        }

        String id = args[2].trim();

        String[] entries = data.split("\\|");
        for (String entry : entries) {
            if (Utils.dataSplitter(entry, "ID").equals(id)) {
                Inventory  targetInv = target.getInventory();
                ItemStack[] inventory = Utils.deserializeInventory(Utils.dataSplitter(entry, "INV"));

                targetInv.setContents(inventory);

                target.updateInventory();

                return true;
            }
        }
        player.sendMessage(target.getName() + " does not have an entry matching that number.");
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /dg view <name> <reason#>");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            player.sendMessage("Player not found. They are either offline or do not exist.");
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            player.sendMessage(target.getName() + " has no data to view.");
            return true;
        }

        String id = args[2].trim();

        String[] entries = data.split("\\|");
        for (String entry : entries) {
            if (Utils.dataSplitter(entry, "ID").equals(id)) {
                Inventory shownInventory = Bukkit.createInventory(null, 54, "Death Inventory");

                ItemStack[] inventory = Utils.deserializeInventory(Utils.dataSplitter(entry, "INV"));

                shownInventory.setContents(inventory);

                player.openInventory(shownInventory);
                return true;
            }
        }

        player.sendMessage(target.getName() + " does not have an entry matching that number.");
        return true;
    }

    private boolean handlePurge(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                pendingConfirmations.remove(player.getUniqueId().toString());

                pendingConfirmations.put(player.getUniqueId().toString(), "purge");
                sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to purge all death data? Type '/dg purge confirm' to confirm.");
                return true;
            } else {
                sender.sendMessage("Only players can perform this action.");
                return true;
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String confirmationType = pendingConfirmations.remove(player.getUniqueId().toString());
                if (confirmationType == null || !confirmationType.equals("purge")) {
                    sender.sendMessage(ChatColor.RED + "No purge request found. Type '/dg purge' to initiate a purge.");
                    return true;
                }
                database.purgeAllData();
                sender.sendMessage(ChatColor.GREEN + "All death data has been purged.");
                return true;
            } else {
                sender.sendMessage("Only players can perform this action.");
                return true;
            }
        } else {
            sender.sendMessage("Usage: /dg purge or /dg purge confirm");
            return true;
        }
    }

    private boolean handlePurgeUser(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String targetName = args[1];

                Player target = plugin.getServer().getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player " + targetName + " is not online or does not exist.");
                    return true;
                }

                String targetUUID = target.getUniqueId().toString();

                pendingConfirmations.remove(player.getUniqueId().toString());

                pendingConfirmations.put(player.getUniqueId().toString(), "purgeuser " + targetUUID);
                sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to purge death data for " + targetName + "? Type '/dg purgeuser " + targetName + " confirm' to confirm.");
                return true;
            } else {
                sender.sendMessage("Only players can perform this action.");
                return true;
            }
        } else if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String confirmationType = pendingConfirmations.remove(player.getUniqueId().toString());
                if (confirmationType == null || !confirmationType.startsWith("purgeuser ")) {
                    sender.sendMessage(ChatColor.RED + "No purgeuser request found. Type '/dg purgeuser <name>' to initiate a purge for a user.");
                    return true;
                }

                String expectedUUID = confirmationType.substring("purgeuser ".length());
                String providedName = args[1];

                Player target = plugin.getServer().getPlayer(providedName);
                if (target == null || !target.getUniqueId().toString().equals(expectedUUID)) {
                    sender.sendMessage(ChatColor.RED + "The provided name does not match the original request or the player is not online. Please type '/dg purgeuser <name>' to initiate a new request.");
                    return true;
                }

                database.purgeUserData(expectedUUID);
                sender.sendMessage(ChatColor.GREEN + "Death data for " + providedName + " has been purged.");
                return true;
            } else {
                sender.sendMessage("Only players can perform this action.");
                return true;
            }
        } else {
            sender.sendMessage("Usage: /dg purgeuser <name> or /dg purgeuser <name> confirm");
            return true;
        }
    }

}
