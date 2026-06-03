package com.ibdgs;

import com.ibdgs.model.Bus;
import com.ibdgs.model.Driver;
import com.ibdgs.repository.BusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 2 - Bus Unit Testing.
 *
 * <p>At least 15 unit tests covering bus conditions B1-B5, with at least three
 * cases per condition (normal, invalid and edge), mirroring the documented test
 * case table (B1-TC.. through B5-TC..).
 *
 * <p>B1 and B2 are exercised through the repository (uniqueness, capacity
 * persistence). B3-B5 are per-driver eligibility rules tested through the
 * static helper methods on {@link Bus}.
 */
public class BusUnitTest {

    private BusRepository repo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        Path file = tempDir.resolve("unit_buses.txt");
        repo = new BusRepository(file.toString());
    }

    // ===================== B1 - Bus ID Rules =====================

    @Test
    @DisplayName("B1-TC01: Valid 8-digit unique busID is added (normal)")
    void b1_validBus() {
        Bus bus = new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL);
        assertTrue(repo.add(bus));
        assertEquals(bus, repo.retrieve("12345678"));
    }

    @Test
    @DisplayName("B1-TC02: Duplicate busID is rejected (invalid)")
    void b1_duplicateBus() {
        assertTrue(repo.add(new Bus("12345678", 40, 80.0, Bus.FUEL_DIESEL)));
        assertFalse(repo.add(new Bus("12345678", 30, 50.0, Bus.FUEL_HYBRID)));
        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("B1-TC03: busID shorter than 8 digits is rejected (invalid)")
    void b1_tooShort() {
        assertFalse(Bus.isValidBusID("1234567"));
        assertFalse(repo.add(new Bus("1234567", 30, 50.0, Bus.FUEL_HYBRID)));
    }

    @Test
    @DisplayName("B1-TC04: busID longer than 8 digits is rejected (invalid)")
    void b1_tooLong() {
        assertFalse(Bus.isValidBusID("123456789"));
        assertFalse(repo.add(new Bus("123456789", 45, 60.0, Bus.FUEL_DIESEL)));
    }

    @Test
    @DisplayName("B1-TC05: busID with non-digit characters is rejected (edge)")
    void b1_nonDigit() {
        assertFalse(Bus.isValidBusID("1234AB78"));
        assertFalse(repo.add(new Bus("1234AB78", 30, 70.0, Bus.FUEL_DIESEL)));
    }

    // ===================== B2 - Capacity Update Restriction =====================

    @Test
    @DisplayName("B2-TC01: Capacity decrease is accepted (normal)")
    void b2_decreaseAllowed() {
        repo.add(new Bus("11111111", 50, 80.0, Bus.FUEL_DIESEL));
        assertTrue(repo.update(new Bus("11111111", 40, 80.0, Bus.FUEL_DIESEL)));
        assertEquals(40, repo.retrieve("11111111").getCapacity());
    }

    @Test
    @DisplayName("B2-TC02: Capacity increase is rejected (invalid)")
    void b2_increaseRejected() {
        repo.add(new Bus("11111111", 50, 80.0, Bus.FUEL_DIESEL));
        assertFalse(repo.update(new Bus("11111111", 60, 80.0, Bus.FUEL_DIESEL)));
        assertEquals(50, repo.retrieve("11111111").getCapacity());
    }

    @Test
    @DisplayName("B2-TC03: Same capacity is accepted (edge)")
    void b2_sameCapacity() {
        repo.add(new Bus("11111111", 50, 80.0, Bus.FUEL_DIESEL));
        assertTrue(repo.update(new Bus("11111111", 50, 70.0, Bus.FUEL_DIESEL)));
        assertEquals(50, repo.retrieve("11111111").getCapacity());
    }

    // ===================== B3 - Driver Age Restriction =====================

    @Test
    @DisplayName("B3-TC01: Driver aged 45 with capacity 50 allowed (normal)")
    void b3_under50Allowed() {
        assertTrue(Bus.isAgeAllowed(45, 50));
    }

    @Test
    @DisplayName("B3-TC02: Driver aged 55 with capacity 50 rejected (invalid)")
    void b3_over50Rejected() {
        assertFalse(Bus.isAgeAllowed(55, 50));
    }

    @Test
    @DisplayName("B3-TC03: Driver aged exactly 50 with capacity 50 allowed (edge)")
    void b3_exactly50Allowed() {
        // Rule is strictly > 50, so age 50 is permitted.
        assertTrue(Bus.isAgeAllowed(50, 50));
    }

    @Test
    @DisplayName("B3-TC04: Driver aged 55 with capacity 49 allowed (edge)")
    void b3_capacityUnder50Allowed() {
        assertTrue(Bus.isAgeAllowed(55, 49));
    }

    // ===================== B4 - Electric Bus Restriction =====================

    @Test
    @DisplayName("B4-TC01: 7 years experience on electric bus allowed (normal)")
    void b4_experiencedAllowed() {
        assertTrue(Bus.isExperienceAllowed(7, Bus.FUEL_ELECTRICITY));
    }

    @Test
    @DisplayName("B4-TC02: 3 years experience on electric bus rejected (invalid)")
    void b4_inexperiencedRejected() {
        assertFalse(Bus.isExperienceAllowed(3, Bus.FUEL_ELECTRICITY));
    }

    @Test
    @DisplayName("B4-TC03: Exactly 5 years experience on electric bus allowed (edge)")
    void b4_boundaryAllowed() {
        assertTrue(Bus.isExperienceAllowed(5, Bus.FUEL_ELECTRICITY));
    }

    @Test
    @DisplayName("B4-TC04: 3 years experience on diesel bus allowed (normal)")
    void b4_nonElectricUnaffected() {
        assertTrue(Bus.isExperienceAllowed(3, Bus.FUEL_DIESEL));
    }

    // ===================== B5 - Driver Licence Restriction =====================

    @Test
    @DisplayName("B5-TC01: Heavy licence on hybrid bus allowed (normal)")
    void b5_heavyHybridAllowed() {
        assertTrue(Bus.isLicenseAllowed(Driver.LICENSE_HEAVY, Bus.FUEL_HYBRID));
    }

    @Test
    @DisplayName("B5-TC02: PublicTransport licence on electric bus allowed (normal)")
    void b5_publicTransportElectricAllowed() {
        assertTrue(Bus.isLicenseAllowed(Driver.LICENSE_PUBLIC_TRANSPORT, Bus.FUEL_ELECTRICITY));
    }

    @Test
    @DisplayName("B5-TC03: Light licence on electric bus rejected (invalid)")
    void b5_lightElectricRejected() {
        assertFalse(Bus.isLicenseAllowed(Driver.LICENSE_LIGHT, Bus.FUEL_ELECTRICITY));
    }

    @Test
    @DisplayName("B5-TC04: Medium licence on hybrid bus rejected (invalid)")
    void b5_mediumHybridRejected() {
        assertFalse(Bus.isLicenseAllowed(Driver.LICENSE_MEDIUM, Bus.FUEL_HYBRID));
    }

    @Test
    @DisplayName("B5-TC05: Light licence on diesel bus allowed (edge)")
    void b5_lightDieselAllowed() {
        assertTrue(Bus.isLicenseAllowed(Driver.LICENSE_LIGHT, Bus.FUEL_DIESEL));
    }
}
