package de.mw.spring.example.app;

import de.mw.spring.asyncjpastreaming.AsyncJPAStreaming;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    @AsyncJPAStreaming(clearEntityManager = true, bufferCapacity = 1000)
    public Stream<PersonDto> streamAllPersons() {
        return personRepository.findAll()
                .stream()
                .map(person -> new PersonDto(person.getName()));
    }

}