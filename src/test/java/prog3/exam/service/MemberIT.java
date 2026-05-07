package prog3.exam.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import prog3.exam.api.ApiClient;
import prog3.exam.api.model.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MemberIT {
    final ApiClient apiClient = new ApiClient();

    @Test
    void member_created_ok() {
        var createMember = new CreateMember();
        createMember.firstName = "random";
        createMember.lastName = "random";
        createMember.birthDate = LocalDate.now();
        createMember.collectivityIdentifier = "col-1";
        createMember.occupation = MemberOccupation.JUNIOR;
        createMember.gender = Gender.MALE;
        createMember.phoneNumber = 719203;
        createMember.membershipDuesPaid = true;
        createMember.email = "test@mail.com";
        createMember.registrationFeePaid = true;
        createMember.profession = "random";
        createMember.address = "random";
        createMember.referees = List.of("C1-M1", "C1-M2");

        var actual = apiClient.post("/members", List.of(createMember), new ParameterizedTypeReference<List<Member>>() {
        });

        assertNotNull(actual);
        log.info(actual.toString());

    }

    @Test
    void member_created_ko() {
        var createMember = new CreateMember();
        createMember.firstName = "random";
        createMember.lastName = "random";
        createMember.birthDate = LocalDate.now();
        createMember.collectivityIdentifier = "col-1";
        createMember.occupation = MemberOccupation.JUNIOR;
        createMember.phoneNumber = 719203;
        createMember.membershipDuesPaid = false;
        createMember.registrationFeePaid = false;
        createMember.address = "random";
        createMember.referees = List.of("C1-M1", "C1-M2");

        var exception = assertThrows(RuntimeException.class, () -> apiClient.post("/members", List.of(createMember), new ParameterizedTypeReference<List<Member>>() {
        }));

        var exceptionMessage = exception.getMessage();
        assertTrue(exception.getMessage().contains("HTTP Error: 400"));
        log.info(exceptionMessage);
    }

    @Test
    void member_add_payment_ok() {
        var memberId = "C1-M8";
        var createMemberPayment = new CreateMemberPayment();
        createMemberPayment.paymentMode = PaymentMode.CASH;
        createMemberPayment.amount = 20000;
        createMemberPayment.accountCreditedIdentifier = "C1-A-CASH";
        createMemberPayment.membershipFeeIdentifier = "cot-1";

        var actualPayments = apiClient.post("/members/" + memberId + "/payments", List.of(createMemberPayment), new ParameterizedTypeReference<List<MemberPayment>>() {
        });

        assertNotNull(actualPayments);
        log.info(actualPayments.toString());
    }
}
