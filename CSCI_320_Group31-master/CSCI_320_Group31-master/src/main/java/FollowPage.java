import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * This class contains methods to display a set of following-related actions to users, as well as allow them to search,
 * follow, and unfollow other users as well as view their own followers.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class FollowPage {
    private static final Scanner InputReader = new Scanner(System.in);
    private static int UserID;

    /**
     * This method prints out the options of the main follow page, allowing users to see followed/following users as
     * well as enter a submenu for managing these lists. If a user exits, they are taken back to the main page.
     *
     * @param id The int representing the uid of the currently logged-in user.
     */
    public static void FollowMenu(int id) throws SQLException {
        UserID = id;
        while (true) {
            System.out.println("Please select one of the following options.");
            System.out.println("1. Profile - display user statistics and top 10 movies.");
            System.out.println("2. Followers - display the list of your followers.");
            System.out.println("3. Following - display the list of those you follow.");
            //Will only show users who you do not already follow
            System.out.println("4. Manage - search, follow, and unfollow other users.");
            System.out.println("5. Exit - return to the main screen.");
            String userCommand = InputReader.nextLine().trim();
            switch (userCommand) {
                case "Exit" -> {
                    return;
                }
                case "Profile" -> ProfileStats();
                case "Followers" -> FollowerList();
                case "Following" -> FollowedList();
                case "Manage" -> ManageFollows();
                default -> System.out.println("Please enter a valid command from those provided.");
            }
        }
    }

    /**
     * This method prints out the emails of all the users currently following the logged-in user.
     */
    private static void FollowerList() throws SQLException {
        ResultSet followers;
        try (PreparedStatement followerStmt = InitConnection.getCon().prepareStatement(
                "SELECT email FROM users, follows WHERE followeduid = ? AND followeruid = uid ORDER BY email")) {
            followerStmt.setInt(1, UserID);
            followers = followerStmt.executeQuery();
            if (!followers.isBeforeFirst()) {
                System.out.println("You have no followers yet.");
                return;
            }
            System.out.println("Followers:");
            while (followers.next()) {
                System.out.println(followers.getString("email"));
            }
        }
    }

    /**
     * This method prints out the emails of all the users currently followed by the logged-in user.
     */
    private static void FollowedList() throws SQLException {
        ResultSet followed;
        try (PreparedStatement followedStmt = InitConnection.getCon().prepareStatement(
                "SELECT email FROM users, follows WHERE followeruid = ? AND followeduid = uid ORDER BY email")) {
            followedStmt.setInt(1, UserID);
            followed = followedStmt.executeQuery();
            if (!followed.isBeforeFirst()) {
                System.out.println("You do not follow anyone yet, check out the manage page to follow other users!");
                return;
            }
            System.out.println("Followed users:");
            while (followed.next()) {
                System.out.println(followed.getString("email"));
            }
        }
    }

    /**
     * This method displays the follower management menu, which allows users to search for other users by partial matches
     * on emails, or to follow/unfollow a user by providing their email. Exiting here returns to the main follow menu.
     */
    private static void ManageFollows() throws SQLException {
        System.out.println("Please use the following commands to search for and follow other users.");
        System.out.println("1. Search [string] - returns all users whose emails are a partial match with the given string.");
        System.out.println("2. Follow [email] - follow the user whose email is provided.");
        System.out.println("3. Unfollow [email] - unfollow the user whose email is provided.");
        System.out.println("4. Exit - return to the profile menu.");
        while (true) {
            String userCommand = InputReader.nextLine().trim();
            if (userCommand.equals("Exit")) {
                return;
            } else if (userCommand.startsWith("Search")) {
                String targetMatch = userCommand.substring(6).trim();
                // Nested query gets the uids of everyone we follow, then returns the emails of everyone who matches
                // the given substring and is not us or on our follow list already.
                ResultSet targetUsers;
                try (PreparedStatement findUserStmt = InitConnection.getCon().prepareStatement(
                        "SELECT email FROM users WHERE email LIKE ? AND uid <> ? AND uid NOT IN " +
                                "(SELECT followeduid FROM follows WHERE followeruid = ?) ORDER BY email")) {
                    findUserStmt.setString(1, "%" + targetMatch + "%");
                    findUserStmt.setInt(2, UserID);
                    findUserStmt.setInt(3, UserID);
                    targetUsers = findUserStmt.executeQuery();
                    if (!targetUsers.isBeforeFirst()) {
                        System.out.println("No new users were found for this criteria.");
                        continue;
                    }
                    while (targetUsers.next()) {
                        System.out.println(targetUsers.getString("email"));
                    }
                }
            } else if (userCommand.startsWith("Follow") || userCommand.startsWith("Unfollow")) {
                int substringLength = userCommand.startsWith("Follow") ? 6 : 8;
                String targetEmail = userCommand.substring(substringLength).trim();
                ResultSet targetUser;
                int targetId;
                try (PreparedStatement retrieveIDStmt = InitConnection.getCon().prepareStatement("SELECT uid " +
                        "FROM users WHERE email = ?")) {
                    retrieveIDStmt.setString(1, targetEmail);
                    targetUser = retrieveIDStmt.executeQuery();
                    if (!targetUser.next()) {
                        System.out.println("This email does not match any users, please enter a valid email.");
                        continue;
                    }
                    targetId = targetUser.getInt("uid");
                }

                if (userCommand.startsWith("Follow")) {
                    try (PreparedStatement followUserStmt = InitConnection.getCon().prepareStatement("INSERT INTO " +
                            "follows(followeruid, followeduid) VALUES (?, ?)")) {
                        followUserStmt.setInt(1, UserID);
                        followUserStmt.setInt(2, targetId);
                        try {
                            followUserStmt.executeUpdate();
                            System.out.println("You have successfully followed " + targetEmail + "!");
                        } catch (SQLException e) {
                            String errorMessage = e.getMessage();
                            if (errorMessage.contains("check constraint")) {
                                // Also corresponds to SQLState 23514, but trying to avoid magic numbers
                                System.out.println("You cannot follow yourself, please enter the email of another user.");
                            } else if (errorMessage.contains("unique constraint")) { // Corresponds to SQLState 23505
                                System.out.println("You are already following this user, please enter the email of a " +
                                        "new user.");
                            } else {
                                // If it is not one of the 2 errors we expect, throw it because something else bad happened.
                                throw e;
                            }
                        }
                    }
                } else {
                    int rowsDeleted;
                    try (PreparedStatement unfollowUserStmt = InitConnection.getCon().prepareStatement("DELETE FROM " +
                            "follows WHERE followeruid = ? AND followeduid = ?")) {
                        unfollowUserStmt.setInt(1, UserID);
                        unfollowUserStmt.setInt(2, targetId);
                        rowsDeleted = unfollowUserStmt.executeUpdate();
                        // While deleting no rows is technically not an error, let the user know so we can try to avoid
                        // needless updates.
                        if (rowsDeleted == 1) {
                            System.out.println("You have successfully unfollowed " + targetEmail + "!");
                        } else {
                            System.out.println("You were not following " + targetEmail + ".");
                        }
                    }
                }
            } else {
                System.out.println("Please enter a valid command.");
            }
        }
    }

    /**
     * This method displays the user profile, which shows the counts of your collections, users that follow you and users
     * that you follow. Finally, it prints out your top 10 movies, ordered first by most to least watches and second by
     * highest to lowest rating. Note that it will only show movies if you have watched any, so you can see top 10 movies
     * if you have watched but not rated movies, but if you have rated but not watched any movies, you will not see
     * movies displayed.
     */
    private static void ProfileStats() throws SQLException {
        int numCollections;
        int numFollowers;
        int numFollowing;
        // Get the counts of collections, followers, and followings associated with this user.
        try (PreparedStatement collectionStmt = InitConnection.getCon().prepareStatement(
                "SELECT count(cid) AS totalCollections FROM collection WHERE creatoruid = ?")) {
            collectionStmt.setInt(1, UserID);
            ResultSet rs = collectionStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                numCollections = 0;
            } else {
                rs.next();
                numCollections = rs.getInt("totalCollections");
            }
        }

        try (PreparedStatement followerStmt = InitConnection.getCon().prepareStatement(
                "SELECT count(followeruid) AS totalFollowers FROM follows WHERE followeduid = ?")) {
            followerStmt.setInt(1, UserID);
            ResultSet rs = followerStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                numFollowers = 0;
            } else {
                rs.next();
                numFollowers = rs.getInt("totalFollowers");
            }
        }

        try (PreparedStatement followedStmt = InitConnection.getCon().prepareStatement(
                "SELECT count(followeduid) AS totalFollowed FROM follows WHERE followeruid = ?")) {
            followedStmt.setInt(1, UserID);
            ResultSet rs = followedStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                numFollowing = 0;
            } else {
                rs.next();
                numFollowing = rs.getInt("totalFollowed");
            }
        }

        System.out.println("User Profile:");
        System.out.println("Collections: " + numCollections + " Followers: " + numFollowers + " Following: " + numFollowing);
        System.out.println("Top 10 movies:");
        System.out.println(String.format("%-30s", "Title") + " | " + String.format("%-5s", "MID")  +
                " | Watches | Your Rating");
        // Finally, as the most complex part, get the top 10 movies for this user by plays, first ordered by number of
        // watches and second by rating (with unrated movies functionally considered a rating of 0).
        try (PreparedStatement topMoviesStmt = InitConnection.getCon().prepareStatement(
                "SELECT m.mid, m.title, totalWatches, r.rating FROM movie m " +
                        "INNER JOIN (SELECT mid, count(starttime) AS totalWatches " +
                        "FROM watches WHERE uid = ? GROUP BY mid) AS w ON w.mid = m.mid " +
                        "LEFT JOIN rates r ON r.uid = ? AND r.mid = w.mid AND r.mid = m.mid " +
                        "ORDER BY totalWatches DESC, r.rating DESC NULLS LAST LIMIT 10")) {
            topMoviesStmt.setInt(1, UserID);
            topMoviesStmt.setInt(2, UserID);
            ResultSet rs = topMoviesStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                System.out.println("You have not watched any movies yet.");
            } else {
                while (rs.next()) {
                    String title = rs.getString("title");
                    int mid = rs.getInt("mid");
                    int watches = rs.getInt("totalWatches");
                    String rating;
                    if (rs.getObject("rating") != null) {
                        rating = rs.getString("rating");
                    }else{
                        rating = "Unrated";
                    }
                    System.out.println(String.format("%-30s", title).substring(0, 30) + " | " + String.format("%-5s", mid)
                                    + " | " + String.format("%-7s", watches) + "  | " + String.format("%-11s", rating));
                }
            }
        }
        System.out.println("-------");
    }
}