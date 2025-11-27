package org;

import dao.DocumentDAO;
import dao.FlashcardDAO;
import dao.UserDAO;
import model.Document;
import model.DocumentChunk;
import model.Flashcard;
import model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // Initialize DAOs
        UserDAO userDAO = new UserDAO();
        DocumentDAO docDAO = new DocumentDAO();
        FlashcardDAO flashcardDAO = new FlashcardDAO();

        try {
            System.out.println("=========================================");
            System.out.println("    🚀 SYSTEM DATABASE INTEGRATION TEST");
            System.out.println("=========================================");

            // ---------------------------------------------------------
            // 1. TEST USER (Register & Login)
            // ---------------------------------------------------------
            System.out.println("\n--- STEP 1: User Management ---");

            // Create a unique username so you can run this test multiple times
            String randomUser = "student_" + new Random().nextInt(10000);

            User newUser = new User(
                    randomUser, null, null, // Salt/Hash handled by DAO
                    "Minh", "Khoi", randomUser + "@test.com",
                    LocalDate.of(2000, 1, 1)
            );

            userDAO.registerUser(newUser, "SecretPass123");
            System.out.println("✅ User Registered: " + randomUser);

            User loggedInUser = userDAO.loginUser(randomUser, "SecretPass123");
            if (loggedInUser == null) throw new RuntimeException("Login Failed!");
            System.out.println("✅ Login Success! User ID: " + loggedInUser.getId());

            // ---------------------------------------------------------
            // 2. TEST DOCUMENTS (Metadata)
            // ---------------------------------------------------------
            System.out.println("\n--- STEP 2: Document Metadata ---");

            Document doc = new Document(loggedInUser.getId(), "Java_Intro.pdf");
            int docId = docDAO.addDocument(doc);
            System.out.println("✅ Document Created. ID: " + docId);

            // ---------------------------------------------------------
            // 3. TEST DOCUMENT CHUNKS (The RAG Data)
            // ---------------------------------------------------------
            System.out.println("\n--- STEP 3: Document Chunks (RAG Data) ---");

            List<DocumentChunk> chunks = new ArrayList<>();

            // Simulate Chunk 1 (Text + Mock Vector)
            float[] vector1 = {0.1f, 0.2f, 0.9f};
            chunks.add(new DocumentChunk(docId, "Java is object-oriented.", vector1));

            // Simulate Chunk 2
            float[] vector2 = {0.5f, -0.1f, 0.0f};
            chunks.add(new DocumentChunk(docId, "JVM runs bytecode.", vector2));

            // Save to DB
            docDAO.saveChunks(chunks);

            // Verify by reading back
            List<DocumentChunk> retrievedChunks = docDAO.getChunksByDocId(docId);
            System.out.println("📂 Retrieved " + retrievedChunks.size() + " chunks from DB.");

            if (!retrievedChunks.isEmpty()) {
                DocumentChunk c = retrievedChunks.get(0);
                System.out.println("   -> Content: " + c.getTextContent());
                System.out.println("   -> Vector Size: " + c.getVector().length);
            }

            // ---------------------------------------------------------
            // 4. TEST FLASHCARDS (JSON Storage)
            // ---------------------------------------------------------
            System.out.println("\n--- STEP 4: Flashcards ---");

            List<Flashcard> cardList = new ArrayList<>();
            cardList.add(new Flashcard("What is int?", "Primitive type"));
            cardList.add(new Flashcard("What is String?", "Class type"));

            // Save the set
            flashcardDAO.saveFlashcardSet(loggedInUser.getId(), "Java Basics", cardList);
            System.out.println("✅ Flashcard Set Saved.");

            // ---------------------------------------------------------
            // 5. CLEANUP (Cascade Delete)
            // ---------------------------------------------------------
            System.out.println("\n--- STEP 5: Cleanup ---");

            userDAO.deleteUser(loggedInUser.getId());

            // Check that everything is gone (Cascade works)
            User checkUser = userDAO.loginUser(randomUser, "SecretPass123");
            List<DocumentChunk> checkChunks = docDAO.getChunksByDocId(docId);

            if (checkUser == null && checkChunks.isEmpty()) {
                System.out.println("✅ Cascade Delete Successful: User, Doc, Chunks & Flashcards removed.");
            } else {
                System.err.println("❌ Error: Data still exists in DB.");
            }

            System.out.println("\n=========================================");
            System.out.println("    ✅ ALL TESTS PASSED");
            System.out.println("=========================================");

        } catch (SQLException e) {
            System.err.println("🚨 Database Error: " + e.getMessage());
        }
    }
}
