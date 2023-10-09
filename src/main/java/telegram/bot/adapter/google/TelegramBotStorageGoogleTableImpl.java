package telegram.bot.adapter.google;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.GoogleSheetConfig;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.User;
import telegram.bot.storage.GoogleSheetUtils;
import telegram.bot.storage.Storage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("google")
@RequiredArgsConstructor
public class TelegramBotStorageGoogleTableImpl extends Storage implements TelegramBotStorage {
    private final GoogleSheetUtils googleSheetUtils;

    @Override
    public User saveUser(User user) {
        if (contacts.containsKey(user.getFullName()))
            return null;

        var cellRangeBegin = getCellAddress(GoogleSheetConfig.getSheetContactsRowStart() + contacts.size(), GoogleSheetConfig.getSheetContactsColumnFirst());
        var cellRangeEnd = getCellAddress(GoogleSheetConfig.getSheetContactsRowStart() + contacts.size(), GoogleSheetConfig.getSheetContactsColumnLast());
        if (!googleSheetUtils.writesValues(
                GoogleSheetConfig.getSheetContacts(),
                cellRangeBegin + ":" + cellRangeEnd,
                List.of(List.of(
                        user.getFullName(),
                        user.getTelegram(),
                        user.getCode()))))
            return null;

        contacts.put(user.getFullName(), user);
        return user;
    }

    @Override
    public User getUserByTelegram(String telegram) {
        return contacts.values().stream().filter(user -> Objects.equals(user.getTelegram(), telegram)).findFirst().orElse(null);
    }

    @Override
    public User getUserByCode(String code) {
        return contacts.values().stream().filter(user -> Objects.equals(user.getCode(), code)).findFirst().orElse(null);
    }

    @Override
    public List<User> getUsers() {
        return new LinkedList<>(contacts.values());
    }

    @Override
    public List<Participation> getParticipantsByDate(LocalDate date) {
        return events.get(date).getParticipants();
    }

    @Override
    public Participation saveParticipation(Participation participation) {
        var event = events.get(participation.getEventDate());
        if (Objects.isNull(event))
            return null;

        var participant = event.getParticipants()
                .stream()
                .filter(obj -> obj.getRowNumber() == participation.getRowNumber())
                .findFirst().orElse(null);

        assert participant != null;
        if (!Objects.isNull(participant.getUser()))
            return null;

        var cellAddress = getCellAddress(participation.getRowNumber(), event.getColumnNumber());
        if (!googleSheetUtils.writeCellValue(GoogleSheetConfig.getSheetVolunteers(), cellAddress, participation.getUser().getFullName()))
            return null;

        participant.setUser(participation.getUser());
        return participation;
    }

    @Override
    public void deleteParticipation(Participation participation) {
        var cellAddress = getCellAddress(participation.getRowNumber(), events.get(participation.getEventDate()).getColumnNumber());
        if (googleSheetUtils.writeCellValue(GoogleSheetConfig.getSheetContacts(), cellAddress, participation.getUser().getTelegram())) {
            var participant = events.get(participation.getEventDate()).getParticipants()
                    .stream()
                    .filter(obj -> obj.getRowNumber() == participation.getRowNumber())
                    .findFirst()
                    .orElse(null);
            assert participant != null;
            participant.setUser(null);
        }
    }

    @PostConstruct
    private void postConstruct() {
        loadDataFromGoogleSheets();

//        var participation = Participation.builder()
//                .user(User.builder()
//                        .name("Елена")
//                        .surname("ДЕНИСКИН").build()
//                )
//                .eventDate(string2LocalDate("22.04.2023"))
//                .role("Фотограф2")
//                .rowNumber(7)
//                .build();
//
//        saveParticipation(participation);

//        saveUser(User.builder().name("Петя").surname("Иванов").telegram("@noir74").code("74").build());
    }

    private void loadDataFromGoogleSheets() {
        loadContacts();
        loadEvents();
    }

    private void loadContacts() {
        contacts = new HashMap<>();
        var rangeBegin = getCellAddress(GoogleSheetConfig.getSheetContactsRowStart(), GoogleSheetConfig.getSheetContactsColumnFirst());
        var rangeEnd = getCellAddress(null, GoogleSheetConfig.getSheetContactsColumnLast());
        googleSheetUtils.readValuesRange(GoogleSheetConfig.getSheetContacts(), rangeBegin, rangeEnd)
                .forEach(userProperty -> {
                    var user = new User(userProperty);
                    contacts.put(user.getFullName(), user);
                });
    }

    private void loadEvents() {
        events = new HashMap<>();
        var roles = getRoles();
        var dates = getEventsDate();
        var volunteers = getVolunteers(roles, dates);
        prepareEvents(roles, dates, volunteers);
    }

