package hotstuff.demo.EVsharing;

import hotstuff.tom.ServiceProxy;

import java.io.*;
import java.util.*;

public class EVclient {

    ServiceProxy serviceProxy;

    public EVclient(int clientId) {
        serviceProxy = new ServiceProxy(clientId);
    }

    public String registerVehicle(Vehicle vehicle) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

            System.out.println("Registering vehicle " + vehicle.getVehicleID());

            objOut.writeObject(EVRequestType.REGISTERVEHICLE);
            objOut.writeObject(vehicle);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
            if (reply.length == 0)
                return null;
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (String)objIn.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception registering a vehicle: " + e.getMessage());
        }
        return null;
    }

    public String registerUser(User user) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

            System.out.println("Registering user " + user.getUserID() + " with balance: " + user.getUserBalance());

            objOut.writeObject(EVRequestType.REGISTERUSER);
            objOut.writeObject(user);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
            if (reply.length == 0)
                return null;
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (String)objIn.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception registering a user: " + e.getMessage());
        }
        return null;
    }

    public String bookVehicle(String userID, String vehicleID) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

            objOut.writeObject(EVRequestType.BOOKVEHICLE);
            AbstractMap.SimpleEntry<String, String> pair = new AbstractMap.SimpleEntry<>(userID, vehicleID);
            objOut.writeObject(pair);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
            if (reply.length == 0)
                return null;
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (String)objIn.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception booking a vehicle: " + e.getMessage());
        }
        return null;
    }

    public String returnVehicle(String userID, String vehicleID, float distance, float time) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
            System.out.println("Returning vehicle " + vehicleID + " by user " + userID);

            objOut.writeObject(EVRequestType.RETURNVEHICLE);
//            AbstractMap.SimpleEntry<String, String> pair = new AbstractMap.SimpleEntry<>(userID, vehicleID);
//            objOut.writeObject(pair);
            Quartet<String, String, Float, Float> quartet = new Quartet<>(userID, vehicleID, distance, time);
            objOut.writeObject(quartet);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
            if (reply.length == 0)
                return null;
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (String)objIn.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception returning a vehicle: " + e.getMessage());
        }
        return null;
    }

    public String dispute(String vehicleID, String userID) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

            objOut.writeObject(EVRequestType.DISPUTE);
            AbstractMap.SimpleEntry<String, String> pair = new AbstractMap.SimpleEntry<>(userID, vehicleID);
            objOut.writeObject(pair);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeOrdered(byteOut.toByteArray());
            if (reply.length == 0)
                return null;
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (String)objIn.readObject();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception while disputing: " + e.getMessage());
        }
        return null;
    }

    public void close() {
        serviceProxy.close();
    }

}
