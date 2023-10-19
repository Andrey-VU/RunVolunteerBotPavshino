package telegram.bot.storage;

import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.SheetConfig;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Storage implements TelegramBotStorage {
    protected StorageUtils storageUtils;
    protected Map<String, User> contacts;
    protected Map<LocalDate, Event> events;

    @Override
    public User saveUser(User user) {
        if (contacts.containsKey(user.getFullName()))
            return null;

        var cellRangeBegin = getCellAddress(SheetConfig.getSheetContactsRowStart() + contacts.size(), SheetConfig.getSheetContactsColumnFirst());
        var cellRangeEnd = getCellAddress(SheetConfig.getSheetContactsRowStart() + contacts.size(), SheetConfig.getSheetContactsColumnLast());
        if (!storageUtils.writesValues(
                SheetConfig.getSheetContacts(),
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
    public List<Participation> getAvailableParticipationByDate(LocalDate date) {
        return events.get(date).getParticipants()
                .stream()
                .filter(participation -> Objects.isNull(participation.getUser()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public Participation saveParticipation(Participation participation) {
        loadDataFromStorage();
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
        if (!storageUtils.writeCellValue(SheetConfig.getSheetVolunteers(), cellAddress, participation.getUser().getFullName()))
            return null;

        participant.setUser(participation.getUser());
        return participation;
    }

    @Override
    public void deleteParticipation(Participation participation) {
        var cellAddress = getCellAddress(participation.getRowNumber(), events.get(participation.getEventDate()).getColumnNumber());
        if (storageUtils.writeCellValue(SheetConfig.getSheetContacts(), cellAddress, participation.getUser().getTelegram())) {
            var participant = events.get(participation.getEventDate()).getParticipants()
                    .stream()
                    .filter(obj -> obj.getRowNumber() == participation.getRowNumber())
                    .findFirst()
                    .orElse(null);
            assert participant != null;
            participant.setUser(null);
        }
    }

    protected void loadDataFromStorage() {
        loadContacts();
        loadEvents();
    }

    protected void loadContacts() {
        contacts = new HashMap<>();
        var rangeBegin = getCellAddress(SheetConfig.getSheetContactsRowStart(), SheetConfig.getSheetContactsColumnFirst());
        var rangeEnd = getCellAddress(null, SheetConfig.getSheetContactsColumnLast());
        storageUtils.readValuesRange(SheetConfig.getSheetContacts(), rangeBegin, rangeEnd)
                .forEach(userProperty -> {
                    var user = User.createFrom(userProperty);
                    contacts.put(user.getFullName(), user);
                });
    }

    protected void loadEvents() {
        events = new HashMap<>();
        var roles = getRoles();
        var dates = getEventsDate();
        var volunteers = getVolunteers(roles, dates);
        prepareEvents(roles, dates, volunteers);
    }

    protected List<String> getRoles() {
        var rangeBegin = getCellAddress(SheetConfig.getSheetVolunteersRoleRowStart(), SheetConfig.getSheetVolunteersRoleColumn());
        var rangeEnd = getCellAddress(null, SheetConfig.getSheetVolunteersRoleColumn());
        return storageUtils.readValuesList(SheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    protected List<String> getEventsDate() {
        var rangeBegin = getCellAddress(SheetConfig.getSheetVolunteersEventRow(), SheetConfig.getSheetVolunteersEventColumnStart());
        var rangeEnd = getCellAddress(SheetConfig.getSheetVolunteersEventRow(), null);
        var dates = storageUtils.readValuesRange(SheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd).get(0);
        addSaturdaysIfNeeded(dates);
        return dates;
    }

    protected void addSaturdaysIfNeeded(List<String> dates) {
        var nextSaturdaysCounter = 0;
        var saturdayColumn = SheetConfig.getSheetVolunteersEventColumnStart();
        LocalDate saturday = LocalDate.now();
        for (String stringDate : dates) {
            saturday = string2LocalDate(stringDate);
            nextSaturdaysCounter += saturday.isEqual(LocalDate.now()) || saturday.isAfter(LocalDate.now()) ? 1 : 0;
            saturdayColumn++;
        }
        while (nextSaturdaysCounter < SheetConfig.getSheetSaturdaysAhead()) {
            saturday = getNextSaturday(saturday);
            var cellAddress = getCellAddress(SheetConfig.getSheetVolunteersEventRow(), saturdayColumn++);
            storageUtils.writeCellValue(SheetConfig.getSheetVolunteers(), cellAddress, saturday.format(SheetConfig.DATE_FORMATTER));
            dates.add(localDate2String(saturday));
            nextSaturdaysCounter++;
        }
    }

    protected List<List<String>> getVolunteers(List<String> roles, List<String> dates) {
        var rangeBegin = getCellAddress(SheetConfig.getSheetVolunteersRoleRowStart(), SheetConfig.getSheetVolunteersRoleColumn() + 1);
        var rangeEnd = getCellAddress(SheetConfig.getSheetVolunteersRoleRowStart() + roles.size() - 1, SheetConfig.getSheetVolunteersRoleColumn() + dates.size());
        return storageUtils.readValuesRange(SheetConfig.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    protected void prepareEvents(List<String> roles, List<String> dates, List<List<String>> volunteers) {
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
                        .rowNumber(SheetConfig.getSheetVolunteersRoleRowStart() + roleIndex).build());
                roleIndex++;
            }
            events.put(date, Event.builder()
                    .eventDate(date)
                    .participants(participants)
                    .columnNumber(SheetConfig.getSheetVolunteersEventColumnStart() + dateIndex).build());
            dateIndex++;
        }
    }

    protected User getVolunteerForEvent(List<String> roleVolunteers, int dateIndex) {
        return !Objects.isNull(roleVolunteers) && roleVolunteers.size() >= dateIndex + 1 ? contacts.get(roleVolunteers.get(dateIndex)) : null;
    }

    protected LocalDate string2LocalDate(String value) {
        return LocalDate.parse(value, SheetConfig.DATE_FORMATTER);
    }

    protected String localDate2String(LocalDate value) {
        return value.format(SheetConfig.DATE_FORMATTER);
    }

    protected LocalDate getNextSaturday(LocalDate day) {
        do day = day.plusDays(1);
        while (day.getDayOfWeek() != DayOfWeek.SATURDAY);
        return day;
    }

    protected String getCellAddress(Integer rowNumber, Integer columnNumber) {
        var rowPrefix = "R";
        var columnPrefix = "C";
        var rowAddress = Objects.isNull(rowNumber) ? "" : rowPrefix + rowNumber;
        var columnAddress = Objects.isNull(columnNumber) ? "" : columnPrefix + columnNumber;
        return rowAddress + columnAddress;
    }
}
