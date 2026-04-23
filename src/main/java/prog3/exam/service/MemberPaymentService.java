package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.MemberPayment;
import prog3.exam.model.requests.CreateMemberPaymentRequest;
import prog3.exam.repository.CollectivityTransactionRepository;
import prog3.exam.repository.MemberPaymentRepository;
import prog3.exam.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberPaymentService {

    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberRepository memberRepository;
    private final CollectivityTransactionRepository transactionRepository;

    public MemberPaymentService(MemberPaymentRepository memberPaymentRepository,
                                 MemberRepository memberRepository,
                                 CollectivityTransactionRepository transactionRepository) {
        this.memberPaymentRepository = memberPaymentRepository;
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<MemberPayment> createPayments(int memberId, List<CreateMemberPaymentRequest> requests) {
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException("Member not found: " + memberId);
        }

        List<MemberPayment> result = new ArrayList<>();

        for (CreateMemberPaymentRequest req : requests) {
            MemberPayment payment = memberPaymentRepository.save(memberId, req);
            result.add(payment);

            Integer collectivityId = memberRepository.findCollectivityId(memberId);
            if (collectivityId != null) {
                transactionRepository.save(collectivityId, memberId, payment);
            }
        }

        return result;
    }
}
