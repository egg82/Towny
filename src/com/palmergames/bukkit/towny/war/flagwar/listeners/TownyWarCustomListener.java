package com.palmergames.bukkit.towny.war.flagwar.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.war.flagwar.CellUnderAttack;
import com.palmergames.bukkit.towny.war.flagwar.TownyWar;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import com.palmergames.bukkit.towny.war.flagwar.events.CellAttackCanceledEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellAttackEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellDefendedEvent;
import com.palmergames.bukkit.towny.war.flagwar.events.CellWonEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class TownyWarCustomListener implements Listener {

	private final Towny plugin;

	public TownyWarCustomListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCellAttackEvent(CellAttackEvent event) {
		if (event.isCancelled())
			return;

		try {
			CellUnderAttack cell = event.getData();
			TownyWar.registerAttack(cell);
		} catch (Exception e) {
			event.setCancelled(true);
			event.setReason(e.getMessage());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCellDefendedEvent(CellDefendedEvent event) {
		
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		CellUnderAttack cell = event.getCell().getAttackData();

		TownyUniverse universe = TownyUniverse.getInstance();

		WorldCoord worldCoord = new WorldCoord(cell.getWorldName(), cell.getX(), cell.getZ());
		universe.removeWarZone(worldCoord);

		plugin.updateCache(worldCoord);

		String playerName;
		if (player == null) {
			playerName = "Greater Forces";
		} else {
			playerName = player.getName();
			try {
				playerName = universe.getDatabase().getResident(player.getName()).getFormattedName();
			} catch (TownyException ignored) {
			}
		}

		plugin.getServer().broadcastMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_defended"), playerName, cell.getCellString()));

		// Defender Reward
		// It doesn't entirely matter if the attacker can pay.
		// Also doesn't take into account of paying as much as the attacker can afford (Eg: cost=10 and balance=9).
		if (TownySettings.isUsingEconomy()) {
			try {
				Resident attackingPlayer, defendingPlayer = null;
				attackingPlayer = universe.getDatabase().getResident(cell.getNameOfFlagOwner());
				if (player != null) {
					try {
						defendingPlayer = universe.getDatabase().getResident(player.getName());
					} catch (NotRegisteredException ignored) {
					}
				}

				String formattedMoney = TownyEconomyHandler.getFormattedBalance(TownyWarConfig.getDefendedAttackReward());
				if (defendingPlayer == null) {
					if (attackingPlayer.pay(TownyWarConfig.getDefendedAttackReward(), "War - Attack Was Defended (Greater Forces)"))
						try {
							TownyMessaging.sendResidentMessage(attackingPlayer, String.format(TownySettings.getLangString("msg_enemy_war_area_defended_greater_forces"), formattedMoney));
						} catch (TownyException ignored) {
						}
				} else {
					if (attackingPlayer.payTo(TownyWarConfig.getDefendedAttackReward(), defendingPlayer, "War - Attack Was Defended")) {
						try {
							TownyMessaging.sendResidentMessage(attackingPlayer, String.format(TownySettings.getLangString("msg_enemy_war_area_defended_attacker"), defendingPlayer.getFormattedName(), formattedMoney));
						} catch (TownyException ignored) {
						}
						try {
							TownyMessaging.sendResidentMessage(defendingPlayer, String.format(TownySettings.getLangString("msg_enemy_war_area_defended_defender"), attackingPlayer.getFormattedName(), formattedMoney));
						} catch (TownyException ignored) {
						}
					}
				}
			} catch (EconomyException | NotRegisteredException e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCellWonEvent(CellWonEvent event) {
		
		if (event.isCancelled())
			return;

		CellUnderAttack cell = event.getCellAttackData();

		TownyUniverse universe = TownyUniverse.getInstance();
		try {
			Resident attackingResident = universe.getDatabase().getResident(cell.getNameOfFlagOwner());
			Town attackingTown = attackingResident.getTown();
			Nation attackingNation = attackingTown.getNation();

			WorldCoord worldCoord = TownyWar.cellToWorldCoord(cell);
			universe.removeWarZone(worldCoord);

			TownBlock townBlock = worldCoord.getTownBlock();
			Town defendingTown = townBlock.getTown();

			// Payments
			double amount = 0;
			String moneyTranserMsg = null;
			if (TownySettings.isUsingEconomy()) {
				try {
					String reasonType;
					if (townBlock.isHomeBlock()) {
						amount = TownyWarConfig.getWonHomeblockReward();
						reasonType = "Homeblock";
					} else {
						amount = TownyWarConfig.getWonTownblockReward();
						reasonType = "Townblock";
					}

					if (amount > 0) {
						// Defending Town -> Attacker (Pillage)
						String reason = String.format("War - Won Enemy %s (Pillage)", reasonType);
						amount = Math.min(amount, defendingTown.getHoldingBalance());
						defendingTown.payTo(amount, attackingResident, reason);

						// Message
						moneyTranserMsg = String.format(TownySettings.getLangString("msg_enemy_war_area_won_pillage"), attackingResident.getFormattedName(), TownyEconomyHandler.getFormattedBalance(amount), defendingTown.getFormattedName());
					} else if (amount < 0) {
						// Attacker -> Defending Town (Rebuild cost)
						amount = -amount; // Inverse the amount so it's positive.
						String reason = String.format("War - Won Enemy %s (Rebuild Cost)", reasonType);
						if (!attackingResident.payTo(amount, defendingTown, reason)) {
							// Could Not Pay Defending Town the Rebuilding Cost.
							TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_won"), attackingResident.getFormattedName(), (attackingNation.hasTag() ? attackingNation.getTag() : attackingNation.getFormattedName()), cell.getCellString()));
						}

						// Message
						moneyTranserMsg = String.format(TownySettings.getLangString("msg_enemy_war_area_won_rebuilding"), attackingResident.getFormattedName(), TownyEconomyHandler.getFormattedBalance(amount), defendingTown.getFormattedName());
					}
				} catch (EconomyException x) {
					x.printStackTrace();
				}
			}

			// Defender loses townblock
			universe.getDatabase().removeTownBlock(townBlock);

			// Attacker Claim Automatically
			try {
				List<WorldCoord> selection = new ArrayList<>();
				selection.add(worldCoord);
				TownCommand.checkIfSelectionIsValid(attackingTown, selection, false, 0, false);
				new TownClaim(plugin, null, attackingTown, selection, false, true, false).start();
			} catch (TownyException te) {
				// Couldn't claim it.
			}

			// Cleanup
			plugin.updateCache(worldCoord);

			// Event Message
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_enemy_war_area_won"), attackingResident.getFormattedName(), (attackingNation.hasTag() ? attackingNation.getTag() : attackingNation.getFormattedName()), cell.getCellString()));

			// Money Transfer message.
			if (TownySettings.isUsingEconomy()) {
				if (amount != 0 && moneyTranserMsg != null) {
					try {
						TownyMessaging.sendResidentMessage(attackingResident, moneyTranserMsg);
					} catch (TownyException ignored) {
					}
					TownyMessaging.sendTownMessage(defendingTown, moneyTranserMsg);
				}
			}
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCellAttackCanceledEvent(CellAttackCanceledEvent event) {

		if (event.isCancelled())
			return;

		CellUnderAttack cell = event.getCell();

		TownyUniverse universe = TownyUniverse.getInstance();

		WorldCoord worldCoord = new WorldCoord(cell.getWorldName(), cell.getX(), cell.getZ());
		universe.removeWarZone(worldCoord);
		plugin.updateCache(worldCoord);

		System.out.println(cell.getCellString());
	}
}
