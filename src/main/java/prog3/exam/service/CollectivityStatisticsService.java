package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.CollectivityLocalStatistics;
import prog3.exam.model.Member;
import prog3.exam.model.MemberDescription;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.CollectivityStatisticsRepository;
import prog3.exam.repository.MemberRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CollectivityStatisticsService {

        private final CollectivityRepository collectivityRepository;
        private final CollectivityStatisticsRepository statisticsRepository;
        private final MemberRepository memberRepository;

        public CollectivityStatisticsService(CollectivityRepository collectivityRepository,
                        CollectivityStatisticsRepository statisticsRepository,
                        MemberRepository memberRepository) {
                this.collectivityRepository = collectivityRepository;
                this.statisticsRepository = statisticsRepository;
                this.memberRepository = memberRepository;
        }

        public List<CollectivityLocalStatistics> getStatistics(String collectivityId,
                        LocalDate from, LocalDate to) {
                if (!collectivityRepository.existsById(collectivityId)) {
                        throw new NotFoundException("Collectivity not found: " + collectivityId);
                }

                List<String> memberIds = statisticsRepository.findMemberIds(collectivityId);
                List<CollectivityLocalStatistics> result = new ArrayList<>();

                for (String memberId : memberIds) {
                        Member member = memberRepository.findById(memberId).orElse(null);
                        if (member == null)
                                continue;

                        MemberDescription description = MemberDescription.builder()
                                        .id(member.getId())
                                        .firstName(member.getFirstName())
                                        .lastName(member.getLastName())
                                        .email(member.getEmail())
                                        .occupation(member.getOccupation())
                                        .build();

                        double earnedAmount = statisticsRepository.getTotalPaid(memberId, from, to);
                        double unpaidAmount = statisticsRepository.getPotentialUnpaid(
                                        memberId, collectivityId, from, to);

                        Double assiduityPercentage = statisticsRepository.getAssiduityPercentage(
                                        memberId, collectivityId, from, to);

                        result.add(CollectivityLocalStatistics.builder()
                                        .memberDescription(description)
                                        .earnedAmount(earnedAmount)
                                        .unpaidAmount(unpaidAmount)
                                        .assiduityPercentage(assiduityPercentage)
                                        .build());
                }

                return result;
        }
}
