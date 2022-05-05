package com.demo.app.user.controllers;

import com.demo.app.user.entities.Personal;
import com.demo.app.user.models.SavingAccountType;
import com.demo.app.user.services.PersonalService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/personal")
public class PersonalController {
    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public ResponseEntity<Flux<Personal>> findAll(){
        Flux<Personal> personal = personalService.findAll();
        return ResponseEntity.ok(personal);
    }
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Personal>> findById(@PathVariable String id){
        return personalService.findById(id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/saveCurrentAccount")
    public ResponseEntity<Mono<Personal>> saveCurrentAccount(@RequestBody Personal personal){
        return ResponseEntity.ok(personalService.saveCurrentAccount(personal));
    }
    @PostMapping("/saveSavingAccount/{type}")
    public ResponseEntity<Mono<Personal>> saveSavingAccount(@RequestBody Personal personal, @PathVariable SavingAccountType type){
        Mono<Personal> result;
        if (type.equals(SavingAccountType.NORMAL)) result = personalService.saveNormalSavingAccount(personal);
        else result = personalService.saveVipSavingAccount(personal);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/saveFixedTermAccount")
    public ResponseEntity<Mono<Personal>> saveFixedTermAccount(@RequestBody Personal personal){
        return ResponseEntity.ok(personalService.saveFixedTermAccount(personal));
    }
    @PostMapping("/creditAccount")
    public ResponseEntity<Mono<Personal>> saveCreditAccount(@RequestBody Personal personal){
        return ResponseEntity.ok(personalService.saveCreditAccount(personal));
    }
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Personal>> update(@RequestBody Personal personal, @PathVariable String id){
        return personalService.update(personal,id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id){
        return personalService.delete(id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
