package de.mw.spring.example.app;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping(value = "/persons", produces = /* not MediaType.APPLICATION_NDJSON_VALUE for Array compatibility */ MediaType.APPLICATION_JSON_VALUE)
    public Stream<PersonDto> streamPersons() {
        return personService.streamAllPersons();
    }

}