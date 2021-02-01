package my.nvinz.plugin.events;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import my.nvinz.plugin.service.EntityService;
import my.nvinz.plugin.service.StringService;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.springframework.context.ApplicationContext;
import my.nvinz.plugin.service.DBService;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Events implements Listener {

    private EntityService entityService;
    private StringService stringService;
    private DBService dbService;
    private ProtocolManager protocolManager;
    private Plugin plugin; // TODO отстой, переделать

    // Имя кожи, для проверки и получения ника в #onItemPickup
    private static String trophyName ="'s trophy";

    public Events(ApplicationContext context, Plugin plugin) {
        this.entityService = context.getBean(EntityService.class);
        this.stringService = context.getBean(StringService.class);
        this.dbService = context.getBean(DBService.class);
        this.protocolManager = context.getBean(ProtocolManager.class);
        this.plugin = plugin; // TODO отстой, переделать
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
                    String playerName = itemName.substring(0, itemName.indexOf(trophyName));
                    int[] entitiesToDestroy = entityService.getArmorStandsByName(playerName);

                    // Пакет с удалением стойки
                    PacketContainer destroyEntityPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    //destroyEntityPacket.getIntegers().write(0, entitiesToDestroy.length); // Говно в документации
                    destroyEntityPacket.getIntegerArrays().write(0, entitiesToDestroy);

                    // TODO мб вынести в отдельный метод
                    try {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            protocolManager.sendServerPacket(onlinePlayer, destroyEntityPacket);
                        }
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Cannot send packet " + destroyEntityPacket, e);
                    }

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
                int id = stringService.generateId(5);

                // 5. При убийстве оцелота с него всегда должна падать одна кожа, над которой будет отображаться ник игрока.
                ItemStack leather = new ItemStack(Material.LEATHER);
                ItemMeta leatherMeta = leather.getItemMeta();
                leatherMeta.setDisplayName(player.getName() + trophyName);
                leather.setItemMeta(leatherMeta);
                event.getDrops().clear();
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        Objects.requireNonNull(entity.getWorld())
                            .dropItem(entity.getLocation(), leather)
                            .setVelocity(new Vector(0, 0, 0)));

                // Пакет спавна стойки
                PacketContainer spawnEntityPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                spawnEntityPacket.getIntegers().write(0, id);
                spawnEntityPacket.getIntegers().write(6, 78); // В документаии индекс 0
                spawnEntityPacket.getDoubles().write(0, entity.getLocation().getX());
                spawnEntityPacket.getDoubles().write(1, entity.getLocation().getY() - 1.2);
                spawnEntityPacket.getDoubles().write(2, entity.getLocation().getZ());
                spawnEntityPacket.getUUIDs().write(0, UUID.randomUUID());
                spawnEntityPacket.getShorts().writeDefaults();

                // TODO мб вынести в отдельный метод
                try {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        protocolManager.sendServerPacket(onlinePlayer, spawnEntityPacket);
                    }
                    // Сохранение всех id стоек в посланных пакетах
                    entityService.addArmorStand(player.getName(), id);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot send packet " + spawnEntityPacket, e);
                }

                // TODO мб вынести в отдельный метод
                try {
                    // Для каждого смотрящего игрока ник должен быть свой
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        // Пакет с метадатой для стойки
                        PacketContainer entityMetadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                        WrappedDataWatcher watcher = new WrappedDataWatcher(entity);

                        // Custom name visible
                        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), true);
                        // Has no gravity
                        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class)), true);
                        // Invisible, ебля в битовые маски
                        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 0x20);
                        // Ник, но выглядит как залупа
                        Optional<?> optionalName = Optional.of(WrappedChatComponent.fromChatMessage(onlinePlayer.getDisplayName())[0].getHandle());
                        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), optionalName);

                        entityMetadataPacket.getEntityModifier(entity.getWorld()).write(0, entity);
                        entityMetadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                        entityMetadataPacket.getIntegers().write(0, id);

                        protocolManager.sendServerPacket(onlinePlayer, entityMetadataPacket);
                    }

                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot send packet ENTITY_METADATA", e);
                }

                // 3. При убийстве данного оцелота игроком в базу заносится запись
                dbService.saveKill("kills", player.getName(), entity.getName());
            }
        }
    }
}
