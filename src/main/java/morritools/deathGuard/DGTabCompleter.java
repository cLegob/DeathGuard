package morritools.deathGuard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DGTabCompleter implements TabCompleter {

    private final DeathGuard plugin;
    private final Database database;

    public DGTabCompleter(DeathGuard plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = List.of("lookup", "rollback", "purge", "purgeuser");
            StringUtil.copyPartialMatches(args[0], commands, suggestions);
        } else if (args.length == 2 && isCommandWithPlayerArg(args[0])) {
            List<String> playerNames = plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[1], playerNames, suggestions);
        } else if (args.length >= 3 && "lookup".equalsIgnoreCase(args[0])) {
            handleLookupTabCompletion(args, suggestions);
        }

        Collections.sort(suggestions);
        return suggestions;
    }

    private boolean isCommandWithPlayerArg(String command) {
        return List.of("lookup", "l", "view", "restore").contains(command.toLowerCase());
    }

    private void handleLookupTabCompletion(String[] args, List<String> suggestions) {
        Player target = plugin.getServer().getPlayer(args[1]);

        if (target == null) return;

        String data = database.getPlayerData(target.getUniqueId().toString());
        if (data == null || data.isEmpty()) return;

        Set<String> reasons = extractDataFromEntries(data, "TYPE");
        Set<String> worlds = extractDataFromEntries(data, "WORLD");

        String lastArg = args[args.length - 1];
        if (lastArg.startsWith("r:")) {
            suggestMatchingEntries(lastArg.substring(2).toLowerCase(), "r:", reasons, suggestions);
        } else if (lastArg.startsWith("w:")) {
            suggestMatchingEntries(lastArg.substring(2).toLowerCase(), "w:", worlds, suggestions);
        } else {
            suggestions.addAll(List.of("r:", "w:"));
        }
    }

    private Set<String> extractDataFromEntries(String data, String type) {
        return Stream.of(data.split("\\|"))
                .map(entry -> Utils.dataSplitter(entry, type))
                .collect(Collectors.toSet());
    }

    private void suggestMatchingEntries(String prefix, String label, Set<String> entries, List<String> suggestions) {
        suggestions.addAll(entries.stream()
                .filter(entry -> entry.toLowerCase().startsWith(prefix))
                .map(entry -> label + entry)
                .collect(Collectors.toList()));
    }
}
