package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.Member;
import prog3.exam.model.MemberPayment;
import prog3.exam.model.requests.CreateMemberPaymentRequest;
import prog3.exam.model.requests.CreateMemberRequest;
import prog3.exam.service.MemberPaymentService;
import prog3.exam.service.MemberService;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberPaymentService memberPaymentService;

    public MemberController(MemberService memberService,
                             MemberPaymentService memberPaymentService) {
        this.memberService = memberService;
        this.memberPaymentService = memberPaymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Member> createMembers(@RequestBody List<CreateMemberRequest> requests) {
        return memberService.createMembers(requests);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MemberPayment> createPayments(
            @PathVariable int id,
            @RequestBody List<CreateMemberPaymentRequest> requests) {
        return memberPaymentService.createPayments(id, requests);
    }
}