    private List<String> getRoles() {
        var rangeBegin = getCellAddress(GoogleSheetConfig.getSheetVolunteersRoleRowStart(), GoogleSheetConfig.getSheetVolunteersRoleColumn());
        var rangeEnd = getCellAddress(null, GoogleSheetConfig.getSheetVolunteersRoleColumn());
        return googleSheetUtils.reagValuesList(GoogleSheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    private List<String> getEventsDate() {
        var rangeBegin = getCellAddress(GoogleSheetConfig.getSheetVolunteersEventRow(), GoogleSheetConfig.getSheetVolunteersEventColumnStart());
        var rangeEnd = getCellAddress(GoogleSheetConfig.getSheetVolunteersEventRow(), null);
        var dates = googleSheetUtils.readValuesRange(GoogleSheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd).get(0);
        addSaturdaysIfNeeded(dates);
        return dates;
    }

    private void addSaturdaysIfNeeded(List<String> dates) {
        var nextSaturdaysCounter = 0;
        var saturdayColumn = GoogleSheetConfig.getSheetVolunteersEventColumnStart();
        LocalDate saturday = LocalDate.now();
        for (String stringDate : dates) {
            saturday = string2LocalDate(stringDate);
            nextSaturdaysCounter += saturday.isEqual(LocalDate.now()) || saturday.isAfter(LocalDate.now()) ? 1 : 0;
            saturdayColumn++;
        }
        while (nextSaturdaysCounter < GoogleSheetConfig.getSheetSaturdaysAhead()) {
            saturday = getNextSaturday(saturday);
            var cellAddress = getCellAddress(GoogleSheetConfig.getSheetVolunteersEventRow(), saturdayColumn++);
            googleSheetUtils.writeCellValue(GoogleSheetConfig.getSheetVolunteers(), cellAddress, saturday.format(GoogleSheetConfig.DATE_FORMATTER));
            dates.add(localDate2String(saturday));
            nextSaturdaysCounter++;
        }
    }

    private List<List<String>> getVolunteers(List<String> roles, List<String> dates) {
        var rangeBegin = getCellAddress(GoogleSheetConfig.getSheetVolunteersRoleRowStart(), GoogleSheetConfig.getSheetVolunteersRoleColumn() + 1);
        var rangeEnd = getCellAddress(GoogleSheetConfig.getSheetVolunteersRoleRowStart() + roles.size() - 1, GoogleSheetConfig.getSheetVolunteersRoleColumn() + dates.size());
        return googleSheetUtils.readValuesRange(GoogleSheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    private void prepareEvents(List<String> roles, List<String> dates, List<List<String>> volunteers) {
        var dateIndex = 0;
        for (String stringDate : dates) {
            var date = string2LocalDate(stringDate);
            List<Participation> participants = new LinkedList<>();
            var roleIndex = 0;
            for (String stringRole : roles) {
                participants.add(Participation.builder()
                        .eventDate(date)
                        .role(roles.get(roleIndex))
                        .user(getVolunteerForEvent(volunteers.get(roleIndex), dateIndex))
                        .rowNumber(GoogleSheetConfig.getSheetVolunteersRoleRowStart() + roleIndex).build());
                roleIndex++;
            }
            events.put(date, Event.builder()
                    .eventDate(date)
                    .participants(participants)
                    .columnNumber(GoogleSheetConfig.getSheetVolunteersEventColumnStart() + dateIndex).build());
            dateIndex++;
        }
    }

    private User getVolunteerForEvent(List<String> roleVolunteers, int dateIndex) {
        return !Objects.isNull(roleVolunteers) && roleVolunteers.size() >= dateIndex + 1 ? contacts.get(roleVolunteers.get(dateIndex)) : null;
    }

    private LocalDate string2LocalDate(String value) {
        return LocalDate.parse(value, GoogleSheetConfig.DATE_FORMATTER);
    }

    private String localDate2String(LocalDate value) {
        return value.format(GoogleSheetConfig.DATE_FORMATTER);
    }

    private LocalDate getNextSaturday(LocalDate day) {
        do day = day.plusDays(1);
        while (day.getDayOfWeek() != DayOfWeek.SATURDAY);
        return day;
    }

    private String getCellAddress(Integer rowNumber, Integer columnNumber) {
        var rowPrefix = "R";
        var columnPrefix = "C";
        var rowAddress = Objects.isNull(rowNumber) ? "" : rowPrefix + rowNumber;
        var columnAddress = Objects.isNull(columnNumber) ? "" : columnPrefix + columnNumber;
        return rowAddress + columnAddress;
    }
}
