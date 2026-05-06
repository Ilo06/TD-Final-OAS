package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.CollectivityActivity;
import prog3.exam.model.requests.CreateCollectivityActivityRequest;
import prog3.exam.repository.CollectivityActivityRepository;
import prog3.exam.repository.CollectivityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CollectivityActivityService {

    private final CollectivityActivityRepository activityRepository;
    private final CollectivityRepository collectivityRepository;

    public CollectivityActivityService(CollectivityActivityRepository activityRepository,
                                       CollectivityRepository collectivityRepository) {
        this.activityRepository = activityRepository;
        this.collectivityRepository = collectivityRepository;
    }

    public List<CollectivityActivity> addActivities(String collectivityId,
                                                    List<CreateCollectivityActivityRequest> requests) {
        if (!collectivityRepository.existsById(collectivityId)) {
            throw new NotFoundException("Collectivity not found: " + collectivityId);
        }

        List<CollectivityActivity> result = new ArrayList<>();
        for (CreateCollectivityActivityRequest req : requests) {
            // Validation: recurrenceRule and executiveDate are mutually exclusive
            if (req.getRecurrenceRule() != null && req.getExecutiveDate() != null) {
                throw new BadRequestException(
                        "An activity cannot have both a recurrence rule and an executive date at the same time.");
            }
            // At least one must be provided
            if (req.getRecurrenceRule() == null && req.getExecutiveDate() == null) {
                throw new BadRequestException(
                        "An activity must have either a recurrence rule or an executive date.");
            }
            // Validate recurrence rule fields if present
            if (req.getRecurrenceRule() != null) {
                Integer weekOrdinal = req.getRecurrenceRule().getWeekOrdinal();
                if (weekOrdinal == null || weekOrdinal < 1 || weekOrdinal > 5) {
                    throw new BadRequestException("weekOrdinal must be between 1 and 5.");
                }
                if (req.getRecurrenceRule().getDayOfWeek() == null) {
                    throw new BadRequestException("dayOfWeek is required in recurrence rule.");
                }
            }

            String activityId = UUID.randomUUID().toString();
            CollectivityActivity saved = activityRepository.save(collectivityId, activityId, req);
            result.add(saved);
        }
        return result;
    }

    public List<CollectivityActivity> getActivities(String collectivityId) {
        if (!collectivityRepository.existsById(collectivityId)) {
            throw new NotFoundException("Collectivity not found: " + collectivityId);
        }
        return activityRepository.findByCollectivityId(collectivityId);
    }
}
