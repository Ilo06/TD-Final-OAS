package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.enums.Gender;
import prog3.exam.model.enums.MemberOccupation;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateMemberRequest {
    private String id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String profession;
    private String phoneNumber;
    private String email;
    private MemberOccupation occupation;

    private String collectivityIdentifier;
    private List<String> referees;
    private Boolean registrationFeePaid;
    private Boolean membershipDuesPaid;
}
