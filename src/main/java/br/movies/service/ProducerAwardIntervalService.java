package br.movies.service;

import br.movies.datasource.entity.Movie;
import br.movies.datasource.entity.Producer;
import br.movies.datasource.repository.MovieRepository;
import br.movies.dto.ProducerInterval;
import br.movies.dto.ProducerIntervalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProducerAwardIntervalService {

    private final MovieRepository movieRepository;

    public ProducerIntervalResponse getProducerIntervals() {
        // Busca todos filmes vencedores ordenado por ano
        List<Movie> winningMovies = movieRepository.findByWinnerTrueOrderByYearMovie();

        // Agrupa filmes por produtor e os anos vencedores
        Map<Producer, List<Integer>> producerYears = new HashMap<>();

        for (Movie movie : winningMovies) {
            for (Producer producer : movie.getProducers()) {
                producerYears.computeIfAbsent(producer, k -> new ArrayList<>()).add(movie.getYearMovie());
            }
        }

        // Cálcula intervalo para produtores com múltiplos prêmios
        List<ProducerInterval> intervals = new ArrayList<>();

        for (Map.Entry<Producer, List<Integer>> entry : producerYears.entrySet()) {
            List<Integer> years = entry.getValue();
            if (years.size() >= 2) {
                Collections.sort(years);
                Producer producer = entry.getKey();

                // Calcula todos intervalos consecutivos do produtor
                for (int i = 1; i < years.size(); i++) {
                    int interval = years.get(i) - years.get(i - 1);
                    intervals.add(new ProducerInterval(
                            producer.getName(),
                            interval,
                            years.get(i - 1),
                            years.get(i)
                    ));
                }
            }
        }

        // Caso não haja intervalos
        if (intervals.isEmpty()) {
            return new ProducerIntervalResponse(new ArrayList<>(), new ArrayList<>());
        }

        // Calcula o valor mínimo e o máximo
        int minIntervalValue = intervals.stream()
                .mapToInt(ProducerInterval::getInterval)
                .min()
                .orElse(0);

        int maxIntervalValue = intervals.stream()
                .mapToInt(ProducerInterval::getInterval)
                .max()
                .orElse(0);

        // Agrupa por intervalo mínimo e máximo, garantido que intervalos iguais sejam considerados
        List<ProducerInterval> minIntervals = intervals.stream()
                .filter(pi -> pi.getInterval() == minIntervalValue)
                .collect(Collectors.toList());

        List<ProducerInterval> maxIntervals = intervals.stream()
                .filter(pi -> pi.getInterval() == maxIntervalValue)
                .collect(Collectors.toList());

        return new ProducerIntervalResponse(minIntervals, maxIntervals);
    }
}