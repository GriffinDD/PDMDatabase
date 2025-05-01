import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * This class contains methods that show a menu to users, allowing them to search, watch, and rate movies.
 *
 * @author Aditya Kumar ak6169
 * @author Donald Tsang dht1455
 * @author Griffin Danner-Doran gtd6864
 * @author Soban Mahmud sm9614
 * @author Veronika Zsenits vmz5751
 */
public class MoviePage {
    private static final Scanner InputReader = new Scanner(System.in);
    private static int UserID;

    /**
     * This method prints out the options of the menu options for the Movie page, allowing users to enter a submenu
     * to search movies, watch a movie, or rate a movie. If a user exits, they are taken back to the main page
     *
     * @param id The int representing the uid of the currently logged-in user.
     */
    public static void MovieMenu(int id) throws SQLException {
        UserID = id;
        while (true) {
            System.out.println("Movie Menu: Please select one of the following options.");
            System.out.println("1. Search ");
            System.out.println("2. Watch [id] - watch the movie with the given ID.");
            System.out.println("3. Rate [id] - rate the movie with the given ID.");
            System.out.println("4. Browse - view recommendations & top movies");
            System.out.println("5. Exit - return to the main menu.");
            String userCommand = InputReader.nextLine().trim();
            if (userCommand.equals("Exit")) {
                return;
            } else if (userCommand.startsWith("Search")) {
                searchMovies();
            } else if (userCommand.startsWith("Watch")) {
                String movieIdString = userCommand.substring(5).trim();
                try {
                    int movieId = Integer.parseInt(movieIdString);
                    WatchMovie(movieId);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for movie ID.");
                }
            } else if (userCommand.startsWith("Rate")) {
                String movieIdString = userCommand.substring(4).trim();
                try {
                    int movieId = Integer.parseInt(movieIdString);
                    // On our service, users can rate movies they have not yet watched.
                    RateMovie(movieId);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide a number for movie ID.");
                }
            } else if (userCommand.startsWith("Browse")) {
                browseMovies();
            } else {
                System.out.println("Please enter a valid command from those provided.");
            }
        }
    }

    /**
     * This method prints out the options for the Browse Movies page. Users can browse movies by trending movies,
     * movies popular among users the user follows, new releases, and personalized recommendations. If a user exits,
     * they are taken back to the movie menu.
     */
    private static void browseMovies() throws SQLException {
        while (true) {
            System.out.println("Browse Movies Menu: Please select one of the following options.");
            System.out.println("1. Trending - View list of most watched 20 movies in the last 90 days.");
            System.out.println("2. Following - View list of most watched 20 movies among users you follow.");
            System.out.println("3. New Releases - View list of most watched 5 new releases of the month.");
            System.out.println("4. Recommendations - Find movies recommended just for you.");
            System.out.println("5. Exit - Return to the Movie Menu. ");

            String userCommand = InputReader.nextLine().trim();

            switch(userCommand){
                case "Trending" -> trending();
                case "Following" -> following();
                case "New Releases" -> newReleases();
                case "Recommendations" -> MovieRecommendationPage.FindMovies(UserID);
                case "Exit" -> {
                    return;
                }
                default -> System.out.println("Please enter a valid command from those provided.");
            }
        }
    }

    /**
     * This method prints out the 20 most-watched movies in the last 90 days (rolling).
     */
    private static void trending() throws SQLException {
        String trendingQuery = "SELECT m.mid, m.title, r.date, m.length, m.mpaa FROM movie m, " +
                "(SELECT mid, count(starttime) AS totalWatches FROM watches " +
                "WHERE starttime >= now() - INTERVAL '90 DAY' GROUP BY mid) AS w, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r " +
                "WHERE w.mid = m.mid AND m.mid = r.mid " +
                "GROUP BY totalWatches, m.mid, r.date " +
                "ORDER BY totalWatches DESC LIMIT 20";
        ResultSet movieSet;
        try (PreparedStatement trendingStmt = InitConnection.getCon().prepareStatement(trendingQuery)) {
            movieSet = trendingStmt.executeQuery();
            if (movieSet.isBeforeFirst()) {
                printSearchResults(movieSet);
            }
        }
    }

