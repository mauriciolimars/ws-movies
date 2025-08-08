package br.movies.controller.impl;

import br.movies.dto.ResponseDto;
import br.movies.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class MovieControllerImpl {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public ResponseEntity<ResponseDto> getMovies(){
        return null;
    }
}
