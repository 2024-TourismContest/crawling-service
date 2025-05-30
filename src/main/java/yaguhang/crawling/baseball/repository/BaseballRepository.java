package yaguhang.crawling.baseball.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yaguhang.crawling.baseball.domain.Baseball;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BaseballRepository extends JpaRepository<Baseball,Long> {
    @Query("SELECT b FROM Baseball b WHERE b.time = :time AND b.home = :home AND b.away = :away AND b.location = :location")
    Optional<Baseball> findByTimeAndHomeAndAwayAndLocation(@Param("time") LocalDateTime time, @Param("home") String home, @Param("away") String away, @Param("location") String location);

    Page<Baseball> findByStatusNotAndTimeIsAfterOrderByTime(String status, LocalDateTime start, Pageable pageable);

    Optional<Baseball> findFirstByTimeIsAfterOrderByTimeAsc(LocalDateTime start);

    @Query("SELECT b FROM Baseball b WHERE b.status <> :status AND b.time > :start AND (b.home = :team OR b.away = :team) order by b.time")
    Page<Baseball> findByStatusNotAndTimeIsAfterAndHomeOrAwayOrderByTime(@Param("status") String status, @Param("start") LocalDateTime start, @Param("team") String team, Pageable pageable);

    boolean existsByTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    boolean existsByHomeAndTimeBetween(String home, LocalDateTime localDateTime, LocalDateTime localDateTime1);

    boolean existsByAwayAndTimeBetween(String away, LocalDateTime localDateTime, LocalDateTime localDateTime1);
}