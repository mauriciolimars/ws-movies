package br.movies.datasource.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Studio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}