package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.Collectivity;
import prog3.exam.model.requests.AssignCollectivityIdentityRequest;
import prog3.exam.model.requests.CreateCollectivityRequest;
import prog3.exam.service.CollectivityService;

import java.util.List;

@RestController
@RequestMapping("/collectivities")
public class CollectivityController {

    private final CollectivityService collectivityService;

    public CollectivityController(CollectivityService collectivityService) {
        this.collectivityService = collectivityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Collectivity> createCollectivities(
            @RequestBody List<CreateCollectivityRequest> requests) {
        return collectivityService.createCollectivities(requests);
    }


    @PutMapping("/{id}/identity")
    @ResponseStatus(HttpStatus.OK)
    public Collectivity assignIdentity(
            @PathVariable int id,
            @RequestBody AssignCollectivityIdentityRequest request) {
        return collectivityService.assignIdentity(id, request);
    }
}
