package telegram.bot.config;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:bot-config.properties", ignoreResourceNotFound = false)
public class BotConfiguration {

    private final Environment environment;

    @Setter
    private static BotModes mode;

    public BotConfiguration(Environment environment) {
        this.environment = environment;
    }
}
