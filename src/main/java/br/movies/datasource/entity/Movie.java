package br.movies.datasource.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year_movie;
    private String title;

    @ManyToMany
    private List<Studio> studios;

    @ManyToMany
    private List<Producer> producers;

    private Boolean winner = false;
}