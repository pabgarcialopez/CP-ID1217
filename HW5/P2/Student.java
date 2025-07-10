import java.io.*;
import java.net.*;
import java.util.*;

public class Student extends Thread {

    private final int basePort;
    private final int studentID;

    private ServerSocket serverSocket;
    private BufferedReader fromReacher;
    private PrintWriter toReacher;

    public Student(int studentID, int basePort) {
        this.studentID = studentID;
        this.basePort = basePort;
        try {
            this.serverSocket = beginConnection();
        } catch (IOException e) {
            System.out.println("ERROR while trying to start server socket of student " + studentID);
        }
    }

    @Override
    public void run() {
        try {
            handleConnections(serverSocket.accept());
            closeConnection(serverSocket);
        } catch(IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private ServerSocket beginConnection () throws IOException {
        System.out.println("Student " + studentID + " connected to port " + (studentID + basePort));
        return new ServerSocket(studentID + basePort);
    }

    private int selectCandidate() {
        int index;
        Random rnd = new Random();
        HashSet<Integer> unpaired = Teacher.unpairedStudents;

        do {index = rnd.nextInt(unpaired.size());}
        while ((int) unpaired.toArray()[index] == this.studentID && unpaired.size() != 1);

        return (int) unpaired.toArray()[index];
    }

    private void selfPairing () {
        System.out.println("Student " + studentID + ": I will have to pair up with myself.");
        // Inform teacher about this auto pairing.
        toReacher.println("Self-pairing: " + studentID);
        toReacher.flush();
    }

    private String sendProposalTo (int partnerID) throws IOException {
        // Establish connection
        Socket partnerSocket = new Socket("localhost", basePort + partnerID);
        System.out.println("Student " + studentID + ": trying to partner up with student " + partnerID);
        PrintWriter toPartner = new PrintWriter(partnerSocket.getOutputStream(), true);
        BufferedReader fromPartner = new BufferedReader(new InputStreamReader(partnerSocket.getInputStream()));

        // Communicate with partner.
        toPartner.println("Proposal from student: " + this.studentID);
        String response = fromPartner.readLine();
        toPartner.close(); fromPartner.close();  partnerSocket.close();
        return response;
    }

    private void createPair (int partnerID) {
        System.out.println("Student " + studentID + ": I got accepted by student " + partnerID);
        // Let the teacher know the student's partner index.
        toReacher.println("Accepted: " + partnerID);
        toReacher.flush();
    }

    private boolean handleCandidateResponse (String response, int partnerID) {

        boolean outcome = false;

        if (response.equals("Accept")) {
            createPair(partnerID);
            outcome = true;
        }

        else if(response.equals("Reject")) {
            System.out.println("Student " + studentID + ": I got rejected by student " + partnerID);
            toReacher.println("Rejected: " + partnerID);
            toReacher.flush();
        }

        return outcome;
    }

    private boolean handleProposal (int proposingStudentID) {

        String decision = new Random().nextBoolean() ? "Accept" : "Reject";

        if (decision.equals("Accept")) {
            System.out.println("Student " + studentID + ": I accept the proposal from student " + proposingStudentID);
            toReacher.println("Accept");
            toReacher.flush();
            return true;

        } else {
            System.out.println("Student " + studentID + ": I reject the proposal from student " + proposingStudentID);
            toReacher.println("Reject");
            toReacher.flush();
            return false;
        }
    }

    private void handleConnections (Socket reacherSocket) throws IOException {

        boolean stop = false;

        while(!stop) {

            // Reacher can either be the teacher or another student.
            fromReacher = new BufferedReader(new InputStreamReader(reacherSocket.getInputStream()));
            toReacher = new PrintWriter(reacherSocket.getOutputStream(), true);

            String message = fromReacher.readLine();
            System.out.println("Student " + studentID + ": received message \"" + message  + "\"");

            if (message.equals("Choose partner.")) { // Message comes from teacher.
                boolean partnerIsFound = false;

                // Continue sending proposals until a partner is found
                while (!partnerIsFound) {

                    int candidateID = selectCandidate();

                    // There are at least 2 distinct students left.
                    if(studentID != candidateID) {
                        String response = sendProposalTo(candidateID);
                        stop = partnerIsFound = handleCandidateResponse(response, candidateID);
                    }

                    // Self-pairing because of an odd number of students.
                    else {
                        selfPairing();
                        stop = partnerIsFound = true;
                    }
                }
            }

            else if(message.startsWith("Proposal")) {
                stop = handleProposal(Integer.parseInt(message.split(":")[1].trim()));
                if(!stop)
                    reacherSocket = serverSocket.accept();
            }
        }

        reacherSocket.close();
        fromReacher.close();
        toReacher.close();
    }

    private void closeConnection (ServerSocket serverSocket) throws IOException {
        serverSocket.close();
    }
}
