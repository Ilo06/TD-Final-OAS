package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.ConflictException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.Collectivity;
import prog3.exam.model.CollectivityStructure;
import prog3.exam.model.Member;
import prog3.exam.model.requests.AssignCollectivityIdentityRequest;
import prog3.exam.model.requests.CreateCollectivityRequest;
import prog3.exam.model.requests.CreateCollectivityStructureRequest;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;

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

            String presidentId     = structureReq != null ? structureReq.getPresident()     : null;
            String vicePresidentId = structureReq != null ? structureReq.getVicePresident() : null;
            String treasurerId     = structureReq != null ? structureReq.getTreasurer()     : null;
            String secretaryId     = structureReq != null ? structureReq.getSecretary()     : null;

            validateMemberExists(presidentId,     "President");
            validateMemberExists(vicePresidentId, "Vice-president");
            validateMemberExists(treasurerId,     "Treasurer");
            validateMemberExists(secretaryId,     "Secretary");

            collectivityRepository.save(
                    req.getId(),
                    req.getLocation(),
                    Boolean.TRUE.equals(req.getFederationApproval()),
                    presidentId, vicePresidentId, treasurerId, secretaryId
            );

            List<Member> members = new ArrayList<>();
            if (req.getMembers() != null) {
                for (String memberId : req.getMembers()) {
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
                    .id(req.getId())
                    .location(req.getLocation())
                    .structure(structure)
                    .members(members)
                    .build());
        }

        return result;
    }

    public Collectivity assignIdentity(String id, AssignCollectivityIdentityRequest request) {
        if (request.getNumber() == null || request.getName() == null
                || request.getName().isBlank()) {
            throw new BadRequestException("Both number and name are required.");
        }

        Collectivity collectivity = collectivityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collectivity not found: " + id));

        if (collectivityRepository.hasNumber(id)) {
            throw new ConflictException(
                    "Collectivity " + id + " already has a number assigned; it cannot be changed.");
        }
        if (collectivityRepository.hasName(id)) {
            throw new ConflictException(
                    "Collectivity " + id + " already has a name assigned; it cannot be changed.");
        }

        if (collectivityRepository.numberExistsElsewhere(request.getNumber(), id)) {
            throw new ConflictException(
                    "Number " + request.getNumber() + " is already used by another collectivity.");
        }
        if (collectivityRepository.nameExistsElsewhere(request.getName(), id)) {
            throw new ConflictException(
                    "Name '" + request.getName() + "' is already used by another collectivity.");
        }

        collectivityRepository.updateIdentity(id, request.getNumber(), request.getName());

        collectivity.setNumber(request.getNumber());
        collectivity.setName(request.getName());
        return collectivity;
    }

    private void validateCreateCollectivity(CreateCollectivityRequest req) {
        if (!Boolean.TRUE.equals(req.getFederationApproval())) {
            throw new BadRequestException("Collectivity must have federation approval.");
        }
        if (req.getStructure() == null) {
            throw new BadRequestException("Collectivity must have a structure.");
        }
    }

    private void validateMemberExists(String memberId, String role) {
        if (memberId != null && !memberRepository.existsById(memberId)) {
            throw new NotFoundException(role + " member not found: " + memberId);
        }
    }

    private Member resolveMember(String memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId).orElse(null);
    }
}
