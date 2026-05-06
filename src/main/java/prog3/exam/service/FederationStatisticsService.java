package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.model.CollectivityInformation;
import prog3.exam.model.CollectivityOverallStatistics;
import prog3.exam.repository.FederationStatisticsRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FederationStatisticsService {

    private final FederationStatisticsRepository federationStatisticsRepository;

    public FederationStatisticsService(FederationStatisticsRepository federationStatisticsRepository) {
        this.federationStatisticsRepository = federationStatisticsRepository;
    }

    public List<CollectivityOverallStatistics> getOverallStatistics(LocalDate from, LocalDate to) {
        List<CollectivityOverallStatistics> result = new ArrayList<>();

        for (FederationStatisticsRepository.CollectivityRow row
                : federationStatisticsRepository.findAllCollectivities()) {

            int newMembers = federationStatisticsRepository.countNewMembers(row.id(), from, to);

            double percentage = federationStatisticsRepository
                    .computeCurrentDuePercentage(row.id(), from, to);

            CollectivityInformation info = CollectivityInformation.builder()
                    .id(row.id())
                    .number(row.number())
                    .name(row.name())
                    .build();

            result.add(CollectivityOverallStatistics.builder()
                    .collectivityInformation(info)
                    .newMembersNumber(newMembers)
                    .overallMemberCurrentDuePercentage(percentage)
                    .build());
        }

        return result;
    }
}