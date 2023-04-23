package hotstuff.demo.EVsharing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import hotstuff.tom.MessageContext;
import hotstuff.tom.ServiceReplica;
import hotstuff.tom.server.defaultservices.DefaultSingleRecoverable;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hotstuff.tom.MessageContext;
import hotstuff.tom.ServiceReplica;
import hotstuff.tom.server.defaultservices.DefaultSingleRecoverable;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import java.lang.Math;

import java.util.Random;

public class EVThroughputLatencyServer extends DefaultSingleRecoverable{
    private Map<String, Vehicle> vehiclesRegistered;
    // private Map<String, Integer> usersRegistered;
    private Map<String, User> usersRegistered;
    private Logger logger;

    private byte[] state;
    private long startTime;
    private long numRequests;
    private final Set<Integer> senders;
    private double maxThroughput;

    //private Random random;

    public EVThroughputLatencyServer(int id) {

        vehiclesRegistered = new HashMap<>();

        usersRegistered = new HashMap<>();

        senders = new HashSet<>(1000);

        logger = Logger.getLogger(EVThroughputLatencyServer.class.getName());
        new ServiceReplica(id, this, this);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: demo.EVsharing.EVserver <server id>");
            System.exit(-1);
        }
        new EVThroughputLatencyServer(Integer.parseInt(args[0]));
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {

        Random random = new Random();
        random.setSeed(1234);
        numRequests++;
        senders.add(msgCtx.getSender());
        byte[] reply = null;
        boolean hasReply = false;
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn);
             ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
            EVRequestType reqType = (EVRequestType) objIn.readObject();
            switch (reqType) {
                case REGISTERVEHICLE:
                    Vehicle vehicle = (Vehicle)objIn.readObject();
                    if (vehiclesRegistered.containsKey(vehicle.getVehicleID())) {
                        objOut.writeObject("Not possible. Vehicle " + vehicle.getVehicleID() + " is already registered.");
                    } else {
                        vehiclesRegistered.put(vehicle.getVehicleID(), vehicle);
                        objOut.writeObject("The vehicle " + vehicle.getVehicleID() + " is now successfully registered.");
                    }
                    hasReply = true;
                    break;
                case REGISTERUSER:
                    User user = (User)objIn.readObject();
                    if (usersRegistered.containsKey(user.getUserID())) {
                        objOut.writeObject("Not possible. User with the user Id " + user.getUserID() + " is already registered.");
                    } else {
                        usersRegistered.put(user.getUserID(), user);
                        objOut.writeObject("The user with the user Id " + user.getUserID() + " is now successfully registered.");
                    }
                    hasReply = true;
                    break;
                case BOOKVEHICLE:
                    AbstractMap.SimpleEntry<String, String> pair = (AbstractMap.SimpleEntry<String, String>)objIn.readObject();
                    String userID = pair.getKey();
                    if (!usersRegistered.containsKey(userID)) {
                        objOut.writeObject("Not possible. User with the user Id " + userID + " is not registered.");
                    } else {
                        String vehicleID = pair.getValue();
                        if (!vehiclesRegistered.containsKey(vehicleID)) {
                            objOut.writeObject("Not possible. Vehicle with the vehicle Id " + vehicleID + " is not registered.");
                        } else {
                            Vehicle vehicle1 = vehiclesRegistered.get(vehicleID);
                            if (!vehicle1.getIsAvailable()) {
                                objOut.writeObject("Not possible. Vehicle with the vehicle Id " + vehicleID + " is already booked.");
                            } else {
                                if (usersRegistered.get(userID).getUserBalance() < vehicle1.getDepositPrice()) {
                                    objOut.writeObject("Not possible. User with the user Id " + userID + " does not have enough balance.");
                                } else {
                                    vehicle1.setIsAvailable(false);
                                    vehicle1.setCurrentUserID(userID);

                                    // producing a deposit payment and giving an access code for the vehicle
                                    User user1 = usersRegistered.get(userID);
                                    user1.setCurrentVehicleAccessCode(vehicle1.getVehicleAccessCode());
                                    user1.setUserBalance(user1.getUserBalance() - vehicle1.getDepositPrice());
                                    usersRegistered.put(userID, user1);
                                    vehicle1.setVehicleOwnerBalance(vehicle1.getVehicleOwnerBalance() + vehicle1.getDepositPrice());

                                    vehiclesRegistered.put(vehicleID, vehicle1);
                                    objOut.writeObject("The vehicle with the vehicle Id " + vehicleID + " is now successfully booked. The current user of the vehicle is " + vehiclesRegistered.get(vehicleID).getCurrentUserID());
                                }
                            }
                        }
                    }
                    hasReply = true;
                    break;
                case RETURNVEHICLE:
                    //AbstractMap.SimpleEntry<String, String> quartet = (AbstractMap.SimpleEntry<String, String>)objIn.readObject();
                    //String vehicleID1 = quartet.getValue();
                    Quartet<String, String, Float, Float> quartet = (Quartet<String, String, Float, Float>)objIn.readObject();
                    String vehicleID1 = quartet.getSecond();
                    if (!vehiclesRegistered.containsKey(vehicleID1)) {
                        objOut.writeObject("Not possible. Vehicle with the vehicle Id " + vehicleID1 + " is not registered.");
                    } else {
                        String userID1 = quartet.getFirst();
                        //String userID1 = quartet.getKey();
                        if (!Objects.equals(vehiclesRegistered.get(vehicleID1).getCurrentUserID(), userID1)) {
                            objOut.writeObject("Not possible. User " + userID1 + " is not using the vehicle " + vehicleID1 + ". The current user of the vehicle " + vehicleID1 + " is " + vehiclesRegistered.get(vehicleID1).getCurrentUserID());
                        } else {

                            float price = vehiclesRegistered.get(vehicleID1).getVehiclePricePerHour() * quartet.getThird() + vehiclesRegistered.get(vehicleID1).getVehiclePricePerKm() * quartet.getFourth() - vehiclesRegistered.get(vehicleID1).getDepositPrice();
                            // float price = vehiclesRegistered.get(vehicleID1).getVehiclePricePerHour() * 20 + vehiclesRegistered.get(vehicleID1).getVehiclePricePerKm() * 15 - vehiclesRegistered.get(vehicleID1).getDepositPrice();
                            float finalPrice = price + price * vehiclesRegistered.get(vehicleID1).getVehicleRepairPercentageOfFee() / 100;

                            // producing a random number between 0 and 1 to simulate if the vehicle needs repair
                            boolean needsRepair = random.nextBoolean();
                            int maintenanceRepairCost = 0;
                            if (needsRepair) {
                                // for the sake of simplicity, we assume that the cost lies in the range between the deposit price and the deposite price x2
                                maintenanceRepairCost = vehiclesRegistered.get(vehicleID1).getDepositPrice() + random.nextInt(vehiclesRegistered.get(vehicleID1).getDepositPrice() + 1);
                            }

                            finalPrice += maintenanceRepairCost;

                            Vehicle vehicle2 = vehiclesRegistered.get(vehicleID1);
                            User user2 = usersRegistered.get(userID1);

                            // simulating transaction
                            vehicle2.setVehicleOwnerBalance(vehicle2.getVehicleOwnerBalance() + finalPrice);
                            user2.setUserBalance(user2.getUserBalance() - finalPrice);

                            // updating other fields of current vehicle and user
                            user2.setCurrentVehicleAccessCode(null);
                            vehicle2.setIsAvailable(true);
                            vehicle2.setCurrentUserID(null);
                            // new access code is generated for the vehicle
                            vehicle2.setVehicleAccessCode();

                            List<String> helperListForUsers = user2.getIDsOfVehiclesUsed();
                            helperListForUsers.add(vehicleID1);
                            vehicle2.setIDsOfUsersThatUsedVehicle(helperListForUsers);

                            List<String> helperListForVehicles = vehicle2.getIDsOfUsersThatUsedVehicle();
                            helperListForVehicles.add(userID1);
                            vehicle2.setIDsOfUsersThatUsedVehicle(helperListForVehicles);

                            vehiclesRegistered.put(vehicleID1, vehicle2);
                            usersRegistered.put(userID1, user2);
                            objOut.writeObject("The vehicle with the vehicle Id " + vehicleID1 + " is now successfully returned.");
                        }
                    }
                    hasReply = true;
                    break;
                case DISPUTE:
                    AbstractMap.SimpleEntry<String, String> pair1 = (AbstractMap.SimpleEntry<String, String>)objIn.readObject();
                    String userID2 = pair1.getKey();
                    if (!usersRegistered.containsKey(userID2)) {
                        objOut.writeObject("Not possible. User with the user Id " + userID2 + " is not registered.");
                    } else {
                        String vehicleID2 = pair1.getValue();
                        if (!vehiclesRegistered.containsKey(vehicleID2)) {
                            objOut.writeObject("Not possible. Vehicle with the vehicle Id " + vehicleID2 + " is not registered.");
                        } else {
                            if ((!vehiclesRegistered.get(vehicleID2).getIDsOfUsersThatUsedVehicle().contains(userID2)) || (!usersRegistered.get(userID2).getIDsOfVehiclesUsed().contains(vehicleID2))) {
                                objOut.writeObject("Not possible. The ID history of the user and the vehicle do not coincide. User " + userID2 + " did not use the vehicle " + vehicleID2);
                            } else {
                                // simulating the outcome of the dispute. If the outcome is true, the user wins the dispute and gets some compensation.
                                boolean disputeOutcome = random.nextBoolean();
                                if (disputeOutcome) {
                                    // producing a random number to simulate the compensation
                                    // for the sake of simplicity, we assume that the compensation is always a refund,
                                    // and it equals to the deposit price
                                    float compensation = vehiclesRegistered.get(vehicleID2).getDepositPrice() ;
                                    // simulating transaction
                                    // if the user wins the dispute, the vehicle owner pays the compensation to user
                                    // if the user loses the dispute, nothing happens
                                    Vehicle vehicle3 = vehiclesRegistered.get(vehicleID2);
                                    User user3 = usersRegistered.get(userID2);
                                    vehicle3.setVehicleOwnerBalance(vehicle3.getVehicleOwnerBalance() - compensation);
                                    user3.setUserBalance(user3.getUserBalance() + compensation);

                                    // storing the results
                                    vehiclesRegistered.put(vehicleID2, vehicle3);
                                    usersRegistered.put(userID2, user3);
                                    objOut.writeObject("The user " + userID2 + " wins the dispute. The user gets a compensation of " + compensation + " pounds.");
                                } else {
                                    objOut.writeObject("The user " + userID2 + " loses the dispute. The user does not get any compensation.");
                                }
                            }
                        }
                    }
                    hasReply = true;
                    break;
            }
            if (hasReply) {
                objOut.flush();
                byteOut.flush();
                reply = byteOut.toByteArray();
            } else {
                reply = new byte[0];
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Ocurred during EV operation execution", e);
        }
        printMeasurement();
        return reply;
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        byte[] reply = null;

        return reply;
    }

    private void printMeasurement() {
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - startTime) / 1_000_000_000.0;
        if ((int) (deltaTime / 2) > 0) {
            long delta = currentTime - startTime;
            double throughput = numRequests / deltaTime;
            if (throughput > maxThroughput)
                maxThroughput = throughput;
            System.out.println("M:(clients[#]|requests[#]|delta[ns]|throughput[ops/s], max[ops/s])>("+senders.size()+"|"+numRequests+"|"+delta+"|"+throughput+"|"+maxThroughput+")");
            numRequests = 0;
            startTime = currentTime;
            senders.clear();
        }
    }

    @Override
    public byte[] getSnapshot() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
            objOut.writeObject(vehiclesRegistered);
            objOut.writeObject(usersRegistered);
            return byteOut.toByteArray();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while taking snapshot", e);
        }
        return new byte[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            vehiclesRegistered = (Map<String, Vehicle>)objIn.readObject();
            // usersRegistered = (Map<String, Integer>)objIn.readObject();
            usersRegistered = (Map<String, User>)objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error while installing snapshot", e);
        }
    }
}
