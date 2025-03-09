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
            pstmt.setString(1, embeddings);

            System.out.println("Inserting embeddings..., sql: " + sql);

//            pstmt.setInt(1, uid);

            pstmt.executeUpdate();

            pstmt.close();
        } catch (Exception ex) {
            System.out.println("caught exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
    
    public void initEuclidean() {
        String sql = """
                create or replace function l2_distance(v1 varchar, v2 varchar)
                returns float
                language python
                {
                	import math
                	def to_list(v):
                		return list(map(lambda x: float(x), list(v.split(","))))
                    print(v1, v2)
                	v1 = to_list(v1)
                	v2 = to_list(v2)
                	return math.sqrt(sum([(val1 - val2) ** 2 for val1, val2 in zip(v1, v2)]))
                }
        """;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            System.out.println(pstmt);
            pstmt.executeUpdate();
            pstmt.close();
        }  catch (Exception ex) {
            System.out.println("caught exception: " + ex.getClass().getName() + ": " + ex.getMessage());
        }

    }

    public float nearestNeighbor(String embedding, boolean inDB) {
        float distance = -1;
        try {
            // Create sql query from the embedding
                       
            String sql = "SELECT TOP 2 l2_distance(%external(vec1), ?) AS distance FROM Sample.Vectors ORDER BY distance DESC";
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
