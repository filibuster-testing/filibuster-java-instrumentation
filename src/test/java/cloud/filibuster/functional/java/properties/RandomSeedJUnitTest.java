package cloud.filibuster.functional.java.properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Random;

import static cloud.filibuster.instrumentation.helpers.Property.getRandomSeedProperty;
import static cloud.filibuster.instrumentation.helpers.Property.setRandomSeedProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RandomSeedJUnitTest {
    private static int originalSeed;
    @BeforeAll
    public static void beforeAll() {
        originalSeed = getRandomSeedProperty();
    }
    @Test
    @Order(1)
    public void testRedisPortNonDeterminism() {
        // Create a new seed
        Random rand = new Random();
        int randomSeed = rand.nextInt();

        // Set the seed property
        setRandomSeedProperty(randomSeed);

        // Check that the seed property is correctly set
        assertEquals(randomSeed, getRandomSeedProperty());
    }

    @AfterAll
    public static void afterAll() {
        setRandomSeedProperty(originalSeed);
    }

}
