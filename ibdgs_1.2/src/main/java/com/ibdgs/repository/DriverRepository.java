package com.ibdgs.repository;

import com.ibdgs.model.Driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File-backed repository for {@link Driver} objects.
 *
 * <p>Drivers are persisted to a human-readable, pipe-delimited TXT file. One
 * driver per line in the order:
 * {@code driverID|name|experienceYears|licenseType|address|birthdate}.
 *
 * <p>Because the address itself uses '|' as its internal separator (D2), the
 * address field is escaped on write (its internal '|' becomes the placeholder
 * "&lt;PIPE&gt;") and unescaped on read, so the record split remains unambiguous.
 *
 * <p>Supported operations: add, retrieve, update, count. The repository loads
 * existing data from the file on construction, so creating a new instance over
 * the same file simulates an application restart.
 */
public class DriverRepository {

    private static final String FIELD_SEP = "|";
    private static final String ADDRESS_PIPE_PLACEHOLDER = "<PIPE>";

    private final String filePath;
    // LinkedHashMap preserves insertion order for stable, human-readable files.
    private final Map<String, Driver> drivers = new LinkedHashMap<>();

    /**
     * Creates a repository backed by the given file. If the file already exists
     * its contents are loaded; otherwise the repository starts empty.
     */
    public DriverRepository(String filePath) {
        this.filePath = filePath;
        load();
    }

    /**
     * Adds a new driver after validating D1 (ID), D2 (address) and D3 (birthdate),
     * and enforcing ID uniqueness (D1).
     *
     * @return true if the driver was added and persisted; false otherwise.
     */
    public boolean add(Driver driver) {
        if (driver == null) {
            return false;
        }
        // D1/D2/D3 structural validation.
        if (!driver.isValid()) {
            return false;
        }
        // D1 uniqueness: reject duplicate IDs.
        if (drivers.containsKey(driver.getDriverID())) {
            return false;
        }
        drivers.put(driver.getDriverID(), driver);
        save();
        return true;
    }

    /**
     * Retrieves a driver by ID.
     *
     * @return the stored driver, or null if no driver with that ID exists.
     */
    public Driver retrieve(String driverID) {
        return drivers.get(driverID);
    }

    /**
     * Updates an existing driver with the supplied values, enforcing:
     * <ul>
     *   <li>D5 - driverID and name are immutable (any attempt to change name is rejected);</li>
     *   <li>D4 - if the existing driver has more than 10 years of experience, the
     *       licenseType cannot be changed.</li>
     * </ul>
     * The driverID identifies the record to update. New address/birthdate values
     * are validated against D2/D3 before being persisted.
     *
     * @return true if the update was applied and persisted; false otherwise.
     */
    public boolean update(Driver updated) {
        if (updated == null) {
            return false;
        }
        Driver existing = drivers.get(updated.getDriverID());
        if (existing == null) {
            // Cannot update a driver that does not exist.
            return false;
        }

        // D5 - name is immutable.
        if (!existing.getName().equals(updated.getName())) {
            return false;
        }

        // D4 - if the existing driver has > 10 years experience, licenseType is locked.
        if (existing.getExperienceYears() > 10
                && !existing.getLicenseType().equals(updated.getLicenseType())) {
            return false;
        }

        // Validate any new address / birthdate against D2 / D3.
        if (!Driver.isValidAddress(updated.getAddress())) {
            return false;
        }
        if (!Driver.isValidBirthdate(updated.getBirthdate())) {
            return false;
        }

        // Apply the mutable fields onto the existing record.
        existing.setExperienceYears(updated.getExperienceYears());
        existing.setLicenseType(updated.getLicenseType());
        existing.setAddress(updated.getAddress());
        existing.setBirthdate(updated.getBirthdate());

        save();
        return true;
    }

    /** @return the number of stored drivers. */
    public int count() {
        return drivers.size();
    }


    // Persistence


    /** Writes all drivers to the backing file in a human-readable format. */
    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Driver d : drivers.values()) {
                writer.write(serialize(d));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save drivers to " + filePath, e);
        }
    }

    /** Loads drivers from the backing file if it exists and is non-empty. */
    private void load() {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Driver d = deserialize(line);
                if (d != null) {
                    drivers.put(d.getDriverID(), d);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load drivers from " + filePath, e);
        }
    }

    /** Converts a driver to a single pipe-delimited line, escaping the address. */
    private String serialize(Driver d) {
        String escapedAddress = d.getAddress().replace(FIELD_SEP, ADDRESS_PIPE_PLACEHOLDER);
        List<String> fields = new ArrayList<>();
        fields.add(d.getDriverID());
        fields.add(d.getName());
        fields.add(Integer.toString(d.getExperienceYears()));
        fields.add(d.getLicenseType());
        fields.add(escapedAddress);
        fields.add(d.getBirthdate());
        return String.join(FIELD_SEP, fields);
    }

    /** Parses a line back into a Driver, unescaping the address. */
    private Driver deserialize(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 6) {
            return null;
        }
        String driverID = parts[0];
        String name = parts[1];
        int experienceYears = Integer.parseInt(parts[2]);
        String licenseType = parts[3];
        String address = parts[4].replace(ADDRESS_PIPE_PLACEHOLDER, FIELD_SEP);
        String birthdate = parts[5];
        return new Driver(driverID, name, experienceYears, licenseType, address, birthdate);
    }
}
