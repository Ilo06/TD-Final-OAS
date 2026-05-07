package prog3.exam.service;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import prog3.exam.api.ApiClient;
import prog3.exam.api.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class CollectivityIT {
    final ApiClient apiClient = new ApiClient();

    @Test
    void collectivity_not_found() {
        var id = "col-10";

        var exception = assertThrows(RuntimeException.class, () -> apiClient.get("/collectivities/" + id, Collectivity.class));

        var exceptionMessage = exception.getMessage();
        log.info(exceptionMessage);
        assertTrue(exceptionMessage.contains("HTTP Error: 404"));
    }

    @Test
    void retrieve_collectivity_by_id_one() {
        var id = "col-1";

        var collectivityOne = apiClient.get("/collectivities/" + id, Collectivity.class);

        log.info(collectivityOne.toString());
        assertNotNull(collectivityOne, "Unable to obtain collectivity");
        assertTrue(collectivityOne.name.contains("Mpanorina"), "Collectivity name obtained not as expected " + collectivityOne.name);
        assertTrue(collectivityOne.location.contains("Ambatondrazaka"), "Collectivity location obtained not as expected " + collectivityOne.location);
        assertNotNull(collectivityOne.structure, "Collectivity structure not null");
        assertNotNull(collectivityOne.members, "Null members for collectivity");
        collectivityOne.members.forEach(member ->
        {
            if (!member.id.equals("C1-M1") && !member.id.equals("C1-M2")) {
                assertNotNull(member.referees, "Referees null");
                var refereeIds = member.referees.stream().map(m -> m.id).toList();
                assertTrue(refereeIds.contains("C1-M1") || refereeIds.contains("C1-M6"), "Unexpected referee ID for member " + member.id);
                assertTrue(refereeIds.contains("C1-M2") || refereeIds.contains("C1-M7"));
            } else {
                assertTrue(member.referees == null || member.referees.isEmpty(), "Referees for C1-M1 and C1-M2 empty");
            }
        });
        var collectivityStructure = collectivityOne.structure;
        assertTrue(collectivityStructure.president != null && collectivityStructure.president.id.equals("C1-M1"));
        assertTrue(collectivityStructure.vicePresident != null && collectivityStructure.vicePresident.id.equals("C1-M2"));
        assertTrue(collectivityStructure.secretary != null && collectivityStructure.secretary.id.equals("C1-M3"));
        assertTrue(collectivityStructure.treasurer != null && collectivityStructure.treasurer.id.equals("C1-M4"));
    }

    @Test
    void create_collectivity_ko() {
        var createCollectivity = new CreateCollectivity();
        var createCollectivityStructure = new CreateCollectivityStructure();
        createCollectivityStructure.president = "C3-M7";
        createCollectivityStructure.vicePresident = "C3-M8";
        createCollectivityStructure.secretary = "C3-M6";
        createCollectivityStructure.treasurer = "C3-M5";
        createCollectivity.structure = createCollectivityStructure;
        createCollectivity.location = "Antananarivo";
        createCollectivity.federationApproval = true;
        createCollectivity.members = List.of("C1-M1", "C1-M2", "C1-M3", "C1-M4");

        var exception = assertThrows(RuntimeException.class,
                () -> apiClient.post("/collectivities", List.of(createCollectivity), new ParameterizedTypeReference<List<Collectivity>>() {
                        }
                ));

        log.info(exception.getMessage());
    }

    @Test
    void change_name() {
        var id = "col-1";
        var payload = new CollectivityInformation();
        payload.name = "Other";

        var actualCollectivity = apiClient.put("/collectivities/" + id + "/informations", payload, Collectivity.class);

        assertNotNull(actualCollectivity, "Unable to obtain financial accounts for collectivity.id=" + id);
        log.info("Collectivity: " + actualCollectivity);
    }

    @Test
    void get_financial_account() {
        var id = "col-1";

        var financialAccounts = apiClient.get("/collectivities/" + id + "/financialAccounts", String.class);

        assertNotNull(financialAccounts, "Unable to obtain financial accounts for collectivity.id=" + id);
        log.info("FinancialAccounts: " + financialAccounts);
    }

    @Test
    void get_financial_account_at() {
        var id = "col-1";
        var date = "2026-01-31";

        var financialAccounts = apiClient.get("/collectivities/" + id + "/financialAccounts?at=" + date, String.class);

        assertNotNull(financialAccounts, "Unable to obtain financial accounts at " + date + " for collectivity.id=" + id);
        log.info("FinancialAccounts: " + financialAccounts);
    }

    @Test
    void get_transactions_from_a_period() {
        var id = "col-1";
        var from = "2026-01-01";
        var to = "2026-04-30";

        var collectivityTransactions = apiClient.get("/collectivities/" + id + "/transactions?from=" + from + "&to=" + to, new ParameterizedTypeReference<List<CollectivityTransaction>>() {
        });

        assertNotNull(collectivityTransactions, "Unable to obtain collectivity transactions for collectivity.id=" + id);
        log.info("Transactions : " + collectivityTransactions);
        var totalAmount = collectivityTransactions.stream()
                .map(collectivityTransaction -> collectivityTransaction.amount.doubleValue())
                .reduce(0.0, Double::sum);
        assertTrue(770000.0 == totalAmount || totalAmount == 750000.0, "Collectivity transactions amount not as expected, actual is = " + totalAmount);
    }
}
