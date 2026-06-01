package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @PostMapping("/cafes/{id}/approve")
    @ResponseBody
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        cafeService.approve(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cafes/{id}/reject")
    @ResponseBody
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        cafeService.reject(id);
        return ResponseEntity.ok().build();
    }
}
