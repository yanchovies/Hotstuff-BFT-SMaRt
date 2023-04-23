package hotstuff.demo.EVsharing;

// import bftsmart.benchmark.Operation;
// import bftsmart.benchmark.ThroughputLatencyClient;
import hotstuff.tom.ServiceProxy;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import java.lang.Math;

public class EVThroughputLatencyClient {
    private static int initialClientId;
    private static byte[] data;
    private static byte[] serializedReadRequest;
    private static byte[] serializedWriteRequest;

    private static Random random = new Random();

    private static ArrayList<String> registeredVehicles = new ArrayList<String>();
    private static ArrayList<String> registeredUsers = new ArrayList<String>();

    private static byte[] paddingToExtend;

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 4) {
            System.out.println("USAGE: hotstuff.demo.EVsharing.EVThroughputLatencyClient <initial client id> " +
                    "<num clients> <number of operations per client> <request size>");// <isWrite?> <measurement leader?>
            System.exit(-1);
        }
        random.setSeed(123);

        initialClientId = Integer.parseInt(args[0]);
        int numClients = Integer.parseInt(args[1]);
        int numOperationsPerClient = Integer.parseInt(args[2]);
        int requestSize = Integer.parseInt(args[3]);
        boolean isWrite = true;//Boolean.parseBoolean(args[4]);
        boolean measurementLeader = true;//Boolean.parseBoolean(args[5]);
        CountDownLatch latch = new CountDownLatch(numClients);
        EVThroughputLatencyClient.Client[] clients = new EVThroughputLatencyClient.Client[numClients];
        data = new byte[requestSize];
        for (int i = 0; i < requestSize; i++) {
            data[i] = (byte) i;
        }

        paddingToExtend = data;

//        ByteBuffer writeBuffer = ByteBuffer.allocate(1 + Integer.BYTES + requestSize);
//        writeBuffer.put((byte) REGISTERVEHICLE);
//        writeBuffer.putInt(requestSize);
//        writeBuffer.put(data);
//        serializedWriteRequest = writeBuffer.array();

