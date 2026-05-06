package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.CollectivityActivity;
import prog3.exam.model.requests.CreateCollectivityActivityRequest;
import prog3.exam.service.CollectivityActivityService;

import java.util.List;

@RestController
@RequestMapping("/collectivities/{id}/activities")
public class CollectivityActivityController {

    private final CollectivityActivityService activityService;

    public CollectivityActivityController(CollectivityActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CollectivityActivity> addActivities(
            @PathVariable String id,
            @RequestBody List<CreateCollectivityActivityRequest> requests) {
        return activityService.addActivities(id, requests);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CollectivityActivity> getActivities(@PathVariable String id) {
        return activityService.getActivities(id);
    }
}
