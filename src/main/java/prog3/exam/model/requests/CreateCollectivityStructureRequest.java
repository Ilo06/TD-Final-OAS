package prog3.exam.model.requests;

import lombok.Data;

@Data
public class CreateCollectivityStructureRequest {
    private String president;
    private String vicePresident;
    private String treasurer;
    private String secretary;
}
