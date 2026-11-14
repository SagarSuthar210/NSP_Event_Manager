import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * MySQL-backed storage for CollegeEvent data.
 * Drop-in replacement for FileStorageManager: same method signatures
 * (loadEvents / saveEvents), so EventManagerApp only needs its two
 * call sites updated to point here instead.
 */
public class DatabaseStorageManager {

    /**
     * DriverShim allows a JDBC driver loaded in a child ClassLoader (e.g. dynamically
     * loaded JAR) to be accepted by java.sql.DriverManager.
     */
    private static class DriverShim implements Driver {
        private final Driver driver;
        public DriverShim(Driver d) { this.driver = d; }
        @Override public boolean acceptsURL(String u) throws SQLException { return driver.acceptsURL(u); }
        @Override public Connection connect(String u, Properties p) throws SQLException { return driver.connect(u, p); }
        @Override public int getMajorVersion() { return driver.getMajorVersion(); }
        @Override public int getMinorVersion() { return driver.getMinorVersion(); }
        @Override public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException { return driver.getPropertyInfo(u, p); }
        @Override public boolean jdbcCompliant() { return driver.jdbcCompliant(); }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return driver.getParentLogger(); }
    }

    private static boolean driverRegistered = false;

    /**
     * Ensures MySQL JDBC driver is registered, even if the JAR was omitted from the launcher classpath.
     */
    private static synchronized void ensureDriverLoaded() {
        if (driverRegistered) return;

        // 1. Try standard classpath loading
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverRegistered = true;
            return;
        } catch (ClassNotFoundException ignored) {
        }

        // 2. If not on classpath, search for the connector JAR locally
        File[] candidates = new File[] {
            new File("mysql-connector-j-26.7.0.jar"),
            new File("lib/mysql-connector-j-26.7.0.jar")
        };

        File jarFile = null;
        for (File candidate : candidates) {
            if (candidate.exists()) {
                jarFile = candidate;
                break;
            }
        }

        if (jarFile == null) {
            File currentDir = new File(".");
            File[] matches = currentDir.listFiles((dir, name) -> name.startsWith("mysql-connector") && name.endsWith(".jar"));
            if (matches != null && matches.length > 0) {
                jarFile = matches[0];
            }
        }

        if (jarFile != null && jarFile.exists()) {
            try {
                URLClassLoader loader = new URLClassLoader(
                    new URL[]{ jarFile.toURI().toURL() },
                    DatabaseStorageManager.class.getClassLoader()
                );
                Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, loader)
                                             .getDeclaredConstructor()
                                             .newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
                driverRegistered = true;
                System.out.println("Loaded MySQL driver dynamically from " + jarFile.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Warning: could not dynamically load MySQL driver from jar: " + ex.getMessage());
            }
        }
    }

    static {
        ensureDriverLoaded();
    }

    // ---- Database connection configuration ----
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/piet_events?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "sqlSagar3690";
    private static final String DB_FALLBACK_USER = "piet_app";
    // -------------------------------------------

    private static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException ex) {
            if (DB_FALLBACK_USER != null && !DB_FALLBACK_USER.equalsIgnoreCase(DB_USER)) {
                try {
                    return DriverManager.getConnection(DB_URL, DB_FALLBACK_USER, DB_PASSWORD);
                } catch (SQLException ignored) {
                }
            }
            throw ex;
        }
    }

    /**
     * Ensures the events table exists so the application can run out-of-the-box.
     */
    private static void ensureTableExists(Connection conn) {
        String createSql = "CREATE TABLE IF NOT EXISTS events (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "event_date DATE NOT NULL, " +
                "slot VARCHAR(20) NOT NULL, " +
                "venue VARCHAR(30) NOT NULL, " +
                "category VARCHAR(20) NOT NULL, " +
                "organizer VARCHAR(255) NOT NULL DEFAULT 'Not specified', " +
                "description TEXT, " +
                "participants INT NOT NULL DEFAULT 0, " +
                "form_url VARCHAR(500) DEFAULT NULL, " +
                "UNIQUE KEY uniq_venue_date_slot (event_date, venue, slot)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSql);
        } catch (SQLException ignored) {
        }
    }

    /** Loads all saved events from the database. Returns an empty list if the table is empty. */
    public static List<EventManagerApp.CollegeEvent> loadEvents() throws SQLException {
        List<EventManagerApp.CollegeEvent> result = new ArrayList<>();
        String sql = "SELECT id, name, event_date, slot, venue, category, organizer, description, " +
                     "participants, form_url FROM events ORDER BY id";

        try (Connection conn = getConnection()) {
            ensureTableExists(conn);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    try {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        LocalDate date = rs.getDate("event_date").toLocalDate();
                        EventManagerApp.TimeSlot slot = EventManagerApp.TimeSlot.valueOf(rs.getString("slot"));
                        EventManagerApp.Venue venue = EventManagerApp.Venue.valueOf(rs.getString("venue"));
                        EventManagerApp.Category category = EventManagerApp.Category.valueOf(rs.getString("category"));
                        String organizer = rs.getString("organizer");
                        String description = rs.getString("description");
                        int participants = rs.getInt("participants");
                        String formUrl = rs.getString("form_url");

                        result.add(new EventManagerApp.CollegeEvent(
                                id, name, date, slot, venue, category, organizer, description,
                                participants, formUrl));
                    } catch (Exception rowEx) {
                        // Skip any row that fails to map instead of losing the whole result set.
                    }
                }
            }
        }
        return result;
    }

    /**
     * Replaces the full contents of the events table with the current in-memory list.
     * Mirrors FileStorageManager.saveEvents(), which is called after every add/delete.
     * Runs as a single transaction so a failure mid-way doesn't leave the table half-written.
     */
    public static void saveEvents(List<EventManagerApp.CollegeEvent> events) throws SQLException {
        String deleteAll = "DELETE FROM events";
        String insert = "INSERT INTO events " +
                "(id, name, event_date, slot, venue, category, organizer, description, participants, form_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            ensureTableExists(conn);
            conn.setAutoCommit(false);
            try (Statement del = conn.createStatement();
                 PreparedStatement ps = conn.prepareStatement(insert)) {

                del.executeUpdate(deleteAll);

                for (EventManagerApp.CollegeEvent ev : events) {
                    ps.setInt(1, ev.id);
                    ps.setString(2, ev.name);
                    ps.setDate(3, Date.valueOf(ev.date));
                    ps.setString(4, ev.slot.name());
                    ps.setString(5, ev.venue.name());
                    ps.setString(6, ev.category.name());
                    ps.setString(7, ev.organizer);
                    ps.setString(8, ev.description);
                    ps.setInt(9, ev.participants);
                    if (ev.formUrl == null || ev.formUrl.isEmpty()) {
                        ps.setNull(10, Types.VARCHAR);
                    } else {
                        ps.setString(10, ev.formUrl);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
