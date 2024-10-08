import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSuiteFile {

    String filePath = "src/main/resources/downloads.txt"; // Update the file path accordingly

    // EX 1
    @Test
    public void Ex1_MostListenedPodcastFromSanFrancisco() throws IOException {
        // Read the file

        List<String> jsonLines = Files.readAllLines(Paths.get(filePath));

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> showDownloadCount = new HashMap<>();

        // Parse each JSON line
        for (String line : jsonLines) {
            JsonNode rootNode = mapper.readTree(line);
            JsonNode downloadIdentifier = rootNode.get("downloadIdentifier");
            String showId = downloadIdentifier.get("showId").asText();
            String city = rootNode.get("city").asText();

            // Filter for downloads from San Francisco
            if ("san francisco".equalsIgnoreCase(city)) {
                // Count the number of downloads per showId
                showDownloadCount.put(showId, showDownloadCount.getOrDefault(showId, 0) + 1);
            }
        }

        // Find the most popular show
        Map.Entry<String, Integer> mostPopularShow = showDownloadCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new IllegalArgumentException("No data found"));

        // Verify the result
        assertEquals("Who Trolled Amber", getShowNameById(mostPopularShow.getKey()));
        assertEquals(24, mostPopularShow.getValue());

        System.out.println("Most popular show is: " + getShowNameById(mostPopularShow.getKey()));
        System.out.println("Number of downloads is: " + mostPopularShow.getValue());
    }

    // Utility method to map showId to a show name
    private String getShowNameById(String showId) {
        Map<String, String> showIdToNameMap = new HashMap<>();
        showIdToNameMap.put("Who Trolled Amber", "Who Trolled Amber"); // Update based on actual showId to name mapping
        return showIdToNameMap.getOrDefault(showId, "Unknown Show");
    }

    // EX 2
    @Test
    public void Ex2_MostUsedDeviceForPodcasts() throws IOException {
        // Citește fișierul
        List<String> jsonLines = Files.readAllLines(Paths.get(filePath));

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> deviceDownloadCount = new HashMap<>();

        // Parcurge fiecare linie JSON
        for (String line : jsonLines) {
            JsonNode rootNode = mapper.readTree(line);
            String deviceType = rootNode.get("deviceType").asText();

            // Incrementează numărul de download-uri pentru deviceType
            deviceDownloadCount.put(deviceType, deviceDownloadCount.getOrDefault(deviceType, 0) + 1);
        }

        // Găsește cel mai popular deviceType
        Map.Entry<String, Integer> mostPopularDevice = deviceDownloadCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new IllegalArgumentException("Nu s-au găsit date"));

        // Verifică rezultatul
        assertEquals("mobiles & tablets", mostPopularDevice.getKey());
        assertEquals(60, mostPopularDevice.getValue());

        // Printează rezultatul în formatul cerut
        System.out.println("Most popular device is: " + mostPopularDevice.getKey());
        System.out.println("Number of downloads is: " + mostPopularDevice.getValue());
    }

    //EX3
    @Test
    public void Ex3_PrerollOpportunitiesPerShow() throws IOException {
        // Citește fișierul
        List<String> jsonLines = Files.readAllLines(Paths.get(filePath));

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> prerollOpportunitiesCount = new HashMap<>();

        // Parcurge fiecare linie JSON
        for (String line : jsonLines) {
            JsonNode rootNode = mapper.readTree(line);
            JsonNode downloadIdentifier = rootNode.get("downloadIdentifier");
            String showId = downloadIdentifier.get("showId").asText();
            JsonNode opportunities = rootNode.get("opportunities");

            // Parcurge oportunitățile de reclame
            for (JsonNode opportunity : opportunities) {
                JsonNode adBreakIndex = opportunity.get("positionUrlSegments").get("aw_0_ais.adBreakIndex");

                // Verifică dacă "preroll" este în lista adBreakIndex
                if (adBreakIndex != null && adBreakIndex.isArray()) {
                    for (JsonNode adBreak : adBreakIndex) {
                        if ("preroll".equals(adBreak.asText())) {
                            // Incrementează numărul de oportunități pentru showId
                            prerollOpportunitiesCount.put(showId, prerollOpportunitiesCount.getOrDefault(showId, 0) + 1);
                        }
                    }
                }
            }
        }

        // Sortează descrescător după numărul de oportunități
        List<Map.Entry<String, Integer>> sortedOpportunities = new ArrayList<>(prerollOpportunitiesCount.entrySet());
        sortedOpportunities.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Printează rezultatele în formatul cerut
        for (Map.Entry<String, Integer> entry : sortedOpportunities) {
            System.out.println("Show Id: " + entry.getKey() + ", Preroll Opportunity Number: " + entry.getValue());
        }
    }

    //EX4
    @Test
    public void Ex4_WeeklyPodcasts() throws IOException {
        // Citește fișierul
        List<String> jsonLines = Files.readAllLines(Paths.get(filePath));

        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<Long>> showEventTimes = new HashMap<>();

        // Parcurge fiecare linie JSON
        for (String line : jsonLines) {
            JsonNode rootNode = mapper.readTree(line);
            JsonNode downloadIdentifier = rootNode.get("downloadIdentifier");
            String showId = downloadIdentifier.get("showId").asText();
            JsonNode opportunities = rootNode.get("opportunities");

            // Extrage originalEventTime pentru fiecare oportunitate
            for (JsonNode opportunity : opportunities) {
                long eventTime = opportunity.get("originalEventTime").asLong();
                showEventTimes.computeIfAbsent(showId, k -> new ArrayList<>()).add(eventTime);
            }
        }

        // Identifică emisiunile difuzate săptămânal
        Map<String, String> weeklyShows = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : showEventTimes.entrySet()) {
            String showId = entry.getKey();
            List<Long> eventTimes = entry.getValue();
            Collections.sort(eventTimes);  // Sortează datele în ordine cronologică

            if (isWeekly(eventTimes)) {
                // Convertim timpul în ziua săptămânii și ora
                String dayAndTime = formatEventTime(eventTimes.get(0));  // Folosim primul timp ca referință
                weeklyShows.put(showId, dayAndTime);
            }
        }

        // Printează emisiunile săptămânale
        System.out.println("Weekly shows are:");
        for (Map.Entry<String, String> entry : weeklyShows.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    // Metodă pentru a verifica dacă o emisiune este difuzată săptămânal și în aceeași zi și oră
    private boolean isWeekly(List<Long> eventTimes) {
        if (eventTimes.size() < 2) {
            return false;
        }

        // Convertim primul timestamp la ziua săptămânii și ora
        LocalDateTime firstEventTime = toLocalDateTime(eventTimes.get(0));
        DayOfWeek expectedDayOfWeek = firstEventTime.getDayOfWeek();
        int expectedHour = firstEventTime.getHour();
        int expectedMinute = firstEventTime.getMinute();

        for (int i = 1; i < eventTimes.size(); i++) {
            LocalDateTime currentEventTime = toLocalDateTime(eventTimes.get(i));

            // Verificăm dacă ziua săptămânii și ora coincid
            if (currentEventTime.getDayOfWeek() != expectedDayOfWeek ||
                    currentEventTime.getHour() != expectedHour ||
                    currentEventTime.getMinute() != expectedMinute) {
                return false;
            }

        }

        return true;
    }

    // Metodă pentru a converti timestamp-ul UNIX la LocalDateTime
    private LocalDateTime toLocalDateTime(long eventTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(eventTime), ZoneId.of("UTC"));
    }

    // Metodă pentru a converti timestamp-ul în ziua săptămânii și ora (ex: Mon 20:00)
    private String formatEventTime(long eventTime) {
        LocalDateTime dateTime = toLocalDateTime(eventTime);
        return dateTime.getDayOfWeek().name().substring(0, 3) + " " + String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
    }
}
