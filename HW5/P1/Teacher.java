import java.io.*;
import java.net.*;

public class Teacher {
    public static void main(String[] args) {

        if(args.length != 2) {
            System.out.println("Usage: java Server <port> <numStudents>");
            System.exit(1);
        }

        final int PORT = Integer.parseInt(args[0]);
        final int NUM_STUDENTS = Integer.parseInt(args[1]);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for clients...");
            int pairedStudents = 0;

            while(pairedStudents < NUM_STUDENTS) {
                System.out.println("Teacher has paired up " + pairedStudents + "/" + NUM_STUDENTS + " students.");
                Socket student1 = serverSocket.accept();
                pairedStudents++;

                if(pairedStudents == NUM_STUDENTS && NUM_STUDENTS % 2 != 0)
                    System.out.println("Last student is paired up with himself.");

                else {
                    pairedStudents++;
                    Socket student2 = serverSocket.accept();
                    pairUp(student1, student2);
                }
            }

            System.out.println("All students have been paired up!");

        } catch (IOException ignored) {}
    }

    private static void pairUp (Socket student1, Socket student2) {

        try {
            BufferedReader reader1 = new BufferedReader(new InputStreamReader(student1.getInputStream()));
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(student2.getInputStream()));
            PrintWriter writer1 = new PrintWriter(student1.getOutputStream(), true);
            PrintWriter writer2 = new PrintWriter(student2.getOutputStream(), true);

            // Read student's index from client.
            int studentIndex1 = Integer.parseInt(reader1.readLine());
            int studentIndex2 = Integer.parseInt(reader2.readLine());

            // Send to the client.
            writer1.println("You're student " + studentIndex1 + " and your partner is student " + studentIndex2);
            writer2.println("You're student " + studentIndex2 + " and your partner is student " + studentIndex1);

            // Printed on the server's console.
            System.out.println("Student " + studentIndex1 + " and student " + studentIndex2 + " are paired up!");
        }

        catch (IOException ignored) {}
    }
}
