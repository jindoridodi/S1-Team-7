package model;

import java.util.UUID;

/**
 * Immutable vehicle record associated with a driver account.
 *
 * One constructor is used for newly entered data before persistence and the
 * other reconstructs a row loaded back from MySQL.
 */
public class Vehicle {
    private final String id;
    private final String ownerEmail;
    private final String make;
    private final String model;
    private final String color;
    private final String plate;
    private final int totalSeats;


    /**
     * Creates a new vehicle record before the database has assigned an integer id.
     *
     * @param ownerEmail email of the driver who owns the vehicle
     * @param make vehicle make entered on the dashboard form
     * @param model vehicle model entered on the dashboard form
     * @param color vehicle color entered on the dashboard form
     * @param plate license plate stored with the vehicle row
     * @param totalSeats total passenger seats entered on the dashboard form
     */
    public Vehicle(String ownerEmail, String make, String model, String color, String plate, int totalSeats) {
        this.id = UUID.randomUUID().toString();
        this.ownerEmail = ownerEmail;
        this.make = make;
        this.model = model;
        this.color = color;
        this.plate = plate;
        this.totalSeats = totalSeats;
    }   

    /**
     * Recreates a persisted vehicle row using the database id.
     *
     * @param id database vehicle identifier converted to a string
     * @param ownerEmail email of the owning driver
     * @param make vehicle make from the database row
     * @param model vehicle model from the database row
     * @param color vehicle color from the database row
     * @param plate license plate from the database row
     * @param totalSeats total passenger seats from the database row
     */
    public Vehicle(String id, String ownerEmail, String make, String model, String color, String plate, int totalSeats) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.make = make;
        this.model = model;
        this.color = color;
        this.plate = plate;
        this.totalSeats = totalSeats;
    }

    public String getId()         { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getMake()       { return make; }
    public String getModel()      { return model; }
    public String getColor()      { return color; }
    public String getPlate()      { return plate; }
    public int getTotalSeats()    { return totalSeats; }

}
