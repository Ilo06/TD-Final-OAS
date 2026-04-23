package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.MemberPayment;
import prog3.exam.model.requests.CreateMemberPaymentRequest;
import prog3.exam.service.MemberPaymentService;

import java.util.List;

@RestController
@RequestMapping("/members/{id}/payments")
public class MemberPaymentController {

    private final MemberPaymentService memberPaymentService;

    public MemberPaymentController(MemberPaymentService memberPaymentService) {
        this.memberPaymentService = memberPaymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<MemberPayment> createPayments(@PathVariable String id,
                                               @RequestBody List<CreateMemberPaymentRequest> requests) {
        return memberPaymentService.createPayments(id, requests);
    }
}
