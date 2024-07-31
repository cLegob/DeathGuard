package morritools.deathGuard;

import org.bukkit.plugin.java.JavaPlugin;

public final class DeathGuard extends JavaPlugin {

    private Database database;

    @Override
    public void onEnable() {
        database = new Database(this);
        database.connect();
        database.createTable();
        getServer().getPluginManager().registerEvents(new DeathHandler(database), this);

    }

    @Override
    public void onDisable() {
        database.disconnect();
    }
}
