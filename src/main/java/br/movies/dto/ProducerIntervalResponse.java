package br.movies.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProducerIntervalResponse {
    private List<ProducerInterval> min;
    private List<ProducerInterval> max;
}