//        ByteBuffer readBuffer = ByteBuffer.allocate(1);
//        readBuffer.put((byte) Operation.GET.ordinal());
//        serializedReadRequest = readBuffer.array();

        for (int i = 0; i < numClients; i++) {
            clients[i] = new EVThroughputLatencyClient.Client(initialClientId + i,
                    numOperationsPerClient, isWrite, measurementLeader, latch);
            clients[i].start();
            Thread.sleep(10);
        }
        new Thread(() -> {
            try {
                latch.await();
                System.out.println("Executing experiment");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class Client extends Thread {
        private final int clientId;
        private final int numOperations;
        private final boolean isWrite;
        private final ServiceProxy proxy;
        private final CountDownLatch latch;
        private final boolean measurementLeader;

        public Client(int clientId, int numOperations, boolean isWrite, boolean measurementLeader, CountDownLatch latch) {
            this.clientId = clientId;
            this.numOperations = numOperations;
            this.isWrite = isWrite;
            this.measurementLeader = measurementLeader;
            this.proxy = new ServiceProxy(clientId);
            this.latch = latch;
            this.proxy.setInvokeTimeout(100); // in seconds
        }

        @Override
        public void run() {
            try {
                latch.countDown();
                if (initialClientId == clientId) {
                    String randomID = String.valueOf(random.nextInt(1000000000));
                    int randUserBalance = 1000000 + random.nextInt(9000000);
                    User user = new User(randomID, randUserBalance);
                    registeredUsers.add(randomID);
                    registerUser(user);
                }
                long start = System.nanoTime();
                for (int i = 0; i < numOperations; i++) {
                    long t1, t2, latency;
                    byte[] response;
                    // option between 1 and 5
                    int option = (int)(Math.random()*5+1);

                    // start timer
                    t1 = System.nanoTime();
                    switch (option) {
                        case 1:
                            // register vehicle
                            String randomVehicleID = String.valueOf(100000000 + random.nextInt(899999999));
                            float randomVehicleOwnerBalance = 1000000 + random.nextInt(9000000 + 1);
                            boolean randomIsAvailable = random.nextBoolean();
                            int randomDepositPrice = 1000 + random.nextInt(9000 + 1);
                            int randomVehiclePricePerHour = 5 + random.nextInt(45 + 1);
                            int randomVehiclePricePerKm = 5 + random.nextInt(45 + 1);
                            int randomVehicleRepairPercentageOfFee = 1 + random.nextInt(5);
                            Vehicle vehicle = new Vehicle(randomVehicleID, randomVehicleOwnerBalance, randomIsAvailable, randomDepositPrice, randomVehiclePricePerHour, randomVehiclePricePerKm, randomVehicleRepairPercentageOfFee);
                            registeredVehicles.add(randomVehicleID);
                            registerVehicle(vehicle);
                            break;
                        case 2:
                            // register user
                            String randomUserID = String.valueOf(100000000 + random.nextInt(899999999));
                            int randUserBalance = 100000000 + random.nextInt(900000000 + 1);
                            User user1 = new User(randomUserID, randUserBalance);
                            registeredUsers.add(randomUserID);
                            registerUser(user1);
                            break;
                        case 3:
                            // booking a vehicle
                            String idOfVehicle;
                            if (registeredVehicles.size() == 0) {
                                idOfVehicle = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomVehicleIndex = random.nextInt(registeredVehicles.size());
                                idOfVehicle = registeredVehicles.get(randomVehicleIndex);
                            }

                            String idOfUser;
                            if (registeredUsers.size() == 0) {
                                idOfUser = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomUserIndex = random.nextInt(registeredUsers.size());
                                idOfUser = registeredUsers.get(randomUserIndex);
                            }

                            bookVehicle(idOfUser, idOfVehicle);
                            break;
                        case 4:
                            // returning a vehicle
                            String idOfVehicle1;
                            if (registeredVehicles.size() == 0) {
                                idOfVehicle1 = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomVehicleIndex1 = random.nextInt(registeredVehicles.size());
                                idOfVehicle1 = registeredVehicles.get(randomVehicleIndex1);
                            }

                            String idOfUser1;
                            if (registeredUsers.size() == 0) {
                                idOfUser1 = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomUserIndex1 = random.nextInt(registeredUsers.size());
                                idOfUser1 = registeredUsers.get(randomUserIndex1);
                            }

                            float randomDistance = 1 + random.nextInt(100 + 1);
                            float randomDuration = 1 + random.nextInt(100 + 1);
                            returnVehicle(idOfUser1, idOfVehicle1, randomDistance, randomDuration);
                            break;
                        case 5:
                            // dispute
                            String idOfVehicle2;
                            if (registeredVehicles.size() == 0) {
                                idOfVehicle2 = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomVehicleIndex2 = random.nextInt(registeredVehicles.size());
                                idOfVehicle2 = registeredVehicles.get(randomVehicleIndex2);
                            }

                            String idOfUser2;
                            if (registeredUsers.size() == 0) {
                                idOfUser2 = String.valueOf(100000000 + random.nextInt(899999999));
                            } else {
                                int randomUserIndex2 = random.nextInt(registeredUsers.size());
                                idOfUser2 = registeredUsers.get(randomUserIndex2);
                            }

                            dispute(idOfVehicle2, idOfUser2);
                            break;
                    }
                    t2 = System.nanoTime();
                    latency = t2 - t1;
//                    if (!isWrite && !Arrays.equals(data, response)) {
//                        throw new IllegalStateException("The response is wrong");
//                    }
                    if (initialClientId == clientId && measurementLeader) {
                        System.out.println("Num:"+i+"Latency[ns/ops]: " + latency);
                    }
                }
                long end = System.nanoTime();
                System.out.println("Average latency[ms]: " + ((float)(end-start)/(numOperations+1))/1_000_000);
            } finally {
                proxy.close();
            }
        }

        public String registerUser(User user) {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                 ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

                System.out.println("Registering user " + user.getUserID() + " with balance: " + user.getUserBalance());

                objOut.writeObject(EVRequestType.REGISTERUSER);
                objOut.writeObject(user);
                objOut.writeObject(data);

                objOut.flush();
                byteOut.flush();

                byte[] reply = proxy.invokeOrdered(byteOut.toByteArray());
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

        public String registerVehicle(Vehicle vehicle) {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                 ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

                System.out.println("Registering vehicle " + vehicle.getVehicleID());

                objOut.writeObject(EVRequestType.REGISTERVEHICLE);
                objOut.writeObject(vehicle);
                objOut.writeObject(data);

                objOut.flush();
                byteOut.flush();

                byte[] reply = proxy.invokeOrdered(byteOut.toByteArray());
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

        public String bookVehicle(String userID, String vehicleID) {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                 ObjectOutput objOut = new ObjectOutputStream(byteOut);) {

                objOut.writeObject(EVRequestType.BOOKVEHICLE);
                AbstractMap.SimpleEntry<String, String> pair = new AbstractMap.SimpleEntry<>(userID, vehicleID);
                objOut.writeObject(pair);
                objOut.writeObject(data);

                objOut.flush();
                byteOut.flush();

                byte[] reply = proxy.invokeOrdered(byteOut.toByteArray());
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
                objOut.writeObject(data);

                objOut.flush();
                byteOut.flush();

                byte[] reply = proxy.invokeOrdered(byteOut.toByteArray());
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
                objOut.writeObject(data);

                objOut.flush();
                byteOut.flush();

                byte[] reply = proxy.invokeOrdered(byteOut.toByteArray());
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
    }
}
