package com.ibdgs;

import com.ibdgs.model.Bus;
import com.ibdgs.repository.BusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 4 - Bus Integration Testing.
 *
 * <p>Integration tests that exercise {@link BusRepository} against a real TXT
 * file using the real model classes. They verify that valid buses are stored,
 * invalid buses are rejected, updates are persisted (including B2 capacity and
 * B3 age restrictions), and counts are correct across a simulated restart.
 *
 * <p>These mirror the documented integration test table (IT-B1 .. IT-B4).
 */
public class BusIntegrationTest {

    private String filePath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        filePath = tempDir.resolve("test_buses.txt").toString();
    }

    // ---------- IT-B1: valid buses stored correctly ----------

    @Test
    @DisplayName("IT-B1-01: Valid bus is added and retrieved from storage")
    void itB1_01_addAndRetrieve() {
        BusRepository repo = new BusRepository(filePath);
        assertTrue(repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL)));
        Bus got = repo.retrieve("12345678");
        assertNotNull(got);
        assertEquals(40, got.getCapacity());
        assertEquals("12345678", got.getBusID());
    }

    @Test
    @DisplayName("IT-B1-02: All bus fields persist through a write-read cycle")
    void itB1_02_allFieldsPersist() {
        BusRepository repo = new BusRepository(filePath);
        assertTrue(repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID)));

        BusRepository reloaded = new BusRepository(filePath);
        Bus got = reloaded.retrieve("87654321");
        assertNotNull(got);
        assertEquals(30, got.getCapacity());
        assertEquals(65.5, got.getFuelLevel());
        assertEquals(Bus.FUEL_HYBRID, got.getFuelType());
    }

    @Test
    @DisplayName("IT-B1-03: Multiple valid buses are all stored and retrievable")
    void itB1_03_multipleBuses() {
        BusRepository repo = new BusRepository(filePath);
        assertTrue(repo.add(new Bus("11111111", 20, 50.0, Bus.FUEL_DIESEL)));
        assertTrue(repo.add(new Bus("22222222", 35, 60.0, Bus.FUEL_DIESEL)));
        assertEquals(20, repo.retrieve("11111111").getCapacity());
        assertEquals(35, repo.retrieve("22222222").getCapacity());
    }

    // ---------- IT-B2: invalid buses rejected ----------

    @Test
    @DisplayName("IT-B2-01: Duplicate busID is rejected, original unchanged (B1)")
    void itB2_01_duplicateRejected() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        assertFalse(repo.add(new Bus("12345678", 25, 30.0, Bus.FUEL_HYBRID)));
        assertEquals(40, repo.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B2-02: Bus with letters in ID is rejected (B1)")
    void itB2_02_nonDigitRejected() {
        BusRepository repo = new BusRepository(filePath);
        assertFalse(repo.add(new Bus("ABCD1234", 40, 80.0, Bus.FUEL_DIESEL)));
        assertNull(repo.retrieve("ABCD1234"));
    }

    @Test
    @DisplayName("IT-B2-03: Bus with ID shorter than 8 digits is rejected (B1)")
    void itB2_03_tooShortRejected() {
        BusRepository repo = new BusRepository(filePath);
        assertFalse(repo.add(new Bus("123", 40, 80.0, Bus.FUEL_DIESEL)));
        assertNull(repo.retrieve("123"));
    }

    // ---------- IT-B3: updates persisted correctly ----------

    @Test
    @DisplayName("IT-B3-01: Valid capacity decrease is persisted (B2)")
    void itB3_01_decreasePersisted() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        assertTrue(repo.update(new Bus("12345678", 35, 80.0, Bus.FUEL_DIESEL), 30));

        BusRepository reloaded = new BusRepository(filePath);
        assertEquals(35, reloaded.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B3-02: Capacity increase is rejected on update (B2)")
    void itB3_02_increaseRejected() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 35, 80.0, Bus.FUEL_DIESEL));
        assertFalse(repo.update(new Bus("12345678", 60, 80.0, Bus.FUEL_DIESEL), 30));
        assertEquals(35, repo.retrieve("12345678").getCapacity());
    }

    @Test
    @DisplayName("IT-B3-03: Driver older than 50 cannot update capacity >= 50 (B3)")
    void itB3_03_ageRestriction() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("99999999", 55, 90.0, Bus.FUEL_DIESEL));
        // Driver aged > 50 attempts to set capacity 50 -> rejected by B3.
        assertFalse(repo.update(new Bus("99999999", 50, 90.0, Bus.FUEL_DIESEL), 56));
        assertEquals(55, repo.retrieve("99999999").getCapacity());
    }

    // ---------- IT-B4: counts updated correctly ----------

    @Test
    @DisplayName("IT-B4-01: count() is correct after multiple additions")
    void itB4_01_countAfterAdds() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID));
        repo.add(new Bus("11111111", 20, 50.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("22222222", 35, 60.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("99999999", 55, 90.0, Bus.FUEL_DIESEL));
        assertEquals(5, repo.count());
    }

    @Test
    @DisplayName("IT-B4-02: count() unchanged when an invalid bus is rejected")
    void itB4_02_countUnchangedOnReject() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        int before = repo.count();
        repo.add(new Bus("12345678", 20, 10.0, Bus.FUEL_HYBRID)); // duplicate
        assertEquals(before, repo.count());
    }

    @Test
    @DisplayName("IT-B4-03: count() is correct after reloading from file (restart)")
    void itB4_03_countAfterReload() {
        BusRepository repo = new BusRepository(filePath);
        repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL));
        repo.add(new Bus("87654321", 30, 65.5, Bus.FUEL_HYBRID));
        int original = repo.count();

        BusRepository reloaded = new BusRepository(filePath);
        assertEquals(original, reloaded.count());
    
    }

    @Test
    @DisplayName("IT-B5-02: Updating a non-existing bus is rejected")
    void itB5_02_updateNonExistingBus() {
    BusRepository repo = new BusRepository(filePath);
    boolean updated = repo.update(
            new Bus("77777777", 40, 80.0, Bus.FUEL_DIESEL),
            30
    );
    assertFalse(updated);
    }
}
