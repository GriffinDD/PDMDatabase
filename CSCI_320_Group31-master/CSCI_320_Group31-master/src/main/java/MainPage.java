import java.sql.SQLException;
import java.util.Scanner;

/**
 * This class contains a simple menu to redirect users to different task pages.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class MainPage {
    private static final Scanner InputReader = new Scanner(System.in);

    /**
     * This method prints the different options representing the primary functions of our application, allowing a user
     * to look at movies, manage followers, and manage collections. A user can also log out of their session to
     * return to the login page.
     *
     * @param  id The int representing the uid of the currently logged-in user.
     */
    public static void MainMenu(int id) throws SQLException {
        while (true) {
            System.out.println("Please select one of the following options.");
            System.out.println("1. Movies - Search, browse, watch, and rate available movies.");
            System.out.println("2. Collections - Create and manage your collections.");
            // For now, we are keeping the follower stuff in the user profile, we can move it if needed.
            System.out.println("3. Profile - Manage your following/follower lists");
            System.out.println("4. Logout - Log out of this session.");
            String userCommand = InputReader.nextLine().trim();
            switch (userCommand) {
                case "Logout" -> {
                    System.out.println("Thank you for using our service!");
                    return;
                }
                case "Movies" -> MoviePage.MovieMenu(id);
                case "Collections" -> CollectionPage.CollectionMenu(id);
                case "Profile" -> FollowPage.FollowMenu(id);
                default -> System.out.println("Please enter a valid command from those provided.");
            }
        }
    }
}
