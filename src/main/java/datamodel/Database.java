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

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT * FROM Shows WHERE day = '" + mDate + "' AND movieName = '" + mTitle + "'");

            while (rs.next()) {
                mFreeSeats = rs.getInt("freeSeats");
                mVenue = rs.getString("theaterName");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        return new Show(mTitle, mDate, mVenue, mFreeSeats);
    }

    public boolean login(String uname) {

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT username FROM Users WHERE username = '" + uname + "'");
            if (rs.next()) {
                currName = uname;
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getDates(String m) {
        List<String> dates = new ArrayList<>();

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT day FROM Shows WHERE movieName = '" + m + "'");
            while (rs.next()) {
                dates.add(rs.getString("day"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dates;
    }

    public List<String> getMovies() {
        List<String> movies = new ArrayList<>();

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT DISTINCT(movieName) AS mName FROM Shows");
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

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT theaterName FROM Shows WHERE movieName = '" + movieName + "' " +
                            "AND day = '" + date + "';");
            while (rs.next()) {
                theaterName = rs.getString("theaterName");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (theaterName != null) {
            try {
                conn.createStatement()
                        .executeUpdate("INSERT INTO Reservations(username, movieName, theaterName, day) " +
                                "VALUES ('" + currName + "', '" + movieName + "', '" + theaterName + "', '" + date + "')");
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public List<Reservation> getReservations() {
        List<Reservation> reservations = new ArrayList<>();

        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT * FROM Reservations WHERE username = '" + currName + "'");
            while (rs.next()) {
                reservations.add(new Reservation(rs.getInt("nbr"), rs.getString("movieName"),
                        rs.getString("day"), rs.getString("theaterName")));
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return reservations;
    }
}
