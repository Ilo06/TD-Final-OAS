package prog3.exam.model.requests;

import lombok.Data;

import java.util.List;

@Data
public class CreateCollectivityRequest {
    private String id;
    private String location;
    private List<String> members;
    private Boolean federationApproval;
    private CreateCollectivityStructureRequest structure;
}
