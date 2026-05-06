package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.MemberOccupation;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDescription {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private MemberOccupation occupation;
}
