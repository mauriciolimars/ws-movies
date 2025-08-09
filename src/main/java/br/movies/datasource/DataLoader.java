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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

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
            String producersString = tokens[3].trim();
            boolean winner;
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

            // Processa produtores com "," e "and"
            List<Producer> producers = parseProducers(producersString);

            Movie movie = new Movie();
            movie.setYearMovie(year);
            movie.setTitle(title);
            movie.setWinner(winner);
            movie.setStudios(studios);
            movie.setProducers(producers);

            movieRepository.save(movie);
        }

        reader.close();
    }

    private List<Producer> parseProducers(String producersString) {
        List<Producer> producers = new ArrayList<>();

        if (producersString == null || producersString.trim().isEmpty()) {
            return producers;
        }

        // "Produtor A and Produtor B"
        String[] producerGroups = producersString.split("\\s+and\\s+");

        for (String group : producerGroups) {
            // Separação por "," "Produtor A, Produtor B and Produtor C"
            String[] individualProducers = group.split(",");

            for (String producerName : individualProducers) {
                String cleanName = producerName.trim();
                if (!cleanName.isEmpty()) {
                    producers.add(findOrCreateProducer(cleanName));
                }
            }
        }

        return producers;
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