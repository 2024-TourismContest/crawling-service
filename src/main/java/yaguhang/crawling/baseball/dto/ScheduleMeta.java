package yaguhang.crawling.baseball.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ScheduleMeta {
    LocalDateTime gameTime;
    String homeTeam;
    String awayTeam;
    String location;
    String status;
    int homeScore;
    int awayScore;
    String homePitcher;
    String awayPitcher;
}