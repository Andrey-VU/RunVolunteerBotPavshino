package telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class Pavshino5verstApplication {

	private final static String HELP = "Телеграмм бот для проекта 5 верст.\n\nКлючи для запуска:\n" +
			"    -h           - вывести эту подсказку\n" +
			"    -l, --local  - запустить бота с локальной БД\n" +
			"    -g, --google - запустить бота на тадблицах Google Sheets";

	public static void main(String[] args) {
		if (Arrays.asList(args).contains("-h")) {
			System.out.println(HELP);
			return;
		}
		SpringApplication.run(Pavshino5verstApplication.class, args);
	}
}
