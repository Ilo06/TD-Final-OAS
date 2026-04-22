package prog3.exam.model.requests;

import lombok.Data;
@Data
public class CreateCollectivityStructureRequest {
    private Integer president;
    private Integer vicePresident;
    private Integer treasurer;
    private Integer secretary;
}