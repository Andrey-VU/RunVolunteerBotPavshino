package telegram.bot.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.model.Participation;
import telegram.bot.model.User;
import telegram.bot.service.enums.OrganizerResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizerInformer {
    private final String pathToFileOrganizer = "./local_storage/organizer.txt";

    public List<Long> getOrganizersIdsTelegram(List<Participation> organizers) {
        List<String> savedOrganizers = readFile();

        return organizers.stream()
                .filter(participant -> !Objects.isNull(participant.getUser()))
                .map(participant -> participant.getUser().getTelegram())
                .map(telegram -> idFromListOrgInfo(savedOrganizers, telegram))
                .filter(id -> !id.equals(0L)).toList();
    }

    public long idFromListOrgInfo(List<String> infoOrganizers, String usernameTelegram) {

        return infoOrganizers.stream().filter(orgInfo -> orgInfo.contains(usernameTelegram)).map(this::idFromOrgInfo).findFirst().orElse(0L);
    }

    public boolean isListContainsUserName(List<String> orgInfo, String userName) {
        return orgInfo.stream().map(this::userNameFromOrgInfo).anyMatch(l -> l.equalsIgnoreCase(userName.trim()));
    }

    public boolean isListContainsUserId(List<String> orgInfo, String userId) {
        return orgInfo.stream().map(this::userIdFromOrgInfo).anyMatch(l -> l.equalsIgnoreCase(userId.trim()));
    }

    public String userNameFromOrgInfo(String orgInfo) {
        String username = "";
        if (!orgInfo.isBlank()) {
            String[] parts = orgInfo.split(";");
            username = removeLineSeparator(parts[1]);
        }
        return username;
    }

    public OrganizerResponse addOrganizer(Map.Entry<Long, String> userKeys, List<String> organizers, List<User> allUsers) {
        if (isUserIdInDatabase(String.valueOf(userKeys.getKey()))) {
            log.info("Organizer is already recorded");
            return OrganizerResponse.PRESENT;
        } else if (isOrganizer(userKeys.getValue(), organizers, allUsers)) {
            WriteToFile(userKeys.getKey() + ";" + userKeys.getValue() + System.lineSeparator());
            log.info("Organizer is already recorded");
            return OrganizerResponse.ADD;
        }
        log.info("User is not added to the file because it is not the organizer");

        return OrganizerResponse.REJECT;
    }

    private boolean isUserIdInDatabase(String userId) {
        List<String> savedOrganizers = readFile();

        return isListContainsUserId(savedOrganizers, userId);
    }

    private long idFromOrgInfo(String orgInfo) {
        String[] parts = orgInfo.split(";");
        return Long.parseLong(parts[0]);
    }

    private String userIdFromOrgInfo(String orgInfo) {
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

    private List<String> readFile() {
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

    private boolean isOrganizer(String usernameTelegram, List<String> organizers, List<User> allUsers) {
        String userFullName = getFullNameByTelegram(allUsers, usernameTelegram).trim();

        return organizers.stream().anyMatch(name -> name.equalsIgnoreCase(userFullName.trim()));
    }

    private String getFullNameByTelegram(List<User> allUsers, String telegram) {

        return allUsers.stream()
                .filter(user -> user.getTelegram().equalsIgnoreCase(telegram.trim()))
                .map(User::getFullName)
                .findFirst()
                .orElse("Not found");
    }
}
