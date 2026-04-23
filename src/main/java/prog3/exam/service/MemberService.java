package prog3.exam.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.Member;
import prog3.exam.model.enums.MemberOccupation;
import prog3.exam.model.requests.CreateMemberRequest;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.MemberRepository;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final CollectivityRepository collectivityRepository;

    public MemberService(MemberRepository memberRepository,
                         CollectivityRepository collectivityRepository) {
        this.memberRepository = memberRepository;
        this.collectivityRepository = collectivityRepository;
    }

    public List<Member> createMembers(List<CreateMemberRequest> requests) {
        List<Member> created = new ArrayList<>();

        for (CreateMemberRequest req : requests) {
            validateCreateMember(req);

            int memberId = memberRepository.save(req);

            if (req.getReferees() != null) {
                for (int refereeId : req.getReferees()) {
                    if (!memberRepository.existsById(refereeId)) {
                        throw new NotFoundException("Referee not found: " + refereeId);
                    }
                    memberRepository.saveReferee(memberId, refereeId);
                }
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalStateException("Member not persisted: " + memberId));
            created.add(member);
        }

        return created;
    }

    private void validateCreateMember(CreateMemberRequest req) {
        if (!Boolean.TRUE.equals(req.getRegistrationFeePaid())) {
            throw new BadRequestException("Registration fee not paid.");
        }
        if (!Boolean.TRUE.equals(req.getMembershipDuesPaid())) {
            throw new BadRequestException("Membership dues not paid.");
        }

        if (req.getCollectivityIdentifier() != null &&
                !collectivityRepository.existsById(req.getCollectivityIdentifier())) {
            throw new NotFoundException("Collectivity not found: " + req.getCollectivityIdentifier());
        }

        if (req.getReferees() == null || req.getReferees().size() < 2) {
            throw new BadRequestException("Member must have at least 2 referees.");
        }

        int targetCollectivityId = req.getCollectivityIdentifier() != null
                ? req.getCollectivityIdentifier()
                : -1;

        int internalReferees = 0;
        int externalReferees = 0;

        for (int refereeId : req.getReferees()) {
            Member referee = memberRepository.findById(refereeId)
                    .orElseThrow(() -> new NotFoundException("Referee not found: " + refereeId));

            if (referee.getOccupation() == null ||
                    referee.getOccupation() == MemberOccupation.JUNIOR) {
                throw new BadRequestException(
                        "Referee " + refereeId + " is not a confirmed member (must be SENIOR or higher).");
            }

            Integer refereeCollectivityId = memberRepository.findCollectivityId(refereeId);
            if (targetCollectivityId != -1
                    && refereeCollectivityId != null
                    && refereeCollectivityId == targetCollectivityId) {
                internalReferees++;
            } else {
                externalReferees++;
            }
        }

        if (internalReferees < externalReferees) {
            throw new BadRequestException(
                    "The number of referees from the target collectivity (" + internalReferees +
                    ") must be >= the number of referees from other collectivities (" + externalReferees + ").");
        }
    }
}
