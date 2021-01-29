package plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import plugin.events.Events;
import plugin.utils.DBUtils;

import java.util.Map;
import java.util.Objects;

public class Plugin extends JavaPlugin {

    private ApplicationContext context;
    private ProtocolManager protocolManager;
    private DBUtils dbUtils;

    public void onEnable() {
        context = new AnnotationConfigApplicationContext(SpringConfig.class);

        dbUtils = context.getBean(DBUtils.class);

        protocolManager = context.getBean(ProtocolManager.class);
        protocolManager.addPacketListener(
                new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                            int Type = event.getPacket().getIntegers().read(1);
                            System.out.println("Type " + Type);
                        }
                    }
                });

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
                Map<String, Object> dbSettings = Objects.requireNonNull(getConfig().getConfigurationSection("db")).getValues(false);
                System.out.println("dbSettings: " + dbSettings);

                dbUtils.setUserName(String.valueOf(dbSettings.get("login")));
                dbUtils.setUserPassword(String.valueOf(dbSettings.get("password")));
                dbUtils.setUrl("jdbc:postgresql://" +
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
