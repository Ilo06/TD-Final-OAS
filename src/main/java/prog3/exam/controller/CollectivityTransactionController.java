package prog3.exam.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;

import prog3.exam.model.CollectivityTransaction;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.CollectivityTransactionRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/collectivities/{id}/transactions")
public class CollectivityTransactionController {

    private final CollectivityTransactionRepository transactionRepository;
    private final CollectivityRepository collectivityRepository;

    public CollectivityTransactionController(CollectivityTransactionRepository transactionRepository,
                                              CollectivityRepository collectivityRepository) {
        this.transactionRepository = transactionRepository;
        this.collectivityRepository = collectivityRepository;
    }

    @GetMapping
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
}
