package telegram.bot.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrganizerInformerTest {
    private final String orgInfo = "119649111" + ";" + "molyavkin" + System.lineSeparator();
    private final String orgInfoOther = "111111111" + ";" + "username" + System.lineSeparator();
    private final String emptyOrgInfoSep = System.lineSeparator();
    private final String emptyOrgInfo = "";

    @Test
    void isListContainsLineTest() {
        List<String> organizers = new ArrayList<>();
        organizers.add(orgInfo);
        organizers.add(orgInfoOther);

        assertTrue(OrganizerInformer.isListContainsUserName(organizers, "molyavkin"));
        assertFalse(OrganizerInformer.isListContainsUserName(organizers, "olyavkin"));
        assertTrue(OrganizerInformer.isListContainsUserName(organizers, "username"));
        assertFalse(OrganizerInformer.isListContainsUserName(organizers, "usernam"));
    }

    @Test
    void getOrganizersIds() {
    }

    @Test
    void idFromOrgInfo() {
    }

    @Test
    void userNameFromOrgInfo() {
        assertEquals(OrganizerInformer.userNameFromOrgInfo(orgInfo), "molyavkin");
        assertEquals(OrganizerInformer.userNameFromOrgInfo(orgInfoOther), "username");
        assertEquals(OrganizerInformer.userNameFromOrgInfo(emptyOrgInfoSep), "");
        assertEquals(OrganizerInformer.userNameFromOrgInfo(emptyOrgInfo), "");
    }
}
/*
public boolean isUserCodeInDatabase(String userName) {
        List<String> organizers = ReadFile();
        Optional<String> result = organizers.stream().map(OrganizerInformer::userNameFromOrgInfo).findFirst();
        return result.isPresent();
    }

    public List<Long> getOrganizersIds(LocalDate date) {
        List<String> organizers = ReadFile();
        return organizers.stream().map(OrganizerInformer::idFromOrgInfo).toList();
    }

    private long idFromOrgInfo(String orgInfo) {
        String[] parts = orgInfo.split(";");
        return Long.parseLong(parts[0]);
    }

    private String userNameFromOrgInfo(String orgInfo) {
        String[] parts = orgInfo.split(";");
        String username = parts[1];
        if (username != null && username.length() > 0) {
            username = username.substring(0, username.length() - 1);
        }
        return username;
    }
 */