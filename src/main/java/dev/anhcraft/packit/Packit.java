package dev.anhcraft.packit;

import dev.anhcraft.craftkit.cb_common.nbt.CompoundTag;
import dev.anhcraft.craftkit.cb_common.nbt.ListTag;
import dev.anhcraft.craftkit.cb_common.nbt.NBTTag;
import dev.anhcraft.craftkit.chat.Chat;
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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Packit extends JavaPlugin implements CommandExecutor, Listener {
    private final Chat chat = new Chat("&b[Packit] ");
    private final Map<Player, ItemStack[]> INV = new WeakHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (sender instanceof Player) {
            if (sender.hasPermission("packit.admin")) {
                Player p = (Player) sender;
                if(args.length == 0){
                    Chat.noPrefix().message(sender, "&f/packit: show all commands")
                            .message(sender, "&f/packit pack: pack the item in hand")
                            .message(sender, "&f/packit unpack: unpack the item in hand")
                            .message(sender, "&f/packit editor: open the editor")
                            .message(sender, "&f/packit scan: scan the pack in hand")
                            .message(sender, "&aAliases: /pi, /pack");
                } else if(args[0].equals("pack")){
                    ItemStack hand = p.getInventory().getItemInMainHand();
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
                    } else chat.message(p, "&cPlease hold an item in your hand!");
                } else if(args[0].equals("unpack")){
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (!ItemUtil.isNull(hand)) {
                        CompoundTag nbt = CompoundTag.of(hand);
                        CompoundTag tag = nbt.get("tag", CompoundTag.class);
                        if(tag != null) {
                            ListTag pack = tag.get("packit", ListTag.class);
                            if(pack != null) {
                                unpack(p, pack);
                                chat.message(p, "&aUnpacked your item!");
                                return true;
                            }
                        }
                        chat.message(p, "&cThis item is not a package created by Packit!");
                    } else chat.message(p, "&cPlease hold an item in your hand!");
                } else if(args[0].equals("editor")){
                    ItemStack[] items = INV.get(p);
                    Inventory inv = getServer().createInventory(null, 54, "Packit Editor");
                    if(items != null) inv.setContents(items);
                    p.openInventory(inv);
                } else if(args[0].equals("scan")){
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (!ItemUtil.isNull(hand)) {
                        CompoundTag nbt = CompoundTag.of(hand);
                        CompoundTag tag = nbt.get("tag", CompoundTag.class);
                        if(tag != null) {
                            ListTag pack = tag.get("packit", ListTag.class);
                            if(pack != null) {
                                ItemStack[] items = scan(pack);
                                INV.put(p, items);
                                chat.message(p, "&aScanned successfully!");
                                chat.message(p, "&eThere are "+items.length+" items in total.");
                                chat.message(p, "&b/packit editor to view them.");
                                return true;
                            }
                        }
                        chat.message(p, "&cThis item is not a package created by Packit!");
                    } else chat.message(p, "&cPlease hold an item in your hand!");
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

    private ItemStack[] scan(ListTag pack) {
        List<NBTTag> items = pack.getValue();
        ItemStack[] itemStacks = new ItemStack[items.size()];
        int i = 0;
        for (NBTTag item : items) {
            itemStacks[i++] = ((CompoundTag) item).save(new ItemStack(Material.APPLE, 1));
        }
        return itemStacks;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void openGift(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != null && e.getHand().equals(EquipmentSlot.OFF_HAND)) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                && !ItemUtil.isNull(hand)) {
            CompoundTag nbt = CompoundTag.of(hand);
            CompoundTag tag = nbt.get("tag", CompoundTag.class);
            if (tag != null){
                ListTag pack = tag.get("packit", ListTag.class);
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
