package prog3.exam.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.CollectivityLocalStatistics;
import prog3.exam.service.CollectivityStatisticsService;

import java.time.LocalDate;
import java.util.List;

@RestController
public class CollectivityStatisticsController {

    private final CollectivityStatisticsService statisticsService;

    public CollectivityStatisticsController(CollectivityStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    // TODO: typo here on collectivites
    @GetMapping("/collectivites/{id}/statistics")
    public List<CollectivityLocalStatistics> getStatistics(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getStatistics(id, from, to);
    }
}
