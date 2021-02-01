package my.nvinz.plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import my.nvinz.plugin.service.EntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import my.nvinz.plugin.service.DBService;
import my.nvinz.plugin.service.StringService;

@Configuration
@ComponentScan
public class SpringConfig {

    @Bean
    public EntityService entityService() {
        return new EntityService();
    }

    @Bean
    public StringService stringService() {
        return new StringService();
    }

    @Bean
    public DBService dbService() {
        try {
            Class.forName("org.postgresql.Driver");
        }
        catch(ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        }

        return new DBService();
    }

    @Bean
    public ProtocolManager protocolManager() {
        return ProtocolLibrary.getProtocolManager();
    }
}
