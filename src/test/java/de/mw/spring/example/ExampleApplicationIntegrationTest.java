package de.mw.spring.example;

import de.mw.spring.example.app.Person;
import de.mw.spring.example.app.PersonDto;
import de.mw.spring.example.app.PersonRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExampleApplicationIntegrationTest {

    private final static int COUNT = 10000;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    void setUp() {
        for (int i = 0; i < COUNT; i++) {
            personRepository.save(new Person("John Doe " + i));
        }
    }

    @RepeatedTest(20)
    void testAsyncStreaming() {
        var response = restTemplate.getForObject("/persons", PersonDto[].class);


        assertThat(response)
                .hasSize(COUNT)
                .allSatisfy(person -> assertThat(person.getName()).startsWith("John Doe"));
    }

}