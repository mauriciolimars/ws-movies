package br.movies.datasource;

import br.movies.datasource.entity.Movie;
import br.movies.datasource.entity.Producer;
import br.movies.datasource.entity.Studio;
import br.movies.datasource.repository.MovieRepository;
import br.movies.datasource.repository.ProducerRepository;
import br.movies.datasource.repository.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoaderCmd implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final StudioRepository studioRepository;
    private final ProducerRepository producerRepository;

    @Override
    public void run(String... args) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Movielist.csv");
        if (inputStream == null) {
            throw new FileNotFoundException("Movielist.csv not found in resources folder");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        boolean isFirstLine = true;

        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }

            String[] tokens = line.split(";");

            Integer year = Integer.parseInt(tokens[0].trim());
            String title = tokens[1].trim();
            String[] studiosArray = tokens[2].split(",");
            String[] producersArray = tokens[3].split(",");
            Boolean winner;
            try {
                winner = tokens[4].trim().equalsIgnoreCase("yes");
            } catch (Exception e){
                winner = false;
            }

            List<Studio> studios = Arrays.stream(studiosArray)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(this::findOrCreateStudio)
                    .collect(Collectors.toList());

            List<Producer> producers = Arrays.stream(producersArray)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(this::findOrCreateProducer)
                    .collect(Collectors.toList());

            Movie movie = new Movie();
            movie.setYear_movie(year);
            movie.setTitle(title);
            movie.setWinner(winner);
            movie.setStudios(studios);
            movie.setProducers(producers);

            movieRepository.save(movie);
        }

        reader.close();
    }

    private Studio findOrCreateStudio(String name) {
        return studioRepository.findByName(name)
                .orElseGet(() -> {
                    Studio studio = new Studio();
                    studio.setName(name);
                    return studioRepository.save(studio);
                });
    }

    private Producer findOrCreateProducer(String name) {
        return producerRepository.findByName(name)
                .orElseGet(() -> {
                    Producer producer = new Producer();
                    producer.setName(name);
                    return producerRepository.save(producer);
                });
    }
}