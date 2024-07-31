package morritools.deathGuard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathHandler implements Listener {

    private final Database database;

    public DeathHandler(Database database) {
        this.database = database;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();
        int deathId = database.getNextDeathId(uuidString);
        String serializedInventory = Utils.serializeInventory(player);
        long time = System.currentTimeMillis();
        String reason = Utils.simplifyReason(e.getDeathMessage());
        String location = Utils.simplifyLocation(player);
        String world = Utils.simplifyWorld(player);
        String data = String.format("%d.%s.%d.%s.%s.%s",
                deathId, serializedInventory, time, reason, location, world);

        database.insertPlayerData(uuidString, data);
    }
}
