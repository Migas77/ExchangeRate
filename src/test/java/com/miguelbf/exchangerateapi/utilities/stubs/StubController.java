package com.miguelbf.exchangerateapi.utilities.stubs;

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

}
