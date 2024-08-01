package morritools.deathGuard;

import org.bukkit.plugin.java.JavaPlugin;

public final class DeathGuard extends JavaPlugin {

    private Database database;

    @Override
    public void onEnable() {
        database = new Database(this);
        database.connect();
        database.createTable();
        getServer().getPluginManager().registerEvents(new DGListener(database), this);
        this.getCommand("deathguard").setExecutor(new DGCommandExecutor(this, database));
        getCommand("deathguard").setTabCompleter(new DGTabCompleter(this, database));
    }

    @Override
    public void onDisable() {
        database.disconnect();
    }
}
