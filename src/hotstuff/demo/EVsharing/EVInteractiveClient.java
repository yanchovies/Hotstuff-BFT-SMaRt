package hotstuff.demo.EVsharing;


import java.io.Console;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class EVInteractiveClient {
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: demo.EVsharing.EVInteractiveClient <client id>");
        }

        int clientId = Integer.parseInt(args[0]);
        EVclient client = new EVclient(clientId);

        boolean exit = false;
        String result;
        Scanner sc = new Scanner(System.in);
        while(!exit) {
            System.out.println("Select an option:");
            System.out.println("0 - Terminate this client");
            System.out.println("1 - Register EV");
            System.out.println("2 - Register User");
            System.out.println("3 - Book an EV");
            System.out.println("4 - Return an EV");
            System.out.println("5 - Dispute");

            int cmd = Integer.parseInt(sc.nextLine());

            switch (cmd) {
                case 0:
                    client.close();
                    exit = true;
                    break;
                case 1:
                    System.out.println("Enter the vehicle ID:");
                    String vehicleID = sc.nextLine();
                    System.out.println("Enter the vehicle owner's balance:");
                    float vehicleOwnerBalance = Integer.parseInt(sc.nextLine());
                    System.out.println("Specify if the vehicle is available for rental:");
                    boolean isAvailable = Boolean.parseBoolean(sc.nextLine());
                    System.out.println("Enter the vehicle's deposit price:");
                    int depositPrice = Integer.parseInt(sc.nextLine());
                    System.out.println("Enter the vehicle's rental price (per hour):");
                    int rentalPricePerHour = Integer.parseInt(sc.nextLine());
                    System.out.println("Enter the vehicle's rental price (per km):");
                    int rentalPricePerKm = Integer.parseInt(sc.nextLine());
                    System.out.println("Enter the repair&maintenance percentage of the final fee:");
                    int vehicleRepairPercentageOfFee = Integer.parseInt(sc.nextLine());
                    Vehicle vehicle = new Vehicle(vehicleID, vehicleOwnerBalance, isAvailable, depositPrice, rentalPricePerHour, rentalPricePerKm, vehicleRepairPercentageOfFee);
                    result = client.registerVehicle(vehicle);
                    System.out.println(result);
                    break;
                case 2:
                    System.out.println("Enter the user ID:");
                    String userID = sc.nextLine();
                    System.out.println("Enter the initial balance of the user:");
                    float balance = Integer.parseInt(sc.nextLine());
                    // creating a User class object
                    User user = new User(userID, balance);
                    result = client.registerUser(user);
                    System.out.println(result);
                    break;
                case 3:
                    System.out.println("Enter the user ID of the user that would like to book an EV:");
                    String userId = sc.nextLine();
                    System.out.println("Enter the vehicle ID of the vehicle that the user would like to book:");
                    String vehicleId = sc.nextLine();
                    result = client.bookVehicle(userId, vehicleId);
                    System.out.println(result);
                    break;
                case 4:
                    System.out.println("Enter the user ID of the user that would like to return an EV:");
                    String userid = sc.nextLine();
                    System.out.println("Enter the vehicle ID of the vehicle that the user would like to return:");
                    String vehicleid = sc.nextLine();
                    System.out.println("Enter the distance in kilometers that the user has driven:");
                    float distance = Float.parseFloat(sc.nextLine());
                    System.out.println("Enter the number of hours that the user has rented the vehicle:");
                    float time = Float.parseFloat(sc.nextLine());
                    result = client.returnVehicle(userid, vehicleid, distance, time);
                    //result = client.returnVehicle(userid, vehicleid);
                    System.out.println(result);
                    break;
                case 5:
                    System.out.println("Enter your user ID:");
                    String userid1 = sc.nextLine();
                    System.out.println("Enter the vehicle ID that you have used:");
                    String vehicleid1 = sc.nextLine();

                    result = client.dispute(vehicleid1, userid1);
                    System.out.println(result);
                    break;
                default:
                    break;
            }
        }
    }
}
