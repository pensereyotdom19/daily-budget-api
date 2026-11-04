package com.pensereyotdom19.dailybudget.bill;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.bill.BillDtos.BillRequest;
import com.pensereyotdom19.dailybudget.bill.BillDtos.BillResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillController {
    private final BillService bills;

    public BillController(BillService bills) {
        this.bills = bills;
    }

    @GetMapping
    public List<BillResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return bills.list(CurrentUser.id(jwt));
    }

    /** PUT is safe to repeat: sending the same bill twice gives the same result. Good for flaky phone internet. */
    @PutMapping("/{id}")
    public BillResponse put(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody BillRequest body) {
        return bills.put(CurrentUser.id(jwt), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        bills.delete(CurrentUser.id(jwt), id);
    }
}
