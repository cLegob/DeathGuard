Here's a comprehensive documentation on how to use the DeathGuard plugin, based on the provided `DGCommandExecutor` class:

---

## DeathGuard Plugin Documentation

### Overview
The DeathGuard plugin allows players and admins to manage and restore player inventories from previous deaths. It provides several commands for viewing, restoring, and purging death data.

### Commands

#### `/dg lookup <name> [page] [r:reason] [w:world]`
- **Usage**: `/dg lookup <name> [page] [r:reason] [w:world]`
- **Description**: Retrieves and displays death entries for a specified player. You can filter results by reason (`r:reason`) and world (`w:world`), and paginate through results.
- **Arguments**:
  - `<name>`: The name of the player whose death data you want to view.
  - `[page]`: (Optional) Page number for pagination. Default is 1.
  - `[r:reason]`: (Optional) Filter results by the reason of death.
  - `[w:world]`: (Optional) Filter results by the world in which the death occurred.
- **Permissions**: `deathguard.user` or `deathguard.admin`

#### `/dg rollback <name> <reason#>`
- **Usage**: `/dg rollback <name> <reason#>`
- **Description**: Rolls back the inventory of a specified player to a previous state based on the death entry ID.
- **Arguments**:
  - `<name>`: The name of the player whose inventory you want to restore.
  - `<reason#>`: The ID of the death entry you want to roll back to.
- **Permissions**: `deathguard.admin`
- **Note**: This command can only be used by players.

#### `/dg view <name> <reason#>`
- **Usage**: `/dg view <name> <reason#>`
- **Description**: Opens an inventory view for a specified player from a specific death entry.
- **Arguments**:
  - `<name>`: The name of the player whose inventory you want to view.
  - `<reason#>`: The ID of the death entry to view.
- **Permissions**: `deathguard.user` or `deathguard.admin`
- **Note**: This command can only be used by players.

#### `/dg purge`
- **Usage**: `/dg purge` or `/dg purge confirm`
- **Description**: Purges all death data from the database. Requires confirmation.
- **Arguments**:
  - `confirm`: Confirms the purge request.
- **Permissions**: `deathguard.admin`
- **Note**: Only players can perform this action. Initiates a confirmation request that must be confirmed with `/dg purge confirm`.

#### `/dg purgeuser <name>`
- **Usage**: `/dg purgeuser <name>` or `/dg purgeuser <name> confirm`
- **Description**: Purges death data for a specific player. Requires confirmation.
- **Arguments**:
  - `<name>`: The name of the player whose death data you want to purge.
  - `confirm`: Confirms the purge request.
- **Permissions**: `deathguard.admin`
- **Note**: Only players can perform this action. Initiates a confirmation request that must be confirmed with `/dg purgeuser <name> confirm`.

### Permissions
- **`deathguard.user`**: Allows usage of `lookup`, `view`, and `rollback` commands.
- **`deathguard.admin`**: Allows usage of all commands, including `purge` and `purgeuser`.

### Example Usages

1. **Lookup Player Death Data**:
   ```
   /dg lookup Steve
   ```
   Retrieves the death data for the player "Steve".

2. **Lookup Player Death Data with Pagination and Filters**:
   ```
   /dg lookup Steve 2 r:lava w:overworld
   ```
   Retrieves the second page of death data for "Steve", filtered by deaths caused by lava in the Overworld.

3. **Rollback Inventory**:
   ```
   /dg rollback Steve 5
   ```
   Rolls back the inventory of "Steve" to the state it was in at death entry #5.

4. **View Inventory from a Death Entry**:
   ```
   /dg view Steve 5
   ```
   Opens an inventory view for "Steve" from death entry #5.

5. **Purge All Death Data**:
   ```
   /dg purge
   ```
   Initiates a confirmation request to purge all death data.

6. **Purge Death Data for a Specific Player**:
   ```
   /dg purgeuser Steve
   ```
   Initiates a confirmation request to purge death data for "Steve".

### Notes
- The plugin uses a confirmation system for critical actions like purging data to prevent accidental data loss.
- Commands may not work if the specified player is offline or does not exist.

For more detailed information or troubleshooting, consult the plugin’s documentation or support channels.

--- 

Feel free to adjust or expand this documentation as needed for your plugin!
