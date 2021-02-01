package my.nvinz.plugin.events;

import com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import my.nvinz.plugin.service.EntityService;
import my.nvinz.plugin.service.StringService;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.springframework.context.ApplicationContext;
import my.nvinz.plugin.service.DBService;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;

public class Events implements Listener {

    private EntityService entityService;
    private StringService stringService;
    private DBService dbService;
    private ProtocolManager protocolManager;

    // Имя кожи, для проверки и получения ника в #onItemPickup
    private static String trophyName ="'s trophy";

    public Events(ApplicationContext context) {
        this.entityService = context.getBean(EntityService.class);
        this.stringService = context.getBean(StringService.class);
        this.dbService = context.getBean(DBService.class);
        this.protocolManager = context.getBean(ProtocolManager.class);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Item item = event.getItem();

            // Удаление привязанных к игроку стоек при подбирании кожи любым энтити
            if (item.getItemStack().getType().equals(Material.LEATHER)) {

                Objects.requireNonNull(item.getItemStack().getItemMeta()).getDisplayName();
                String itemName = item.getItemStack().getItemMeta().getDisplayName();

                if (itemName.contains(trophyName)) {
                    Player player = (Player) event.getEntity();
                    String playerName = itemName.substring(0, itemName.indexOf(trophyName));

                    int[] entitiesToDestroy = entityService.getArmorStandsByName(playerName);

                    // Чистый пакет по удалению стойки
                    PacketContainer destroyEntityPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    //destroyEntityPacket.getIntegers().write(0, entitiesToDestroy.length); // Говно в документации
                    destroyEntityPacket.getIntegerArrays().write(0, entitiesToDestroy);

                    try {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            protocolManager.sendServerPacket(onlinePlayer, destroyEntityPacket);
                        }
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Cannot send packet " + destroyEntityPacket, e);
                    }

                    // Пакет во враппере
                    /*WrapperPlayServerEntityDestroy destroyEntity = new WrapperPlayServerEntityDestroy();
                    destroyEntity.setEntityIds(entityService.getArmorStandsByName(playerName));

                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        destroyEntity.sendPacket(onlinePlayer);
                    }*/

                    entityService.removeArmorStands(playerName);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Zombie) {
            // 1. При убийстве зомби на его месте появляется оцелот.
            Zombie zombie = (Zombie) event.getEntity();
            Ocelot ocelot = (Ocelot) zombie.getWorld().spawnEntity(zombie.getLocation(), EntityType.OCELOT);

            // 2. Имя оцелота должно состоять из 5 случайных символов
            // Генерация имени и сохранение в списке
            String name = stringService.generateRandomString(5);
            ocelot.setCustomName(name);
            entityService.addOcelotName(name);

            // 4. Оцелот должен не убегать от игрока, а атаковать его
            ocelot.setTarget(zombie.getKiller());
        }
        else if (entity instanceof Ocelot) {
            if (entityService.getOcelotNames().contains(entity.getCustomName())) {
                entityService.removeOcelotName(entity.getCustomName());

                Player player = ((Ocelot) entity).getKiller();

                // 5. При убийстве оцелота с него всегда должна падать одна кожа, над которой будет отображаться ник игрока.
                ItemStack leather = new ItemStack(Material.LEATHER);
                ItemMeta leatherMeta = leather.getItemMeta();
                leatherMeta.setDisplayName(player.getName() + trophyName);
                leather.setItemMeta(leatherMeta);
                event.getDrops().clear();
                event.getDrops().add(leather);

                // Стойка для пакета, старая реализация
                /*Location standLocation = new Location(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY()-1, entity.getLocation().getZ());
                ArmorStand stand = (ArmorStand) entity.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
                stand.remove();
                stand.setCustomNameVisible(true);
                stand.setVisible(false);
                stand.setGravity(false);*/

                int id = stringService.generateId(5);

                // Чистый пакет спавна стойки
                PacketContainer spawnEntityPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                spawnEntityPacket.getIntegers().write(0, id);
                spawnEntityPacket.getIntegers().write(6, 78); // В документаии индекс 0
                spawnEntityPacket.getDoubles().write(0, entity.getLocation().getX());
                spawnEntityPacket.getDoubles().write(1, entity.getLocation().getY() - 1);
                spawnEntityPacket.getDoubles().write(2, entity.getLocation().getZ());
                spawnEntityPacket.getUUIDs().write(0, UUID.randomUUID());
                spawnEntityPacket.getShorts().writeDefaults();
                //spawnEntityPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);

                try {
                    protocolManager.sendServerPacket(player, spawnEntityPacket);
                    entityService.addArmorStand(player.getName(), id);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot send packet " + spawnEntityPacket, e);
                }

                // Пакет во враппере, старая реализация
                /*WrapperPlayServerSpawnEntity spawnEntity = new WrapperPlayServerSpawnEntity();
                spawnEntity.setEntityID(id);
                spawnEntity.setType(EntityType.ARMOR_STAND); // Вот эта срака сделано по разному в разных версиях
                spawnEntity.setX(entity.getLocation().getX());
                spawnEntity.setY(entity.getLocation().getY() - 1);
                spawnEntity.setZ(entity.getLocation().getZ());
                spawnEntity.sendPacket(player);*/


                // Чистый пакет с метадатой для стойки, не работает
                PacketContainer entityMetadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                entityMetadataPacket.getIntegers().write(0, id);

                try {
                    // Для каждого смотрящего игрока ник должен быть свой
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        // Должен ставиться каким-то полем
                        //entityMetadataPacket.getStrings().write(0, onlinePlayer.getDisplayName());
                        protocolManager.sendServerPacket(player, entityMetadataPacket);
                    }

                } catch (InvocationTargetException e) {
                    throw new RuntimeException(
                            "Cannot send packet " + entityMetadataPacket, e);
                }

                // Тот же пакет во враппере, тоже не работает, срет ошибки
               /* WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata();
                entityMetadata.setEntityID(id);

                WrappedDataWatcher dataWatcher = new WrappedDataWatcher(entityMetadata.getMetadata());
                System.out.println(dataWatcher.getEntity().toString());

                WrappedDataWatcher.WrappedDataWatcherObject isInvisibleIndex = new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
                dataWatcher.setObject(isInvisibleIndex, (byte) 0x20);

                WrappedDataWatcher.WrappedDataWatcherObject nameValue = new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class));
                WrappedDataWatcher.WrappedDataWatcherObject nameVisible = new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class));

                dataWatcher.setObject(nameVisible, true);
                entityMetadata.setMetadata(dataWatcher.getWatchableObjects());

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    dataWatcher.setObject(nameValue, onlinePlayer.getDisplayName());
                    entityMetadata.sendPacket(player);
                }*/

                // 3. При убийстве данного оцелота игроком в базу заносится запись
                dbService.saveKill("kills", player.getName(), entity.getName());
            }
        }
    }
}
