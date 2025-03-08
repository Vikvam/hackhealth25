public class main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        VectorSearch vectorSearch;
        vectorSearch = new VectorSearch();
        vectorSearch.createConnection();
        System.out.println("Inserting embeddings...");
        vectorSearch.InsertEmbeddings("1,2,3");
        System.out.println("Embeddings inserted!");
        float dist = vectorSearch.nearestNeighbor("1,2");
        System.out.println("Nearest neighbor distance: " + dist);
    }
}
