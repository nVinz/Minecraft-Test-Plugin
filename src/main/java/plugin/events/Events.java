package plugin.events;

import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntity;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.springframework.context.ApplicationContext;
import plugin.utils.DBUtils;
import plugin.utils.EntityUtils;
import plugin.utils.StringUtils;

public class Events implements Listener {

    private EntityUtils entityUtils;
    private StringUtils stringUtils;
    private DBUtils dbUtils;
    private ProtocolManager protocolManager;

    private static String trophyName ="'s trophy";

    public Events(ApplicationContext context) {
        this.entityUtils = context.getBean(EntityUtils.class);
        this.stringUtils = context.getBean(StringUtils.class);
        this.dbUtils = context.getBean(DBUtils.class);
        this.protocolManager = context.getBean(ProtocolManager.class);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();

        // Отмена спавна энтити для пакетов
        if (entity.getType().equals(EntityType.ARMOR_STAND)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();

        if (item.getItemStack().getType().equals(Material.LEATHER)) {
            if (item.getCustomName().contains(trophyName)) {
                String playerName = item.getCustomName().substring(0, item.getCustomName().indexOf(trophyName));

                WrapperPlayServerEntityDestroy destroyEntity = new WrapperPlayServerEntityDestroy();
                destroyEntity.setEntityIds(entityUtils.getArmorStandsByName(playerName));

                // Пакеты для удаления энтити
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    destroyEntity.sendPacket(onlinePlayer);
                }

                entityUtils.removeArmorStands(playerName);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            Ocelot ocelot = (Ocelot) zombie.getWorld().spawnEntity(zombie.getLocation(), EntityType.OCELOT);

            String name = stringUtils.generateRandomString(5);
            ocelot.setCustomName(name);
            entityUtils.addOcelotName(name);

            ocelot.setTarget(zombie.getKiller());
        }
        else if (entity instanceof Ocelot) {
            if (entityUtils.getOcelotNames().contains(entity.getCustomName())) {
                entityUtils.removeOcelotName(entity.getCustomName());

                Player player = ((Ocelot) entity).getKiller();

                // Дроп кожи
                ItemStack leather = new ItemStack(Material.LEATHER);
                ItemMeta leatherMeta = leather.getItemMeta();
                leatherMeta.setDisplayName(player.getName() + trophyName);
                leather.setItemMeta(leatherMeta);
                event.getDrops().clear();
                event.getDrops().add(leather);

                // Энтити для пакета
                Location standLocation = new Location(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY()-1, entity.getLocation().getZ());
                ArmorStand stand = (ArmorStand) entity.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.setCustomNameVisible(true);
                //stand.setInvisible(true);
                stand.setGravity(false);
                stand.setGlowing(true);
                entityUtils.addArmorStand(player.getName(), stand.getEntityId());

                // Рассылка пакетов
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    stand.setCustomName(onlinePlayer.getName());
                    WrapperPlayServerSpawnEntity spawnEntity = new WrapperPlayServerSpawnEntity(stand, 0, 0);
                    spawnEntity.sendPacket(onlinePlayer);
                }

                // Сохранение в базу
                dbUtils.saveKill("kills", player.getName(), entity.getName());
            }
        }
    }
}
