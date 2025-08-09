package br.movies.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProducerInterval {
    private String producer;
    private Integer interval;
    private Integer previousWin;
    private Integer followingWin;
}