//package yaguhang.crawling;
//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.Rollback;
//import yaguhang.crawling.baseball.application.BaseballService;
//import yaguhang.crawling.baseball.domain.Baseball;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@Transactional
//@Rollback(false)
//class CrawlingTest {
//
//    private static final Logger log = LoggerFactory.getLogger(CrawlingTest.class);
//    @Autowired
//    private BaseballService baseballService;
//
//    /**
//     * 전체 경기 일정을 크롤링하고, 반환된 리스트가 null이 아니고 빈 리스트가 아닌지 검증합니다.
//     */
//    @Test
//    void testScrapeAllSchedule() {
//        List<Baseball> schedules = assertDoesNotThrow(
//                () -> baseballService.crawlingAllBaseballSchedules(),
//                "scrapeAllSchedule 호출 중 예외 발생"
//        );
//        log.info("경기 일정 수 : ${}",schedules.size());
//        log.info("경기 일정 내용 : ${}",schedules);
//    }
//
//    @Test
//    void testScrapeTodayBaseballSchedule(){
//        List<Baseball> schedules = assertDoesNotThrow(
//                () -> baseballService.crawlingTodayGame(),
//                "scrapeTodayBaseballSchedule 호출 중 예외 발생"
//        );
//        log.info("경기 일정 수 : ${}",schedules.size());
//        log.info("오늘의 경기 : ${}",schedules);
//    }
//}