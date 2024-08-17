package morritools.deathGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
    private static final int MAX_PAGE = 7;
    private final Map<String, String> pendingConfirmations = new HashMap<>();

    public DGCommandExecutor(DeathGuard plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deathguard.admin") || !sender.hasPermission("deathguard.user")) {
            sender.sendMessage(Utils.alert("You do not have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Utils.alert("Usage: /dg <lookup|rollback> <name> [page] [r:reason] [w:world]"));
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "lookup", "l":
                if (!sender.hasPermission("deathguard.user") || !sender.hasPermission("deathguard.admin")) {
                    sender.sendMessage(Utils.alert("You do not have permission to use this command."));
                    return true;
                }
                return handleLookup(sender, args);
            case "rollback":
                if (!sender.hasPermission("deathguard.admin")) {
                    sender.sendMessage(Utils.alert("You do not have permission to use this command."));
                    return true;
                }
                return handleRollback(sender, args);
            case "view":
                if (!sender.hasPermission("deathguard.user") || !sender.hasPermission("deathguard.admin")) {
                    sender.sendMessage(Utils.alert("You do not have permission to use this command."));
                    return true;
                }
                return handleView(sender, args);
            case "purge":
                if (!sender.hasPermission("deathguard.admin")) {
                    sender.sendMessage(Utils.alert("You do not have permission to use this command."));
                    return true;
                }
                return handlePurge(sender, args);
            case "purgeuser":
                if (!sender.hasPermission("deathguard.admin")) {
                    sender.sendMessage(Utils.alert("You do not have permission to use this command."));
                    return true;
                }
                return handlePurgeUser(sender, args);
            default:
                return false;
        }
    }

    private boolean handleLookup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.alert("Usage: /dg lookup <name> [page] [r:reason] [w:world]"));
            return true;
        }

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            sender.sendMessage(Utils.alert("Player not found. They are either offline or do not exist."));
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            sender.sendMessage(Utils.alert("No death data found for " + target.getName()));
            return true;
        }

        int page = 1;
        String reasonFilter = "";
        String worldFilter = "";

        for (int i = 2; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.matches("\\d+")) {
                page = Integer.parseInt(arg);
            } else if (arg.startsWith("r:")) {
                reasonFilter = arg.substring(2).trim();
            } else if (arg.startsWith("w:")) {
                worldFilter = arg.substring(2).trim();
            }
        }

        String[] entries = data.split("\\|");
        String finalWorldFilter = worldFilter;
        String finalReasonFilter = reasonFilter;
        entries = Arrays.stream(entries)
                .filter(entry -> finalReasonFilter.isEmpty() || Utils.dataSplitter(entry, "TYPE").equalsIgnoreCase(finalReasonFilter))
                .filter(entry -> finalWorldFilter.isEmpty() || Utils.dataSplitter(entry, "WORLD").equalsIgnoreCase(finalWorldFilter))
                .toArray(String[]::new);

        if (entries.length == 0) {
            String noEntriesMessage = Utils.alert("No death entries found");
            if (!reasonFilter.isEmpty()) {
                noEntriesMessage += " for reason: " + reasonFilter;
            }
            if (!worldFilter.isEmpty()) {
                noEntriesMessage += " in world: " + worldFilter;
            }
            sender.sendMessage(noEntriesMessage);
            return true;
        }

        Arrays.sort(entries, (a, b) -> Integer.compare(
                Integer.parseInt(Utils.dataSplitter(b, "ID")),
                Integer.parseInt(Utils.dataSplitter(a, "ID"))
        ));

        int totalPages = (int) Math.ceil((double) entries.length / MAX_PAGE);
        if (page > totalPages || page < 1) {
            sender.sendMessage(Utils.alert("Invalid page number. Please enter a number between 1 and " + totalPages));
            return true;
        }

        int startIndex = (page - 1) * MAX_PAGE;
        int endIndex = Math.min(startIndex + MAX_PAGE, entries.length);

        sender.sendMessage("----- " + ChatColor.DARK_PURPLE + "DeathGuard" + ChatColor.RESET + " ----- " + ChatColor.GRAY + "(" + target.getName() + ")");
        for (int i = startIndex; i < endIndex; i++) {
            String entry = entries[i];
            TextComponent dataMessage = Component.text()
                    .content("#" + Utils.dataSplitter(entry, "ID"))
                    .append(Component.text(": " + Utils.dataSplitter(entry, "TYPE")))
                    .append(Component.text(", " + Utils.simplifyTime(Utils.dataSplitter(entry, "TIME"))))
                    .append(Component.text(" (" + Utils.dataSplitter(entry, "LOC") + ")").color(NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand("/dg view " + target.getName() + " " + Utils.dataSplitter(entry, "ID")))
                    .hoverEvent(HoverEvent.showText(Component.text("View Saved Inventory #" + Utils.dataSplitter(entry, "ID"))))
                    .build();
            sender.sendMessage(dataMessage);
        }
        sender.sendMessage("-----");

        TextComponent pageString;

        if (page != 1) {
            pageString = Component.text()
                    .append(createPageControl("< ", page - 1, target.getName(), reasonFilter, worldFilter))
                    .build();
        } else {
            pageString = Component.text().build();
        }

        pageString = Component.text()
                .append(pageString)
                .append(Component.text("Page " + page + " of " + totalPages))
                .build();

        if (page != totalPages) {
            pageString = Component.text()
                    .append(pageString)
                    .append(createPageControl(" >", page + 1, target.getName(), reasonFilter, worldFilter))
                    .build();
        }

        sender.sendMessage(pageString);
        return true;
    }

    private TextComponent createPageControl(String label, int page, String name, String reasonFilter, String worldFilter) {
        return Component.text()
                .content(label)
                .clickEvent(ClickEvent.runCommand("/dg lookup " + name + " " + Math.max(1, page) + (reasonFilter.isEmpty() ? "" : " r:" + reasonFilter) + (worldFilter.isEmpty() ? "" : " w:" + worldFilter)))
                .hoverEvent(Component.text(label.equals("< ") ? "Previous Page" : "Next Page"))
                .build();
    }

    private boolean handleRollback(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.alert("Usage: /dg rollback <name> <reason #>"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Utils.alert("This command can only be used by a player."));
            return true;
        }

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            player.sendMessage(Utils.alert("Player not found. They are either offline or do not exist."));
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            player.sendMessage(Utils.alert(target.getName() + " has no data to rollback."));
            return true;
        }

        String id = args[2].trim();

        String[] entries = data.split("\\|");
        for (String entry : entries) {
            if (Utils.dataSplitter(entry, "ID").equals(id)) {
                Inventory targetInv = target.getInventory();
                ItemStack[] inventory = Utils.deserializeInventory(Utils.dataSplitter(entry, "INV"));
                
                targetInv.setContents(inventory);
                target.updateInventory();

                player.sendMessage(Utils.alert("Rolled back inventory for " + target.getName() + " to entry #" + id));
                return true;
            }
        }

        player.sendMessage(Utils.alert("No matching entry found for the provided ID."));
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.alert("Usage: /dg view <name> <entry#>"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Utils.alert("This command can only be used by a player."));
            return true;
        }

        String name = args[1].trim();
        Player target = plugin.getServer().getPlayer(name);

        if (target == null) {
            player.sendMessage(Utils.alert("Player not found. They are either offline or do not exist."));
            return true;
        }

        String targetUUID = target.getUniqueId().toString();
        String data = database.getPlayerData(targetUUID);

        if (data == null || data.isEmpty()) {
            player.sendMessage(Utils.alert(target.getName() + " has no data to view."));
            return true;
        }

        String id = args[2].trim();

        String[] entries = data.split("\\|");
        for (String entry : entries) {
            if (Utils.dataSplitter(entry, "ID").equals(id)) {
                Inventory shownInventory = Bukkit.createInventory(null, 45, target.getName() + ", #" + id);
                ItemStack[] inventory = Utils.deserializeInventory(Utils.dataSplitter(entry, "INV"));
                shownInventory.setContents(inventory);

                player.openInventory(shownInventory);

                TextComponent showingMessage = Component.text()
                        .append(Component.text(Utils.alert("Showing inventory for " + target.getName() + " from entry #" + id)))
                        .clickEvent(ClickEvent.runCommand("/dg view " + target.getName() + " " + Utils.dataSplitter(entry, "ID")))
                        .hoverEvent(HoverEvent.showText(Component.text("View Saved Inventory #" + Utils.dataSplitter(entry, "ID"))))
                        .build();
                player.sendMessage(showingMessage);
                return true;
            }
        }

        player.sendMessage(Utils.alert("No matching entry found for the provided ID."));
        return true;
    }

    private boolean handlePurge(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                pendingConfirmations.remove(player.getUniqueId().toString());

                pendingConfirmations.put(player.getUniqueId().toString(), "purge");
                sender.sendMessage(Utils.alert("Are you sure you want to purge all death data? Type '/dg purge confirm' to confirm."));
                return true;
            } else {
                sender.sendMessage(Utils.alert("Only players can perform this action."));
                return true;
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String confirmationType = pendingConfirmations.remove(player.getUniqueId().toString());
                if (confirmationType == null || !confirmationType.equals("purge")) {
                    sender.sendMessage(Utils.alert("No purge request found. Type '/dg purge' to initiate a purge."));
                    return true;
                }
                database.purgeAllData();
                sender.sendMessage(Utils.alert("All death data has been purged."));
                return true;
            } else {
                sender.sendMessage(Utils.alert("Only players can perform this action."));
                return true;
            }
        } else {
            sender.sendMessage(Utils.alert("Usage: /dg purge or /dg purge confirm"));
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
                    sender.sendMessage(Utils.alert("Player " + targetName + " is not online or does not exist."));
                    return true;
                }

                String targetUUID = target.getUniqueId().toString();

                pendingConfirmations.remove(player.getUniqueId().toString());

                pendingConfirmations.put(player.getUniqueId().toString(), "purgeuser " + targetUUID);
                sender.sendMessage(Utils.alert("Are you sure you want to purge death data for " + targetName + "? Type '/dg purgeuser " + targetName + " confirm' to confirm."));
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
                    sender.sendMessage(Utils.alert("No purgeuser request found. Type '/dg purgeuser <name>' to initiate a purge for a user."));
                    return true;
                }

                String expectedUUID = confirmationType.substring("purgeuser ".length());
                String providedName = args[1];

                Player target = plugin.getServer().getPlayer(providedName);
                if (target == null || !target.getUniqueId().toString().equals(expectedUUID)) {
                    sender.sendMessage(Utils.alert("The provided name does not match the original request or the player is not online. Please type '/dg purgeuser <name>' to initiate a new request."));
                    return true;
                }

                database.purgeUserData(expectedUUID);
                sender.sendMessage(Utils.alert("Death data for " + providedName + " has been purged."));
                return true;
            } else {
                sender.sendMessage(Utils.alert("Only players can perform this action."));
                return true;
            }
        } else {
            sender.sendMessage(Utils.alert("Usage: /dg purgeuser <name> or /dg purgeuser <name> confirm"));
            return true;
        }
    }
}