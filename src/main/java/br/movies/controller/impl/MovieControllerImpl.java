package br.movies.controller.impl;

import br.movies.dto.ProducerIntervalResponse;
import br.movies.service.ProducerAwardIntervalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController()
@RequiredArgsConstructor
public class MovieControllerImpl {

    private final ProducerAwardIntervalService producerAwardIntervalService;

    @GetMapping("/producers/awards-intervals")
    public ResponseEntity<ProducerIntervalResponse> getProducerAwardIntervals() {
        ProducerIntervalResponse response = producerAwardIntervalService.getProducerIntervals();
        return ResponseEntity.ok(response);
    }
}
