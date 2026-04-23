package prog3.exam.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.Collectivity;
import prog3.exam.model.requests.AssignCollectivityIdentityRequest;
import prog3.exam.model.requests.CreateCollectivityRequest;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.FinancialAccountRepository;
import prog3.exam.service.CollectivityService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/collectivities")
public class CollectivityController {

    private final CollectivityService collectivityService;
    private final CollectivityRepository collectivityRepository;
    private final FinancialAccountRepository financialAccountRepository;

    public CollectivityController(CollectivityService collectivityService,
                                   CollectivityRepository collectivityRepository,
                                   FinancialAccountRepository financialAccountRepository) {
        this.collectivityService = collectivityService;
        this.collectivityRepository = collectivityRepository;
        this.financialAccountRepository = financialAccountRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Collectivity> createCollectivities(
            @RequestBody List<CreateCollectivityRequest> requests) {
        return collectivityService.createCollectivities(requests);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collectivity getCollectivity(@PathVariable String id) {
        return collectivityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collectivity not found: " + id));
    }

    @PutMapping("/{id}/identity")
    @ResponseStatus(HttpStatus.OK)
    public Collectivity assignIdentity(
            @PathVariable String id,
            @RequestBody AssignCollectivityIdentityRequest request) {
        return collectivityService.assignIdentity(id, request);
    }

    @GetMapping("/{id}/financialAccounts")
    @ResponseStatus(HttpStatus.OK)
    public List<Object> getFinancialAccounts(
            @PathVariable String id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate at) {

        if (!collectivityRepository.existsById(id)) {
            throw new NotFoundException("Collectivity not found: " + id);
        }
        return financialAccountRepository.findByCollectivityId(id, at);
    }
}
