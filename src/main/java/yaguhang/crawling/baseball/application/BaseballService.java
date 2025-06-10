package yaguhang.crawling.baseball.application;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import yaguhang.crawling.baseball.domain.Baseball;
import yaguhang.crawling.baseball.dto.ScheduleDateInfo;
import yaguhang.crawling.baseball.dto.ScheduleMeta;
import yaguhang.crawling.baseball.repository.BaseballRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class BaseballService {
    private static final long PAGE_LOAD_WAIT_MS = 2000;
    private final BaseballRepository baseballRepository;

    @Transactional
    public List<Baseball> crawlingAllBaseballSchedules() {
        WebDriver driver = createWebDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<Baseball> schedules = new ArrayList<>();
        try {
            for (int month = 3; month <= 10; month++) {
                Document doc = fetchMonthlyDocument(driver, month);
                schedules.addAll(parseMonthlySchedules(doc, month, wait));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching schedules", e);
        } finally {
            driver.quit();
        }
        return schedules;
    }

    @Transactional
    public List<Baseball> crawlingTodayGame() {
        WebDriver driver = createWebDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<Baseball> todaySchedules = new ArrayList<>();
        try {
            LocalDate today = LocalDate.now();
            Document doc = fetchMonthlyDocument(driver, today.getMonthValue());
            Elements days = doc.select(".ScheduleLeagueType_match_list_container__1v4b0 > div");
            for (Element day : days) {
                Element dateEl = day.selectFirst(
                        ".ScheduleLeagueType_group_title__S2Z_g .ScheduleLeagueType_title_area__3v4qt .ScheduleLeagueType_title__2Kalm"
                );
                if (dateEl == null) continue;
                ScheduleDateInfo dateInfo = parseDateInfo(dateEl.text());
                if (dateInfo.getMonth() != today.getMonthValue() || dateInfo.getDay() != today.getDayOfMonth()) {
                    continue;
                }
                // 오늘 날짜 컨테이너 처리
                for (Element gameEl : day.select("ul > li")) {
                    Optional<Baseball> saved = updateOrInsertGame(gameEl, dateInfo, wait);
                    saved.ifPresent(todaySchedules::add);
                }
                break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while scraping today games", e);
        } finally {
            driver.quit();
        }
        return todaySchedules;
    }

    /**
     * 신규 경기면 저장, 기존이면 업데이트 후 반환
     */
    private Optional<Baseball> updateOrInsertGame(Element gameEl, ScheduleDateInfo dateInfo, WebDriverWait wait) {
        ScheduleMeta meta = extractMeta(gameEl, dateInfo, wait);
        if (meta == null) return Optional.empty();
        Optional<Baseball> existingOpt = baseballRepository
                .findByTimeAndHomeAndAwayAndLocation(
                        meta.getGameTime(), meta.getHomeTeam(), meta.getAwayTeam(), meta.getLocation()
                );
        Baseball entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setStatus(meta.getStatus());
            entity.setHomeScore(meta.getHomeScore());
            entity.setAwayScore(meta.getAwayScore());
            entity.setHomePitcher(meta.getHomePitcher());
            entity.setAwayPitcher(meta.getAwayPitcher());
        } else {
            entity = Baseball.builder()
                    .time(meta.getGameTime())
                    .weekDay(dateInfo.getWeekday())
                    .home(meta.getHomeTeam())
                    .away(meta.getAwayTeam())
                    .location(meta.getLocation())
                    .status(meta.getStatus())
                    .homeScore(meta.getHomeScore())
                    .awayScore(meta.getAwayScore())
                    .homePitcher(meta.getHomePitcher())
                    .awayPitcher(meta.getAwayPitcher())
                    .build();
        }
        Baseball saved = baseballRepository.save(entity);
        return Optional.of(saved);
    }

    private List<Baseball> parseMonthlySchedules(Document doc, int targetMonth, WebDriverWait wait) {
        List<Baseball> list = new ArrayList<>();
        Elements days = doc.select(".ScheduleLeagueType_match_list_container__1v4b0 > div");
        for (Element day : days) {
            Element dateEl = day.selectFirst(
                    ".ScheduleLeagueType_group_title__S2Z_g .ScheduleLeagueType_title__2Kalm");
            if (dateEl == null) continue;

            ScheduleDateInfo dateInfo = parseDateInfo(dateEl.text());
            if (dateInfo.getMonth() != targetMonth) continue;

            for (Element gameEl : day.select("ul > li")) {
                list.addAll(processGameElement(gameEl, dateInfo, wait));
            }
        }
        return list;
    }

    private List<Baseball> processGameElement(Element gameEl, ScheduleDateInfo dateInfo, WebDriverWait wait) {
        List<Baseball> results = new ArrayList<>();
        try {
            ScheduleMeta meta = extractMeta(gameEl, dateInfo, wait);
            Optional<Baseball> existing = baseballRepository
                    .findByTimeAndHomeAndAwayAndLocation(
                            meta.getGameTime(), meta.getHomeTeam(), meta.getAwayTeam(), meta.getLocation());
            if (existing.isPresent()) return results;

            System.out.println("meta = " + meta);
            Baseball schedule = Baseball.builder()
                    .time(meta.getGameTime())
                    .weekDay(dateInfo.getWeekday())
                    .home(meta.getHomeTeam())
                    .away(meta.getAwayTeam())
                    .location(meta.getLocation())
                    .status(meta.getStatus())
                    .homeScore(meta.getHomeScore())
                    .awayScore(meta.getAwayScore())
                    .homePitcher(meta.getHomePitcher())
                    .awayPitcher(meta.getAwayPitcher())
                    .build();

            baseballRepository.save(schedule);
            results.add(schedule);
        } catch (Exception ex) {
            System.err.println("Failed to process game: " + ex.getMessage());
        }
        return results;
    }

    private ScheduleDateInfo parseDateInfo(String text) {
        // "5월 12일 (화)" 형식 처리
        String[] parts = text.split(" ");
        int month = Integer.parseInt(parts[0].replace("월", ""));
        int day = Integer.parseInt(parts[1].replace("일", ""));
        String weekday = parts[2].replace("(", "").replace(")", "");
        return new ScheduleDateInfo(month, day, weekday);
    }

    private Document fetchMonthlyDocument(WebDriver driver, int month) throws InterruptedException {
        LocalDate firstOfMonth = LocalDate.of(LocalDate.now().getYear(), month, 1);
        String dateParam = firstOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = String.format(
                "https://m.sports.naver.com/kbaseball/schedule/index?category=kbo&date=%s&postSeason=Y",
                dateParam);
        driver.get(url);
        Thread.sleep(PAGE_LOAD_WAIT_MS);
        return Jsoup.parse(Objects.requireNonNull(driver.getPageSource()));
    }

    private ScheduleMeta extractMeta(Element game, ScheduleDateInfo dateInfo, WebDriverWait wait) {
        Element timeEl = game.selectFirst(".MatchBox_time__nIEfd");
        if (timeEl == null) throw new IllegalStateException("Time element not found");
        String timeText = timeEl.text().replace("경기 시간", "").trim();

        Element statusEl = game.selectFirst(".MatchBox_status__2pbzi");
        String status = statusEl != null ? statusEl.text() : "";

        String[] hm = timeText.split(":");
        LocalDateTime gameTime = LocalDateTime.of(
                LocalDate.now().getYear(), dateInfo.getMonth(), dateInfo.getDay(),
                Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));
        
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".MatchBoxHeadToHeadArea_team_item__25jg6")
        ));
        
        //팀 정보
        Elements teamEls = game.select(".MatchBoxHeadToHeadArea_team_item__25jg6");
        if (teamEls.size() < 2) return null;
        Element awayEl = teamEls.first();
        Element homeEl = teamEls.last();

        String awayTeam = Optional.ofNullable(
                awayEl.selectFirst(".MatchBoxHeadToHeadArea_team__40JQL")
        ).map(Element::text).orElse("");
        String homeTeam = Optional.ofNullable(
                homeEl.selectFirst(".MatchBoxHeadToHeadArea_team__40JQL")
        ).map(Element::text).orElse("");

        // 점수
        int awayScore = parseScore(awayEl, ".MatchBoxHeadToHeadArea_score__e2D7k");
        int homeScore = parseScore(homeEl, ".MatchBoxHeadToHeadArea_score__e2D7k");

        // 투수
        String awayPitcher = parsePitcher(awayEl, ".MatchBoxHeadToHeadArea_item__1IPbQ:last-child");
        String homePitcher = parsePitcher(homeEl, ".MatchBoxHeadToHeadArea_item__1IPbQ:last-child");

        // 위치
        String location = Optional.ofNullable(
                        game.selectFirst(".MatchBox_stadium__13gft")
                ).map(Element::text)
                .orElse("")
                .replace("경기장", "").replace("(신)", "").trim();

        return new ScheduleMeta(
                gameTime, homeTeam, awayTeam,
                location, status,
                homeScore, awayScore,
                homePitcher, awayPitcher
        );
    }

    private int parseScore(Element teamEl, String selector) {
        return Optional.ofNullable(teamEl.selectFirst(selector))
                .map(Element::text)
                .map(t -> t.replaceAll("\\D", ""))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .orElse(0);
    }

    private String parsePitcher(Element teamEl, String selector) {
        return Optional.ofNullable(teamEl.selectFirst(selector))
                .map(Element::text)
                .orElse("");
    }

    private WebDriver createWebDriver() {
        setUpWebDriver();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(options);
    }

    private void setUpWebDriver() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.setProperty("webdriver.chrome.driver", "drivers/chromedriver_win.exe");
        } else if (os.contains("mac")) {
            System.setProperty("webdriver.chrome.driver", "/Users/minseok/chromedriver-mac-arm64/chromedriver");
        } else if (os.contains("linux")) {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        }
    }
}
