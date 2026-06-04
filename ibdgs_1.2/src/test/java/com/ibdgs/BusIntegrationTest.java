package com.ibdgs;

import com.ibdgs.model.Bus;
import com.ibdgs.repository.BusRepository;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BusIntegrationTest {

    private static final String FILE_PATH = "buses.txt";

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(Path.of(FILE_PATH), "");
    }

   /* @AfterEach
    void tearDown() throws Exception {
        Files.writeString(Path.of(FILE_PATH), "");
    }
*/
    //creating a dedicated test case for buses.txt
   @Test
   void demoPopulateFile() {
       BusRepository repo = new BusRepository("buses.txt");

       repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
       repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID));
       repo.add(new Bus("11111111", 20, 50.0, Bus.FUEL_DIESEL));
       repo.add(new Bus("22222222", 35, 60.0, Bus.FUEL_DIESEL));
       repo.add(new Bus("99999999", 55, 90.0, Bus.FUEL_DIESEL));
   }

    @Test
    @DisplayName("IT-B1-01: Valid bus is added and retrieved from storage")
    void itB1_01_addAndRetrieve() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL)));

        Bus got = repo.retrieve("12345678");

        assertNotNull(got);
        assertEquals(40, got.getCapacity());
        assertEquals("12345678", got.getBusID());
    }

    @Test
    @DisplayName("IT-B1-02: All bus fields persist through a write-read cycle")
    void itB1_02_allFieldsPersist() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID)));

        BusRepository reloaded = new BusRepository(FILE_PATH);

        Bus got = reloaded.retrieve("87654321");

        assertNotNull(got);
        assertEquals(30, got.getCapacity());
        assertEquals(65.5, got.getFuelLevel());
        assertEquals(Bus.FUEL_HYBRID, got.getFuelType());
    }

    @Test
    @DisplayName("IT-B1-03: Multiple valid buses are all stored and retrievable")
    void itB1_03_multipleBuses() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("11111111", 20, 50.0, Bus.FUEL_DIESEL)));
        assertTrue(repo.add(new Bus("22222222", 35, 60.0, Bus.FUEL_DIESEL)));

        assertEquals(20, repo.retrieve("11111111").getCapacity());
        assertEquals(35, repo.retrieve("22222222").getCapacity());
    }

    @Test
    @DisplayName("IT-B2-01: Duplicate busID is rejected")
    void itB2_01_duplicateRejected() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL)));

        assertFalse(repo.add(new Bus("12345678", 25, 30.0, Bus.FUEL_HYBRID)));

        assertEquals(40, repo.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B2-02: Bus with letters in ID is rejected")
    void itB2_02_nonDigitRejected() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertFalse(repo.add(new Bus("ABCD1234", 40, 80.0, Bus.FUEL_DIESEL)));

        assertNull(repo.retrieve("ABCD1234"));
    }

    @Test
    @DisplayName("IT-B2-03: Bus with ID shorter than 8 digits is rejected")
    void itB2_03_tooShortRejected() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertFalse(repo.add(new Bus("123", 40, 80.0, Bus.FUEL_DIESEL)));

        assertNull(repo.retrieve("123"));
    }

    @Test
    @DisplayName("IT-B3-01: Capacity decrease is persisted")
    void itB3_01_decreasePersisted() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL)));

        assertTrue(repo.update(
                new Bus("12345678", 35, 80.0, Bus.FUEL_DIESEL),
                30));

        BusRepository reloaded = new BusRepository(FILE_PATH);

        assertEquals(35,
                reloaded.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B3-02: Capacity increase is rejected")
    void itB3_02_increaseRejected() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("12345678", 35, 80.0, Bus.FUEL_DIESEL)));

        assertFalse(repo.update(
                new Bus("12345678", 60, 80.0, Bus.FUEL_DIESEL),
                30));

        assertEquals(35,
                repo.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B3-03: Driver older than 50 cannot update capacity >= 50")
    void itB3_03_ageRestriction() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertTrue(repo.add(new Bus("99999999", 55, 90.0, Bus.FUEL_DIESEL)));

        assertFalse(repo.update(
                new Bus("99999999", 50, 90.0, Bus.FUEL_DIESEL),
                56));

        assertEquals(55,
                repo.retrieve("99999999").getCapacity());
    }

    @Test
    @DisplayName("IT-B4-01: count() after additions")
    void itB4_01_countAfterAdds() {
        BusRepository repo = new BusRepository(FILE_PATH);

        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID));
        repo.add(new Bus("11111111", 20, 50.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("22222222", 35, 60.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("99999999", 55, 90.0, Bus.FUEL_DIESEL));

        assertEquals(5, repo.count());
    }

    @Test
    @DisplayName("IT-B4-02: count unchanged when invalid bus rejected")
    void itB4_02_countUnchangedOnReject() {
        BusRepository repo = new BusRepository(FILE_PATH);

        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));

        int before = repo.count();

        repo.add(new Bus("12345678", 20, 10.0, Bus.FUEL_HYBRID));

        assertEquals(before, repo.count());
    }

    @Test
    @DisplayName("IT-B4-03: count correct after reload")
    void itB4_03_countAfterReload() {
        BusRepository repo = new BusRepository(FILE_PATH);

        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID));

        int original = repo.count();

        BusRepository reloaded = new BusRepository(FILE_PATH);

        assertEquals(original, reloaded.count());
    }

    @Test
    @DisplayName("IT-B5-02: Updating non-existing bus is rejected")
    void itB5_02_updateNonExistingBus() {
        BusRepository repo = new BusRepository(FILE_PATH);

        assertFalse(repo.update(
                new Bus("77777777", 40, 80.0, Bus.FUEL_DIESEL),
                30));
    }
}