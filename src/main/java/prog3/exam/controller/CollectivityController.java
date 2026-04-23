package prog3.exam.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.Collectivity;
import prog3.exam.model.CollectivityTransaction;
import prog3.exam.model.MembershipFee;
import prog3.exam.model.requests.AssignCollectivityIdentityRequest;
import prog3.exam.model.requests.CreateCollectivityRequest;
import prog3.exam.model.requests.CreateMembershipFeeRequest;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.CollectivityTransactionRepository;
import prog3.exam.repository.FinancialAccountRepository;
import prog3.exam.service.CollectivityService;
import prog3.exam.service.MembershipFeeService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/collectivities")
public class CollectivityController {

    private final CollectivityService collectivityService;
    private final CollectivityRepository collectivityRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final CollectivityTransactionRepository transactionRepository;
    private final MembershipFeeService membershipFeeService;

    public CollectivityController(CollectivityService collectivityService,
                                   CollectivityRepository collectivityRepository,
                                   FinancialAccountRepository financialAccountRepository,
                                   CollectivityTransactionRepository transactionRepository,
                                   MembershipFeeService membershipFeeService) {
        this.collectivityService = collectivityService;
        this.collectivityRepository = collectivityRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.transactionRepository = transactionRepository;
        this.membershipFeeService = membershipFeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Collectivity> createCollectivities(
            @RequestBody List<CreateCollectivityRequest> requests) {
        return collectivityService.createCollectivities(requests);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collectivity getCollectivity(@PathVariable int id) {
        return collectivityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collectivity not found: " + id));
    }

    @PutMapping("/{id}/informations")
    @ResponseStatus(HttpStatus.OK)
    public Collectivity assignIdentity(
            @PathVariable int id,
            @RequestBody AssignCollectivityIdentityRequest request) {
        return collectivityService.assignIdentity(id, request);
    }

    @GetMapping("/{id}/membershipFees")
    public List<MembershipFee> getMembershipFees(@PathVariable int id) {
        return membershipFeeService.getByCollectivity(id);
    }

    @PostMapping("/{id}/membershipFees")
    @ResponseStatus(HttpStatus.OK)
    public List<MembershipFee> createMembershipFees(
            @PathVariable int id,
            @RequestBody List<CreateMembershipFeeRequest> requests) {
        return membershipFeeService.create(id, requests);
    }

    @GetMapping("/{id}/transactions")
    public List<CollectivityTransaction> getTransactions(
            @PathVariable int id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (!collectivityRepository.existsById(id)) {
            throw new NotFoundException("Collectivity not found: " + id);
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must be before 'to'.");
        }
        return transactionRepository.findByCollectivityAndPeriod(id, from, to);
    }

    @GetMapping("/{id}/financialAccounts")
    @ResponseStatus(HttpStatus.OK)
    public List<Object> getFinancialAccounts(
            @PathVariable int id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate at) {
        if (!collectivityRepository.existsById(id)) {
            throw new NotFoundException("Collectivity not found: " + id);
        }
        return financialAccountRepository.findByCollectivityId(id, at);
    }
}
