import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * UI for the second screen that allows the user to switch to the leecher mode,
 * connect to the server, list available files, and download files over UDP.
 */
public class UITwoClass {

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;

    /**
     * Constructor that initializes the second UI screen.
     * 
     * @param pane         The parent StackPane to display the UI.
     * @param socket       The DatagramSocket for UDP communication.
     * @param remoteAddress The remote address for communication.
     * @param remotePort   The remote port for communication.
     */
    public UITwoClass(StackPane pane, DatagramSocket socket, InetAddress remoteAddress, int remotePort) {
        this.socket = socket;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;

        pane.getChildren().clear();
        pane.setPrefSize(550, 500);
        Pane mainPane = new Pane();

        // Sender Button (returns to sender UI)
        Button btnSender = new Button("SENDER");
        btnSender.setLayoutX(200);
        btnSender.setLayoutY(0);
        btnSender.setPrefSize(60, 35);
        btnSender.setOnAction(e -> {
            new UDP().UIOne(); // Returns to the Sender mode
        });

        // Leecher Button (inactive in this screen, but added for consistency)
        Button btnLeecher = new Button("LEECHER");
        btnLeecher.setLayoutX(260);
        btnLeecher.setLayoutY(0);
        btnLeecher.setPrefSize(70, 35);

        // TextField to input the host address
        TextField Host = new TextField();
        Host.setPromptText("Host");
        Host.setPrefSize(150, 35);
        Host.setLayoutX(1);
        Host.setLayoutY(65);

        // TextField to input the port number
        TextField Port = new TextField();
        Port.setPromptText("Port");
        Port.setPrefSize(50, 35);
        Port.setLayoutX(160);
        Port.setLayoutY(65);

        // Status TextField to display connection status
        TextField txtStatus = new TextField();
        txtStatus.setPromptText("Status");
        txtStatus.setPrefSize(150, 35);
        txtStatus.setLayoutY(65);
        txtStatus.setLayoutX(380);

        // TextArea to display the list of available files
        TextArea TA = new TextArea();
        TA.setPrefSize(300, 200);
        TA.setLayoutX(130);
        TA.setLayoutY(180);

        // Button to request the list of available files
        Button btnList = new Button("List");
        btnList.setLayoutX(1);
        btnList.setLayoutY(180);
        btnList.setPrefSize(100, 35);
        btnList.setOnAction(e -> {
            new Thread(() -> {
                sendMessage("LIST");
                String response = receiveResponse();
                TA.appendText(response + "\n");
                if (response != null) {
                    System.out.println("Response received: " + response);
                }
            }).start();
        });

        // TextField to input the file ID for downloading
        TextField id = new TextField();
        id.setPromptText("ID");
        id.setPrefSize(50, 35);
        id.setLayoutX(155);
        id.setLayoutY(120);

        // Button to download the selected file by ID
        Button btnChooseFile = new Button("DOWNLOAD");
        btnChooseFile.setLayoutX(1);
        btnChooseFile.setLayoutY(120);
        btnChooseFile.setPrefSize(150, 35);
        btnChooseFile.setOnAction(e -> {
            new Thread(() -> downloadFile(id.getText())).start();
        });

        // Button to connect to the server with the specified host and port
        Button btnConnect = new Button("Connect");
        btnConnect.setPrefSize(150, 35);
        btnConnect.setLayoutX(220);
        btnConnect.setLayoutY(65);
        btnConnect.setOnAction(e -> {
            try {
                this.remoteAddress = InetAddress.getByName(Host.getText());
                this.remotePort = Integer.parseInt(Port.getText());
                txtStatus.setText("Connected");
                System.out.println("Connected to " + remoteAddress + " on port " + remotePort);
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
                txtStatus.setText("Connection failed");
            }
        });

        mainPane.getChildren().addAll(btnSender, btnLeecher, Host, Port, btnConnect, txtStatus, btnChooseFile, btnList, TA, id);
        pane.getChildren().addAll(mainPane);
    }

    /**
     * Sends a message to the remote server via UDP.
     * 
     * @param message The message to send.
     */
    private void sendMessage(String message) {
        if (remoteAddress != null) {
            try {
                byte[] msgBuffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(msgBuffer, msgBuffer.length, remoteAddress, remotePort);
                socket.send(packet);
                System.out.println("Message sent: " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Remote address not set.");
        }
    }

    /**
     * Receives a response from the remote server via UDP.
     * 
     * @return The received response as a string.
     */
    private String receiveResponse() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.setSoTimeout(20000);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Downloads a file from the server based on the file ID.
     * 
     * @param fileId The ID of the file to download.
     */
    private void downloadFile(String fileId) {
        try {
            // Send request for the file
            String requestMessage = "FILE " + fileId;
            sendMessage(requestMessage);

            // Receive file name
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String fileName = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("File name received: " + fileName);

            // Create output directory and file
            File outputDir = new File("data/leecher");
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // Create the directory if it doesn't exist
            }
            File outputFile = new File(outputDir, fileName);

            // Receive and write the file content
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                byte[] buffer = new byte[2048]; // Buffer size for file transfer
                while (true) {
                    DatagramPacket filePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(filePacket);

                    String receivedMessage = new String(filePacket.getData(), 0, filePacket.getLength());

                    // Check if the file transfer is complete
                    if ("EOF".equals(receivedMessage)) {
                        System.out.println("File transfer complete.");
                        break;
                    }

                    // Write the received data to the file
                    bos.write(filePacket.getData(), 0, filePacket.getLength());
                    bos.flush(); 
                }

                System.out.println("Downloaded: " + outputFile.getName());

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
