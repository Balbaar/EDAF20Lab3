package datamodel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database is a class that specifies the interface to the 
 * movie database. Uses JDBC and the MySQL Connector/J driver.
 */
public class Database {
    /** 
     * The database connection.
     */
    private Connection conn;
    private String currName;
        
    /**
     * Create the database interface object. Connection to the database
     * is performed later.
     */
    public Database() {
        conn = null;
    }
       
    /* --- TODO: Change this method to fit your choice of DBMS --- */
    /** 
     * Open a connection to the database, using the specified user name
     * and password.
     *
     * @param userName The user name.
     * @param password The user's password.
     * @return true if the connection succeeded, false if the supplied
     * user name and password were not recognized. Returns false also
     * if the JDBC driver isn't found.
     */
    public boolean openConnection(String userName, String password) {
        try {
        	// Connection strings for included DBMS clients:
        	// [MySQL]       jdbc:mysql://[host]/[database]
        	// [PostgreSQL]  jdbc:postgresql://[host]/[database]
        	// [SQLite]      jdbc:sqlite://[filepath]
        	
        	// Use "jdbc:mysql://puccini.cs.lth.se/" + userName if you using our shared server
        	// If outside, this statement will hang until timeout.
            conn = DriverManager.getConnection 
                ("jdbc:mysql://puccini.cs.lth.se/" + userName, userName, password);
        }
        catch (SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            return false;
        }
        return true;
    }
        
    /**
     * Close the connection to the database.
     */
    public void closeConnection() {
        try {
            if (conn != null)
                conn.close();
        }
        catch (SQLException e) {
        	e.printStackTrace();
        }
        conn = null;
        
        System.err.println("Database connection closed.");
    }
        
    /**
     * Check if the connection to the database has been established
     *
     * @return true if the connection has been established
     */
    public boolean isConnected() {
        return conn != null;
    }
	


    /* --- TODO: insert more own code here --- */

public Show getShowData(String mTitle, String mDate) {

    Integer mFreeSeats = 0;
    String mVenue = null;

    String query = "SELECT * FROM Shows WHERE day = ? AND movieName = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setString(1, mDate);
        pstmt.setString(2, mTitle);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                mFreeSeats = rs.getInt("freeSeats");
                mVenue = rs.getString("theaterName");
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return new Show(mTitle, mDate, mVenue, mFreeSeats);
}

    public boolean login(String uname) {
        String query = "SELECT username FROM Users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, uname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currName = uname;
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getDates(String m) {
        List<String> dates = new ArrayList<>();

        String query = "SELECT day FROM Shows WHERE movieName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, m);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getString("day"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dates;
    }

    public List<String> getMovies() {
        List<String> movies = new ArrayList<>();

        String query = "SELECT DISTINCT(movieName) AS mName FROM Shows";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                movies.add(rs.getString("mName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    public boolean bookTicket(String movieName, String date) {
        String theaterName = null;
        boolean success = false;

        try {
            conn.setAutoCommit(false); // Start transaction

            // Check available seats
            String checkSeatsQuery = "SELECT freeSeats FROM Shows WHERE movieName = ? AND day = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSeatsQuery)) {
                pstmt.setString(1, movieName);
                pstmt.setString(2, date);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int freeSeats = rs.getInt("freeSeats");
                        if (freeSeats <= 0) {
                            conn.rollback(); // Rollback transaction if no seats are available
                            return false;
                        }
                    }
                }
            }

            // Get theater name
            String queryTheater = "SELECT theaterName FROM Shows WHERE movieName = ? AND day = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(queryTheater)) {
                pstmt.setString(1, movieName);
                pstmt.setString(2, date);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        theaterName = rs.getString("theaterName");
                    }
                }
            }

            if (theaterName != null) {
                // Insert reservation
                String insertReservation = "INSERT INTO Reservations(username, movieName, theaterName, day) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertReservation)) {
                    pstmt.setString(1, currName);
                    pstmt.setString(2, movieName);
                    pstmt.setString(3, theaterName);
                    pstmt.setString(4, date);
                    pstmt.executeUpdate();
                    success = true;
                }
            }

            //Decrement freeSeats
            String updateShow = "UPDATE Shows SET freeSeats = freeSeats - 1 WHERE movieName = ? AND day = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateShow)) {
                pstmt.setString(1, movieName);
                pstmt.setString(2, date);
                pstmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback transaction on error
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true); // Restore default auto-commit mode
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return success;
    }

    public List<Reservation> getReservations() {
        List<Reservation> reservations = new ArrayList<>();

        String query = "SELECT * FROM Reservations WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, currName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(rs.getInt("nbr"), rs.getString("movieName"),
                            rs.getString("day"), rs.getString("theaterName")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }
}
