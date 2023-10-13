package telegram.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import telegram.bot.storage.LocalExcelUtils;

@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/localExcel.properties", ignoreResourceNotFound = false, encoding = "UTF-8")
public class LocalExcelConfig {
    private String LOCAL_STORAGE_PATH;

    public LocalExcelConfig(@Value("${local.storage.path}") String LOCAL_STORAGE_PATH) {
        this.LOCAL_STORAGE_PATH = LOCAL_STORAGE_PATH;
    }

    public String getLOCAL_STORAGE_PATH() {
        return LOCAL_STORAGE_PATH;
    }
    @Bean(name = "LocalExcelUtils")
    public LocalExcelUtils getLocalExcelUtils() {
        return new LocalExcelUtils(LOCAL_STORAGE_PATH);
    }
}
