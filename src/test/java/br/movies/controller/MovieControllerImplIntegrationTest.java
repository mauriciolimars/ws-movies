package br.movies.controller;

import br.movies.datasource.entity.Movie;
import br.movies.datasource.entity.Producer;
import br.movies.datasource.entity.Studio;
import br.movies.datasource.repository.MovieRepository;
import br.movies.datasource.repository.ProducerRepository;
import br.movies.datasource.repository.StudioRepository;
import br.movies.dto.ProducerInterval;
import br.movies.dto.ProducerIntervalResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerImplIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ProducerRepository producerRepository;

    @Autowired
    private StudioRepository studioRepository;

    @BeforeAll
    void globalSetup() {
        clearAllData();
    }

    @AfterAll
    void globalShutdown() {
        clearAllData();
    }

    @BeforeEach
    void setup() {
        // Garante que a base de dados esteja limpa antes de iniciar cada teste
        clearAllData();
    }

    private void clearAllData() {
        try {
            movieRepository.deleteAll();
            movieRepository.flush();

            producerRepository.deleteAll();
            producerRepository.flush();

            studioRepository.deleteAll();
            studioRepository.flush();

            // Verifica se a base está limpa
            assertThat(movieRepository.count()).isEqualTo(0);
            assertThat(producerRepository.count()).isEqualTo(0);
            assertThat(studioRepository.count()).isEqualTo(0);
        } catch (Exception e) {
            System.err.println("Error clearing database: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should return producer intervals when multiple producers have wins")
    void shouldReturnProducerIntervalsWithMultipleWins() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());
        Producer producer2 = producerRepository.save(Producer.builder().name("Producer Two").build());
        Producer producer3 = producerRepository.save(Producer.builder().name("Producer Three").build());

        // Cria filmes com diferentes intervalos para produtores
        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1985, producer1, studio); // intervalo de 5 anos
        createWinningMovie("Movie 3", 1990, producer1, studio); // intervalo de 5 anos

        createWinningMovie("Movie 4", 1982, producer2, studio);
        createWinningMovie("Movie 5", 1984, producer2, studio); // intervalo de 2 anos (min)

        createWinningMovie("Movie 6", 1981, producer3, studio);
        createWinningMovie("Movie 7", 1991, producer3, studio); // intervalo de 10 anos (max)

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();

        // Verifica intervalo mínimo
        assertThat(body.getMin()).hasSize(1);
        ProducerInterval minInterval = body.getMin().get(0);
        assertThat(minInterval.getProducer()).isEqualTo("Producer Two");
        assertThat(minInterval.getInterval()).isEqualTo(2);
        assertThat(minInterval.getPreviousWin()).isEqualTo(1982);
        assertThat(minInterval.getFollowingWin()).isEqualTo(1984);

        // Verifica intervalo máximo
        assertThat(body.getMax()).hasSize(1);
        ProducerInterval maxInterval = body.getMax().get(0);
        assertThat(maxInterval.getProducer()).isEqualTo("Producer Three");
        assertThat(maxInterval.getInterval()).isEqualTo(10);
        assertThat(maxInterval.getPreviousWin()).isEqualTo(1981);
        assertThat(maxInterval.getFollowingWin()).isEqualTo(1991);
    }

    @Test
    @DisplayName("Should return empty lists when no producers have multiple wins")
    void shouldReturnEmptyWhenNoMultipleWins() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());
        Producer producer2 = producerRepository.save(Producer.builder().name("Producer Two").build());
        Producer producer3 = producerRepository.save(Producer.builder().name("Producer Three").build());

        // Cada produtor tem somente um prêmio
        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1985, producer2, studio);
        createWinningMovie("Movie 3", 1990, producer3, studio);

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Não deve haver vencedores consecutivos
        ProducerIntervalResponse body = response.getBody();
        assertThat(body.getMin()).isEmpty();
        assertThat(body.getMax()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty lists when no winning movies exist")
    void shouldReturnEmptyWhenNoWinningMovies() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());
        Producer producer2 = producerRepository.save(Producer.builder().name("Producer Two").build());

        // Cria filmes sem prêmio
        createNonWinningMovie("Movie 1", 1980, producer1, studio);
        createNonWinningMovie("Movie 2", 1985, producer2, studio);

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Não deve haver vencedores
        ProducerIntervalResponse body = response.getBody();
        assertThat(body.getMin()).isEmpty();
        assertThat(body.getMax()).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple producers with same minimum interval")
    void shouldHandleMultipleProducersWithSameMinInterval() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());
        Producer producer2 = producerRepository.save(Producer.builder().name("Producer Two").build());
        Producer producer3 = producerRepository.save(Producer.builder().name("Producer Three").build());

        // Dois produtores com o mesmo intervalo
        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1981, producer1, studio); // 1 ano de intervalo

        createWinningMovie("Movie 3", 1985, producer2, studio);
        createWinningMovie("Movie 4", 1986, producer2, studio); // 1 ano de intervalo

        createWinningMovie("Movie 5", 1990, producer3, studio);
        createWinningMovie("Movie 6", 1995, producer3, studio); // 5 anos de intervalo (max)

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();

        // Verifica se os 2 produtores estão no intervalo mínimo
        assertThat(body.getMin()).hasSize(2);
        assertThat(body.getMin())
                .extracting(ProducerInterval::getProducer)
                .containsExactlyInAnyOrder("Producer One", "Producer Two");

        assertThat(body.getMin())
                .allMatch(interval -> interval.getInterval() == 1);

        // Verifica o intervalo máximo
        assertThat(body.getMax()).hasSize(1);
        assertThat(body.getMax().get(0).getProducer()).isEqualTo("Producer Three");
        assertThat(body.getMax().get(0).getInterval()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle producer with multiple consecutive wins")
    void shouldHandleMultipleConsecutiveWins() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());

        // Produtor com múltiplos intervalos
        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1981, producer1, studio); // 1 ano de intervalo
        createWinningMovie("Movie 3", 1983, producer1, studio); // 2 anos de intervalo
        createWinningMovie("Movie 4", 1987, producer1, studio); // 4 anos de intervalo

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();

        // Deve haver intervalo mínimo de 1 ano
        assertThat(body.getMin()).hasSize(1);
        ProducerInterval minInterval = body.getMin().get(0);
        assertThat(minInterval.getProducer()).isEqualTo("Producer One");
        assertThat(minInterval.getInterval()).isEqualTo(1);
        assertThat(minInterval.getPreviousWin()).isEqualTo(1980);
        assertThat(minInterval.getFollowingWin()).isEqualTo(1981);

        // Deve haver intervalo máximno de 4 anos
        assertThat(body.getMax()).hasSize(1);
        ProducerInterval maxInterval = body.getMax().get(0);
        assertThat(maxInterval.getProducer()).isEqualTo("Producer One");
        assertThat(maxInterval.getInterval()).isEqualTo(4);
        assertThat(maxInterval.getPreviousWin()).isEqualTo(1983);
        assertThat(maxInterval.getFollowingWin()).isEqualTo(1987);
    }

    @Test
    @DisplayName("Should handle movies with multiple producers")
    void shouldHandleMoviesWithMultipleProducers() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());
        Producer producer2 = producerRepository.save(Producer.builder().name("Producer Two").build());

        // Filme com múltiplos produtores
        createWinningMovieWithMultipleProducers("Movie 1", 1980, Arrays.asList(producer1, producer2), studio);
        createWinningMovie("Movie 2", 1985, producer1, studio); // 5 anos de intervalo produtor 1
        createWinningMovie("Movie 3", 1983, producer2, studio); // 3 anos de intervalo produtor 2

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();

        // Deve haver os 2 produtores com seus respectivos intervalos
        assertThat(body.getMin()).hasSize(1);
        assertThat(body.getMin().get(0).getProducer()).isEqualTo("Producer Two");
        assertThat(body.getMin().get(0).getInterval()).isEqualTo(3);

        assertThat(body.getMax()).hasSize(1);
        assertThat(body.getMax().get(0).getProducer()).isEqualTo("Producer One");
        assertThat(body.getMax().get(0).getInterval()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle same min and max intervals")
    void shouldHandleSameMinAndMaxIntervals() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());

        // Apenas 1 produtor com prêmio consecutivo (intervalo mínimo e máximo será o mesmo)
        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1985, producer1, studio); // 5 anos de intervalo

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();

        // Mesmo intervalo deve ser o max e o min
        assertThat(body.getMin()).hasSize(1);
        assertThat(body.getMax()).hasSize(1);

        ProducerInterval minInterval = body.getMin().get(0);
        ProducerInterval maxInterval = body.getMax().get(0);

        assertThat(minInterval.getInterval()).isEqualTo(5);
        assertThat(maxInterval.getInterval()).isEqualTo(5);
        assertThat(minInterval.getProducer()).isEqualTo("Producer One");
        assertThat(maxInterval.getProducer()).isEqualTo("Producer One");
    }

    @Test
    @DisplayName("Should return correct response structure")
    void shouldReturnCorrectResponseStructure() {
        Studio studio = studioRepository.save(Studio.builder().name("Test Studio").build());
        Producer producer1 = producerRepository.save(Producer.builder().name("Producer One").build());

        createWinningMovie("Movie 1", 1980, producer1, studio);
        createWinningMovie("Movie 2", 1985, producer1, studio);

        // When
        String url = "http://localhost:" + port + "/ws-movies/producers/awards-intervals";
        ResponseEntity<ProducerIntervalResponse> response = restTemplate.getForEntity(url, ProducerIntervalResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ProducerIntervalResponse body = response.getBody();
        assertThat(body.getMin()).isNotNull();
        assertThat(body.getMax()).isNotNull();

        if (!body.getMin().isEmpty()) {
            ProducerInterval interval = body.getMin().get(0);
            assertThat(interval.getProducer()).isNotNull();
            assertThat(interval.getInterval()).isNotNull();
            assertThat(interval.getPreviousWin()).isNotNull();
            assertThat(interval.getFollowingWin()).isNotNull();
        }
    }

    // Métodos úteis
    private void createWinningMovie(String title, Integer year, Producer producer, Studio studio) {
        Set<Producer> producers = new HashSet<>();
        producers.add(producer);

        Set<Studio> studios = new HashSet<>();
        studios.add(studio);

        Movie movie = Movie.builder()
                .title(title)
                .yearMovie(year)
                .winner(true)
                .producers(new ArrayList<>(producers))
                .studios(new ArrayList<>(studios))
                .build();

        movieRepository.save(movie);
    }

    private void createNonWinningMovie(String title, Integer year, Producer producer, Studio studio) {
        Set<Producer> producers = new HashSet<>();
        producers.add(producer);

        Set<Studio> studios = new HashSet<>();
        studios.add(studio);

        Movie movie = Movie.builder()
                .title(title)
                .yearMovie(year)
                .winner(false)
                .producers(new ArrayList<>(producers))
                .studios(new ArrayList<>(studios))
                .build();

        movieRepository.save(movie);
    }

    private void createWinningMovieWithMultipleProducers(String title, Integer year, List<Producer> producers, Studio studio) {
        Set<Producer> producerSet = new HashSet<>(producers);
        Set<Studio> studios = new HashSet<>();
        studios.add(studio);

        Movie movie = Movie.builder()
                .title(title)
                .yearMovie(year)
                .winner(true)
                .producers(new ArrayList<>(producerSet))
                .studios(new ArrayList<>(studios))
                .build();

        movieRepository.save(movie);
    }
}