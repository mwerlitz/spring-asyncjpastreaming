package de.mw.spring.example.app;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    @QueryHints(value = {
            @QueryHint(name = AvailableHints.HINT_FETCH_SIZE, value = "1000"),
            @QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "false"),
            @QueryHint(name = AvailableHints.HINT_READ_ONLY, value = "true")
    })
    @Override
    List<Person> findAll();

}