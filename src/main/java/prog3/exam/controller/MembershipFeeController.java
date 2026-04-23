package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.MembershipFee;
import prog3.exam.model.requests.CreateMembershipFeeRequest;
import prog3.exam.service.MembershipFeeService;

import java.util.List;

@RestController
@RequestMapping("/collectivities/{id}/membershipFees")
public class MembershipFeeController {

    private final MembershipFeeService membershipFeeService;

    public MembershipFeeController(MembershipFeeService membershipFeeService) {
        this.membershipFeeService = membershipFeeService;
    }

    @GetMapping
    public List<MembershipFee> getByCollectivity(@PathVariable String id) {
        return membershipFeeService.getByCollectivity(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MembershipFee> create(@PathVariable String id,
                                       @RequestBody List<CreateMembershipFeeRequest> requests) {
        return membershipFeeService.create(id, requests);
    }
}
