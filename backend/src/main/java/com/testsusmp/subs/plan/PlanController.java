package com.testsusmp.subs.plan;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlanView> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.list(activeOnly);
    }

    @GetMapping("/{id}")
    public PlanView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanView create(@Valid @RequestBody PlanRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PlanView update(@PathVariable long id, @Valid @RequestBody PlanRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {
        service.deactivate(id);
    }

    /** Sincroniza con los planes existentes en Mercado Pago. */
    @PostMapping("/sync")
    public PlanService.PlanSyncResult sync() {
        return service.syncFromGateway();
    }

    @PostMapping("/{id}/activate")
    public PlanView reactivate(@PathVariable long id) {
        return service.reactivate(id);
    }
}
