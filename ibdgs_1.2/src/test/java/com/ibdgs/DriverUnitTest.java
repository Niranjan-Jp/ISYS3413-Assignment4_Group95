package com.ibdgs;

import com.ibdgs.model.Driver;
import com.ibdgs.repository.DriverRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 Task 1 - Driver Unit Testing.
 <p>At least 15 unit tests covering driver conditions D1-D5, with at least
 three cases per condition (normal, invalid and edge cases). These mirror the
 documented test case table (TC1-TC15).
 <p>Each test uses a fresh, temporary repository file so the tests are
 independent and do not depend on previously stored data.
 */
public class DriverUnitTest {

    private DriverRepository repo;
    private Path file;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        file = tempDir.resolve("unit_drivers.txt");
        repo = new DriverRepository(file.toString());
    }

    @AfterEach
    void tearDown() {
        // TempDir is cleaned up automatically by JUnit.
    }

    // ===================== D1 - Driver ID Validation =====================

    @Test
    @DisplayName("TC1: Valid Driver ID is accepted (normal)")
    void tc1_validDriverId() {
        // "23@@12#4AB": digits 2-3, specials @@ #, ends AB -> valid per D1.
        Driver d = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d), "Driver with valid ID should be added");
        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("TC2: Duplicate Driver ID is rejected (invalid)")
    void tc2_duplicateDriverId() {
        Driver first = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        Driver duplicate = new Driver("23@@12#4AB", "Mary", 8, Driver.LICENSE_HEAVY,
                "20|Queen Street|Sydney|NSW|Australia", "01-01-1990");
        assertTrue(repo.add(first));
        assertFalse(repo.add(duplicate), "Duplicate driver ID must be rejected");
        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("TC3: Driver ID of wrong length is rejected (edge)")
    void tc3_invalidDriverIdLength() {
        // 9 characters -> violates the exactly-10 rule.
        assertFalse(Driver.isValidDriverID("23@@12#AB"));
        Driver d = new Driver("23@@12#AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertFalse(repo.add(d));
    }

    @Test
    @DisplayName("D1 extra: leading non-digit / too few specials rejected")
    void d1_extraInvalidForms() {
        // First two not digits 2-9.
        assertFalse(Driver.isValidDriverID("AB@@12#4CD"));
        // Only one special char between positions 3-8.
        assertFalse(Driver.isValidDriverID("23ab12c4AB"));
        // Last two not uppercase letters.
        assertFalse(Driver.isValidDriverID("23@@12#4a9"));
        // Leading digit "1" out of the 2-9 range.
        assertFalse(Driver.isValidDriverID("13@@12#4AB"));
    }

    // ===================== D2 - Address Format =====================

    @Test
    @DisplayName("TC4: Valid address format is accepted (normal)")
    void tc4_validAddress() {
        assertTrue(Driver.isValidAddress("15|King Street|Melbourne|VIC|Australia"));
    }

    @Test
    @DisplayName("TC5: Missing field in address is rejected (invalid)")
    void tc5_missingAddressField() {
        // State missing -> only four fields.
        assertFalse(Driver.isValidAddress("15|King Street|Melbourne|Australia"));
    }

    @Test
    @DisplayName("TC6: Wrong address separator is rejected (edge)")
    void tc6_wrongAddressSeparator() {
        assertFalse(Driver.isValidAddress("15, King Street, Melbourne, VIC, Australia"));
    }

    // ===================== D3 - Birthdate Validation =====================

    @Test
    @DisplayName("TC7: Valid birthdate format is accepted (normal)")
    void tc7_validBirthdate() {
        assertTrue(Driver.isValidBirthdate("15-08-1995"));
    }

    @Test
    @DisplayName("TC8: Wrong birthdate format is rejected (invalid)")
    void tc8_wrongBirthdateFormat() {
        // ISO format instead of DD-MM-YYYY.
        assertFalse(Driver.isValidBirthdate("1995-08-15"));
    }

    @Test
    @DisplayName("TC9: Invalid calendar date is rejected (edge)")
    void tc9_invalidDateValue() {
        // Day 35 does not exist.
        assertFalse(Driver.isValidBirthdate("35-12-1995"));
    }

    // ===================== D4 - License Update Restriction =====================

    @Test
    @DisplayName("TC10: License update allowed for < 10 years experience (normal)")
    void tc10_licenseUpdateAllowedUnder10() {
        Driver d = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_LIGHT,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        Driver update = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.update(update), "License change allowed under 10 years");
        assertEquals(Driver.LICENSE_HEAVY, repo.retrieve("23@@12#4AB").getLicenseType());
    }

    @Test
    @DisplayName("TC11: License update rejected for > 10 years experience (invalid)")
    void tc11_licenseUpdateRejectedOver10() {
        Driver d = new Driver("23@@12#4AB", "John", 11, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        Driver update = new Driver("23@@12#4AB", "John", 11, Driver.LICENSE_PUBLIC_TRANSPORT,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertFalse(repo.update(update), "License change must be rejected over 10 years");
        assertEquals(Driver.LICENSE_HEAVY, repo.retrieve("23@@12#4AB").getLicenseType());
    }

    @Test
    @DisplayName("TC12: License update allowed at exactly 10 years (edge)")
    void tc12_licenseUpdateBoundary10() {
        Driver d = new Driver("23@@12#4AB", "John", 10, Driver.LICENSE_MEDIUM,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        Driver update = new Driver("23@@12#4AB", "John", 10, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.update(update), "License change allowed at exactly 10 years (not > 10)");
        assertEquals(Driver.LICENSE_HEAVY, repo.retrieve("23@@12#4AB").getLicenseType());
    }

    // ===================== D5 - Immutable Fields =====================

    @Test
    @DisplayName("TC13: Driver ID cannot be modified (invalid)")
    void tc13_driverIdImmutable() {
        Driver d = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        // Updating under a different ID targets a non-existent record -> rejected.
        Driver update = new Driver("34@@55#7CD", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertFalse(repo.update(update), "Changing driver ID must not succeed");
        assertEquals(1, repo.count());
        assertEquals(null, repo.retrieve("34@@55#7CD"));
    }

    @Test
    @DisplayName("TC14: Driver name cannot be modified (invalid)")
    void tc14_driverNameImmutable() {
        Driver d = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        Driver update = new Driver("23@@12#4AB", "David", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertFalse(repo.update(update), "Changing name must be rejected");
        assertEquals("John", repo.retrieve("23@@12#4AB").getName());
    }

    @Test
    @DisplayName("TC15: Non-immutable field (address) updates successfully (normal)")
    void tc15_nonImmutableFieldUpdate() {
        Driver d = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "15|King Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.add(d));
        Driver update = new Driver("23@@12#4AB", "John", 5, Driver.LICENSE_HEAVY,
                "20|Queen Street|Melbourne|VIC|Australia", "15-08-1995");
        assertTrue(repo.update(update), "Address update should succeed");
        assertEquals("20|Queen Street|Melbourne|VIC|Australia",
                repo.retrieve("23@@12#4AB").getAddress());
    }
}
