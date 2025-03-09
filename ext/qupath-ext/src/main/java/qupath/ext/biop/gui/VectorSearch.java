package qupath.ext.biop.gui;

import com.intersystems.jdbc.IRISDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VectorSearch {
    private Connection conn;

    public void createConnection() {
        try {
            // set connection parameters
            IRISDataSource ds = new IRISDataSource();
            ds.setServerName("172.16.91.29");
            ds.setPortNumber(1972);
            ds.setDatabaseName("%SYS");
            ds.setUser("demo");
            ds.setPassword("demo");
            System.out.println("Connecting to InterSystems IRIS...");

            conn = ds.getConnection();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public void InsertEmbeddings(String embeddings) {
        try {
            // set connection parameters
            IRISDataSource ds = new IRISDataSource();
            ds.setServerName("172.16.91.29");
            ds.setPortNumber(1972);
            ds.setDatabaseName("%SYS");
            ds.setUser("demo");
            ds.setPassword("demo");
            System.out.println("Connecting to InterSystems IRIS...");
            //            listTables(conn);


//            String sql = "INSERT INTO Sample.Vectors (vec1) " +
//                    "VALUES (embeddings)";
            String sql = "INSERT INTO Sample.Vectors (vec1) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            System.out.println("Inserting embeddings..., sql: " + sql);

            pstmt.setString(1, embeddings);
//            pstmt.setInt(1, uid);

            pstmt.executeUpdate();

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public float nearestNeighbor(String embedding, boolean inDB) {
        float distance = -1;
        try {
            // Create sql query from the embedding
            String sql = "SELECT TOP 2 VECTOR_COSINE(vec1, TO_VECTOR(?, DOUBLE)) AS distance FROM Sample.Vectors ORDER BY distance DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, embedding);
            ResultSet rset = pstmt.executeQuery();
            
            if (inDB) {
                rset.next();
                System.out.println("Skip: " + inDB);
            }
            if (rset.next()) {
                distance = rset.getFloat("distance");
            }

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        System.out.println("Distance: " + distance);
        return distance;
    }

    public static void listTables(Connection connection) throws SQLException {
        // Define the SQL query to list tables
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";

        // Prepare the statement
        PreparedStatement pstmt = connection.prepareStatement(sql);

        // Execute the query and retrieve the result set
        ResultSet rs = pstmt.executeQuery();

        // Print the table names
        while (rs.next()) {
            System.out.println(rs.getString("TABLE_NAME"));
        }

        // Close the statement and connection
        pstmt.close();
        connection.close();
    }

    public void  dropTable() {
        try {
            // Create sql query from the embedding
            String sql = "TRUNCATE TABLE Sample.Vectors";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public void createTable() {
        try {
            // Create sql query from the embedding
            String sql = "CREATE TABLE Sample.Vectors (vec1 VECTOR(DOUBLE,2))";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }


    public List<String> getRandomVectors(int n) {
        List<String> randomVectors = new ArrayList<>();
        try {
            String sql = "SELECT TOP " + n + " * FROM Sample.Vectors ORDER BY %EXACT(ID * $HOROLOG)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rset = pstmt.executeQuery();

            while (rset.next()) {
                randomVectors.add(rset.getString("vec1"));
            }

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        return randomVectors;
    }
}
