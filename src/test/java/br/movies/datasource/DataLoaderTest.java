package br.movies.datasource;

import br.movies.datasource.entity.Movie;
import br.movies.datasource.entity.Producer;
import br.movies.datasource.entity.Studio;
import br.movies.datasource.repository.MovieRepository;
import br.movies.datasource.repository.ProducerRepository;
import br.movies.datasource.repository.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DataLoaderTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private ProducerRepository producerRepository;

    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new DataLoader(movieRepository, studioRepository, producerRepository);

        movieRepository.deleteAll();
        studioRepository.deleteAll();
        producerRepository.deleteAll();
    }

    @Test
    void testCsvFileExists() {
        // Verifica se o arquivo .csv existe e está acessível
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Movielist.csv");
        assertNotNull(inputStream, "Movielist.csv should exist in src/main/resources");

        try {
            inputStream.close();
        } catch (IOException e) {
            fail("Failed to close input stream");
        }
    }

    @Test
    void testCsvColumnCount() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Movielist.csv");
        assertNotNull(inputStream);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String headerLine = reader.readLine();
        assertNotNull(headerLine, "CSV should have a header line");

        String[] headers = headerLine.split(";");
        assertEquals(5, headers.length, "CSV should have exactly 5 columns");

        // Validação de nome dos headers
        assertEquals("year", headers[0].trim().toLowerCase());
        assertEquals("title", headers[1].trim().toLowerCase());
        assertEquals("studios", headers[2].trim().toLowerCase());
        assertEquals("producers", headers[3].trim().toLowerCase());
        assertEquals("winner", headers[4].trim().toLowerCase());

        reader.close();
    }

    @Test
    void testLoadCsvFile() throws Exception {
        // Teste utilizando o arquivo .csv atual
        dataLoader.run(); // Carrega o arquivo Movielist.csv

        // Verifica se os dados foram carregados
        List<Movie> movies = movieRepository.findAll();
        List<Studio> studios = studioRepository.findAll();
        List<Producer> producers = producerRepository.findAll();

        assertTrue(movies.size() > 0, "Should have loaded some movies");
        assertTrue(studios.size() > 0, "Should have created some studios");
        assertTrue(producers.size() > 0, "Should have created some producers");

        System.out.printf("Loaded: %d movies, %d studios, %d producers%n",
                movies.size(), studios.size(), producers.size());
    }

}