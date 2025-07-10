import java.io.*;
import java.net.*;
import java.util.*;

public class Teacher {

    public static HashSet<Integer> unpairedStudents = new HashSet<>();

    public static void main(String[] args) {

        if(args.length != 2) {
            System.out.println("Usage: java Teacher <numStudents> <port>");
            System.exit(1);
        }

        Random random = new Random();
        final int numStudents = Integer.parseInt(args[0]);
        final int basePort = Integer.parseInt(args[1]);
        HashMap<Integer, Integer> pairs = new HashMap<>();

        // Initialize set of unpaired students.
        for (int i = 1; i <= numStudents; i++)
            unpairedStudents.add(i);

        // Initialize students.
        Student[] students = new Student[numStudents];
        for(int i = 0; i < numStudents; i++)
            students[i] = new Student(i + 1, basePort);

        // Run students.
        for(Student s : students)
            s.start();

        // While the teacher can pair up students.
        while (!unpairedStudents.isEmpty()) {
            int studentIndex = random.nextInt(unpairedStudents.size());
            int studentID =  (Integer) unpairedStudents.toArray()[studentIndex];

            try (Socket studentSocket = new Socket("localhost", studentID + basePort)) {
                PrintWriter writer = new PrintWriter(studentSocket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(studentSocket.getInputStream()));

                writer.println("Choose partner.");
                System.out.println("\nTeacher: I want student " + studentID + " to choose a partner.\n");

                boolean pairFormed = false;

                // Teacher waits until the student has found a partner
                while(!pairFormed) {

                    String outcome = reader.readLine();
                    int partnerID = Integer.parseInt(outcome.split(":")[1].trim());

                    if (outcome.contains("Accepted") || outcome.contains("Self")) {
                        unpairedStudents.remove(studentID);
                        unpairedStudents.remove(partnerID);
                        pairs.put(studentID, partnerID);
                        if (studentID != partnerID)
                            pairs.put(partnerID, studentID);

                        pairFormed = true;
                    }

                    System.out.println("\nUnpaired students: " + unpairedStudents);
                    System.out.println("Paired students: " + pairs);
                }

                writer.close();
                reader.close();

            } catch (IOException e) {
                System.out.println("ERROR with studentSocket: " + e.getMessage());
            }
        }

        System.out.println("\nAll students have been paired up: " + pairs);
    }
}
