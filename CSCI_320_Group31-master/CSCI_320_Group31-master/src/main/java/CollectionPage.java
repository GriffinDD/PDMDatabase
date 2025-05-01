import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * This class contains methods that show a menu to users, allowing them to modify, create, or delete collections as well
 * as watch all the movies in a collection.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class CollectionPage {
    private static final Scanner InputReader = new Scanner(System.in);
    private static int UserID;

    /**
     * This method displays the options for the collections menu, allowing users to create, review, and watch
     * collections. Users can also exit to return to the main menu.
     *
     * @param id The int representing the uid of the currently logged-in user.
     */
    public static void CollectionMenu(int id) throws SQLException {
        UserID = id;

        while (true) {
            System.out.println("Please select one of the following options: ");
            System.out.println("1. Create Collection.");
            System.out.println("2. Add [id] - Allows user to start adding movies to the collection with given ID.");
            System.out.println("3. Remove [id] - Allows user to start removing movies from the collection with given ID.");
            System.out.println("4. Show Collections.");
            System.out.println("5. Delete [id] - Deletes collection with given ID.");
            System.out.println("6. Rename [id] - Renames the collection with given ID.");
            System.out.println("7. Watch [id] - Watches the collection with given ID.");
            System.out.println("8. Exit");

            String userCommand = InputReader.nextLine().trim();

            if (userCommand.equals("Exit")) {
                return;
            } else if (userCommand.startsWith("Create")) {
                createCollection();
            } else if (userCommand.startsWith("Add")) {
                String collectionIDString = userCommand.substring(3).trim();
                try {
                    int collectionID = Integer.parseInt(collectionIDString);
                    addMovie(collectionID);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for collection ID.");
                }

            } else if (userCommand.startsWith("Remove")) {
                String collectionIDString = userCommand.substring(6).trim();
                try {
                    int collectionID = Integer.parseInt(collectionIDString);
                    removeMovie(collectionID);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for collection ID.");
                }

            } else if (userCommand.startsWith("Show")) {
                showCollections();

            } else if (userCommand.startsWith("Delete")) {
                String collectionIDString = userCommand.substring(6).trim();
                try {
                    int collectionID = Integer.parseInt(collectionIDString);
                    deleteCollection(collectionID);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for collection ID.");
                }
            } else if (userCommand.startsWith("Rename")) {
                String collectionIDString = userCommand.substring(6).trim();
                try {
                    int collectionID = Integer.parseInt(collectionIDString);
                    renameCollection(collectionID);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for collection ID.");
                }
            } else if (userCommand.startsWith("Watch")) {
                String collectionIDString = userCommand.substring(5).trim();
                try {
                    int collectionID = Integer.parseInt(collectionIDString);
                    watchCollection(collectionID);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for collection ID.");
                }
            } else {
                System.out.println("Please enter a valid command from those provided.");
            }
        }
    }


    /**
     * This method prompts users to create a new collection by providing a name and initial movie ID.
     */
    private static void createCollection() throws SQLException {
        System.out.println("Please enter the name of the collection: ");
        String collectionName = InputReader.nextLine().trim();

        System.out.println("Please enter a starting movie ID for this collection or Exit to leave the menu: ");
        while (true) {
            String movieIDString = InputReader.nextLine().trim();
            if (movieIDString.equals("Exit")) {
                return;
            }

            int movieID;
            try {
                movieID = Integer.parseInt(movieIDString);
            } catch (NumberFormatException e) {
                System.out.println("Please provide a number for movie ID.");
                continue;
            }

            if (!checkMovieID(movieID)) {
                System.out.println("Movie ID does not exist.");
                continue;
            }

            // Creates the collection
            try (PreparedStatement createCollectionStmt = InitConnection.getCon().prepareStatement(
                    "INSERT INTO collection (name, creatoruid) VALUES (?, ?);",
                    Statement.RETURN_GENERATED_KEYS)) {
                createCollectionStmt.setString(1, collectionName);
                createCollectionStmt.setInt(2, UserID);
                createCollectionStmt.executeUpdate();

                // Gets the cid from the new collection
                try (ResultSet rs = createCollectionStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int cID = rs.getInt(1);

                        // Adds the collection cid and mid to partof relation
                        try (PreparedStatement partOfStmt = InitConnection.getCon().prepareStatement(
                                "INSERT INTO partof (cid, mid) VALUES (?, ?);")) {
                            partOfStmt.setInt(1, cID);
                            partOfStmt.setInt(2, movieID);
                            partOfStmt.executeUpdate();
                            System.out.println("Collection successfully created!");
                        }
                    }
                }
            }
            return;
        }
    }

    /**
     * This method prompts the user to enter movie IDs to add to the provided collection.
     *
     * @param collectionID The cid of the collection to add movies to.
     */
    public static void addMovie(int collectionID) throws SQLException {
        if (!checkCollectionID(collectionID)) {
            System.out.println("Collection with given ID does not exist.");
            return;
        }

        System.out.println("Please enter the ID of a movie to add or Exit to return to the collection menu: ");
        int movieID;
        String movieIDString;
        while (true) {
            movieIDString = InputReader.nextLine().trim();
            if (movieIDString.equals("Exit")) {
                return;
            }

            try {
                movieID = Integer.parseInt(movieIDString);
            } catch (NumberFormatException e) {
                System.out.println("Please provide a number for movie ID.");
                continue;
            }

            if (!checkMovieID(movieID)) {
                System.out.println("Movie with the given ID does not exist.");
                continue;
            }

            // Checks to see if movie already exists in collection
            try (PreparedStatement stmt = InitConnection.getCon().prepareStatement(
                    "select 1 from partof where cid = ? and mid = ?")) {
                stmt.setInt(1, collectionID);
                stmt.setInt(2, movieID);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("This movie is already in the collection.");
                    continue;
                }
            }

            try (PreparedStatement stmt = InitConnection.getCon().prepareStatement(
                    "INSERT INTO partof (cid, mid) VALUES (?, ?);")) {
                stmt.setInt(1, collectionID);
                stmt.setInt(2, movieID);
                stmt.executeUpdate();
                System.out.println("Movie successfully added to the collection.");
            }
        }
    }

    /**
     * This method prompts the user to enter movie IDs to remove from the provided collection.
     *
     * @param collectionID The cid of the collection to remove movies from.
     */
    public static void removeMovie(int collectionID) throws SQLException {
        if (!checkCollectionID(collectionID)) {
            System.out.println("Collection with given ID does not exist.");
            return;
        }

        System.out.println("Please enter the ID of a movie to remove or Exit to return to the collection menu: ");
        int movieID;
        String movieIDString;
        while (true) {
            movieIDString = InputReader.nextLine().trim();
            if (movieIDString.equals("Exit")) {
                return;
            }

            try {
                movieID = Integer.parseInt(movieIDString);
            } catch (NumberFormatException e) {
                System.out.println("Please provide a number for movie ID.");
                continue;
            }

            if (!checkMovieID(movieID)) {
                System.out.println("Movie with the given ID does not exist.");
                continue;
            }

            try (PreparedStatement removeStmt = InitConnection.getCon().prepareStatement(
                    "delete from partof where cid = ? and mid = ?")) {
                removeStmt.setInt(1, collectionID);
                removeStmt.setInt(2, movieID);
                int rowsDeleted = removeStmt.executeUpdate();
                if (rowsDeleted == 1) {
                    System.out.println("You have successfully deleted a movie.");
                } else {
                    System.out.println("The given movie was not in the collection.");
                }
            }

            // Delete collection if it contains no movies.
            try (PreparedStatement checkStmt = InitConnection.getCon().prepareStatement(
                    "select 1 from partof where cid = ?")) {
                checkStmt.setInt(1, collectionID);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    deleteCollection(collectionID);
                    System.out.println("Collection was empty after removing movie and has been deleted.");
                    return;
                }
            }

        }
    }

    /**
     * This method shows all collections associated with the logged-in user, ordered by name. Each collection displays
     * name, movie count and total watch length.
     */
    public static void showCollections() throws SQLException {
        try (PreparedStatement showStmt = InitConnection.getCon().prepareStatement(
                "SELECT c.cid, c.name, COUNT(p.mid) AS numMovies, SUM(m.length) AS totalLength" +
                        " FROM collection c " +
                        "LEFT JOIN partof p ON c.cid = p.cid " +
                        "LEFT JOIN  movie m ON p.mid = m.mid " +
                        "Where c.creatoruid = ? " +
                        "GROUP BY c.cid, c.name " +
                        "ORDER BY c.name;")) {
            showStmt.setInt(1, UserID);
            ResultSet rs = showStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                System.out.println("You have not made any collections yet.");
                return;
            }
            while (rs.next()) {
                int cid = rs.getInt("cid");
                String collectionName = rs.getString("name");
                int numMovies = rs.getInt("numMovies");
                int totalLength = rs.getInt("totalLength");
                int hours = totalLength / 60;
                int minutes = totalLength % 60;

                System.out.println(
                        "Collection ID: " + cid +
                                " | Collection name: " + collectionName +
                                " | Number of movies: " + numMovies +
                                " | Total length: " + hours + ":" + (minutes < 10 ? "0" + minutes : minutes));
                System.out.println();
            }
        }
    }

    /**
     * This method deletes the provided collection.
     *
     * @param collectionID The cid of the collection that's being deleted.
     */
    public static void deleteCollection(int collectionID) throws SQLException {
        if (!checkCollectionID(collectionID)) {
            System.out.println("Collection with given ID does not exist.");
            return;
        }
        try (PreparedStatement deleteStmt = InitConnection.getCon().prepareStatement(
                "delete from collection where cid = ?")) {
            deleteStmt.setInt(1, collectionID);
            deleteStmt.executeUpdate();
            System.out.println("Collection deleted.");
        }
    }

    /**
     * This method lets the user enter a new name for the provided collection.
     *
     * @param collectionID The cid of the collection being renamed.
     */
    public static void renameCollection(int collectionID) throws SQLException {
        System.out.println("Please enter the new name of the collection: ");
        String collectionName = InputReader.nextLine().trim();
        try (PreparedStatement renameStmt = InitConnection.getCon().prepareStatement(
                "UPDATE collection SET name = ? WHERE cid = ?")) {
            renameStmt.setString(1, collectionName);
            renameStmt.setInt(2, collectionID);
            renameStmt.executeUpdate();
            System.out.println("Collection renamed.");
        }
    }

    /**
     * This is a helper method that checks if a movie with the given mid exists.
     *
     * @param movieID mid of the movie being checked.
     * @return true/false depending on whether the movie exists.
     */
    private static boolean checkMovieID(int movieID) throws SQLException {
        try (PreparedStatement movieIDStmt = InitConnection.getCon().prepareStatement(
                "SELECT mid FROM movie WHERE mid = ?")) {
            movieIDStmt.setInt(1, movieID);
            ResultSet rs = movieIDStmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * This is a helper method that checks if a collection with the given cid exists.
     *
     * @param collectionID cid of the collection being checked.
     * @return true/false depending on whether the collection exists.
     */
    private static boolean checkCollectionID(int collectionID) throws SQLException {
        try (PreparedStatement collectionIDStmt = InitConnection.getCon().prepareStatement(
                "SELECT cid FROM collection WHERE cid = ?")) {
            collectionIDStmt.setInt(1, collectionID);
            ResultSet rs = collectionIDStmt.executeQuery();
            return rs.next();
        }
    }

    /**
     * This method watches all movies in the provided collection, ordered by name then length.
     *
     * @param collectionID The cid of the collection being watched.
     */
    private static void watchCollection(int collectionID) throws SQLException {
        // Get all the movies in the collection.
        try (PreparedStatement watchStmt = InitConnection.getCon().prepareStatement(
                "Select m.mid, m.length " +
                        "from partof p JOIN movie m ON p.mid = m.mid " +
                        "where p.cid = ? order by m.title, m.length")) {
            watchStmt.setInt(1, collectionID);

            int sessionTime = 0;
            try (ResultSet rs = watchStmt.executeQuery()) {
                while (rs.next()) {
                    int mid = rs.getInt("mid");
                    int length = rs.getInt("length");

                    try (PreparedStatement insertStmt = InitConnection.getCon().prepareStatement(
                            "INSERT INTO watches (uid, mid, starttime, endtime) " +
                                    "VALUES (?, ?, NOW() + CAST(? AS interval), NOW() + CAST(? AS interval))")) {
                        insertStmt.setInt(1, UserID);
                        insertStmt.setInt(2, mid);
                        insertStmt.setString(3, (sessionTime / 60) + " hours "
                                + (sessionTime % 60) + " minutes");
                        // Increment time indicator by movie length as if we watched the whole movie.
                        sessionTime += length;
                        insertStmt.setString(4, (sessionTime / 60) + " hours "
                                + (sessionTime % 60) + " minutes");
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }
}
