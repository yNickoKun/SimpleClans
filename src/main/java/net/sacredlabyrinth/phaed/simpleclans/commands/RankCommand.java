package net.sacredlabyrinth.phaed.simpleclans.commands;

import java.text.MessageFormat;
import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.RankPermission;
import net.sacredlabyrinth.phaed.simpleclans.Rank;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.simpleclans.uuid.UUIDMigration;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;

/**
*
* @author RoinujNosde
*/
public class RankCommand {
	private SimpleClans plugin;
	private Player player;
	private ClanPlayer clanPlayer;
	private Clan clan;
	
	/**
     * Execute the command
     *
     * @param player
     * @param args
     */
	public void execute(Player player, String[] args) {
		this.player = player;
		plugin = SimpleClans.getInstance();
		ClanManager clanManager = plugin.getClanManager();
		
		clanPlayer = clanManager.getClanPlayer(player);
		if (clanPlayer == null) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("not.a.member.of.any.clan"));
            return;
        }

        clan = clanPlayer.getClan();
        if (!clan.isVerified()) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("clan.is.not.verified"));
            return;
        }
        
        if (!clanPlayer.isLeader()) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("no.leader.permissions"));
            return;
        }
		
		if (args.length > 0) {
			String subCommand = args[0].toLowerCase();
			args = Helper.removeFirst(args);
			
			switch (subCommand) {
				case "assign":
					assignRank(args);
					return;
				case "unassign":
					unassign(args);
					return;
				case "create":
					createRank(args);
					return;
				case "list":
					listRanks();
					return;
				case "delete":
					deleteRank(args);
					return;
				case "permissions":
					permissions(args);
					return;
				case "setdisplayname":
					setDisplayName(args);
					return;
			}
		}
		
        ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank"),
        		plugin.getSettingsManager().getCommandClan()));
	}
	
	private void assignRank(String[] args) {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.assign")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		if (args.length != 2) {
            ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.assign"),
            		plugin.getSettingsManager().getCommandClan()));
			return;
		}
		UUID uuid = UUIDMigration.getForcedPlayerUUID(args[0]);
        if (uuid == null || (!clan.isMember(uuid) && !clan.isLeader(uuid))) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("no.player.matched"));
            return;
        }
        
        String rank = args[1].toLowerCase();
		if (!clan.hasRank(rank)) {
			ChatBlock.sendMessage(player, ChatColor.RED + lang("rank.0.does.not.exist"));
			return;
		}
        
        ClanPlayer cpTarget = Objects.requireNonNull(plugin.getClanManager().getClanPlayer(uuid));
		if (cpTarget.getRankId().equals(rank)) {
			ChatBlock.sendMessage(player, lang("player.already.has.that.rank"));
			return;
		}
		cpTarget.setRank(rank);
        plugin.getStorageManager().updateClanPlayer(cpTarget);
        ChatBlock.sendMessage(player, ChatColor.AQUA + lang("player.rank.changed"));
	}
	
	private void unassign(String[] args) {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.unassign")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		if (args.length != 1) {
			ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.unassign"),
            		plugin.getSettingsManager().getCommandClan()));
			return;
		}
		
		UUID uuid = UUIDMigration.getForcedPlayerUUID(args[0]);
        if (uuid == null || (!clan.isMember(uuid) && !clan.isLeader(uuid))) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("no.player.matched"));
            return;
        }
        
        ClanPlayer cpTarget = plugin.getClanManager().getClanPlayer(uuid);
		Objects.requireNonNull(cpTarget).setRank(null);
        plugin.getStorageManager().updateClanPlayer(cpTarget);
        ChatBlock.sendMessage(player, ChatColor.AQUA + lang("player.unassigned.from.rank"));
	}

	private void createRank(String[] args) {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.create")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		if (args.length != 1) {
			ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.create"),
            		plugin.getSettingsManager().getCommandClan()));
			return;
		}
		
		String rank = args[0].toLowerCase();
		if (clan.hasRank(rank)) {
			ChatBlock.sendMessage(player, ChatColor.RED + lang("rank.already.exists"));
			return;
		}
		
		clan.createRank(rank);
		plugin.getStorageManager().updateClan(clan, true);
		ChatBlock.sendMessage(player, ChatColor.AQUA + lang("rank.created"));
	}

	private void listRanks() {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.list")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		List<Rank> ranks = clan.getRanks();
		
		if (ranks.isEmpty()) {
			ChatBlock.sendMessage(player, ChatColor.RED + lang("no.ranks"));
			return;
		}
		
		ranks.sort(Comparator.reverseOrder());
		ChatBlock.sendMessage(player, ChatColor.AQUA + lang("clans.ranks"));
		int count = 1;
		for (Rank rank : ranks) {
			ChatBlock.sendMessage(player, ChatColor.AQUA + MessageFormat.format(lang("ranks.list.item"), count, Helper.parseColors(rank.getDisplayName()) + ChatColor.AQUA, rank.getName()));
			count++;
		}
	}

	private void deleteRank(String[] args) {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.delete")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		if (args.length != 1) {
            ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.delete"),
            		plugin.getSettingsManager().getCommandClan()));
			return;
		}
		String rank = args[0].toLowerCase();
		if (!clan.hasRank(rank)) {
			ChatBlock.sendMessage(player, ChatColor.RED + lang("rank.0.does.not.exist"));
			return;
		}
		clan.deleteRank(rank);
		plugin.getStorageManager().updateClan(clan, true);
		ChatBlock.sendMessage(player, ChatColor.AQUA + MessageFormat.format(lang("rank.0.deleted"), rank));
	}
	
	private void setDisplayName(String[] args) {
		if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.setdisplayname")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
            return;
		}
		if (args.length < 2) {
            ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.setdisplayname"),
            		plugin.getSettingsManager().getCommandClan()));
            return;
		}
		
		String rankName = args[0].toLowerCase();
		if (!clan.hasRank(rankName)) {
			ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("rank.0.does.not.exist"), rankName));
			return;
		}
		Rank rank = clan.getRank(rankName);
		String dn = Helper.toMessage(Helper.removeFirst(args));
		if (dn.contains("&") && !plugin.getPermissionsManager().has(player, "simpleclans.leader.coloredrank")) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("you.cannot.set.colored.ranks"));
        	return;
        }
		rank.setDisplayName(dn);
		plugin.getStorageManager().updateClan(clan, true);
		ChatBlock.sendMessage(player, ChatColor.AQUA + lang("rank.displayname.updated"));
	}

	private void permissions(String[] args) {
		String validPermissionsToMessage = Helper.toMessage(Helper.fromPermissionArray(), ",");
		if (args.length == 0) {
			if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.permissions.available")) {
	            ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
	            return;
			}
			
			ChatBlock.sendMessage(player, ChatColor.AQUA + lang("available.rank.permissions"));
			ChatBlock.sendMessage(player, ChatColor.AQUA + validPermissionsToMessage);
			return;
		}
		String rank = args[0].toLowerCase();
		if (!clan.hasRank(rank)) {
			ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("rank.0.does.not.exist"), rank));
			return;
		}
		Set<String> permissions = clan.getRank(rank).getPermissions();

		if (args.length == 1) {
			if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.permissions.list")) {
				ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
				return;
			}
			if (permissions.isEmpty()) {
				ChatBlock.sendMessage(player, ChatColor.RED + lang("rank.no.permissions"));
				return;
			}
			ChatBlock.sendMessage(player, ChatColor.AQUA + MessageFormat.format(lang("rank.0.permissions"), rank));
			ChatBlock.sendMessage(player, ChatColor.AQUA + Helper.toMessage(permissions.toArray(new String[0]), ","));
			return;
		}
		if (args.length == 3) {
			String subCommand = args[1].toLowerCase();
			String permission = args[2].toLowerCase();

			boolean changed = false;

			switch (subCommand) {
				case "add":
					if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.permissions.add")) {
						ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
						return;
					}
					if (!RankPermission.isValid(permission)) {
						ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("invalid.permission"), permission, validPermissionsToMessage));
						return;
					}
					permissions.add(permission);
					ChatBlock.sendMessage(player, ChatColor.AQUA + MessageFormat.format(lang("permission.0.added.to.rank.1"), permission, rank));
					changed = true;
					break;
				case "remove":
					if (!plugin.getPermissionsManager().has(player, "simpleclans.leader.rank.permissions.remove")) {
						ChatBlock.sendMessage(player, ChatColor.RED + lang("insufficient.permissions"));
						return;
					}
					permissions.remove(permission);
					ChatBlock.sendMessage(player, ChatColor.AQUA + MessageFormat.format(lang("permission.0.removed.from.rank.1"), permission, rank));
					changed = true;
					break;
			}

			if (changed) {
				plugin.getStorageManager().updateClan(clan, true);
				return;
			}
		}
		ChatBlock.sendMessage(player, ChatColor.RED + MessageFormat.format(lang("usage.0.rank.permissions"),
        		plugin.getSettingsManager().getCommandClan()));
	}
}
