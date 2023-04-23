package hotstuff.demo.EVsharing;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 2L;
    private String userID;
    private float userBalance;

    private List<String> IDsOfVehiclesUsed;

    private String currentVehicleAccessCode;

    public  User(String userID, float userBalance) {
        this.userID = userID;
        this.userBalance = userBalance;
        this.currentVehicleAccessCode = "";
        this.IDsOfVehiclesUsed = new ArrayList<>();
    }

    // getters

    public String getUserID() {
        return userID;
    }

    public float getUserBalance() {
        return userBalance;
    }

    public String getCurrentVehicleAccessCode() {
        return currentVehicleAccessCode;
    }

    public List<String> getIDsOfVehiclesUsed() {
        return IDsOfVehiclesUsed;
    }

    // setters

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setUserBalance(float userBalance) {
        this.userBalance = userBalance;
    }

    public void setCurrentVehicleAccessCode(String currentVehicleAccessCode) {
        this.currentVehicleAccessCode = currentVehicleAccessCode;
    }

    public void setIDsOfVehiclesUsed(List<String> IDsOfVehiclesUsed) {
        this.IDsOfVehiclesUsed = IDsOfVehiclesUsed;
    }

}
