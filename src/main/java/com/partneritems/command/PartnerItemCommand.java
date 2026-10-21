package com.partneritems.command;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ItemUseService;
import com.partneritems.item.PartnerItem;
import com.partneritems.restriction.Restriction;
import com.partneritems.util.Messages;
import com.partneritems.util.Text;
import com.partneritems.util.TimeFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class PartnerItemCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList("give", "list", "cooldowns", "resetcd", "reload");
    private static final String ADMIN_PERMISSION = "partneritems.admin";

    private final PartnerItemsPlugin plugin;
    private final Messages messages;

    public PartnerItemCommand(PartnerItemsPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase();

        // players can always check their own cooldowns
        if (sub.equals("cooldowns") && args.length == 1) {
            if (!(sender instanceof Player) || !sender.hasPermission("partneritems.cooldowns")) {
                messages.send(sender, "no-permission");
                return true;
            }
            showCooldowns(sender, (Player) sender);
            return true;
        }

        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "give":
                give(sender, args);
                break;
            case "list":
                list(sender);
                break;
            case "cooldowns":
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    messages.send(sender, "player-not-found");
                } else {
                    showCooldowns(sender, target);
                }
                break;
            case "resetcd":
                resetCooldowns(sender, args);
                break;
            case "reload":
                plugin.reloadSettings();
                messages.send(sender, "reloaded");
                break;
            default:
                messages.send(sender, "command-usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "command-usage");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                amount = -1;
            }
            if (amount < 1 || amount > 64) {
                messages.send(sender, "invalid-amount");
                return;
            }
        }

        List<PartnerItem> toGive = new ArrayList<>();
        if (args[2].equalsIgnoreCase("all")) {
            for (PartnerItem item : plugin.getRegistry().all()) {
                if (item.isEnabled()) {
                    toGive.add(item);
                }
            }
        } else {
            PartnerItem item = plugin.getRegistry().get(args[2]);
            if (item == null || !item.isEnabled()) {
                messages.send(sender, "unknown-item");
                return;
            }
            toGive.add(item);
        }

        for (PartnerItem item : toGive) {
            // anything that doesn't fit gets dropped at their feet instead of disappearing
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(item.createItem(amount));
            for (ItemStack stack : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
            }
            messages.send(sender, "given", "amount", amount, "item", item.getDisplayName(), "player", target.getName());
        }
    }

    private void list(CommandSender sender) {
        StringJoiner joiner = new StringJoiner("&7, ");
        for (PartnerItem item : plugin.getRegistry().all()) {
            joiner.add((item.isEnabled() ? "&a" : "&c") + item.getId());
        }
        messages.send(sender, "list-header", "items", Text.color(joiner.toString()));
    }

    private void showCooldowns(CommandSender sender, Player target) {
        Map<String, Long> active = plugin.getCooldowns().getActive(target.getUniqueId());
        if (active.isEmpty()) {
            messages.send(sender, "cooldowns-none");
            return;
        }

        messages.send(sender, "cooldowns-header", "player", target.getName());

        Long global = active.get(ItemUseService.GLOBAL_KEY);
        if (global != null) {
            messages.send(sender, "cooldowns-entry", "item", "Global", "time", TimeFormat.format(global));
        }
        for (PartnerItem item : plugin.getRegistry().all()) {
            Long remaining = active.get(item.getCooldownKey());
            if (remaining != null) {
                messages.send(sender, "cooldowns-entry", "item", item.getDisplayName(), "time", TimeFormat.format(remaining));
            }
        }
        for (Restriction restriction : Restriction.values()) {
            Long remaining = active.get(restriction.key());
            if (remaining != null) {
                messages.send(sender, "cooldowns-entry", "item", restriction.name(), "time", TimeFormat.format(remaining));
            }
        }
    }

    private void resetCooldowns(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command-usage");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return;
        }

        plugin.getCooldowns().clearAll(target.getUniqueId());
        messages.send(sender, "cooldowns-reset", "player", target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return args.length == 1 ? filter(Collections.singletonList("cooldowns"), args[0]) : Collections.<String>emptyList();
        }

        if (args.length == 1) {
            return filter(ADMIN_SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2 && (sub.equals("give") || sub.equals("cooldowns") || sub.equals("resetcd"))) {
            List<String> names = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }

        if (args.length == 3 && sub.equals("give")) {
            List<String> ids = new ArrayList<>();
            ids.add("all");
            for (PartnerItem item : plugin.getRegistry().all()) {
                ids.add(item.getId());
            }
            return filter(ids, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
