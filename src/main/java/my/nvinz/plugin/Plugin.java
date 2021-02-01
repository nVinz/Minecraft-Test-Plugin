package my.nvinz.plugin;

import com.comphenix.protocol.ProtocolManager;
import my.nvinz.plugin.events.Events;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import my.nvinz.plugin.service.DBService;

import java.util.Map;
import java.util.Objects;

public class Plugin extends JavaPlugin {

    private ApplicationContext context;
    private DBService dbService;

    @Override
    public void onEnable() {
        context = new AnnotationConfigApplicationContext(SpringConfig.class);
        dbService = context.getBean(DBService.class);
        registerEvents();
        loadConfig();
    }

    @Override
    public void onDisable() {
    }

    private void registerEvents(){
        try {
            Events events = new Events(context, this);
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
