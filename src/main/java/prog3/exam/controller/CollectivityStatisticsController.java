package prog3.exam.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.CollectivityLocalStatistics;
import prog3.exam.model.CollectivityOverallStatistics;
import prog3.exam.service.CollectivityStatisticsService;
import prog3.exam.service.FederationStatisticsService;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class CollectivityStatisticsController {

     private final CollectivityStatisticsService statisticsService;
    private final FederationStatisticsService federationStatisticsService;

    public CollectivityStatisticsController(CollectivityStatisticsService statisticsService,
                                             FederationStatisticsService federationStatisticsService) {
        this.statisticsService = statisticsService;
        this.federationStatisticsService = federationStatisticsService;
    }

    // TODO: typo here on collectivites
    @GetMapping("/collectivites/{id}/statistics")
    public List<CollectivityLocalStatistics> getStatistics(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.getStatistics(id, from, to);
    }

    @GetMapping("/collectivities/statistics")
    public List<CollectivityOverallStatistics> getOverallStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return federationStatisticsService.getOverallStatistics(from, to);
    }
}
