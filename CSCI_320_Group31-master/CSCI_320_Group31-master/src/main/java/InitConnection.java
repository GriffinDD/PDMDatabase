import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.sql.*;
import java.util.Properties;

/**
 * This class contains methods related to the initiation, querying, and closure of the ssh tunnel and PostgreSQL
 * connection used to connect to the project database.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */

public class InitConnection {

    // Technically does not need to be static since there are no other instances of InitConnection.
    private static Connection con = null;

    private static String url;

    private static Properties props;

    /**
     * This checks if the current connection is still open. If it is open, it is returned, otherwise a new connection
     * is opened and returned. If any errors occur, a message is printed to the user and then the program ends.
     *
     * @return Connection if it was still open or a new one was made, otherwise program exits.
     */
    public static Connection getCon() throws SQLException{
        try {
            // If connection is still open, just return it as is.
            if (!con.isClosed()) {
                return con;
            }

            // Otherwise, attempt to re-open it.
            con = DriverManager.getConnection(url, props);
            if(con == null){
                System.out.println("Connection failed.");
                System.exit(1);
            }else{
                return con;
            }
        }catch(SQLException e){
            System.out.println("Encountered unexpected error:  " + e.getMessage() + " Closing connection.");
            if (con != null && !con.isClosed()) {
                con.close();
            }
            System.exit(1);
        }
        return null;
    }

    /**
     * This method initializes the ssh tunnel and PostgreSQL connection used for all SQL queries during program
     * execution. After initializing the session, the login page is called to begin user input.
     *
     * @param  args  User arguments, though none are used in this program.
     */
    public static void main(String[] args) throws SQLException {
        String username = "";
        String password = "";
        Session session = null;
        try {

            // Set up SSH tunnel.
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            session = jsch.getSession(username, "{server}", 22);
            session.setPassword(password);
            session.setConfig(config);
            session.setConfig("PreferredAuthentications","publickey,keyboard-interactive,password");
            session.connect();
            int assigned_port = session.setPortForwardingL(5432, "127.0.0.1", 5432);
            url = "jdbc:postgresql://127.0.0.1:"+ assigned_port;

            props = new Properties();
            props.put("user", username);
            props.put("password", password);

            // Load the PostgreSQL driver.
            Class.forName("org.postgresql.Driver");
            // Open a connection to the database.
            con = DriverManager.getConnection(url, props);
            if (con != null) {
                // Initialize the application by going to the welcome page.
                LoginPage.WelcomePage();

            } else {
                System.out.println("Connection failed.");
                System.exit(1);
            }
        } catch (SQLException s) {
            // Handle any SQL exceptions that occur, which are all thrown up from lower methods since we should stop
            // if anything unexpected happens.
            System.out.println("Encountered unexpected SQL error:  " + s.getMessage() + " Closing connection.");
        } catch (Exception e) {
            // Handle any other exceptions that may occur.
            System.out.println("Exception " + e.getMessage());
        } finally {
            if (con != null && !con.isClosed()) {
                System.out.println("Closing Database Connection");
                con.close();
            }
            if (session != null && session.isConnected()) {
                System.out.println("Closing SSH Connection");
                session.disconnect();
            }
        }
    }
}
