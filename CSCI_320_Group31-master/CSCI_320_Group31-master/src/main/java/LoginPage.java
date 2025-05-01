import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Scanner;

/**
 * This class contains methods that show a menu to users, allowing them to log in with an existing account or create
 * a new one. In support of creating new users and checking logins, there are also methods for creating a new salt value
 * and creating a password hash from a salt and a password string.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class LoginPage {

    private static final Scanner InputReader = new Scanner(System.in);

    /**
     * This method displays user options when viewing the login page, allowing the user to create an account or log into
     * an existing account. Since this is the entry point to the application, a user can also exit to end the program.
     * This process loops until a user successfully logs into or creates an account, after which the main page is opened.
     */
    public static void WelcomePage() throws SQLException {
        while (true) {
            System.out.println("Welcome user! Please select one of the following options.");
            System.out.println("1. Login - Login with existing account.");
            System.out.println("2. Create Account - Create a new account.");
            System.out.println("3. Exit - Exit the movie app.");
            String userCommand = InputReader.nextLine().trim();
            int id;
            switch (userCommand) {
                case "Exit" -> {
                    System.out.println("Thank you for using our service!");
                    return;
                }
                case "Login" -> {
                    id = ExistingLogin();
                    if (id == -1) {
                        continue;
                    }
                }
                case "Create Account" -> {
                    id = NewLogin();
                    if (id == -1) {
                        continue;
                    }
                }
                default -> {
                    System.out.println("Please enter a valid command from those provided.");
                    continue;
                }
            }
            MainPage.MainMenu(id);
        }
    }

    /**
     * This method generates a secure hash value in bytes, converts it to a string, and returns it.
     *
     * @return the salt string if successful, null otherwise.
     */
    private static String GenerateNewSalt(){
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            // Generate 32 byte random value.
            byte[] salt = new byte[32];
            sr.nextBytes(salt);
            // Encode in base64 rather than in hex to be more concise.
            return Base64.getEncoder().encodeToString(salt);
        } catch (NoSuchAlgorithmException e) {
            // This exception only every occurs when the system this is run on does not support the secure random
            // instance generation method. Since java implementations are required to have an implementation of this,
            // we expect this error to never occur, but it is handled nonetheless.
            System.out.println("Error occurred when generating a salt value, halting user creation.");
            return null;
        }
    }

    /**
     * This method generates a base 64 hash string generated from using the given salt on the given password.
     *
     * @param  password The password string to be hashed.
     * @param  salt The salt string to apply before hashing.
     *
     * @return the password hash string if successful, null otherwise.
     */
    private static String HashPassword(String password, String salt){
        StringBuilder saltedPassword = new StringBuilder();
        //If password length is odd, pad it with a '0' to make it even length.
        if(password.length() % 2 == 1){
            password += "0";
        }
        // Insert the entirety of the salt every 2 characters (including the start and end).
        for(int i = 0; i < password.length(); i += 2){
            saltedPassword.append(salt).append(password, i, i + 2);
        }
        saltedPassword.append(salt);
        // Use sha3 with 256 bit digests to generate the hash, then encode it with base 64 and return the string.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] passwordHash = digest.digest(saltedPassword.toString().getBytes());
            return Base64.getEncoder().encodeToString(passwordHash);
        }catch (NoSuchAlgorithmException e) {
            // Same as above, but occurs when the java platform does not support sha3-256. This is more likely than the
            // previous case since java platforms are not required to implement all hash functions, but since this works
            // on our devices, it likewise is ignored.
            System.out.println("Error occurred when calculating password hash.");
            return null;
        }
    }

    /**
     * This method allows a user to attempt to log in to an existing account using their email and password. When
     * entering an email not associated with an account or an incorrect password, a user can exit back to the main menu.
     * Otherwise, the uid of the accessed account is returned.
     *
     * @return  -1 if the user could not log in for some reason, otherwise the id associated with the user's account.
     */
    private static int ExistingLogin() throws SQLException {
        System.out.println("Enter your email.");
        String userInfo = InputReader.nextLine().trim();
        ResultSet rs;
        int id;
        String username;
        String password;
        String salt;
        try (PreparedStatement emailStmt = InitConnection.getCon().prepareStatement("SELECT uid, username, " +
                "password, salt FROM users WHERE email = ?")) {
            emailStmt.setString(1, userInfo);
            rs = emailStmt.executeQuery();
            while (!rs.next()) {
                System.out.println("This email is not registered with an account. Please choose a valid email or enter " +
                        "'Exit' to return to the login page.");
                userInfo = InputReader.nextLine().trim();
                if (userInfo.equals("Exit")) {
                    return -1;
                } else {
                    emailStmt.setString(1, userInfo);
                    rs = emailStmt.executeQuery();
                }
            }
            // Once we reach here, a user has given a valid email, so lets store the associated id, password, and salt.
            id = rs.getInt("uid");
            username = rs.getString("username");
            password = rs.getString("password");
            salt = rs.getString("salt");
        }

        System.out.println("Please enter your password.");
        String passwordAttempt = InputReader.nextLine().trim();
        // Verify password accuracy by checking stored password hash against newly generated one using salt.
        while (!password.equals(HashPassword(passwordAttempt, salt))) {
            System.out.println("This password is incorrect. Please enter a correct password or enter 'Exit' to return " +
                    "to the main page.");
            passwordAttempt = InputReader.nextLine().trim();
            if (passwordAttempt.equals("Exit")) {
                return -1;
            }
        }

        // Since a returning user is logging in, we need to update their last access date to now.
        try (Statement accessStmt = InitConnection.getCon().createStatement()) {
            String accessQuery = "UPDATE users SET lastaccess = NOW() WHERE uid = " + id;
            accessStmt.executeUpdate(accessQuery);
        }

        System.out.println("Welcome back " + username + "!");
        return id;
    }

    /**
     * This method allows a user to create a new account. Once the user enters an email not associated with an existing
     * user, they are prompted to enter their further log in information. If successful, the new automatically-generated
     * uid associated with the account is returned.
     *
     * @return  -1 if the user failed to create an account, or the user id associated with the new account.
     */
    private static int NewLogin() throws SQLException {
        System.out.println("Enter the email address for this account.");
        String userInfo = InputReader.nextLine().trim();
        // Force users to enter an address containing an @. While this is checked by the database, we check here to
        // avoid entering all the data for an incorrect email.
        while(!userInfo.contains("@")) {
            if (userInfo.equals("Exit")) {
                return -1;
            } else {
                System.out.println("This email is not valid. Please enter a valid email address containing a '@' or " +
                        "enter 'Exit' to return to the main page.");
                userInfo = InputReader.nextLine().trim();
            }
        }
        try (PreparedStatement emailStmt = InitConnection.getCon().prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            emailStmt.setString(1, userInfo);
            ResultSet rs = emailStmt.executeQuery();
            while (rs.next()) {
                System.out.println("This email is already registered with an account. Please choose a new email or " +
                        "enter 'Exit' to return to the main page.");
                userInfo = InputReader.nextLine().trim();
                if (userInfo.equals("Exit")) {
                    return -1;
                } else {
                    emailStmt.setString(1, userInfo);
                    rs = emailStmt.executeQuery();
                }
            }
        }

        // Once we reach here, we know the user has provided a valid email, so now enter the remaining account details.
        String username;
        int id;
        try (PreparedStatement accountStmt = InitConnection.getCon().prepareStatement("INSERT INTO users(email, " +
                "fname, lname, username, password, salt) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            accountStmt.setString(1, userInfo);
            System.out.println("Enter your first name.");
            accountStmt.setString(2, InputReader.nextLine().trim());
            System.out.println("Enter your last name.");
            accountStmt.setString(3, InputReader.nextLine().trim());
            System.out.println("Enter your username for this account.");
            username = InputReader.nextLine().trim();
            accountStmt.setString(4, username);
            System.out.println("Enter your password. This will be required to log in to future sessions.");
            String password = InputReader.nextLine().trim();
            String salt = GenerateNewSalt();
            accountStmt.setString(5, HashPassword(password, salt));
            accountStmt.setString(6, salt);
            accountStmt.executeUpdate();
            ResultSet keys = accountStmt.getGeneratedKeys();
            keys.next();
            id = keys.getInt(1);
        }
        System.out.println("Welcome " + username + "!");
        return id;
    }
}
