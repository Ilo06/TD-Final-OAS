package prog3.exam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collectivity {
    private String id;
    private Integer number;
    private String name;
    private String location;
    private CollectivityStructure structure;
    private List<Member> members;
}
