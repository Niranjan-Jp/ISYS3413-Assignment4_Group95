package com.ibdgs.model;

/**
 * Represents a bus driver in the Intelligent Bus Driver Guidance System.
 *
 * <p>The class is responsible for holding driver data and for validating that
 * data against the driver conditions defined in the specification:
 * <ul>
 *   <li>D1 - Driver ID rules (length, leading digits, special chars, trailing letters)</li>
 *   <li>D2 - Address format (Street Number|Street Name|City|State|Country)</li>
 *   <li>D3 - Birthdate format (DD-MM-YYYY)</li>
 *   <li>D4 - License update restriction (enforced by the repository on update)</li>
 *   <li>D5 - Immutable fields driverID and name (enforced by the repository on update)</li>
 * </ul>
 */
public class Driver {

    // Allowed licence types per the specification.
    public static final String LICENSE_LIGHT = "Light";
    public static final String LICENSE_MEDIUM = "Medium";
    public static final String LICENSE_HEAVY = "Heavy";
    public static final String LICENSE_PUBLIC_TRANSPORT = "PublicTransport";

    private String driverID;
    private String name;
    private int experienceYears;
    private String licenseType;
    private String address;
    private String birthdate;

    /** Full constructor used when building a driver from validated input or from file. */
    public Driver(String driverID, String name, int experienceYears,
                  String licenseType, String address, String birthdate) {
        this.driverID = driverID;
        this.name = name;
        this.experienceYears = experienceYears;
        this.licenseType = licenseType;
        this.address = address;
        this.birthdate = birthdate;
    }

    // ---------------------------------------------------------------------
    // Validation helpers (D1 - D3). D4 and D5 are update-time rules and are
    // enforced in DriverRepository.update().
    // ---------------------------------------------------------------------

    /**
     * D1 - Driver ID rules.
     * The driverID must be exactly 10 characters long, where:
     * - the first two characters are digits between 2 and 9,
     * - there are at least two special characters between characters 3 and 8 (indices 2..7),
     * - the last two characters are uppercase letters (A-Z).
     *
     * @return true if the supplied id satisfies D1.
     */
    public static boolean isValidDriverID(String id) {
        if (id == null || id.length() != 10) {
            return false;
        }
        // First two characters: digits 2-9.
        for (int i = 0; i < 2; i++) {
            char c = id.charAt(i);
            if (c < '2' || c > '9') {
                return false;
            }
        }
        // Last two characters: uppercase letters A-Z.
        for (int i = 8; i < 10; i++) {
            char c = id.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        // At least two special (non-alphanumeric) characters between chars 3 and 8 (indices 2..7).
        int specialCount = 0;
        for (int i = 2; i < 8; i++) {
            char c = id.charAt(i);
            boolean isAlphaNumeric = Character.isLetterOrDigit(c);
            if (!isAlphaNumeric) {
                specialCount++;
            }
        }
        return specialCount >= 2;
    }

    /**
     * D2 - Address format: Street Number|Street Name|City|State|Country.
     * Exactly five non-empty, pipe-separated fields are required.
     *
     * @return true if the supplied address satisfies D2.
     */
    public static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        // -1 limit keeps trailing empty fields so "a|b|c|d|" is correctly rejected.
        String[] parts = address.split("\\|", -1);
        if (parts.length != 5) {
            return false;
        }
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * D3 - Birthdate format DD-MM-YYYY with a real calendar date check.
     *
     * @return true if the supplied birthdate satisfies D3.
     */
    public static boolean isValidBirthdate(String birthdate) {
        if (birthdate == null || !birthdate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return false;
        }
        String[] parts = birthdate.split("-");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);

        if (month < 1 || month > 12) {
            return false;
        }
        int daysInMonth = daysInMonth(month, year);
        return day >= 1 && day <= daysInMonth;
    }

    /** Returns the number of days in a given month, accounting for leap years. */
    private static int daysInMonth(int month, int year) {
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                boolean leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                return leap ? 29 : 28;
            default:
                return 0;
        }
    }

    /**
     * Convenience method: a driver is valid if its id, address and birthdate
     * all satisfy D1, D2 and D3 respectively.
     */
    public boolean isValid() {
        return isValidDriverID(driverID)
                && isValidAddress(address)
                && isValidBirthdate(birthdate);
    }

    // --------------------------- Getters / setters ---------------------------

    public String getDriverID() {
        return driverID;
    }

    public String getName() {
        return name;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "driverID='" + driverID + '\'' +
                ", name='" + name + '\'' +
                ", experienceYears=" + experienceYears +
                ", licenseType='" + licenseType + '\'' +
                ", address='" + address + '\'' +
                ", birthdate='" + birthdate + '\'' +
                '}';
    }
}
