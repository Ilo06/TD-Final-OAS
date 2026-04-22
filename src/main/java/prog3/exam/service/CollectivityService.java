package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.Collectivity;
import prog3.exam.model.CollectivityStructure;
import prog3.exam.model.Member;
import prog3.exam.model.requests.CreateCollectivityRequest;
import prog3.exam.model.requests.CreateCollectivityStructureRequest;
import prog3.exam.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;

import prog3.exam.repository.CollectivityRepository;

@Service
public class CollectivityService {

    private final CollectivityRepository collectivityRepository;
    private final MemberRepository memberRepository;

    public CollectivityService(CollectivityRepository collectivityRepository,
                                MemberRepository memberRepository) {
        this.collectivityRepository = collectivityRepository;
        this.memberRepository = memberRepository;
    }

    public List<Collectivity> createCollectivities(List<CreateCollectivityRequest> requests) {
        List<Collectivity> result = new ArrayList<>();

        for (CreateCollectivityRequest req : requests) {
            validateCreateCollectivity(req);

            CreateCollectivityStructureRequest structureReq = req.getStructure();

            Integer presidentId    = structureReq != null ? structureReq.getPresident()    : null;
            Integer vicePresidentId = structureReq != null ? structureReq.getVicePresident() : null;
            Integer treasurerId    = structureReq != null ? structureReq.getTreasurer()    : null;
            Integer secretaryId    = structureReq != null ? structureReq.getSecretary()    : null;

            validateMemberExists(presidentId,     "President");
            validateMemberExists(vicePresidentId, "Vice-president");
            validateMemberExists(treasurerId,     "Treasurer");
            validateMemberExists(secretaryId,     "Secretary");

            int collectivityId = collectivityRepository.save(
                    req.getLocation(),
                    Boolean.TRUE.equals(req.getFederationApproval()),
                    presidentId, vicePresidentId, treasurerId, secretaryId
            );

            List<Member> members = new ArrayList<>();
            if (req.getMembers() != null) {
                for (int memberId : req.getMembers()) {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new NotFoundException("Member not found: " + memberId));
                    members.add(member);
                }
            }

            CollectivityStructure structure = CollectivityStructure.builder()
                    .president(resolveMember(presidentId))
                    .vicePresident(resolveMember(vicePresidentId))
                    .treasurer(resolveMember(treasurerId))
                    .secretary(resolveMember(secretaryId))
                    .build();

            result.add(Collectivity.builder()
                    .id(collectivityId)
                    .location(req.getLocation())
                    .structure(structure)
                    .members(members)
                    .build());
        }

        return result;
    }

    private void validateCreateCollectivity(CreateCollectivityRequest req) {
        if (!Boolean.TRUE.equals(req.getFederationApproval())) {
            throw new BadRequestException("Collectivity must have federation approval.");
        }
        if (req.getStructure() == null) {
            throw new BadRequestException("Collectivity must have a structure.");
        }
    }

    private void validateMemberExists(Integer memberId, String role) {
        if (memberId != null && !memberRepository.existsById(memberId)) {
            throw new NotFoundException(role + " member not found: " + memberId);
        }
    }

    private Member resolveMember(Integer memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId).orElse(null);
    }
}
