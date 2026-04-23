package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.MembershipFee;
import prog3.exam.model.requests.CreateMembershipFeeRequest;
import prog3.exam.repository.MembershipFeeRepository;

import java.util.List;

@Service
public class MembershipFeeService {

    private final MembershipFeeRepository membershipFeeRepository;

    public MembershipFeeService(MembershipFeeRepository membershipFeeRepository) {
        this.membershipFeeRepository = membershipFeeRepository;
    }

    public List<MembershipFee> getByCollectivity(String collectivityId) {
        if (!membershipFeeRepository.collectivityExists(collectivityId)) {
            throw new NotFoundException("Collectivity not found: " + collectivityId);
        }
        return membershipFeeRepository.findByCollectivityId(collectivityId);
    }

    public List<MembershipFee> create(String collectivityId, List<CreateMembershipFeeRequest> requests) {
        if (!membershipFeeRepository.collectivityExists(collectivityId)) {
            throw new NotFoundException("Collectivity not found: " + collectivityId);
        }
        for (CreateMembershipFeeRequest req : requests) {
            if (req.getFrequency() == null) {
                throw new BadRequestException("Unrecognized or missing frequency.");
            }
            if (req.getAmount() == null || req.getAmount() < 0) {
                throw new BadRequestException("Amount must be >= 0.");
            }
        }
        return membershipFeeRepository.saveAll(collectivityId, requests);
    }
}
