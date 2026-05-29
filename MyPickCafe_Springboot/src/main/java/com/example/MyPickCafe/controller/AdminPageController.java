package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final CafeService cafeService;

    @GetMapping
    public String main(Model model) {
        model.addAttribute("pendingCafes", cafeService.findByStatus(CafeStatus.PENDING));
        model.addAttribute("statTotalCafes", cafeService.countAll());
        model.addAttribute("statApprovedCafes", cafeService.countByStatus(CafeStatus.APPROVED));
        model.addAttribute("statPendingCafes", cafeService.countByStatus(CafeStatus.PENDING));
        model.addAttribute("statRejectedCafes", cafeService.countByStatus(CafeStatus.REJECTED));
        return "admin/main";
    }
}
