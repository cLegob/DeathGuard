package morritools.deathGuard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;


public class DGListener implements Listener {

    private final Database database;

    public DGListener(Database database) {
        this.database = database;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        final Player player = e.getPlayer();

        String data = String.format("%d.%s.%d.%s.%s.%s",
                database.getNextDeathId(player.getUniqueId().toString()),
                Utils.serializeInventory(player),
                System.currentTimeMillis(),
                Utils.simplifyReason(e.getDeathMessage() != null ? e.getDeathMessage() : "", player.getName()),
                Utils.simplifyLocation(player.getLocation()),
                Utils.simplifyWorld(player));

        database.insertPlayerData(player.getUniqueId().toString(), data);
    }
}
