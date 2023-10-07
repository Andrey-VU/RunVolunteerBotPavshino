package telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import telegram.bot.config.BotConfiguration;
import telegram.bot.config.BotModes;

import java.util.Arrays;

@SpringBootApplication
public class Pavshino5verstApplication {

    private final static String HELP = "Телеграмм бот для проекта 5 верст.\n\n" +
            "Запускается командой java -jar [имя_бота] [ключ_запуска]\n\n" +
            "Ключи для запуска:\n" +
            "    -h  - вывести эту подсказку\n" +
            "    -l  - запустить бота с локальной БД\n" +
            "    -g  - запустить бота на тадблицах Google Sheets\n\n";

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-h")) {
            System.out.println(HELP);
            return;
        } else if (Arrays.asList(args).contains("-l")
                && Arrays.asList(args).contains("-g")) {
            System.out.println("Запустите бота только в одно конфигурации");
            return;
        } else if (Arrays.asList(args).contains("-l")) {
            BotConfiguration.setMode(BotModes.LOCAL);
        } else if (Arrays.asList(args).contains("-g")) {
            BotConfiguration.setMode(BotModes.GOOGLE);
        }

        SpringApplication.run(Pavshino5verstApplication.class, args);
    }
}
