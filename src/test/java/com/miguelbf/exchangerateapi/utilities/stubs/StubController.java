package com.miguelbf.exchangerateapi.utilities.stubs;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StubController {

    private final StubService stubService;

    StubController(StubService stubService) {
        this.stubService = stubService;
    }

    @GetMapping("/stub")
    public void get() {
        stubService.call();
    }

    @Secured("ROLE_PREMIUM_TIER")
    @GetMapping("/stub/premium")
    public void getPremium() {
        stubService.premiumCall();
    }

}
