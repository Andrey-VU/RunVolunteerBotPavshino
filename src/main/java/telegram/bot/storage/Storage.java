package telegram.bot.storage;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class Storage implements TelegramBotStorage {
    protected StorageUtils storageUtils;
    protected Map<String, User> contacts;
    protected Map<LocalDate, Event> events;
    protected volatile LocalDateTime cacheLastUpdateTime;
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
        cacheLastUpdateTime = LocalDateTime.now();
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
        if (Objects.isNull(events.get(date))) addNewEvent(date);
        return events.get(date).getParticipants();
    }

    @Override
    public List<Participation> getAvailableParticipationByDate(LocalDate date) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        if (Objects.isNull(events.get(date))) addNewEvent(date);
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
        cacheLastUpdateTime = LocalDateTime.now();

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
            cacheLastUpdateTime = LocalDateTime.now();
        }
    }

    synchronized public void loadDataFromStorage() {
        loadContacts();
        loadEvents();
        cacheLastUpdateTime = LocalDateTime.now();

        if (!isStorageSyncStarted && BotConfiguration.getBotStorageSheetSyncIntervalMilliSec() != 0) {
            new Thread(new SyncStorageRunner()).start();
            isStorageSyncStarted = true;
        }
    }

    private boolean checkIfCacheIsObsoletedAndUpdateIfNeeded() {
        if (BotConfiguration.getBotStorageSheetSyncIntervalMilliSec() == 0) return false;

        log.info("checking cache..");

        var sheetLastUpdateTime = storageUtils.getSheetLastUpdateTime();

        log.info("cacheLastUpdateTime {}: ", cacheLastUpdateTime);
        log.info("sheetLastUpdateTime {}: ", sheetLastUpdateTime);

        if (cacheLastUpdateTime.isBefore(sheetLastUpdateTime)) {
            log.info("cache is obsoleted");
            loadDataFromStorage();
            return true;
        }
        log.info("cache is actual");
        return false;
    }

    protected void loadContacts() {
        log.info("loadContacts is started");
        contacts = new HashMap<>();
        var rangeBegin = getCellAddress(BotConfiguration.getSheetContactsRowStart(), BotConfiguration.getSheetContactsColumnFirst());
        var rangeEnd = getCellAddress(null, BotConfiguration.getSheetContactsColumnLast());
        storageUtils.readValuesRange(BotConfiguration.getSheetContacts(), rangeBegin, rangeEnd)
                .forEach(userProperty -> {
                    var user = User.createFrom(userProperty);
                    contacts.put(user.getFullName(), user);
                });
        log.info("loadContacts is finished");
    }

    protected void loadEvents() {
        log.info("loadEvents is started");
        events = new LinkedHashMap<>();
        var eventRoles = getEventRoles();
        var eventDates = getEventDates();
        addSaturdaysIfNeeded(eventDates);
        var eventVolunteers = getEventVolunteers(eventRoles, eventDates);
        prepareEvents(eventRoles, eventDates, eventVolunteers);
        log.info("loadEvents is finished");
    }

    protected void addNewEvent(LocalDate newEventDate) {
        var lastEvent = events.get(events.keySet().stream().max(LocalDate::compareTo).orElse(null));
        var newEventColumnNumber = lastEvent.getColumnNumber() + 1;
        var newEventParticipants = lastEvent.getParticipants().stream()
                .map(participation -> Participation.builder()
                        .user(null)
                        .eventDate(newEventDate)
                        .eventRole(participation.getEventRole())
                        .sheetRowNumber(participation.getSheetRowNumber()).build())
                .toList();
        var newEvent = Event.builder()
                .eventDate(newEventDate)
                .columnNumber(newEventColumnNumber)
                .participants(newEventParticipants).build();
        events.put(newEventDate, newEvent);
        var cellAddress = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), newEventColumnNumber);
        storageUtils.writeCellValue(BotConfiguration.getSheetVolunteers(), cellAddress, newEventDate.format(BotConfiguration.DATE_FORMATTER));
    }

    protected List<String> getEventRoles() {
        var rangeBegin = getCellAddress(BotConfiguration.getSheetVolunteersRoleRowStart(), BotConfiguration.getSheetVolunteersRoleColumn());
        var rangeEnd = getCellAddress(null, BotConfiguration.getSheetVolunteersRoleColumn());
        return storageUtils.readValuesList(BotConfiguration.getSheetVolunteers(), rangeBegin, rangeEnd);
    }

    protected List<LocalDate> getEventDates() {
        var rangeBegin = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), BotConfiguration.getSheetVolunteersEventColumnStart());
        var rangeEnd = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), null);
        return storageUtils.readValuesRange(BotConfiguration.getSheetVolunteers(), rangeBegin, rangeEnd)
                .get(0)
                .stream()
                .map(eventDateString -> LocalDate.parse(eventDateString, BotConfiguration.DATE_FORMATTER))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    protected void addSaturdaysIfNeeded(List<LocalDate> eventDates) {
        var nextSaturdaysCounter = 0;
        var saturdayColumn = BotConfiguration.getSheetVolunteersEventColumnStart();
        LocalDate lastSaturday = null;
        for (LocalDate eventDate : eventDates) {
            nextSaturdaysCounter += eventDate.isAfter(LocalDate.now()) ? 1 : 0;
            lastSaturday = eventDate;
            saturdayColumn++;
        }

        assert lastSaturday != null;
        while (nextSaturdaysCounter < BotConfiguration.getSheetSaturdaysAhead()) {
            lastSaturday = lastSaturday.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
            var cellAddress = getCellAddress(BotConfiguration.getSheetVolunteersEventRow(), saturdayColumn++);
            storageUtils.writeCellValue(BotConfiguration.getSheetVolunteers(), cellAddress, lastSaturday.format(BotConfiguration.DATE_FORMATTER));
            eventDates.add(lastSaturday);
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
                    log.info("waiting for {} sec", BotConfiguration.getBotStorageSheetSyncIntervalMilliSec() / 1000);
                    Thread.sleep(BotConfiguration.getBotStorageSheetSyncIntervalMilliSec());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                checkIfCacheIsObsoletedAndUpdateIfNeeded();
            }
        }
    }
}

