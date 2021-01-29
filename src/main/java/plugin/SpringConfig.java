package plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import plugin.utils.DBUtils;
import plugin.utils.EntityUtils;
import plugin.utils.StringUtils;

@Configuration
@ComponentScan
public class SpringConfig {

    @Bean
    public EntityUtils nameUtils() {
        return new EntityUtils();
    }

    @Bean
    public StringUtils stringUtils() {
        return new StringUtils();
    }

    @Bean
    public DBUtils dbUtils() {
        try {
            Class.forName("org.postgresql.Driver");
        }
        catch(ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        }

        return new DBUtils();
    }

    @Bean
    public ProtocolManager protocolManager() {
        return ProtocolLibrary.getProtocolManager();
    }
}