    /**
     * This method prints out the 20 most-watched movies among users that the user follows.
     */
    private static void following() throws SQLException{
        String trendingQuery = "SELECT count(w.starttime) as totalwatches, m.mid, m.title, " +
                "r.date, m.length, m.mpaa FROM movie m, " +
                "(SELECT mid, starttime, followeduid FROM watches, follows WHERE followeruid = " + UserID +
                " AND watches.uid = followeduid  GROUP BY followeduid, mid, starttime) AS w, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r " +
                "WHERE w.mid = m.mid AND m.mid = r.mid " +
                "GROUP BY m.mid, r.date ORDER BY totalWatches DESC LIMIT 20";
        ResultSet movieSet;
        try (PreparedStatement followingStmt = InitConnection.getCon().prepareStatement(trendingQuery)) {
            movieSet = followingStmt.executeQuery();
            if (movieSet.isBeforeFirst()) {
                printSearchResults(movieSet);
            }
        }
    }

    /**
     * This method prints out the 5 most-watched movies released this calendar month.
     */
    private static void newReleases() throws SQLException{
        String newReleasesQuery = "SELECT m.mid, m.title, r.date, m.length, m.mpaa FROM movie m, " +
                "(SELECT mid, count(starttime) AS totalWatches FROM watches GROUP BY mid) AS w, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r " +
                "WHERE w.mid = m.mid AND m.mid = r.mid " +
                "AND CAST(r.date AS varchar(7)) = CAST(now() as varchar(7)) " +
                "GROUP BY totalWatches, m.mid, r.date " +
                "ORDER BY totalWatches DESC LIMIT 5";
        ResultSet movieSet;
        try (PreparedStatement newReleasesStmt = InitConnection.getCon().prepareStatement(newReleasesQuery)) {
            movieSet = newReleasesStmt.executeQuery();
            if (movieSet.isBeforeFirst()) {
                printSearchResults(movieSet);
            }
        }
    }

    /**
     * This method prints out the options for the Movie Search page. Users can search by title, release date, cast
     * member name (first and/or last), director name (first and/or last), studio name, or genre. If a user exits,
     * they are taken back to the movie menu.
     */
    private static void searchMovies() throws SQLException {
        while (true) {
            System.out.println("Movie Search Menu: Please select one of the following options.");
            System.out.println("1. Title ");
            System.out.println("2. Release Date ");
            System.out.println("3. Cast Member ");
            System.out.println("4. Director ");
            System.out.println("5. Studio ");
            System.out.println("6. Genre ");
            System.out.println("7. Exit - Return to the Movie Menu.");

            String userCommand = InputReader.nextLine().trim();

            switch (userCommand) {
                case "Exit" -> {
                    return;
                }
                case "Title" -> searchTitle();
                case "Release Date" -> searchReleaseDate();
                case "Cast Member" -> searchCastMember();
                case "Director" -> searchDirector();
                case "Studio" -> searchStudio();
                case "Genre" -> searchGenre();
            }
        }
    }

