package prog3.exam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectivityStructure {
    private Member president;
    private Member vicePresident;
    private Member treasurer;
    private Member secretary;
}
