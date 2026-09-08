package com.domainify.controller;

import com.domainify.dto.MarketplaceOrderDto;
import com.domainify.dto.OrderPayResponse;
import com.domainify.dto.PagedResponse;
import com.domainify.entity.User;
import com.domainify.service.MarketplaceOrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MarketplaceOrderController {

    private final MarketplaceOrderService orderService;

    public MarketplaceOrderController(MarketplaceOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/marketplace/listings/{id}/buy")
    public ResponseEntity<MarketplaceOrderDto> buyNow(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.createBuyNow(user, id));
    }

    @GetMapping("/marketplace/orders")
    public ResponseEntity<PagedResponse<MarketplaceOrderDto>> listMine(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "role", required = false) String role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.listMine(user, role, pageable));
    }

    @GetMapping("/marketplace/orders/{id}")
    public ResponseEntity<MarketplaceOrderDto> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getForUser(user, id));
    }

    @PostMapping("/marketplace/orders/{id}/pay-wallet")
    public ResponseEntity<MarketplaceOrderDto> payWithWallet(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.payWithWallet(user, id));
    }

    @PostMapping("/marketplace/orders/{id}/pay-zarinpal")
    public ResponseEntity<OrderPayResponse> payWithZarinPal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.startZarinPalPay(user, id));
    }
}
