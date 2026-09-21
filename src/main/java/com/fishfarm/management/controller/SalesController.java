package com.fishfarm.management.controller;

import com.fishfarm.management.dto.SalesHistoryResponse;
import com.fishfarm.management.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping("/history")
    public SalesHistoryResponse getHistory() {
        return salesService.getHistory();
    }
}