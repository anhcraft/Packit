package dev.anhcraft.packit;

import dev.anhcraft.craftkit.cb_common.kits.nbt.CompoundTag;
import dev.anhcraft.craftkit.cb_common.kits.nbt.ListTag;
import dev.anhcraft.craftkit.cb_common.kits.nbt.NBTTag;
import dev.anhcraft.craftkit.kits.chat.Chat;
import dev.anhcraft.craftkit.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Packit extends JavaPlugin implements CommandExecutor, Listener {
    private final Chat chat = new Chat("&e[Packit] ");
    private final Map<Player, ItemStack[]> INV = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (sender instanceof Player) {
            if (sender.hasPermission("packit.admin")) {
                Player p = (Player) sender;
                if(args.length == 0){
                    chat.message(sender, "&f/packit: show all commands");
                    chat.message(sender, "&f/packit pack: to pack the item you are holding");
                    chat.message(sender, "&f/packit unpack: to unpack the item you are holding without losing it");
                    chat.message(sender, "&f/packit editor: to open the editor");
                    chat.message(sender, "&aAliases: /pi, /pack");
                } else if(args[0].equals("pack")){
                    @NotNull ItemStack hand = p.getInventory().getItemInMainHand();
                    if (!ItemUtil.isNull(hand)) {
                        ItemStack[] items = INV.get(p);
                        if(items != null) {
                            CompoundTag nbt = CompoundTag.of(hand);
                            CompoundTag tag = nbt.getOrCreateDefault("tag", CompoundTag.class);
                            ListTag pack = new ListTag();
                            for(ItemStack item : items){
                                if(ItemUtil.isNull(item)) continue;
                                pack.getValue().add(CompoundTag.of(item));
                            }
                            tag.put("packit", pack);
                            nbt.put("tag", tag);
                            p.getInventory().setItemInMainHand(nbt.save(hand));
                            chat.message(p, "&aYour package is now ready! Right-click to open it xD");
                        } else chat.message(p, "&cPlease put your items first! Use the command /packit editor");
                    } else chat.message(p, "&cPlease hold your item!");
                } else if(args[0].equals("unpack")){
                    @NotNull ItemStack hand = p.getInventory().getItemInMainHand();
                    if (!ItemUtil.isNull(hand)) {
                        CompoundTag nbt = CompoundTag.of(hand);
                        @Nullable CompoundTag tag = nbt.get("tag", CompoundTag.class);
                        if(tag != null) {
                            @Nullable ListTag pack = tag.get("packit", ListTag.class);
                            if(pack != null) {
                                unpack(p, pack);
                                chat.message(p, "&aUnpacked your item!");
                                return true;
                            }
                        }
                        chat.message(p, "&cThis item is not a package created by Packit!");
                    } else chat.message(p, "&cPlease hold your item!");
                } else if(args[0].equals("editor")){
                    ItemStack[] items = INV.get(p);
                    @NotNull Inventory inv = getServer().createInventory(null, 54, "Packit Editor");
                    if(items != null) inv.setContents(items);
                    p.openInventory(inv);
                }
            } else chat.message(sender, "&cYou do not have permission to execute the command!");
        } else chat.message(sender, "&cThis command only for in-game players");
        return true;
    }

    private void unpack(Player p, ListTag pack) {
        List<NBTTag> items = pack.getValue();
        for (NBTTag item : items) {
            ItemStack x = ((CompoundTag) item).save(new ItemStack(Material.APPLE, 1));
            if (p.getInventory().firstEmpty() == -1)
                p.getWorld().dropItemNaturally(p.getLocation(), x);
            else p.getInventory().addItem(x);
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void openGift(PlayerInteractEvent e) {
        @NotNull Player p = e.getPlayer();
        if (e.getHand() != null && e.getHand().equals(EquipmentSlot.OFF_HAND)) return;

        @NotNull ItemStack hand = p.getInventory().getItemInMainHand();
        if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                && !ItemUtil.isNull(hand)) {
            CompoundTag nbt = CompoundTag.of(hand);
            @Nullable CompoundTag tag = nbt.get("tag", CompoundTag.class);
            if (tag != null){
                @Nullable ListTag pack = tag.get("packit", ListTag.class);
                if(pack != null) {
                    e.setCancelled(true);
                    if (p.hasPermission("packit.use")) {
                        hand.setAmount(hand.getAmount() - 1);
                        p.getInventory().setItemInMainHand(hand);
                        unpack(p, pack);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5f, 1f);
                    } else chat.message(p, "&cYou do not have permission to open this package!");
                }
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player && e.getView().getTitle().equals("Packit Editor")) {
            INV.put((Player) e.getPlayer(), e.getInventory().getContents());
        }
    }
}
