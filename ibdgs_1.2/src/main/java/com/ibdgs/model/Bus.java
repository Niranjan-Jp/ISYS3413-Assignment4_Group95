package com.ibdgs.model;

/**
 * Represents a bus in the Intelligent Bus Driver Guidance System.
 *
 * <p>Holds bus data and validation logic for the bus conditions:
 * <ul>
 *   <li>B1 - Bus ID rules (unique, exactly 8 digits)</li>
 *   <li>B2 - Capacity update restriction (cannot increase, enforced on update)</li>
 *   <li>B3 - Driver age restriction (driver &gt; 50 cannot drive capacity &ge; 50)</li>
 *   <li>B4 - Electric bus restriction (driver needs &ge; 5 years experience)</li>
 *   <li>B5 - Driver licence restriction (Heavy / PublicTransport for electric &amp; hybrid)</li>
 * </ul>
 * Uniqueness for B1 and the capacity-decrease rule for B2 are enforced by
 * {@code BusRepository}; per-driver eligibility rules (B3-B5) are provided here
 * as static helpers so they can be reused on assignment and on update.
 */
public class Bus {

    // Allowed fuel types per the specification.
    public static final String FUEL_DIESEL = "Diesel";
    public static final String FUEL_HYBRID = "Hybrid";
    public static final String FUEL_ELECTRICITY = "Electricity";

    private String busID;
    private int capacity;
    private double fuelLevel;
    private String fuelType;

    /** Full constructor used when building a bus from validated input or from file. */
    public Bus(String busID, int capacity, double fuelLevel, String fuelType) {
        this.busID = busID;
        this.capacity = capacity;
        this.fuelLevel = fuelLevel;
        this.fuelType = fuelType;
    }

    // ---------------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------------

    /**
     * B1 - Bus ID rules: exactly 8 characters, all of which are digits.
     * (Uniqueness is enforced by the repository.)
     */
    public static boolean isValidBusID(String id) {
        if (id == null || id.length() != 8) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** A bus is structurally valid if its id satisfies B1. */
    public boolean isValid() {
        return isValidBusID(busID);
    }

    /**
     * B3 - Driver age restriction. Drivers strictly older than 50 cannot drive
     * buses with a capacity of 50 or more.
     *
     * @param driverAge the driver's age in whole years
     * @param capacity  the bus capacity
     * @return true if the driver is permitted under B3.
     */
    public static boolean isAgeAllowed(int driverAge, int capacity) {
        if (driverAge > 50 && capacity >= 50) {
            return false;
        }
        return true;
    }

    /**
     * B4 - Electric bus restriction. Only drivers with at least 5 years of
     * experience may drive electric buses. Applies only to fuelType "Electricity".
     */
    public static boolean isExperienceAllowed(int experienceYears, String fuelType) {
        if (FUEL_ELECTRICITY.equals(fuelType)) {
            return experienceYears >= 5;
        }
        return true;
    }

    /**
     * B5 - Driver licence restriction. Only Heavy or PublicTransport licence
     * holders may operate electric and hybrid buses.
     */
    public static boolean isLicenseAllowed(String licenseType, String fuelType) {
        boolean restricted = FUEL_ELECTRICITY.equals(fuelType) || FUEL_HYBRID.equals(fuelType);
        if (restricted) {
            return Driver.LICENSE_HEAVY.equals(licenseType)
                    || Driver.LICENSE_PUBLIC_TRANSPORT.equals(licenseType);
        }
        return true;
    }

    // --------------------------- Getters / setters ---------------------------

    public String getBusID() {
        return busID;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(double fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    @Override
    public String toString() {
        return "Bus{" +
                "busID='" + busID + '\'' +
                ", capacity=" + capacity +
                ", fuelLevel=" + fuelLevel +
                ", fuelType='" + fuelType + '\'' +
                '}';
    }
}
