package com.ibdgs;

import com.ibdgs.model.Driver;
import com.ibdgs.repository.DriverRepository;
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
 * Task 3 - Driver Integration Testing.
 *
 * <p>Integration tests that exercise {@link DriverRepository} against a real TXT
 * file using the real model classes. They verify that valid drivers are stored,
 * invalid drivers are rejected, updates are persisted, and counts are correct,
 * including across a simulated application restart (a new repository instance
 * reading the same file).
 *
 * <p>These mirror the documented integration test table (IT-D1 .. IT-D4).
 */
public class DriverIntegrationTest {

    private String filePath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        filePath = tempDir.resolve("test_drivers.txt").toString();
    }

    // ---------- IT-D1: valid drivers stored correctly ----------

    @Test
    @DisplayName("IT-D1-01: Valid driver is added and retrieved from storage")
    void itD1_01_addAndRetrieve() {
        DriverRepository repo = new DriverRepository(filePath);
        Driver d = new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990");
        assertTrue(repo.add(d));
        Driver got = repo.retrieve("45@@abcdAB");
        assertNotNull(got);
        assertEquals("Alice Smith", got.getName());
    }

    @Test
    @DisplayName("IT-D1-02: All driver fields persist through a write-read cycle")
    void itD1_02_allFieldsPersist() {
        DriverRepository repo = new DriverRepository(filePath);
        Driver d = new Driver("23!!xyzwAB", "Bob Jones", 7, Driver.LICENSE_PUBLIC_TRANSPORT,
                "99|Queen St|Sydney|NSW|Australia", "01-01-1985");
        assertTrue(repo.add(d));

        // Reload from file to force a real write-read cycle.
        DriverRepository reloaded = new DriverRepository(filePath);
        Driver got = reloaded.retrieve("23!!xyzwAB");
        assertNotNull(got);
        assertEquals(7, got.getExperienceYears());
        assertEquals(Driver.LICENSE_PUBLIC_TRANSPORT, got.getLicenseType());
        assertEquals("99|Queen St|Sydney|NSW|Australia", got.getAddress());
        assertEquals("01-01-1985", got.getBirthdate());
    }

    @Test
    @DisplayName("IT-D1-03: Multiple valid drivers are all stored and retrievable")
    void itD1_03_multipleDrivers() {
        DriverRepository repo = new DriverRepository(filePath);
        assertTrue(repo.add(new Driver("34##efghCD", "Carol White", 4, Driver.LICENSE_MEDIUM,
                "5|Park Rd|Perth|WA|Australia", "10-10-1992")));
        assertTrue(repo.add(new Driver("56$$ijklEF", "David Brown", 9, Driver.LICENSE_HEAVY,
                "8|Hill St|Hobart|TAS|Australia", "20-02-1988")));
        assertEquals("Carol White", repo.retrieve("34##efghCD").getName());
        assertEquals("David Brown", repo.retrieve("56$$ijklEF").getName());
    }

    // ---------- IT-D2: invalid drivers rejected ----------

    @Test
    @DisplayName("IT-D2-01: Duplicate driverID is rejected, original unchanged")
    void itD2_01_duplicateRejected() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        boolean result = repo.add(new Driver("45@@abcdAB", "Fake Alice", 1, Driver.LICENSE_LIGHT,
                "1|Fake St|Nowhere|VIC|Australia", "01-01-2000"));
        assertFalse(result);
        assertEquals("Alice Smith", repo.retrieve("45@@abcdAB").getName());
    }

    @Test
    @DisplayName("IT-D2-02: Driver with invalid ID format is rejected (D1)")
    void itD2_02_invalidIdRejected() {
        DriverRepository repo = new DriverRepository(filePath);
        // First two chars are letters -> violates D1.
        boolean result = repo.add(new Driver("BADID00000", "Wrong Id", 2, Driver.LICENSE_LIGHT,
                "1|A St|B|VIC|Australia", "01-01-1990"));
        assertFalse(result);
        assertNull(repo.retrieve("BADID00000"));
    }

    @Test
    @DisplayName("IT-D2-03: Driver with invalid birthdate is rejected (D3)")
    void itD2_03_invalidBirthdateRejected() {
        DriverRepository repo = new DriverRepository(filePath);
        // Birthdate uses slashes, not DD-MM-YYYY.
        boolean result = repo.add(new Driver("78%%mnopGH", "Bad Date", 2, Driver.LICENSE_LIGHT,
                "1|A St|B|VIC|Australia", "1990/06/15"));
        assertFalse(result);
        assertEquals(0, repo.count());
    }

    // ---------- IT-D3: updates persisted correctly ----------

    @Test
    @DisplayName("IT-D3-01: Valid update is persisted to the file")
    void itD3_01_updatePersisted() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("23!!xyzwAB", "Bob Jones", 7, Driver.LICENSE_PUBLIC_TRANSPORT,
                "99|Queen St|Sydney|NSW|Australia", "01-01-1985"));
        boolean updated = repo.update(new Driver("23!!xyzwAB", "Bob Jones", 10,
                Driver.LICENSE_PUBLIC_TRANSPORT,
                "10|Park Ave|Brisbane|QLD|Australia", "01-01-1985"));
        assertTrue(updated);

        DriverRepository reloaded = new DriverRepository(filePath);
        Driver got = reloaded.retrieve("23!!xyzwAB");
        assertEquals(10, got.getExperienceYears());
        assertEquals("10|Park Ave|Brisbane|QLD|Australia", got.getAddress());
    }

    @Test
    @DisplayName("IT-D3-02: Immutable name cannot be changed on update (D5)")
    void itD3_02_immutableName() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        boolean updated = repo.update(new Driver("45@@abcdAB", "Hacker Name", 3,
                Driver.LICENSE_HEAVY, "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        assertFalse(updated);
        assertEquals("Alice Smith", repo.retrieve("45@@abcdAB").getName());
    }

    @Test
    @DisplayName("IT-D3-03: licenseType locked for > 10 years experience (D4)")
    void itD3_03_licenseLocked() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("29&&pqrsIJ", "Senior Driver", 11, Driver.LICENSE_HEAVY,
                "3|Long Rd|Darwin|NT|Australia", "05-05-1970"));
        boolean updated = repo.update(new Driver("29&&pqrsIJ", "Senior Driver", 11,
                Driver.LICENSE_LIGHT, "3|Long Rd|Darwin|NT|Australia", "05-05-1970"));
        assertFalse(updated);
        assertEquals(Driver.LICENSE_HEAVY, repo.retrieve("29&&pqrsIJ").getLicenseType());
    }

    // ---------- IT-D4: counts updated correctly ----------

    @Test
    @DisplayName("IT-D4-01: count() is correct after multiple additions")
    void itD4_01_countAfterAdds() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        repo.add(new Driver("23!!xyzwAB", "Bob Jones", 7, Driver.LICENSE_PUBLIC_TRANSPORT,
                "99|Queen St|Sydney|NSW|Australia", "01-01-1985"));
        repo.add(new Driver("34##efghCD", "Carol White", 4, Driver.LICENSE_MEDIUM,
                "5|Park Rd|Perth|WA|Australia", "10-10-1992"));
        repo.add(new Driver("56$$ijklEF", "David Brown", 9, Driver.LICENSE_HEAVY,
                "8|Hill St|Hobart|TAS|Australia", "20-02-1988"));
        repo.add(new Driver("29&&pqrsIJ", "Senior Driver", 11, Driver.LICENSE_HEAVY,
                "3|Long Rd|Darwin|NT|Australia", "05-05-1970"));
        assertEquals(5, repo.count());
    }

    @Test
    @DisplayName("IT-D4-02: count() unchanged when an invalid driver is rejected")
    void itD4_02_countUnchangedOnReject() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        int before = repo.count();
        repo.add(new Driver("45@@abcdAB", "Dup", 1, Driver.LICENSE_LIGHT,
                "1|X St|Y|VIC|Australia", "01-01-2000"));
        assertEquals(before, repo.count());
    }

    @Test
    @DisplayName("IT-D4-03: count() is correct after reloading from file (restart)")
    void itD4_03_countAfterReload() {
        DriverRepository repo = new DriverRepository(filePath);
        repo.add(new Driver("45@@abcdAB", "Alice Smith", 3, Driver.LICENSE_HEAVY,
                "12|Main St|Melbourne|VIC|Australia", "15-06-1990"));
        repo.add(new Driver("23!!xyzwAB", "Bob Jones", 7, Driver.LICENSE_PUBLIC_TRANSPORT,
                "99|Queen St|Sydney|NSW|Australia", "01-01-1985"));
        int original = repo.count();

        DriverRepository reloaded = new DriverRepository(filePath);
        assertEquals(original, reloaded.count());
    }
}
