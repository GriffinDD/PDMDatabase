import java.sql.*;
import java.util.*;

/**
 * This class contains methods that compute similar users based on watch and rating history, then display some
 * recommendations based on those users' highly rated movies.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class MovieRecommendationPage {

    /**
     * This method computes and displays a list of recommended movies to watch via similar users.
     *
     * @param id The int representing the uid of the currently logged-in user.
     */
    public static void FindMovies(int id) throws SQLException {

        ArrayList<Integer> similarUserIds = findSimilarUsers(id);

        // If there are no similar users, the user is probably new. This is very unlikely given the low matching
        // threshold currently set, so really only occurs when you have watched a single movie that no one else has.
        if (similarUserIds.isEmpty()) {
            System.out.println("No similar users found to generate recommendations.");
            return;
        }

        //Otherwise, find and print movie recommendations based on highly rated movies from the similar users.
        getHighlyRatedMovies(similarUserIds, id);
    }

    /**
     * This method finds the most similar users for the currently logged-in user by genre, actor, and director. The
     * arbitrary threshold for similarity is set at 3 matching genres, actors, or directors total.
     *
     * @param userId The int representing the uid of the currently logged-in user.
     *
     * @return  ArrayList of Integer Ids corresponding to similar users.
     */
    private static ArrayList<Integer> findSimilarUsers(int userId) throws SQLException {
        ArrayList<Integer> similarUsers = new ArrayList<>();

        // Takes all the users who have watched the same types of movies according to genre, actors, and directors.
        // This excludes only movies where we have both rated the movie, but our scores differ by more than 2. Thus,
        // unrated movies are taken into consideration as if they had a valid rating.
        // The current "closeness" limit is set as an arbitrary > 3 check to ensure we have results to show since data
        // is randomized. In an actual use case, the limit of 50 users would ensure that only the truly closest users
        // are considered as similar.
        String query = "SELECT similar_users.uid " +
                "FROM (" +
                "  SELECT w2.uid, " +
                "    COUNT(DISTINCT CASE WHEN go1.gid = go2.gid THEN go1.gid END) AS genre_matches, " +
                "    COUNT(DISTINCT CASE WHEN a1.pid = a2.pid THEN a1.pid END) AS actor_matches, " +
                "    COUNT(DISTINCT CASE WHEN d1.pid = d2.pid THEN d1.pid END) AS director_matches " +
                "  FROM watches w1" +
                "  JOIN watches w2 ON w2.uid != ?" +
                "  LEFT JOIN rates r1 ON r1.uid = ? AND r1.mid = w1.mid" +
                "  LEFT JOIN rates r2 ON w1.mid = w2.mid AND r1.mid = r2.mid AND r2.uid != ?" +
                "  LEFT JOIN genreof go1 ON w1.mid = go1.mid " +
                "  LEFT JOIN genreof go2 ON w2.mid = go2.mid " +
                "  LEFT JOIN actsin a1 ON w1.mid = a1.mid " +
                "  LEFT JOIN actsin a2 ON w2.mid = a2.mid " +
                "  LEFT JOIN directs d1 ON w1.mid = d1.mid " +
                "  LEFT JOIN directs d2 ON w2.mid = d2.mid " +
                "  WHERE w1.uid = ? AND (r1.rating is NULL OR r2.rating is NULL or abs(r1.rating - r2.rating) <= 2)" +
                "  GROUP BY w2.uid" +
                ") AS similar_users " +
                "WHERE (similar_users.genre_matches + similar_users.actor_matches + similar_users.director_matches) > 3 " +
                "ORDER BY (similar_users.genre_matches + similar_users.actor_matches + similar_users.director_matches) DESC " +
                "LIMIT 50";

        try (PreparedStatement stmt = InitConnection.getCon().prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                similarUsers.add(rs.getInt("uid"));
            }
        }

        return similarUsers;
    }

    /**
     * This method prints the set of movies that are both highly rated (> 3) by another user and unwatched by the
     * currently logged-in user.
     *
     * @param similarUserIds ArrayList<Integer> containing the Ids of similar users.
     * @param loggedInId The int representing the uid of the currently logged-in user.
     */
    private static void getHighlyRatedMovies(ArrayList<Integer> similarUserIds, int loggedInId) throws SQLException {
        // Query simply gets all ratings from the other users for movies not in our watched list to determine the 20
        // most highly rated movies among our peers.
        // Note: Unlike for measuring similarities, we do not look at unrated movies from other users nor do we look at
        // watches. Instead, we only evaluate recommendations based on ratings by other users.
        String query = "SELECT m.mid, m.title, AVG(r.rating) as avg_rating " +
                "FROM movie m JOIN rates r ON m.mid = r.mid " +
                "WHERE r.uid = ANY(?) AND m.mid NOT IN (SELECT DISTINCT mid FROM watches WHERE uid = ?) " +
                "GROUP BY m.mid " +
                "HAVING AVG(r.rating) > ? " +
                "ORDER BY avg_rating DESC " +
                "LIMIT 20";

        try (PreparedStatement stmt = InitConnection.getCon().prepareStatement(query)) {
            // Convert the ArrayList into the jdbc Array type so we can use it in the query.
            // Note: = ANY(?) is used with the jdbc array because IN is not supported on arrays.
            Array sqlArray = InitConnection.getCon().createArrayOf("INTEGER", similarUserIds.toArray());
            stmt.setArray(1, sqlArray);
            stmt.setInt(2, loggedInId);
            stmt.setDouble(3, 3);
            ResultSet rs = stmt.executeQuery();
            if (rs.isBeforeFirst()) {
                System.out.println("Recommended Movies:");
                System.out.println(String.format("%-30s", "Title") + " | " + String.format("%-5s", "MID")  +
                        " | Average Rating");
                while (rs.next()) {
                    String title = rs.getString("title");
                    int mid = rs.getInt("mid");
                    double rating = rs.getDouble("avg_rating");
                    System.out.println(String.format("%-30s", title).substring(0, 30) + " | " +
                            String.format("%-5s", mid) + " | " + String.format("%-14s", rating));
                }
                System.out.println("-------");
            }else{
                System.out.println("No recommendations found based on your preferences.");
            }

        }
    }
}