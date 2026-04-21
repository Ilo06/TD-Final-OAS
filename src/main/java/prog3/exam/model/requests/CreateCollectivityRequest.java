package prog3.exam.model.requests;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateCollectivityRequest {
    private String location;
    private List<UUID> members;
    private Boolean federationApproval;
    private CreateCollectivityStructureRequest structure;

    @Data
    public static class CreateCollectivityStructureRequest {
        private UUID president;
        private UUID vicePresident;
        private UUID treasurer;
        private UUID secretary;
    }
}
