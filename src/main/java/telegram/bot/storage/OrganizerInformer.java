package telegram.bot.storage;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import telegram.bot.model.Participation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@UtilityClass
//@RequiredArgsConstructor
public class OrganizerInformer {
    // private final String pathToFileOrganizer;
    private final String pathToFileOrganizer = "./local_storage/organizer.txt";

    private boolean isUserIdInDatabase(String userId) {
        List<String> savedOrganizers = ReadFile();

        return isListContainsUserId(savedOrganizers, userId);
    }


    private long idFromOrgInfo(String orgInfo) {
        String[] parts = orgInfo.split(";");
        return Long.parseLong(parts[0]);
    }

    long idFromListOrgInfo(List<String> infoOrganizers, String usernameTelegram) {

        return infoOrganizers
                .stream()
                .filter(orgInfo -> orgInfo.contains(usernameTelegram))
                .map(OrganizerInformer::idFromOrgInfo)
                .findFirst().orElse(0L);
    }

    boolean isListContainsUserName(List<String> orgInfo, String userName) {
        return orgInfo.stream().map(OrganizerInformer::userNameFromOrgInfo).anyMatch(l -> l.equalsIgnoreCase(userName));
    }

    boolean isListContainsUserId(List<String> orgInfo, String userId) {
        return orgInfo.stream().map(OrganizerInformer::userIdFromOrgInfo).anyMatch(l -> l.equalsIgnoreCase(userId));
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

    public static void addOrganizer(Map.Entry<Long, String> userKeys) {
        if (isUserIdInDatabase(String.valueOf(userKeys.getKey()))) {
            log.info("Organizer is already recorded");
        } else {
            WriteToFile(userKeys.getKey() + ";" + userKeys.getValue() + System.lineSeparator());
            log.info("Organizer saved");
        }
    }

    public static List<Long> getOrganizersIdsTelegram(List<Participation> organizers) {
        List<String> savedOrganizers = ReadFile();
        //идем по списку organizers и для каждого достаем код чата из списка savedOrganizers и кладем его в список
        return organizers.stream()
                .filter(participant -> !Objects.isNull(participant.getUser()))
                .map(participant -> participant.getUser().getTelegram())
                .map(telegram -> idFromListOrgInfo(savedOrganizers, telegram))
                .filter(id -> !id.equals(0L))
                .toList();
    }
}
