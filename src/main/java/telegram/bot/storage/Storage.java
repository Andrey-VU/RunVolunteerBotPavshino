package telegram.bot.storage;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.Volunteer;
import telegram.bot.service.utils.AESUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public abstract class Storage implements TelegramBotStorage {
    protected AESUtil aesUtil;
    protected StorageUtils storageUtils;
    protected Map<String, Volunteer> contacts;
    protected Map<LocalDate, Event> events;
    protected volatile LocalDateTime cacheLastUpdateTime;
    private volatile boolean isStorageSyncStarted = false;

    @Override
    public Volunteer saveVolunteer(Volunteer volunteer) {
        if (!checkIfCacheIsObsoletedAndUpdateIfNeeded() && !contacts.containsKey(volunteer.getFullName()) && Objects.nonNull(mergeVolunteerToSheet(volunteer))) {
            contacts.put(volunteer.getFullName(), volunteer);
            cacheLastUpdateTime = LocalDateTime.now();
            return volunteer;
        } else return null;
    }

    @Override
    public Volunteer updateVolunteer(Volunteer volunteer) {
        if (!checkIfCacheIsObsoletedAndUpdateIfNeeded() && Objects.nonNull(mergeVolunteerToSheet(volunteer))) {
            cacheLastUpdateTime = LocalDateTime.now();
            return volunteer;
        } else return null;
    }

    @Override
    public Volunteer getVolunteerByTgUserName(String tgUserName) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return contacts.values().stream().filter(volunteer -> Objects.equals(volunteer.getTgUserName(), tgUserName)).findFirst().orElse(null);
    }

    @Override
    public Volunteer getVolunteerByCode(String code) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return contacts.values().stream().filter(volunteer -> Objects.equals(volunteer.getCode(), code)).findFirst().orElse(null);
    }

    @Override
    public List<Volunteer> getVolunteers() {
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
    public List<String> getOrganizers() {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        return null;
    }

    @Override
    public List<Participation> getAvailableParticipationByDate(LocalDate date) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        if (Objects.isNull(events.get(date))) addNewEvent(date);
        return events.get(date).getParticipants()
                .stream()
                .filter(participation -> Objects.isNull(participation.getVolunteer()))
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
                Optional.ofNullable(participation.getVolunteer())
                        .orElse(Volunteer.builder().build())
                        .getFullName())) return null;

        var participant = event.getParticipants()
                .stream()
                .filter(obj -> obj.getSheetRowNumber() == participation.getSheetRowNumber())
                .findFirst();

        if (participant.isEmpty()) return null;
        else participant.get().setVolunteer(participation.getVolunteer());
        cacheLastUpdateTime = LocalDateTime.now();

        return participation;
    }

    @Override
    public void deleteParticipation(Participation participation) {
        checkIfCacheIsObsoletedAndUpdateIfNeeded();
        var cellAddress = getCellAddress(participation.getSheetRowNumber(), events.get(participation.getEventDate()).getColumnNumber());
        if (storageUtils.writeCellValue(BotConfiguration.getSheetContacts(), cellAddress, participation.getVolunteer().getTgUserName())) {
            var participant = events.get(participation.getEventDate()).getParticipants()
                    .stream()
                    .filter(obj -> obj.getSheetRowNumber() == participation.getSheetRowNumber())
                    .findFirst()
                    .orElse(null);
            assert participant != null;
            participant.setVolunteer(null);
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
        //log.info("checking cache..");
        var sheetLastUpdateTime = storageUtils.getSheetLastUpdateTime();
        if (cacheLastUpdateTime.isBefore(sheetLastUpdateTime)) {
            log.info("cacheLastUpdateTime {}: ", cacheLastUpdateTime);
            log.info("sheetLastUpdateTime {}: ", sheetLastUpdateTime);
            log.info("cache is obsoleted");
            loadDataFromStorage();
            return true;
        }
        //log.info("cache is actual");
        return false;
    }

    protected void loadContacts() {
        log.info("loadContacts is started");
        contacts = new HashMap<>();
        var rangeBegin = getCellAddress(BotConfiguration.getSheetContactsRowStart(), BotConfiguration.getSheetContactsColumnFirst());
        var rangeEnd = getCellAddress(null, BotConfiguration.getSheetContactsColumnLast());
        AtomicInteger sheetContactsRowStart = new AtomicInteger(BotConfiguration.getSheetContactsRowStart());
        storageUtils.readValuesRange(BotConfiguration.getSheetContacts(), rangeBegin, rangeEnd)
                .forEach(volunteerProperty -> {
                    var volunteer = Volunteer.createFrom(volunteerProperty, aesUtil);
                    volunteer.setSheetRowNumber(sheetContactsRowStart.getAndIncrement());
                    contacts.put(volunteer.getFullName(), volunteer);
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
                .map(lastParticipation -> Participation.builder()
                        .volunteer(null)
                        .eventDate(newEventDate)
                        .eventRole(lastParticipation.getEventRole())
                        .sheetRowNumber(lastParticipation.getSheetRowNumber()).build())
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
        for (int dateIndex = 0; dateIndex < eventDates.size(); dateIndex++) { // идем по датам событий
            var eventDate = eventDates.get(dateIndex); // берем очередную дату
            List<Participation> participants = new LinkedList<>(); // инициализируем список участников
            for (int roleIndex = 0; roleIndex < eventRoles.size(); roleIndex++) { // проходим по списку ролей
                var roleForEvent = eventRoles.get(roleIndex); // берем очередную роль
                var isOrganizerRole = roleForEvent.equals(BotConfiguration.getSheetVolunteersRolesOrganizerName()); // определяем является ли это ролью Организатора
                var volunteerForEvent = getVolunteerForEvent(roleIndex, dateIndex, eventVolunteers); // смотрим кто юзер на эту роль
                if (isOrganizerRole && Objects.nonNull(volunteerForEvent) && !volunteerForEvent.getIsOrganizer()) { // если сейчас роль Организатора, юзер на нее есть и он как Организатор еще не отмечен (получается, в файле метка у него не стояла)
                    volunteerForEvent.setIsOrganizer(true); // отмечаем юзера как Организатора
                    mergeVolunteerToSheet(volunteerForEvent); // обновляем информацию о юзере в файле
                }
                participants.add(Participation.builder()
                        .eventDate(eventDate)
                        .eventRole(roleForEvent)
                        .volunteer(volunteerForEvent)
                        .sheetRowNumber(BotConfiguration.getSheetVolunteersRoleRowStart() + roleIndex).build());
            }
            events.put(eventDate, Event.builder()
                    .eventDate(eventDate)
                    .participants(participants)
                    .columnNumber(BotConfiguration.getSheetVolunteersEventColumnStart() + dateIndex).build());
        }
    }

    protected Volunteer getVolunteerForEvent(int roleIndex, int dateIndex, List<List<String>> roleVolunteers) {
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

    protected Volunteer mergeVolunteerToSheet(Volunteer volunteer) {
        var cellRangeBegin = getCellAddress(Optional.ofNullable(volunteer.getSheetRowNumber()).orElse(BotConfiguration.getSheetContactsRowStart() + contacts.size()), BotConfiguration.getSheetContactsColumnFirst());
        var cellRangeEnd = getCellAddress(Optional.ofNullable(volunteer.getSheetRowNumber()).orElse(BotConfiguration.getSheetContactsRowStart() + contacts.size()), BotConfiguration.getSheetContactsColumnLast());
        if (storageUtils.writesValues(
                BotConfiguration.getSheetContacts(),
                cellRangeBegin + ":" + cellRangeEnd,
                List.of(List.of(
                        volunteer.getFullName(),
                        volunteer.getTgUserName(),
                        Optional.ofNullable(volunteer.getCode()).orElse(""),
                        Optional.ofNullable(volunteer.getComment()).orElse(""),
                        aesUtil.encrypt(String.valueOf(Optional.ofNullable(volunteer.getTgUserId()).orElse(0L))),
                        Optional.ofNullable(volunteer.getIsOrganizer()).orElse(false),
                        Optional.ofNullable(volunteer.getIsSubscribed()).orElse(false)))))
            return volunteer;
        else
            return null;
    }

    class SyncStorageRunner implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //log.info("waiting for {} sec", BotConfiguration.getBotStorageSheetSyncIntervalMilliSec() / 1000);
                    Thread.sleep(BotConfiguration.getBotStorageSheetSyncIntervalMilliSec());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                checkIfCacheIsObsoletedAndUpdateIfNeeded();
            }
        }
    }
}

