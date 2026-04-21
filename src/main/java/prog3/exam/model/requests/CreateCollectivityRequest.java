package prog3.exam.model.requests;

import lombok.Data;

import java.util.List;

@Data
public class CreateCollectivityRequest {
    private String location;
    private List<Integer> members;
    private Boolean federationApproval;
    private CreateCollectivityStructureRequest structure;

    @Data
    public static class CreateCollectivityStructureRequest {
        private int president;
        private int vicePresident;
        private int treasurer;
        private int secretary;
    }
}
