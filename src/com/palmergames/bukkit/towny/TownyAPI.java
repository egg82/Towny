package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.db.TownyDatabase;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Towny's class for external API Methods
 * For more dynamic/controlled changing of Towny's behavior, for example Database, War, Permissions
 * The {@link TownyUniverse} class should be used. It contains the map of all objects
 * aswell as serving as an internal API, that Towny uses.
 * @author Lukas Mansour (Articdive)
 */
public class TownyAPI {
    private static TownyAPI instance;
    private final Towny towny;
    private final TownyUniverse townyUniverse;
    
    private TownyAPI() {
        towny = Towny.getPlugin();
        townyUniverse = TownyUniverse.getInstance();
    }
    
    /**
     * Gets the town spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the town spawn.
     * @return {@link Location} of the town spawn or if it is not obtainable null.
     */
    public Location getTownSpawnLocation(Player player) {
        try {
            Resident resident = townyUniverse.getDatabase().getResident(player.getName());
            Town town = resident.getTown();
            return town.getSpawn();
        } catch (TownyException x) {
            return null;
        }
    }
    
    /**
     * Gets the nation spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the nation spawn.
     * @return {@link Location} of the nation spawn or if it is not obtainable null.
     */
    public Location getNationSpawnLocation(Player player) {
        try {
            Resident resident = townyUniverse.getDatabase().getResident(player.getName());
            Nation nation = resident.getTown().getNation();
            return nation.getNationSpawn();
        } catch (TownyException x) {
            return null;
        }
    }
    
    
    /**
     * Find the the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the matching {@link Player}.
     * @return an online {@link Player} or if it's not obtainable.
     */
    public Player getPlayer(Resident resident) {
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null) {
                if (player.getName().equals(resident.getName())) {
                    return player;
                }
            }
        }
        return null;
    }
    
    /**
     * Find the {@link UUID} for the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the {@link UUID}.
     * @return an online {@link Player}'s {@link UUID} or null if it's not obtainable.
     */
    public UUID getPlayerUUID(Resident resident) {
        // TODO: Store UUIDs in the db, so we don't need to rely on the player being online.
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null) {
                if (player.getName().equals(resident.getName())) {
                    return player.getUniqueId();
                }
            }
        }
        return null;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link ResidentList}.
     *
     * @param residentList {@link ResidentList} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link ResidentList}.
     */
    public List<Player> getOnlinePlayers(ResidentList residentList) {
        ArrayList<Player> players = new ArrayList<>();
        
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null) {
                if (residentList.hasResident(player.getName())) {
                    players.add(player);
                }
            }
        }
        return players;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link Town}.
     *
     * @param town {@link Town} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Town}.
     */
    public List<Player> getOnlinePlayers(Town town) {
        ArrayList<Player> players = new ArrayList<>();
        
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null) {
                if (town.hasResident(player.getName())) {
                    players.add(player);
                }
            }
        }
        return players;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link Nation}.
     *
     * @param nation {@link Nation} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Nation}.
     */
    public List<Player> getOnlinePlayers(Nation nation) {
        ArrayList<Player> players = new ArrayList<>();
        
        for (Town town : nation.getTowns()) {
            players.addAll(getOnlinePlayers(town));
        }
        return players;
    }
    
    /**
     * Check if the specified {@link Block} is in the wilderness.
     *
     * @param block {@link Block} to test for.
     * @return {@link true} if the {@link Block} is in the wilderness, false otherwise.
     * @deprecated Use {@link #isWilderness(Location)} with block.getLocation()
     */
    @Deprecated
    public boolean isWilderness(Block block) {
        WorldCoord worldCoord;
        
        try {
            worldCoord = new WorldCoord(townyUniverse.getDatabase().getWorld(block.getWorld().getName()).getName(), Coord.parseCoord(block));
        } catch (NotRegisteredException e) {
            // No record so must be Wilderness
            return true;
        }
        
        try {
            return worldCoord.getTownBlock().getTown() == null;
        } catch (NotRegisteredException e) {
            // Must be wilderness
            return true;
        }
    }
    
    /**
     * Check if the specified {@link Location} is in the wilderness.
     *
     * @param location {@link Location} to test widlerness for.
     * @return {@link true} if the {@link Location} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(Location location) {
        WorldCoord worldCoord;
        
        try {
            worldCoord = new WorldCoord(townyUniverse.getDatabase().getWorld(location.getWorld().getName()).getName(), Coord.parseCoord(location));
        } catch (NotRegisteredException e) {
            // No record so must be Wilderness
            return true;
        }
        
        try {
            return worldCoord.getTownBlock().getTown() == null;
        } catch (NotRegisteredException e) {
            // Must be wilderness
            return true;
        }
    }
    
    /**
     * Get the name of a {@link Town} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} name for.
     * @return {@link String} containg the name of the {@link Town} at this location, or null for none.
     */
    public String getTownName(Location location) {
        try {
            WorldCoord worldCoord = new WorldCoord(townyUniverse.getDatabase().getWorld(location.getWorld().getName()).getName(), Coord.parseCoord(location));
            return worldCoord.getTownBlock().getTown().getName();
        } catch (NotRegisteredException e) {
            // No data so return null
            return null;
        }
    }
    
    
    /**
     * Get the {@link UUID} of a {@link Town} at the specified {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} {@link UUID} for.
     * @return {@link UUID} of any {@link Town} at this {@link Location}, or null for none.
     */
    public UUID getTownUUID(Location location) {
        try {
            WorldCoord worldCoord = new WorldCoord(townyUniverse.getDatabase().getWorld(location.getWorld().getName()).getName(), Coord.parseCoord(location));
            return worldCoord.getTownBlock().getTown().getUuid();
        } catch (NotRegisteredException e) {
            // No data so return null
            return null;
        }
    }
    
    /**
     * Get the {@link TownBlock} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link TownBlock} of.
     * @return {@link TownBlock} at this {@link Location}, or null for none.
     */
    public TownBlock getTownBlock(Location location) {
        try {
            WorldCoord worldCoord = new WorldCoord(townyUniverse.getDatabase().getWorld(location.getWorld().getName()).getName(), Coord.parseCoord(location));
            return worldCoord.getTownBlock();
        } catch (NotRegisteredException e) {
            // No data so return null
            return null;
        }
    }
    
    /**
     * Get a list of active {@link Resident}s.
     *
     * @return {@link List} of active {@link Resident}s.
     */
    public List<Resident> getActiveResidents() {
        List<Resident> activeResidents = new ArrayList<>();
        for (Resident resident : townyUniverse.getDatabase().getResidents()) {
            if (isActiveResident(resident)) {
                activeResidents.add(resident);
            }
        }
        return activeResidents;
    }
    
    /**
     * Check if the specified {@link Resident} is an active Resident.
     *
     * @param resident {@link Resident} to test for activity.
     * @return {@link true) if the player is active, false otherwise.
     */
    public boolean isActiveResident(Resident resident) {
        return ((System.currentTimeMillis() - resident.getLastOnline() < (20 * TownySettings.getInactiveAfter())) || (BukkitTools.isOnline(resident.getName())));
    }
    
    /**
     * Gets Towny's saving Database
     *
     * @return the {@link TownyDatabase}
     * @deprecated use {@link TownyUniverse#getInstance()#getDatabase()}
     */
    public TownyDatabase getDataSource() {
        return townyUniverse.getDatabase();
    }
    
    /**
     * Gets the {@link TownyPermissionSource} that is active.
     *
     * @return {@link TownyPermissionSource} that is in use.
     * @deprecated use {@link TownyUniverse#getInstance()#getPermissionSource()}
     */
    public TownyPermissionSource getPermissionSource() {
        return townyUniverse.getPermissionSource();
    }
    
    /**
     * Checks if server is currently in war-time.
     *
     * @return {@link true} if the server is in war-time.
     */
    public boolean isWarTime() {
        return townyUniverse.getWarEvent() != null && townyUniverse.getWarEvent().isWarTime();
    }
    
    /**
     * Check which {@link Resident}s are online in a {@link ResidentList}
     *
     * @param residentList {@link ResidentList} to check for online {@link Resident}s.
     * @return {@link List} of {@link Resident}s that are online.
     */
    public List<Resident> getOnlineResidents(ResidentList residentList) {
        
        List<Resident> onlineResidents = new ArrayList<>();
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null)
                for (Resident resident : residentList.getResidents()) {
                    if (resident.getName().equalsIgnoreCase(player.getName()))
                        onlineResidents.add(resident);
                }
        }
        return onlineResidents;
    }
    
    /**
     * Teleports the Player to the specified jail {@link Location}.
     *
     * @param player   {@link Player} to be teleported to jail.
     * @param location {@link Location} of the jail to be teleported to.
     */
    public void jailTeleport(final Player player, final Location location) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(towny, () -> player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN),
                TownySettings.getTeleportWarmupTime() * 20);
    }
    
    /**
     * Gets the {@link War} that is currently active
     
     * @return the currently active {@link War}, null if none is active.
     * @deprecated use {@link TownyUniverse#getInstance()#getWarEvent()}
     */
    public War getWarEvent() {
        return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getWarEvent();
    }
    
    public void clearWarEvent() {
        TownyUniverse townyUniverse = TownyUniverse.getInstance();
        townyUniverse.getWarEvent().cancelTasks(BukkitTools.getScheduler());
        townyUniverse.setWarEvent(null);
    }
    public void requestTeleport(Player player, Location spawnLoc) {
        
        try {
            TeleportWarmupTimerTask.requestTeleport(getDataSource().getResident(player.getName().toLowerCase()), spawnLoc);
        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }
    
    public void abortTeleportRequest(Resident resident) {
        
        TeleportWarmupTimerTask.abortTeleportRequest(resident);
    }
    
    public static TownyAPI getInstance() {
        if (instance == null) {
            instance = new TownyAPI();
        }
        return instance;
    }
}
