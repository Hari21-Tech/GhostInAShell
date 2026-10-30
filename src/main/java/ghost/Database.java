package ghost;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + File.separator + "ghost.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public void init() throws SQLException {
        try (Connection con = connect(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS macros (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS steps (id INTEGER PRIMARY KEY AUTOINCREMENT, macro_id INTEGER, position INTEGER, type TEXT, data TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS runs (id INTEGER PRIMARY KEY AUTOINCREMENT, macro_name TEXT, run_time TEXT, steps_done INTEGER, status TEXT)");
        }
    }

    private int findId(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM macros WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        }
    }

    public void saveMacro(Macro macro) throws SQLException {
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            int id = findId(con, macro.getName());
            if (id == -1) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO macros (name) VALUES (?)")) {
                    ps.setString(1, macro.getName());
                    ps.executeUpdate();
                }
                id = findId(con, macro.getName());
            } else {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM steps WHERE macro_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO steps (macro_id, position, type, data) VALUES (?, ?, ?, ?)")) {
                for (int i = 0; i < macro.getSteps().size(); i++) {
                    Step step = macro.getSteps().get(i);
                    ps.setInt(1, id);
                    ps.setInt(2, i);
                    ps.setString(3, step.getType());
                    ps.setString(4, step.getData());
                    ps.executeUpdate();
                }
            }
            con.commit();
        }
    }

    public List<String> getMacroNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection con = connect(); Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT name FROM macros ORDER BY name");
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    public Macro loadMacro(String name) throws SQLException, MacroException {
        Macro macro = new Macro(name);
        try (Connection con = connect()) {
            int id = findId(con, name);
            if (id == -1) {
                throw new MacroException("Macro not found: " + name);
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT type, data FROM steps WHERE macro_id = ? ORDER BY position")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    macro.getSteps().add(Step.create(rs.getString("type"), rs.getString("data")));
                }
            }
        }
        return macro;
    }

    public void deleteMacro(String name) throws SQLException {
        try (Connection con = connect()) {
            int id = findId(con, name);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM steps WHERE macro_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM macros WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    public void logRun(String macroName, int stepsDone, String status) throws SQLException {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("INSERT INTO runs (macro_name, run_time, steps_done, status) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, macroName);
            ps.setString(2, time);
            ps.setInt(3, stepsDone);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    public List<String> getHistory() throws SQLException {
        List<String> lines = new ArrayList<>();
        try (Connection con = connect(); Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM runs ORDER BY id DESC LIMIT 50");
            while (rs.next()) {
                lines.add(rs.getString("run_time") + "   " + rs.getString("macro_name") + "   "
                        + rs.getString("status") + "   " + rs.getInt("steps_done") + " steps");
            }
        }
        return lines;
    }

    public List<String> getStats() throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT macro_name, COUNT(*) AS total, SUM(steps_done) AS steps, "
                + "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS ok "
                + "FROM runs GROUP BY macro_name ORDER BY total DESC";
        try (Connection con = connect(); Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                lines.add(rs.getString("macro_name") + ": " + rs.getInt("total") + " runs, "
                        + rs.getInt("ok") + " completed, " + rs.getInt("steps") + " steps played");
            }
        }
        return lines;
    }
}