    /**
     * This method prompts the user for a title to search by, and then queries database for movies whose titles contain
     * the provided string.
     */
    private static void searchTitle() throws SQLException {
        System.out.println("Please enter the title of the movie.");
        String titleSearch = InputReader.nextLine().trim();

        String titleQuery = "SELECT m.title, m.mid, r.date, m.length, m.mpaa, g.gname, s.sname FROM movie m, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r, " +
                "(SELECT mid, MIN(name) AS gname FROM genreof go, genre gr WHERE go.gid = gr.gid GROUP BY mid) AS g, " +
                "(SELECT mid, MIN(name) AS sname FROM produces pr, studio st WHERE st.sid = pr.sid GROUP BY mid) AS s " +
                "WHERE m.mid = r.mid AND m.mid = g.mid AND m.mid = s.mid AND title LIKE '%" + titleSearch + "%' " +
                "GROUP BY m.mid, r.date, s.sname, g.gname";

        ResultSet movieSet;
        try (PreparedStatement titleStmt = InitConnection.getCon().prepareStatement(titleQuery +
                " ORDER BY title, date")) {
            movieSet = titleStmt.executeQuery();

            if (!movieSet.isBeforeFirst()) {
                System.out.println("No movies with title including " + titleSearch + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(titleQuery);
    }

    /**
     * This method prompts the user for a date to search by, allowing users to enter a year, year + month, or exact
     * year + month + day release date. It should be noted that the release date of a movie is considered to be its
     * earliest release on any platform in order to emulate the way platforms usually display release date (i.e. true
     * release date is when it first came out, not when it was added to the platform). As a result, if a movie was
     * released in 2020 on one platform and then in 2024 on another, for the purposes of searching its release year
     * would be 2020.
     */
    private static void searchReleaseDate() throws SQLException {
        System.out.println("Please enter the release date of the movie (YYYY-MM-DD)");
        String dateSearch = InputReader.nextLine().trim();
        if (!dateSearch.matches("^\\d{4}-\\d{2}-\\d{2}$") && !dateSearch.matches("^\\d{4}-\\d{2}$") &&
                !dateSearch.matches("^\\d{4}$")) {
            searchReleaseDate();
            return;
        }
        StringBuilder dateQuery = new StringBuilder();
        dateQuery.append("SELECT m.title, m.mid, m.length, m.mpaa, g.gname, s.sname, " +
                "min(date(r.releasedate)) as date FROM movie m, releasedon r," +
                "(SELECT mid, MIN(name) AS gname FROM genreof go, genre gr WHERE go.gid = gr.gid GROUP BY mid) AS g, " +
                "(SELECT mid, MIN(name) AS sname FROM produces pr, studio st WHERE st.sid = pr.sid GROUP BY mid) AS s " +
                "WHERE r.mid = m.mid AND m.mid = g.mid AND m.mid = s.mid " +
                "GROUP BY m.mid, s.sname, g.gname HAVING ");
        if(dateSearch.matches("^\\d{4}-\\d{2}-\\d{2}$")){
            dateQuery.append("MIN(DATE(r.releasedate)) = '" + dateSearch + "'");
        }
        else if(dateSearch.matches("^\\d{4}$")){
            dateQuery.append("CAST(MIN(DATE(r.releasedate)) AS varchar(4)) = '" + dateSearch.substring(0,4) + "'");
        }
        else {
            dateQuery.append("CAST(MIN(DATE(r.releasedate)) AS varchar(7)) = '" + dateSearch.substring(0,7) + "'");
        }
        ResultSet movieSet;
        try (PreparedStatement dateStmt = InitConnection.getCon().prepareStatement(dateQuery + " ORDER BY title")) {
            movieSet = dateStmt.executeQuery();

            if (!movieSet.isBeforeFirst()) {
                System.out.println("No movies with release date " + dateSearch + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(dateQuery.toString());
    }

    /**
     * This method prompts the user for first and/or last name to search by, then queries the database for movies with a
     * cast member with that name. While a bit clunky, to avoid searching a first or last name, enter nothing in the
     * parentheses, for example First(Robert) Last().
     */
    private static void searchCastMember() throws SQLException {
        // Allow searching by first and/or last, where at least one is required
        System.out.println("Please enter name of the cast member in the format: " +
                "First([first name] [middle name/initial]) Last([last name] [suffix])");
        String actorSearch = InputReader.nextLine().trim();
        if (!actorSearch.startsWith("First(") || !actorSearch.substring(6).contains(") Last(") ||
                !actorSearch.endsWith(")")) {
            searchCastMember();
            return;
        }
        String firstName = actorSearch.substring(6, actorSearch.indexOf("Last(") - 2).trim();
        String lastName = actorSearch.substring(actorSearch.indexOf("Last(") + 5, actorSearch.length() - 1).trim();
        if (firstName.isBlank() && lastName.isBlank()) {
            searchCastMember();
            return;
        }

        StringBuilder nameQuery = new StringBuilder();
        nameQuery.append("SELECT DISTINCT m.title, m.mid, r.date, g.gname, s.sname, m.length, m.mpaa " +
                "FROM movie m, person p, actsin a, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r, " +
                "(SELECT mid, MIN(name) AS gname FROM genreof go, genre gr WHERE go.gid = gr.gid GROUP BY mid) AS g, " +
                "(SELECT mid, MIN(name) AS sname FROM produces pr, studio st WHERE st.sid = pr.sid GROUP BY mid) AS s " +
                "WHERE m.mid = r.mid AND p.pid = a.pid AND a.mid = m.mid AND m.mid = g.mid AND m.mid = s.mid AND ");
        if (lastName.isBlank()) {
            // If first name given
            nameQuery.append("fname LIKE '%").append(firstName).append("%'");
        } else if (firstName.isBlank()) {
            // If last name given
            nameQuery.append("lname LIKE '%" + lastName + "%'");
        } else {
            // If both given
            nameQuery.append("fname LIKE '%" + firstName + "%' AND lname like '%" + lastName + "%'");
        }
        nameQuery.append(" GROUP BY m.mid, r.date, p.pid, s.sname, g.gname");

        ResultSet movieSet;
        try (Statement nameStmt = InitConnection.getCon().createStatement()) {
            movieSet = nameStmt.executeQuery(nameQuery + " ORDER BY title, date");

            if (!movieSet.isBeforeFirst()) {
                String name = (firstName + " " + lastName).trim();
                System.out.println("No movies with cast member " + name + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(nameQuery.toString());
    }

    /**
     * This method prompts the user for first and/or last name to search by, and then queries database for movies with
     * a director with that name. Uses the same format as cast member searching.
     */
    private static void searchDirector() throws SQLException {
        // Allow searching by first and/or last, where at least one is required
        System.out.println("Please enter name of the director in the format: " +
                "First([first name] [middle name/initial]) Last([last name] [suffix])");
        String directorSearch = InputReader.nextLine().trim();
        if (!directorSearch.startsWith("First(") || !directorSearch.substring(6).contains(") Last(") ||
                !directorSearch.endsWith(")")) {
            searchDirector();
            return;
        }
        String firstName = directorSearch.substring(6, directorSearch.indexOf("Last(") - 2).trim();
        String lastName = directorSearch.substring(directorSearch.indexOf("Last(") + 5, directorSearch.length() - 1).trim();
        if (firstName.isBlank() && lastName.isBlank()) {
            searchDirector();
            return;
        }
        StringBuilder nameQuery = new StringBuilder();
        nameQuery.append("SELECT DISTINCT m.title, m.mid, r.date,  g.gname, s.sname, m.length, m.mpaa " +
                "FROM movie m, person p, directs d, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r, " +
                "(SELECT mid, MIN(name) AS gname FROM genreof go, genre gr WHERE go.gid = gr.gid GROUP BY mid) AS g, " +
                "(SELECT mid, MIN(name) AS sname FROM produces pr, studio st WHERE st.sid = pr.sid GROUP BY mid) AS s " +
                "WHERE m.mid = r.mid AND p.pid = d.pid AND d.mid = m.mid AND m.mid = g.mid AND m.mid = s.mid AND ");
        if (lastName.isBlank()) {
            // If first name given
            nameQuery.append("fname LIKE '%" + firstName + "%'");
        } else if (firstName.isBlank()) {
            // If last name given
            nameQuery.append("lname LIKE '%" + lastName + "%'");
        } else {
            // If both given
            nameQuery.append("fname LIKE '%" + firstName + "%' AND lname like '%" + lastName + "%'");
        }
        nameQuery.append(" GROUP BY m.mid, r.date, p.pid, g.gname, s.sname");

        ResultSet movieSet;
        try (Statement nameStmt = InitConnection.getCon().createStatement()) {
            movieSet = nameStmt.executeQuery(nameQuery + " ORDER BY title, date");

            if (!movieSet.isBeforeFirst()) {
                String name = (firstName + " " + lastName).trim();
                System.out.println("No movies with director " + name + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(nameQuery.toString());
    }

    /**
     * This method prompts the user for a studio name to search by, and then queries database for movies produced by a
     * studio containing that name.
     */
    private static void searchStudio() throws SQLException {
        System.out.println("Please enter the name of the studio.");
        String studioSearch = InputReader.nextLine().trim();

        String studioQuery = "SELECT DISTINCT m.title, m.mid, r.date, g.gname, s.name AS sname, m.length, m.mpaa " +
                "FROM movie m, studio s, produces p, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) AS r, " +
                "(SELECT mid, MIN(name) AS gname FROM genreof go, genre gr WHERE go.gid = gr.gid GROUP BY mid) AS g " +
                "WHERE m.mid = r.mid AND m.mid = p.mid AND s.sid = p.sid AND m.mid = g.mid " +
                "AND s.name LIKE '%" + studioSearch + "%' GROUP BY m.mid, r.date, s.name, g.gname ";
        ResultSet movieSet;
        try (PreparedStatement studioStmt = InitConnection.getCon().prepareStatement(studioQuery +
                " ORDER BY title, date")) {
            movieSet = studioStmt.executeQuery();
            if (!movieSet.isBeforeFirst()) {
                System.out.println("No movies produced by studio " + studioSearch + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(studioQuery);
    }

    /**
     * This method prompts the user for a genre name to search by, and then queries database for movies with that genre.
     */
    private static void searchGenre() throws SQLException {
        System.out.println("Please enter the genre of the movie.");
        String genreSearch = InputReader.nextLine().trim();
        String titleQuery = "SELECT DISTINCT m.title, m.mid, r.date, g.name AS gname, s.sname, m.length, m.mpaa " +
                "FROM movie m, genre g, genreof o, " +
                "(SELECT mid, MIN(releasedate) AS date FROM releasedon GROUP BY mid) r, " +
                "(SELECT mid, MIN(name) AS sname FROM produces pr, studio st WHERE st.sid = pr.sid GROUP BY mid) AS s " +
                "WHERE m.mid = r.mid AND m.mid = o.mid AND g.gid = o.gid AND m.mid = s.mid AND " +
                "g.name = '" + genreSearch + "' " +
                "GROUP BY m.mid, r.date, g.name, s.sname";

        ResultSet movieSet;
        try (PreparedStatement genreStmt = InitConnection.getCon().prepareStatement(titleQuery
                + " ORDER BY title, date")) {
            movieSet = genreStmt.executeQuery();

            if (!movieSet.isBeforeFirst()) {
                System.out.println("No movies with genre " + genreSearch + " found.");
                System.out.println("---");
                return;
            }
            printSearchResults(movieSet);
        }
        filterSearch(titleQuery);
    }

    /**
     * This method prints out options for filtering the search results. Users can sort results by title, release year,
     * genre, or studio; all ascending or descending. The method re-queries the database so the same movies are
     * displayed, now ordered by the specified attribute.
     *
     * @param baseQuery The query that retrieved relevant movies, without an 'order by' component.
     */
    private static void filterSearch(String baseQuery) throws SQLException {
        String userCommand;
        while (true) {
            System.out.println("Please select one of the following options to sort your search results.");
            System.out.println("Each option must be marked (Ascending) or (Descending).");
            System.out.println("1. Title");
            System.out.println("2. Release Year");
            System.out.println("4. Genre");
            System.out.println("5. Studio");
            System.out.println("6. Exit - Return to the Movie Search Menu.");
            userCommand = InputReader.nextLine().trim();
            String ordering;
            switch (userCommand) {
                case "Exit" -> {
                    return;
                }
                case "Title (Ascending)" -> {
                    ordering = " ORDER BY title ASC";
                }
                case "Title (Descending)" -> {
                    ordering = " ORDER BY title DESC";
                }
                case "Release Year (Ascending)" -> {
                    ordering = " ORDER BY date ASC";
                }
                case "Release Year (Descending)" -> {
                    ordering = " ORDER BY date DESC";
                }
                case "Genre (Ascending)" -> {
                    ordering = " ORDER BY gname ASC";
                }
                case "Genre (Descending)" -> {
                    ordering = " ORDER BY gname DESC";
                }
                case "Studio (Ascending)" -> {
                    ordering = " ORDER BY sname ASC";
                }
                case "Studio (Descending)" -> {
                    ordering = " ORDER BY sname DESC";
                }
                default -> {
                    System.out.println("Please choose a valid option.");
                    continue;
                }
            }
            ResultSet movieSet;
            try (PreparedStatement query = InitConnection.getCon().prepareStatement(baseQuery + ordering)) {
                movieSet = query.executeQuery();
                printSearchResults(movieSet);
            }
        }
    }

    /**
     * This method prints the search results, organized in columns by title, movie ID, release year, length,
     * MPAA rating, average user rating, genre(s), director(s), studio(s), and cast member(s).
     *
     * @param movieSet  The result set from the performed query.
     */
    private static void printSearchResults(ResultSet movieSet) throws SQLException {
        System.out.println("Search Results:");
        System.out.println(String.format("%-30s", "Title") + " | " + String.format("%-5s", "MID")  +
                " | Year  | Length  | MPAA Rating  | Average User Rating  | " +
                String.format("%-35s", "Genre(s)") + " | " + String.format("%-30s", "Director(s)") +
                " | " + String.format("%-35s", "Studio(s)") + " | Cast");
        while (movieSet.next()) {
            StringBuilder output = new StringBuilder();
            String title = movieSet.getString("title");
            int mid = movieSet.getInt("mid");
            java.sql.Timestamp date = movieSet.getTimestamp("date");
            int length = movieSet.getInt("length");
            int hours = length / 60;
            int minutes = length % 60;
            String mpaa = movieSet.getString("mpaa");
            output.append(String.format("%-30s", title).substring(0, 30) + " | " + String.format("%-5s", mid) + " | "
                    + date.toString().substring(0, 4) + "  | " +
                    String.format("%-7s", hours + ":" + (minutes < 10 ? "0" + minutes : minutes)) + " | "
                    + String.format("%-11s", mpaa) + "  | ");

            String rateQuery = "SELECT AVG(rating) as avg FROM rates r WHERE mid = " + mid;
            ResultSet rateSet;
            try (PreparedStatement rateStmt = InitConnection.getCon().prepareStatement(rateQuery)) {
                rateSet = rateStmt.executeQuery();
                if (rateSet.isBeforeFirst()) {
                    rateSet.next();
                    double avgRating = rateSet.getDouble("avg");

                    if (avgRating == 0.0) {
                        output.append(String.format("%-20s", "No ratings") + " | ");
                    } else {
                        output.append(String.format("%-20s", (double)Math.round(avgRating * 10) / 10) + " | ");
                    }
                }
            }

            String genreQuery = "SELECT name FROM genre g, genreof o " +
                    "WHERE o.mid = " + mid + " AND g.gid = o.gid ORDER BY name";
            ResultSet genreSet;
            StringBuilder genres = new StringBuilder();
            try (PreparedStatement genreStmt = InitConnection.getCon().prepareStatement(genreQuery)) {
                genreSet = genreStmt.executeQuery();
                if (genreSet.isBeforeFirst()) {
                    genreSet.next();
                    genres.append(genreSet.getString("name"));
                    while (genreSet.next()) {
                        genres.append(", " + genreSet.getString("name"));
                    }
                }
            }
            output.append(String.format("%-35s", genres).substring(0,35) + " | ");

            String directorQuery = "SELECT fname, lname FROM person p, directs d " +
                    "WHERE d.mid = " + mid + " AND d.pid = p.pid ORDER BY fname, lname";
            ResultSet directorSet;
            StringBuilder directors = new StringBuilder();
            try (PreparedStatement directorStmt = InitConnection.getCon().prepareStatement(directorQuery)) {
                directorSet = directorStmt.executeQuery();
                if (directorSet.isBeforeFirst()) {
                    directorSet.next();
                    directors.append(directorSet.getString("fname"));
                    if (directorSet.getObject("lname") != null) {
                        directors.append(" " + directorSet.getString("lname"));
                    }
                    while (directorSet.next()) {
                        directors.append(", " + directorSet.getString("fname"));
                        if (directorSet.getObject("lname") != null) {
                            directors.append(" " + directorSet.getString("lname"));
                        }
                    }
                }
            }
            output.append(String.format("%-30s", directors).substring(0, 30) + " | ");

            String studioQuery = "SELECT name FROM studio s, produces p " +
                    "WHERE p.mid = " + mid + " AND p.sid = s.sid ORDER BY name";
            ResultSet studioSet;
            StringBuilder studios = new StringBuilder();
            try (PreparedStatement studioStmt = InitConnection.getCon().prepareStatement(studioQuery)) {
                studioSet = studioStmt.executeQuery();
                if (studioSet.isBeforeFirst()) {
                    studioSet.next();
                    studios.append(studioSet.getString("name"));
                    while (studioSet.next()) {
                        studios.append(", " + studioSet.getString("name"));
                    }
                }
            }
            output.append(String.format("%-35s", studios).substring(0, 35) + " | ");

            String castQuery = "SELECT fname, lname FROM person p, actsin a " +
                    "WHERE a.mid = " + mid + " AND a.pid = p.pid ORDER BY fname, lname";
            ResultSet castSet;
            try (PreparedStatement castStmt = InitConnection.getCon().prepareStatement(castQuery)) {
                castSet = castStmt.executeQuery();
                if (castSet.isBeforeFirst()) {
                    castSet.next();
                    output.append(castSet.getString("fname"));
                    if (castSet.getObject("lname") != null) {
                        output.append(" " + castSet.getString("lname"));
                    }
                    while (castSet.next()) {
                        output.append(", " + castSet.getString("fname"));
                        if (castSet.getObject("lname") != null) {
                            output.append(" " + castSet.getString("lname"));
                        }
                    }
                }
            }

            System.out.println(output);
        }
        System.out.println("-------");
    }

    /**
     * This method prompts the user to enter how much of the given movie they want to watch, and then logs a watch
     * session between the time the command was run and that time + the provided length.
     *
     * @param movieId The integer representing the ID of the movie that the user wants to watch.
     */
    private static void WatchMovie(int movieId) throws SQLException {
        ResultSet lengthSet;
        int length;
        try (Statement lengthStmt = InitConnection.getCon().createStatement()) {
            String lengthQuery = "SELECT length FROM movie WHERE mid = " + movieId;
            lengthSet = lengthStmt.executeQuery(lengthQuery);
            if (!lengthSet.next()) {
                System.out.println("There is no movie matching this ID, please select a valid movie id.");
                return;
            }
            length = lengthSet.getInt("length");
        }
        System.out.println("Please enter how many minutes out of " + length + " total you want to watch or " +
                "Exit to return to the movie menu.");
        String userLength;
        int watchLength;
        while (true) {
            userLength = InputReader.nextLine().trim();
            if (userLength.equals("Exit")) {
                return;
            }

            try {
                watchLength = Integer.parseInt(userLength);
            } catch (NumberFormatException e) {
                System.out.println("Please provide a number for movie ID.");
                continue;
            }

            // Here, we only consider watching a movie to occur for at least 1 minute.
            if (watchLength <= 0 || watchLength > length) {
                System.out.println("Please only enter a movie watch time between 1 and the total length " + length + ".");
                continue;
            }

            try (PreparedStatement watchStmt = InitConnection.getCon().prepareStatement(
                    "INSERT INTO watches(mid, uid, starttime, endtime) VALUES(?, ?, NOW(), NOW() + CAST(? " +
                            "AS interval))")) {
                watchStmt.setInt(1, movieId);
                watchStmt.setInt(2, UserID);
                watchStmt.setString(3, watchLength + " minutes");
                watchStmt.executeUpdate();
            }
            return;
        }
    }

    /**
     * This method allows the user to rate a movie 1-5 stars, and then records the rating in the database.
     *
     * @param movieId The integer representing the id of the movie the user wants to rate.
     */
    private static void RateMovie(int movieId) throws SQLException {
        ResultSet ratingSet;
        boolean hasRating = false;
        try (Statement currentRatingStmt = InitConnection.getCon().createStatement()) {
            String ratingQuery = "SELECT rating FROM rates WHERE mid = " + movieId + " AND uid = " + UserID;
            ratingSet = currentRatingStmt.executeQuery(ratingQuery);
            if (!ratingSet.next()) {
                System.out.println("You have not yet rated this movie, enter Rating [1-5] to rate the movie or " +
                        "Exit to return to the movie menu.");
            } else {
                System.out.println("You have already rated this movie a " + ratingSet.getInt("rating") +
                        " out of 5, enter Rating [1-5] to re-rate the movie or Exit to return to the movie menu.");
                hasRating = true;
            }
        }

        String userCommand;
        String userRating;
        int movieRating;
        while (true) {
            userCommand = InputReader.nextLine().trim();
            if (userCommand.equals("Exit")) {
                return;
            } else if (!userCommand.startsWith("Rating")) {
                System.out.println("Please enter Rating [1-5] or Exit.");
                continue;
            }

            userRating = userCommand.substring(6).trim();

            try {
                movieRating = Integer.parseInt(userRating);
            } catch (NumberFormatException e) {
                System.out.println("Please provide a number for your rating.");
                continue;
            }

            String rateQuery = hasRating ? "UPDATE rates SET rating = ? WHERE mid = ? AND uid = ?" : "INSERT INTO " +
                    "rates(rating, mid, uid) VALUES(?, ?, ?)";
            // Do update if we already have a rating and insert if not.
            try (PreparedStatement rateStmt = InitConnection.getCon().prepareStatement(rateQuery)) {
                rateStmt.setInt(1, movieRating);
                rateStmt.setInt(2, movieId);
                rateStmt.setInt(3, UserID);
                // As with our other added constraints, let the DB constraints handle bouncing bad requests.
                try {
                    rateStmt.executeUpdate();
                    System.out.println("You have successfully rated movie " + movieId + "! Enter Rating [1-5] to " +
                            "re-rate the movie or Exit to return to the movie menu.");
                    hasRating = true;
                } catch (SQLException e) {
                    String errorMessage = e.getMessage();
                    if (errorMessage.contains("check constraint")) {    // Corresponds to SQLState 23514
                        System.out.println("Please rate the movie 1-5 stars.");
                    } else {
                        // If it is not the check constraint error we expect, throw it.
                        throw e;
                    }
                }
            }
        }
    }
}
