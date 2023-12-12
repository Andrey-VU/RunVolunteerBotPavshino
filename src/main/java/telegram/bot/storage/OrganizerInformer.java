package telegram.bot.storage;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@UtilityClass
//@RequiredArgsConstructor
public class OrganizerInformer {
    private final Map<String, String> organizers = new HashMap<>();
    // private final String pathToFileOrganizer;
    private final String pathToFileOrganizer = "./local_storage/organizer.txt";

    public void saveOrganizer(String organizerId) {
        if (!isUserCodeInDatabase(organizerId)) {
            WriteToFile(organizerId);
            log.info("organizer id saved");
        }
    }

    private boolean isUserCodeInDatabase(String userName) {
        List<String> organizers = ReadFile();

        return isListContainsUserName(organizers, userName);
    }

    public List<Long> getOrganizersIds(LocalDate date) {
        List<String> organizers = ReadFile();
        return organizers.stream().map(OrganizerInformer::idFromOrgInfo).toList();
    }

    private long idFromOrgInfo(String orgInfo) {
        String[] parts = orgInfo.split(";");
        return Long.parseLong(parts[0]);
    }

    boolean isListContainsUserName(List<String> orgInfo, String userName) {
        return orgInfo.stream().map(OrganizerInformer::userNameFromOrgInfo).peek(System.out::println).anyMatch(l -> l.equalsIgnoreCase(userName));
    }
    boolean isListContainsUserId(List<String> orgInfo, String userId) {
        return orgInfo.stream().map(OrganizerInformer::userIdFromOrgInfo).peek(System.out::println).anyMatch(l -> l.equalsIgnoreCase(userId));
    }
    String userNameFromOrgInfo(String orgInfo) {
        String username = "";
        if (!orgInfo.isBlank()) {
            String[] parts = orgInfo.split(";");
            username = removeLineSeparator(parts[1]);
        }
        return username;
    }
    String userIdFromOrgInfo(String orgInfo) {
        String userId = "";
        if (!orgInfo.isBlank()) {
            String[] parts = orgInfo.split(";");
            userId = parts[0];
        }
        return userId;
    }

    private String removeLineSeparator(String line) {
        if (line != null) {
            line = line.replaceAll("(\\r|\\n)", "");
        }
        return line;
    }

    private List<String> ReadFile() {
        Path path = Paths.get(pathToFileOrganizer);
        List<String> lines = new ArrayList<>();

        File file = new File(pathToFileOrganizer);
        if (!file.exists()) {
            log.info("File not found");
        } else {
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);

                lines.forEach(System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lines;
    }

    private void WriteToFile(String text) {
        try (FileWriter writer = new FileWriter(pathToFileOrganizer, true)) {
            writer.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
