import java.io.*;
import java.net.*;

public class Student {
    public static void main(String[] args) {

        if(args.length != 3) {
            System.out.println("Usage: java Client <hostname> <port> <studentID>");
            System.exit(1);
        }

        final String SERVER_IP = args[0];
        final int PORT = Integer.parseInt(args[1]);
        int studentID = Integer.parseInt(args[2]);

        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // Send the server the student's number.
            writer.println(studentID);

            String response = reader.readLine();
            System.out.println(response);

            reader.close();
            writer.close();
            socket.close();

        } catch (IOException ignored) {}
    }
}
