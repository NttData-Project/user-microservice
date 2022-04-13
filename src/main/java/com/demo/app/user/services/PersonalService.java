package com.demo.app.user.services;

import com.demo.app.user.entities.Personal;
import com.demo.app.user.models.CardType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface PersonalService {
    Flux<Personal> findAll();
    Mono<Personal> saveSavingAccount(Personal personal);
    Mono<Personal> saveCurrentAccount(Personal personal);
    Mono<Personal> saveFixedTermAccount(Personal personal);
    Mono<Personal> findById(String id);
    Mono<Personal> update(Personal personal,String id);
    Mono<Void> delete(String id);
}
