// BusRepository: manages add, retrieve, update, and count operations for buses with file-based storage. (Iftekhar)
package com.ibdgs.repository;

import com.ibdgs.model.Bus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File-backed repository for {@link Bus} objects.
 *
 * <p>Buses are persisted to a human-readable, pipe-delimited TXT file, one bus
 * per line in the order: {@code busID|capacity|fuelLevel|fuelType}. Bus fields
 * contain no '|' characters, so no escaping is required.
 *
 * <p>Supported operations: add, retrieve, update, count. Existing data is loaded
 * on construction, so building a new instance over the same file simulates an
 * application restart.
 */
public class BusRepository {

    private static final String FIELD_SEP = "|";

    private final String filePath;
    private final Map<String, Bus> buses = new LinkedHashMap<>();

    /**
     * Creates a repository backed by the given file, loading any existing data.
     */
    public BusRepository(String filePath) {
        this.filePath = filePath;
        load();
    }

    /**
     * Adds a new bus after validating B1 (8-digit ID) and enforcing uniqueness (B1).
     *
     * @return true if the bus was added and persisted; false otherwise.
     */
    public boolean add(Bus bus) {
        if (bus == null) {
            return false;
        }
        // B1 - structural validity (exactly 8 digits).
        if (!bus.isValid()) {
            return false;
        }
        // B1 - uniqueness.
        if (buses.containsKey(bus.getBusID())) {
            return false;
        }
        buses.put(bus.getBusID(), bus);
        save();
        return true;
    }

    /**
     * Retrieves a bus by ID.
     *
     * @return the stored bus, or null if no bus with that ID exists.
     */
    public Bus retrieve(String busID) {
        return buses.get(busID);
    }

    /**
     * Updates an existing bus, enforcing:
     * <ul>
     *   <li>B2 - capacity cannot increase (it may decrease or stay the same);</li>
     *   <li>B3 - a driver older than 50 cannot set/keep a capacity of 50 or more.</li>
     * </ul>
     * B3 uses the optional driver age; pass a non-positive age (e.g. -1) when no
     * driver context applies and only B2 should be checked.
     *
     * @param updated   the bus carrying the new capacity / fuel values
     * @param driverAge the age of the driver performing the update (for B3); use
     *                  -1 to skip the B3 check
     * @return true if the update was applied and persisted; false otherwise.
     */
    public boolean update(Bus updated, int driverAge) {
        if (updated == null) {
            return false;
        }
        Bus existing = buses.get(updated.getBusID());
        if (existing == null) {
            return false;
        }

        // B2 - capacity must not increase.
        if (updated.getCapacity() > existing.getCapacity()) {
            return false;
        }

        // B3 - driver older than 50 cannot operate capacity >= 50.
        if (driverAge > 0 && !Bus.isAgeAllowed(driverAge, updated.getCapacity())) {
            return false;
        }

        existing.setCapacity(updated.getCapacity());
        existing.setFuelLevel(updated.getFuelLevel());
        existing.setFuelType(updated.getFuelType());

        save();
        return true;
    }

    /**
     * Convenience overload of {@link #update(Bus, int)} that skips the B3 age
     * check (only B2 is enforced).
     */
    public boolean update(Bus updated) {
        return update(updated, -1);
    }

    /** @return the number of stored buses. */
    public int count() {
        return buses.size();
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Bus b : buses.values()) {
                writer.write(serialize(b));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save buses to " + filePath, e);
        }
    }

    private void load() {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Bus b = deserialize(line);
                if (b != null) {
                    buses.put(b.getBusID(), b);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load buses from " + filePath, e);
        }
    }

    private String serialize(Bus b) {
        return String.join(FIELD_SEP,
                b.getBusID(),
                Integer.toString(b.getCapacity()),
                Double.toString(b.getFuelLevel()),
                b.getFuelType());
    }

    private Bus deserialize(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        String busID = parts[0];
        int capacity = Integer.parseInt(parts[1]);
        double fuelLevel = Double.parseDouble(parts[2]);
        String fuelType = parts[3];
        return new Bus(busID, capacity, fuelLevel, fuelType);
    }
}
