package telegram.bot.storage;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class Storage implements TelegramBotStorage {
    protected StorageUtils storageUtils;
    protected Map<String, User> contacts;
    protected Map<LocalDate, Event> events;
    protected volatile ZonedDateTime cacheLastUpdateTime;
    private volatile boolean isStorageSyncStarted = false;

    @Override
    public User saveUser(User user) {
        if (checkIfCacheIsObsoletedAndUpdateIfNeeded()) return null;
        if (contacts.containsKey(user.getFullName()))
            return null;

        var cellRangeBegin = getCellAddress(BotConfiguration.getSheetContactsRowStart() + contacts.size(), BotConfiguration.getSheetContactsColumnFirst());
        var cellRangeEnd = getCellAddress(BotConfiguration.getSheetContactsRowStart() + contacts.size(), BotConfiguration.getSheetContactsColumnLast());
        if (!storageUtils.writesValues(
                BotConfiguration.getSheetContacts(),
                cellRangeBegin + ":" + cellRangeEnd,
                List.of(List.of(
                        user.getFullName(),
                        user.getTelegram(),
                        user.getCode()))))
            return null;

        contacts.put(user.getFullName(), user);
        cacheLastUpdateTime = ZonedDateTime.now();
        return user;
    }

    @Override
    public User getUserByTelegram(String telegram) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return contacts.values().stream().filter(user -> Objects.equals(user.getTelegram(), telegram)).findFirst().orElse(null);
    }

    @Override
    public User getUserByCode(String code) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return contacts.values().stream().filter(user -> Objects.equals(user.getCode(), code)).findFirst().orElse(null);
    }

    @Override
    public List<User> getUsers() {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return new LinkedList<>(contacts.values());
    }

    @Override
    public List<Participation> getParticipantsByDate(LocalDate date) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return events.get(date).getParticipants();
    }

    @Override
    public List<Participation> getAvailableParticipationByDate(LocalDate date) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return events.get(date).getParticipants()
                .stream()
                .filter(participation -> Objects.isNull(participation.getUser()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public Participation saveParticipation(Participation participation) {
        if (checkIfCacheIsObsoletedAndUpdateIfNeeded()) return null;
        var event = events.get(participation.getEventDate());
        if (Objects.isNull(event)) return null;

        var cellAddress = getCellAddress(participation.getSheetRowNumber(), event.getColumnNumber());

        if (!storageUtils.writeCellValue(
                BotConfiguration.getSheetVolunteers(),
                cellAddress,
                Optional.ofNullable(participation.getUser())
                        .orElse(User.builder().build())
                        .getFullName())) return null;

        var participant = event.getParticipants()
                .stream()
                .filter(obj -> obj.getSheetRowNumber() == participation.getSheetRowNumber())
                .findFirst();

        if (participant.isEmpty()) return null;
        else participant.get().setUser(participation.getUser());
        cacheLastUpdateTime = ZonedDateTime.now();

        return participation;
    }

    @Override
    public void deleteParticipation(Participation participation) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        var cellAddress = getCellAddress(participation.getSheetRowNumber(), events.get(participation.getEventDate()).getColumnNumber());
        if (storageUtils.writeCellValue(BotConfiguration.getSheetContacts(), cellAddress, participation.getUser().getTelegram())) {
            var participant = events.get(participation.getEventDate()).getParticipants()
                    .stream()
                    .filter(obj -> obj.getSheetRowNumber() == participation.getSheetRowNumber())
                    .findFirst()
                    .orElse(null);
            assert participant != null;
            participant.setUser(null);
            cacheLastUpdateTime = ZonedDateTime.now();
        }
    }

    synchronized public void loadDataFromStorage() {
        loadContacts();
        loadEvents();
        cacheLastUpdateTime = ZonedDateTime.now();

        if (!isStorageSyncStarted) {
            new Thread(new SyncStorageRunner()).start();
            isStorageSyncStarted = true;
        }
    }

    private boolean checkIfCacheIsObsoletedAndUpdateIfNeeded() {
        if (cacheLastUpdateTime.isBefore(storageUtils.getSheetLastUpdateTime())) {
            loadDataFromStorage();
            return true;
        }
        return false;
    }

    protected void loadContacts() {
        contacts = new HashMap<>();
        var rangeBegin = getCellAddress(BotConfiguration.getSheetContactsRowStart(), BotConfiguration.getSheetContactsColumnFirst());
        var rangeEnd = getCellAddress(null, BotConfiguration.getSheetContactsColumnLast());
        storageUtils.readValuesRange(BotConfiguration.getSheetContacts(), rangeBegin, rangeEnd)
                .forEach(userProperty -> {
                    var user = User.createFrom(userProperty);
                    contacts.put(user.getFullName(), user);
                });
    }

    protected void loadEvents() {
        events = new HashMap<>();
        var eventRoles = getEventRoles();
        var eventDates = getEventDates();
        var eventVolunteers = getEventVolunteers(eventRoles, eventDates);
        prepareEvents(eventRoles, eventDates, eventVolunteers);
    }

    protected List<String> getEventRoles() {
        var rangeBegin = getCellAddress(BotConfiguration.getSheetVolunteersRoleRowStart(), BotConfiguration.getSheetVolunteersRoleColumn());
        var rangeEnd = getCellAddress(null, BotConfiguration.getSheetVolunteersRoleColumn());
        return storageUtils.readValuesList(BotConfiguration.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    protected List<LocalDate> getEventDates() {
        var rangeBegin = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), BotConfiguration.getSheetVolunteersEventColumnStart());
        var rangeEnd = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), null);
        var eventDates = storageUtils.readValuesRange(BotConfiguration.getSheetVolunteers(), rangeBegin, rangeEnd)
                .get(0)
                .stream()
                .map(eventDateString -> LocalDate.parse(eventDateString, BotConfiguration.DATE_FORMATTER))
                .collect(Collectors.toCollection(LinkedList::new));
        addSaturdaysIfNeeded(eventDates);
        return eventDates;
    }

    protected void addSaturdaysIfNeeded(List<LocalDate> eventDates) {
        var nextSaturdaysCounter = 0;
        var saturdayColumn = BotConfiguration.getSheetVolunteersEventColumnStart();
        LocalDate saturday = LocalDate.now();
        for (LocalDate eventDate : eventDates) {
            nextSaturdaysCounter += saturday.isEqual(LocalDate.now()) || saturday.isAfter(LocalDate.now()) ? 1 : 0;
            saturdayColumn++;
        }
        while (nextSaturdaysCounter < BotConfiguration.getSheetSaturdaysAhead()) {
            saturday = getNextSaturday(saturday);
            var cellAddress = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), saturdayColumn++);
            storageUtils.writeCellValue(BotConfiguration.getSheetVolunteers(), cellAddress, saturday.format(BotConfiguration.DATE_FORMATTER));
            eventDates.add(saturday);
            nextSaturdaysCounter++;
        }
    }

    protected List<List<String>> getEventVolunteers(List<String> eventRoles, List<LocalDate> eventDates) {
        var rangeBegin = getCellAddress(BotConfiguration.getSheetVolunteersRoleRowStart(), BotConfiguration.getSheetVolunteersRoleColumn() + 1);
        var rangeEnd = getCellAddress(BotConfiguration.getSheetVolunteersRoleRowStart() + eventRoles.size() - 1, BotConfiguration.getSheetVolunteersRoleColumn() + eventDates.size());
        return storageUtils.readValuesRange(BotConfiguration.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    protected void prepareEvents(List<String> eventRoles, List<LocalDate> eventDates, List<List<String>> eventVolunteers) {
        for (int dateIndex = 0; dateIndex < eventDates.size(); dateIndex++) {
            var eventDate = eventDates.get(dateIndex);
            List<Participation> participants = new LinkedList<>();
            for (int roleIndex = 0; roleIndex < eventRoles.size(); roleIndex++) {
                var eventRole = eventRoles.get(roleIndex);
                participants.add(Participation.builder()
                        .eventDate(eventDate)
                        .eventRole(eventRole)
                        .user(getVolunteerForEvent(roleIndex, dateIndex, eventVolunteers))
                        .sheetRowNumber(BotConfiguration.getSheetVolunteersRoleRowStart() + roleIndex).build());
            }
            events.put(eventDate, Event.builder()
                    .eventDate(eventDate)
                    .participants(participants)
                    .columnNumber(BotConfiguration.getSheetVolunteersEventColumnStart() + dateIndex).build());
        }
    }

    protected User getVolunteerForEvent(int roleIndex, int dateIndex, List<List<String>> roleVolunteers) {
        return !Objects.isNull(roleVolunteers) &&
                roleVolunteers.size() >= roleIndex + 1 &&
                roleVolunteers.get(roleIndex).size() >= dateIndex + 1
                ? contacts.get(roleVolunteers.get(roleIndex).get(dateIndex))
                : null;
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

    class SyncStorageRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    log.info("sleep begin");
                    Thread.sleep(BotConfiguration.getBotStorageSheetSyncInterval());
                    log.info("sleep end");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("checkIfCacheIsObsoletedAndUpdateIfNeeded() begin");
                checkIfCacheIsObsoletedAndUpdateIfNeeded();
                log.info("checkIfCacheIsObsoletedAndUpdateIfNeeded() end");
            }
        }
    }
}

