package telegram.bot;

import com.google.api.client.auth.oauth2.Credential;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.security.GeneralSecurityException;

@SpringBootApplication
public class Pavshino5verstApplication {

	public static void main(String[] args) {
		SpringApplication.run(Pavshino5verstApplication.class, args);
		GoogleSheetUtil googleSheetUtil = new GoogleSheetUtil();
		System.out.println("Hello, world");
	}

}
