package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.enums.Gender;
import prog3.exam.model.enums.MemberOccupation;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateMemberRequest {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String profession;
    private String phoneNumber;
    private String email;
    private MemberOccupation occupation;

    private Integer collectivityIdentifier;
    private List<Integer> referees;
    private Boolean registrationFeePaid;
    private Boolean membershipDuesPaid;
}
