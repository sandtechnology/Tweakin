package com.github.sachin.tweakin.betterarmorstands;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.github.sachin.tweakin.BaseTweak;
import com.github.sachin.tweakin.Message;
import com.github.sachin.tweakin.Tweakin;
import com.github.sachin.tweakin.utils.TConstants;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import de.jeff_media.morepersistentdatatypes.DataType;

// tweakin.betterarmorstands.armorswap,tweakin.betterarmorstands.uuidlockbypass,tweakin.betterarmorstands.command
public class BetterArmorStandTweak extends BaseTweak implements Listener{

    private ArmorStandCommand command;
    private Message messageManager;

    public BetterArmorStandTweak(Tweakin plugin) {
        super(plugin, "better-armorstands");
        this.command = new ArmorStandCommand(this);
        this.messageManager = plugin.getTweakManager().getMessageManager();
    }

    @Override
    public void register() {
        super.register();
        registerCommands(command);
    }


    @Override
    public void unregister() {
        super.unregister();
        unregisterCommands(command);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandSpawn(EntitySpawnEvent e){
        if(e.isCancelled() || !getConfig().getBoolean("spawn-with-arms")) return;
        if(e.getEntity() instanceof ArmorStand){
            ArmorStand as = (ArmorStand) e.getEntity();
            if(getBlackListWorlds().contains(as.getWorld().getName())) return;
            as.setArms(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorstandDeath(EntityDamageEvent e){
        if(e.getEntity() instanceof ArmorStand){
            ArmorStand as = (ArmorStand) e.getEntity();
            
            if(as.getPersistentDataContainer().has(TConstants.ARMORSTAND_EDITED, PersistentDataType.INTEGER) || as.getPersistentDataContainer().has(TConstants.UUID_LOCK_KEY, DataType.UUID)){
                e.setCancelled(true);
            } 
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent e){
        
        Player player = e.getPlayer();
        if(getBlackListWorlds().contains(player.getWorld().getName())) return;
        if(e.getRightClicked() instanceof ArmorStand){
            ArmorStand as = (ArmorStand) e.getRightClicked();
            if(as.getPersistentDataContainer().has(TConstants.UUID_LOCK_KEY, DataType.UUID) && !player.hasPermission("tweakin.betterarmorstands.uuidlockbypass")){
                UUID uuid = as.getPersistentDataContainer().get(TConstants.UUID_LOCK_KEY,DataType.UUID);
                if(!uuid.equals(player.getUniqueId())){
                    
                    player.sendMessage(messageManager.getMessage("armorstand-locked").replace("%player%", Bukkit.getOfflinePlayer(uuid).getName()));
                    e.setCancelled(true);
                    return;
                }
            }
            if(as.getPersistentDataContainer().has(TConstants.ARMORSTAND_EDITED, PersistentDataType.INTEGER)){
                player.sendMessage(messageManager.getMessage("armorstand-edited"));
                e.setCancelled(true);
                return;
            }
            if(e.isCancelled()) return;
            if(e.getHand()==EquipmentSlot.HAND && player.isSneaking() && getConfig().getBoolean("armor-swap") && player.hasPermission("tweakin.betterarmorstands.armorswap")){
                e.setCancelled(true);
                for(EquipmentSlot slot : EquipmentSlot.values()){
                    // swapArmor(as, player, slot);
                
                    ItemStack asItem = as.getEquipment().getItem(slot);
                    ItemStack playerItem = player.getEquipment().getItem(slot);
                    as.getEquipment().setItem(slot, playerItem);
                    player.getEquipment().setItem(slot, asItem);
                    
                }
            }

        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if(e.getClickedInventory() == null) return;
        if(e.getClickedInventory().getHolder() instanceof ASGuiHolder){
            ASGuiHolder holder = (ASGuiHolder) e.getClickedInventory().getHolder();
            Inventory inv = holder.getInventory();
            ArmorStand as = holder.armorStand;
            ItemStack cItem = e.getCurrentItem();
            if(!Arrays.asList(1,9,10,11,19,28).contains(e.getSlot()) || e.isShiftClick()){
                e.setCancelled(true);
            }
            if(cItem != null){
                GuiItems gItem = GuiItems.getGuiItem(cItem);
                if(gItem != null)
                    gItem.handleClick(e, as,e.getClick(),inv,e.getSlot(),as.getLocation(),0.0,null);
            }

            new BukkitRunnable(){
                public void run() {
                    int slot = e.getSlot();
                    ItemStack item = inv.getItem(slot);
                    if(slot == 1)
                        as.getEquipment().setHelmet(item);
                    else if(slot == 10)
                        as.getEquipment().setChestplate(item);
                    else if(slot == 19)
                        as.getEquipment().setLeggings(item);
                    else if(slot == 28)
                        as.getEquipment().setBoots(item);
                    else if(slot ==9)
                        as.getEquipment().setItemInMainHand(item);
                    else if(slot == 11)
                        as.getEquipment().setItemInOffHand(item);        
                    
                };
            }.runTaskLater(plugin, 1);    
            
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e){
        if(e.getInventory().getHolder() instanceof ASGuiHolder){
            ASGuiHolder gui = (ASGuiHolder) e.getInventory().getHolder();
            gui.armorStand.getPersistentDataContainer().remove(TConstants.ARMORSTAND_EDITED);
        }
    }



    
}
