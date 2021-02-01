package my.nvinz.plugin;

import com.comphenix.protocol.ProtocolManager;
import my.nvinz.plugin.events.Events;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import my.nvinz.plugin.service.DBService;

import java.util.Map;
import java.util.Objects;

public class Plugin extends JavaPlugin {

    private ApplicationContext context;
    private ProtocolManager protocolManager;
    private DBService dbService;

    public void onEnable() {
        context = new AnnotationConfigApplicationContext(SpringConfig.class);

        dbService = context.getBean(DBService.class);

        protocolManager = context.getBean(ProtocolManager.class);
        /*protocolManager.addPacketListener(
                new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                            int Type = event.getPacket().getIntegers().read(1);
                            System.out.println("Type " + Type);
                        }
                    }
                });*/

        registerEvents();
        loadConfig();
    }

    @Override
    public void onDisable() {
    }

    private void registerEvents(){
        try {
            Events events = new Events(context);
            getServer().getPluginManager().registerEvents(events, this);
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage("Error registering events: " + e.getMessage());
        }
    }

    private void loadConfig(){
        try {
            getConfig().options().copyDefaults(true);
            try {
                // Инициализация настроек БД
                Map<String, Object> dbSettings = Objects.requireNonNull(getConfig().getConfigurationSection("db")).getValues(false);
                dbService.setUserName(String.valueOf(dbSettings.get("login")));
                dbService.setUserPassword(String.valueOf(dbSettings.get("password")));
                dbService.setUrl("jdbc:postgresql://" +
                        dbSettings.get("address") +
                        "/" +
                        dbSettings.get("database"));
            } catch (Exception e) {
                getServer().getConsoleSender().sendMessage("Error reading DB settings: " + e.getMessage());
            }
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage("Error setup config: " + e.getMessage());
        }
    }
}