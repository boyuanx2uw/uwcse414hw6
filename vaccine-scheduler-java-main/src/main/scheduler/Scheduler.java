package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1) done
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1) done
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2) done
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")){
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Please Try Again");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 1: if username exists
        if (usernameExists(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        // check 2: if password is strong
        if(strongpassword(password)) return;

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password,salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" ***** Account created successfully ***** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }
   private static boolean PasswordMix(String password){
     String n = ".*[0-9].*";
     String L = ".*[A-Z].*";
     String l = ".*[a-z].*";
     return password.matches(n) && password.matches(L) && password.matches(l);
    }
    private static boolean PasswordLength(String password){
      int length = password.length();
        return length < 8;
    }

    private static boolean PasswordSpecial(String password){
     return password.contains("!") || password.contains("?") || password.contains("@") || password.contains("#");
    }
    private static boolean strongpassword(String password) {
        if(PasswordLength(password)){
            System.out.println("Password should be at least 8 characters");
            return true;
        }else if (!PasswordMix(password)){
            System.out.println("Password should be a mixture of uppercase letters, lowercase letters and numbers");
            return true;
        }else if(!PasswordSpecial(password) ){
            System.out.println("Password should contain one of the following: !,?,@,#");
            return true;
        }
        return false;
    }
    private static boolean usernameExists(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
            String selectUsername;
        if(currentPatient != null) {
            selectUsername = "SELECT * FROM Patient WHERE Username = (?)";
        }else{
            selectUsername = "Select * from Caregivers where Username = (?)";
        }

        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }
    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExists(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check if the password is strong
        if(strongpassword(password)) return;

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" ***** Account created successfully ***** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }



    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String password = tokens[2];

        String username = tokens[1];


        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // check 1: if the patient or the caregiver is logged in
        if ((currentCaregiver == null) && (currentPatient == null)) {
            System.out.println("Please login first");
            return;
        }
        // check 2: the length for the tokens need to be exactly 2 with the operation name
        if (tokens.length != 2){
            System.out.println("Please enter the correct format");
            return;
        }
        String date = tokens[1];
        //take the caregiver name from the caregiver table using date inputted
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getinformationCare = "Select Username from Availabilities where Time = (?) and Availabilities.username NOT IN (Select Caregiver_Name from Appointments Where Time = (?))";
        try {
            PreparedStatement statement = con.prepareStatement(getinformationCare);
            Date d = Date.valueOf(date);
            statement.setDate(1, d);
            statement.setDate(2, d);
            ResultSet rs = statement.executeQuery();
            if (!rs.isBeforeFirst()) {
                System.out.println("There is no caregiver available at this time");
            } else {
                while (rs.next()) {
                    System.out.println("Caregiver: " + rs.getString("Username"));
                }
                // take the vaccine name and available doses using the name of caregiver
                getinformationDoses();
            }
        }catch (SQLException e){
                System.out.println("Error occurred when show schedule");
                e.printStackTrace();
            }
        }

    private static void getinformationDoses(){
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String getVaccine = "SELECT * FROM Vaccines";

            try {
                PreparedStatement statement = con.prepareStatement(getVaccine);
                ResultSet resultSet = statement.executeQuery();
                System.out.println("****** Vaccine and Available Doses *****");
                while (resultSet.next()) {
                    System.out.print("Vaccine: " + resultSet.getString("Name") + "   " + "\t");
                    System.out.print("Available Doses: " + resultSet.getInt("Doses") + "   " + "\t");
                    System.out.println();
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when show appointments");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }

    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // check 1: if the user is login as patient
        if (currentPatient == null) {
            System.out.println("Please log in as a patient");
            return;
        }
        // check 2: the length of the input should be 3 including the operation name
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        String username = currentPatient.getUsername();
        // check 3: if the patient has an appointment already
        if (Appointment.usernameExists(username)) {
            System.out.println("You already have an appointment");
            return;
        }

       // check 4: if the vaccine exists and if the doses are available
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when getting vaccine");
            e.printStackTrace();
        }
        if (vaccine == null) {
            System.out.println("There is no "+ vaccineName + " available ");
            return;
        }
        if (vaccine.getAvailableDoses() == 0 ) {
            System.out.println("There is not enough " + vaccineName );
            return;
        }

        Appointment appointment;
        try {
            Date d = Date.valueOf(date);
            String assigned_caregiver = getRandomCaregiver(d);
            if (assigned_caregiver == null ) {
                System.out.println("no available caregiver at this time");
                return;
            }
            int id = Appointment.getMaxID()+1;
            appointment = new Appointment.AppointmentBuilder(id, username,assigned_caregiver,vaccineName,d).build();
            vaccine.decreaseAvailableDoses(1);
            appointment.saveToDB();
            System.out.println("***** Reservation created successfully *****");
            System.out.println("Your Application ID is " + id + " and your Caregiver is " + assigned_caregiver);
        }catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date");
        }catch(SQLException e){
            System.out.println("Errors occurred when reserving");
            e.printStackTrace();
        }

    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }
    public static String getRandomCaregiver(Date time) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // Here the system will only select caregiver that is available at this time. in other words, the (time, caregiver_name) set should not in the appointment table
        String getRandom = "Select Username from Availabilities where Time = (?) and Availabilities.username NOT IN (Select Caregiver_name from Appointments Where Time = (?)) order by NEWID()";
        try {
            PreparedStatement statement = con.prepareStatement(getRandom);
            statement.setDate(1, time);
            statement.setDate(2, time);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getString("Username");
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }


    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // check 1: if a patient or a caregiver has logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login before checking appointments");
            return;
        }
        //check 2: the input token length should be exactly 1 including the command name
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getInformation;
        String Username;
        if (currentPatient != null) {
            getInformation = "Select * From Appointments Where Patient_name = (?)";
            Username = currentPatient.getUsername();
        } else {
            getInformation = "Select * From Appointments Where Caregiver_name = (?) ORDER BY Time";
            Username = currentCaregiver.getUsername();
        }

            try {
                PreparedStatement statement = con.prepareStatement(getInformation);
                statement.setString(1, Username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println("Appointment ID: " + resultSet.getInt(1) + "\t");
                    if (currentCaregiver != null){
                    System.out.print("Patient_name: " + resultSet.getString(2) + "\t");
                    }else {
                    System.out.print("Caregiver_name: " + resultSet.getString(3) + "\t");
                    }
                    System.out.print("Vaccine_name: " + resultSet.getString(4) + "\t");
                    System.out.print("Time: " + resultSet.getDate(5) + "\t");
                    System.out.println();
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when show appointment");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
    }
    private static ResultSet cancel(String[] tokens) {
        // check 1: check if anyone has logged in
        if (currentCaregiver == null && currentPatient == null){
            System.out.println("Nobody is logged-in");
            return null;
        }
        //check 2: with the operation name, the length should be exactly 2
        if (tokens.length != 2){
            System.out.println("Please enter only the appointment ID");
            return null;
        }
        // check 3: check if there exists appointments to be cancelled
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String appointmentcount = "Select * From Appointments Where ID = (?)";
        try{
            PreparedStatement statement = con.prepareStatement(appointmentcount);
            statement.setString(1, tokens[1]);
            ResultSet rsnull = statement.executeQuery();
            if (!rsnull.next()){
                System.out.println("Wrong Appointment Number ");
                return rsnull;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        // patient or caregiver can only cancel their appointments.


        String cancelling;
        String username;
        String check;
        // check if the ID is belonged to the patient/caregiver

        if (currentPatient != null) {
            check = "Select ID from Appointments Where Patient_name = (?) and ID = (?)";
            cancelling = "DELETE FROM Appointments Where ID = (?) and Patient_name = (?)";
            username = currentPatient.getUsername();
        } else {
            check = "Select ID from Appointments Where Caregiver_name = (?) and ID = (?)";
            cancelling = "DELETE FROM Appointments Where ID = (?) and Caregiver_name = (?)";
            username = currentCaregiver.getUsername();
        }
        try{
            PreparedStatement statement = con.prepareStatement(check);
            statement.setString(1,username);
            statement.setString(2, tokens[1]);
            ResultSet rsID = statement.executeQuery();
            if (!rsID.next()) {
                System.out.println("Wrong Appointment Number");
                return rsID;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            PreparedStatement statement = con.prepareStatement(cancelling);
            statement.setString(1, tokens[1]);
            statement.setString(2,username);
            statement.executeUpdate();
            System.out.println("successfully cancelled");

        } catch (SQLException e) {
            System.out.println("Error occurred when cancel appointment");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return null;
    }

    private static void logout(String[] tokens)
            {
        // TODO: Part 2
        // check 1: check if anyone has logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Nobody is logged-in");
            return;
        }
        // check 2: with the operation name, the length should be exactly 1
        if (tokens.length != 1) {
            System.out.println("Please type 'logout' to logout");
            return;
        }

        if (currentCaregiver != null) {
            currentCaregiver = null;
        }else {
        currentPatient = null;
        }
        // printout the logout
            System.out.println("You have successfully logged out!");

    }
}
