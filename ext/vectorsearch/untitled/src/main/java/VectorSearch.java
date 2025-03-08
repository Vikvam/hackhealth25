import com.intersystems.jdbc.IRISDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VectorSearch {
    private Connection conn;

    public void createConnection() {
        try {
            // set connection parameters
            IRISDataSource ds = new IRISDataSource();
            ds.setServerName("127.0.0.1");
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
            ds.setServerName("127.0.0.1");
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

            pstmt.setString(0, embeddings);
//            pstmt.setInt(1, uid);

            pstmt.executeUpdate();

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public float nearestNeighbor(String embedding) {
        float distance = -1;
        try {
            // Create sql query from the embedding
            String sql = "SELECT TOP 1 VECTOR_COSINE(vec1, TO_VECTOR(?, DOUBLE)) AS distance FROM Sample.Vectors ORDER BY distance DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, embedding);
            ResultSet rset = pstmt.executeQuery();

            if (rset.next()) {
                distance = rset.getFloat("distance");
            }

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: "
                    + ex.getClass().getName() + ": " + ex.getMessage());
        }
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
}
