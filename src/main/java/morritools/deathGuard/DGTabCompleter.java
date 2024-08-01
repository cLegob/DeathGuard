package morritools.deathGuard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        } else if (args.length == 2) {
            if ("lookup".equalsIgnoreCase(args[0])) {
                List<String> playerNames = plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], playerNames, suggestions);
            }
        } else if (args.length >= 3) {
            if ("lookup".equalsIgnoreCase(args[0])) {
                String name = args[1];
                Player target = plugin.getServer().getPlayer(name);

                if (target != null) {
                    String targetUUID = target.getUniqueId().toString();
                    String data = database.getPlayerData(targetUUID);

                    if (data != null && !data.isEmpty()) {
                        String[] entries = data.split("\\|");
                        Set<String> reasons = Stream.of(entries)
                                .map(entry -> Utils.dataSplitter(entry, "TYPE"))
                                .collect(Collectors.toSet());
                        Set<String> worlds = Stream.of(entries)
                                .map(entry -> Utils.dataSplitter(entry, "WORLD"))
                                .collect(Collectors.toSet());

                        String lastArg = args[args.length - 1];
                        if (lastArg.startsWith("r:")) {
                            String reasonPrefix = lastArg.substring(2).toLowerCase();
                            suggestions.addAll(reasons.stream()
                                    .filter(reason -> reason.toLowerCase().startsWith(reasonPrefix))
                                    .map(reason -> "r:" + reason)
                                    .collect(Collectors.toList()));
                        } else if (lastArg.startsWith("w:")) {
                            String worldPrefix = lastArg.substring(2).toLowerCase();
                            suggestions.addAll(worlds.stream()
                                    .filter(world -> world.toLowerCase().startsWith(worldPrefix))
                                    .map(world -> "w:" + world)
                                    .collect(Collectors.toList()));
                        } else {
                            suggestions.add("r:");
                            suggestions.add("w:");
                        }
                    }
                }
            }
        }

        Collections.sort(suggestions);
        return suggestions;
    }
}
