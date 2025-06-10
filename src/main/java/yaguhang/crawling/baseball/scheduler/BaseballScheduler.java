package yaguhang.crawling.baseball.scheduler;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yaguhang.crawling.baseball.application.BaseballService;

@Component
public class BaseballScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BaseballScheduler.class);

    @Autowired
    private BaseballService baseballService;
    @PostConstruct
    public void init() {
        try {
            baseballService.crawlingAllBaseballSchedules();
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation occurred during PostConstruct initialization", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during PostConstruct initialization", e);
        }
    }

    @Scheduled(cron = "0 0,30 0,13-23 * * *") // Every 30 minutes excluding 01:00-12:00
    public void scrapeGames() {
        System.out.println("BaseballScheduler.scrapeGames");
        baseballService.crawlingTodayGame();
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 00시 정각에 실행
    public void scrapeAllGames() {
        System.out.println("BaseballScheduler.scrapeAllGames");
        baseballService.crawlingAllBaseballSchedules();
    }
}