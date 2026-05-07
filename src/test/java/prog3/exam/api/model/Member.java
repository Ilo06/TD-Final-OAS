package prog3.exam.api.model;

import java.util.List;
import java.util.stream.Collectors;

public class Member extends MemberInformation {

    public String id;
    public List<Member> referees;

    @Override
    public String toString() {
        var refereesIds = "[]";
        if (referees != null) {
            refereesIds = referees.stream().map(member -> member.id).collect(Collectors.joining(","));
        }
        return "Member{" +
                "id='" + id + '\'' +
                ", referees=" + refereesIds +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", birthDate=" + birthDate +
                ", gender=" + gender +
                ", address='" + address + '\'' +
                ", profession='" + profession + '\'' +
                ", phoneNumber=" + phoneNumber +
                ", email='" + email + '\'' +
                ", occupation=" + occupation +
                '}';
    }
}