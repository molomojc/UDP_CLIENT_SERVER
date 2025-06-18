import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;

/**
 * UDP Application for sending and receiving files between a sender and a leecher.
 * 
 * This application provides two modes: Sender and Leecher. 
 * The sender can upload files, and the leecher can download files over UDP communication.
 * 
 * Version: P05
 * Author: JM MoLOMO
 */
public class UDP extends Application {

    private StackPane pane;
    private DatagramSocket socket;
    
    private InetAddress remoteAddress;
    private int remotePort = 12345;
    private int localPort = 12345; 

    /**
     * Initializes the application by setting up the UI and creating a UDP socket.
     * Calls the method to receive messages continuously in the background.
     */
    public UDP() {
        this.pane = new StackPane();
        UIOne();

        try {
            socket = new DatagramSocket(localPort);
            receiveMessage(); // Starts listening for incoming messages
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(pane);
        primaryStage.setTitle("UDP Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Sets up the first UI screen with buttons to select either sender or leecher mode.
     */
    public void UIOne() {
        pane.getChildren().clear();
        pane.setPrefSize(550, 500);
        Pane mainP = new Pane();
        
        // Sender button
        Button btnSender = new Button("SENDER");
        btnSender.setLayoutX(200);
        btnSender.setLayoutY(0);
        btnSender.setPrefSize(60, 35);

        // Leecher button
        Button btnLeecher = new Button("LEECHER");
        btnLeecher.setLayoutX(260);
        btnLeecher.setLayoutY(0);
        btnLeecher.setPrefSize(70, 35);
        btnLeecher.setOnAction(e -> {
            try {
                remoteAddress = InetAddress.getByName("localhost");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            UITwo();
        });

        // Status text field
        TextField Status = new TextField();
        Status.setPromptText("ENTIRE ID FIRST![1-100]");
        Status.setLayoutX(180);
        Status.setLayoutY(65);
        Status.setPrefSize(150, 35);

        // Choose File button for uploading
        Button btnChooseFile = new Button("Choose File");
        btnChooseFile.setLayoutX(25);
        btnChooseFile.setLayoutY(65);
        btnChooseFile.setPrefSize(150, 35);
        btnChooseFile.setOnAction(e -> upload(Status));

        // List button to fetch available files
        Button btnList = new Button("List");
        btnList.setLayoutX(25);
        btnList.setLayoutY(120);
        btnList.setPrefSize(100, 35);
        btnList.setOnAction(e -> pull(new TextArea()));

        // Text Area to display list of files
        TextArea TA = new TextArea();
        TA.setPrefSize(300, 200);
        TA.setLayoutX(130);
        TA.setLayoutY(120);
        
        mainP.getChildren().addAll(btnSender, btnLeecher, btnChooseFile, Status, btnList, TA);
        pane.getChildren().add(mainP);
    }

    /**
     * Switches to the second UI screen for downloading files.
     */
    private void UITwo() {
        new UITwoClass(pane, socket, remoteAddress, remotePort);
    }

    /**
     * Handles the file upload functionality.
     * The file is copied to the "data/sender" folder, and its entry is logged in "List.txt".
     * 
     * @param status TextField containing the file ID.
     */
    public void upload(TextField status) {
    	  FileChooser fileChooser = new FileChooser();
          fileChooser.setTitle("Select File to Upload");
          File selectedFile = fileChooser.showOpenDialog(null);

          if (selectedFile != null) {
              try {
                  File destinationDir = new File("data/sender");
                  if (!destinationDir.exists()) {
                      destinationDir.mkdirs(); // Create the directory if it doesn't exist
                  }

                  File destinationFile = new File(destinationDir, selectedFile.getName());
                  PrintWriter fileWriter = new PrintWriter(new FileOutputStream("data/sender/List.txt", true));
                  
                  fileWriter.write(status.getText()+ " " + selectedFile.getName() + "\n");
                  fileWriter.flush();
                  fileWriter.close();

                  // Copy file to destination
                  try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(selectedFile));
                       BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFile))) {
                      byte[] byteArray = new byte[1024];
                      int bytesRead;
                      while ((bytesRead = bis.read(byteArray)) != -1) {
                          bos.write(byteArray, 0, bytesRead);
                      }
                      bos.flush();
                      System.out.println(destinationFile.getAbsolutePath());
                      status.setText("Success");
                  }
              } catch (IOException e) {
                  e.printStackTrace();
                  status.setText("Upload failed.");
              }
          } else {
              status.setText("No file selected.");
          }
    }

    /**
     * Fetches the list of files from "data/Leecher/List.txt" and displays it in the provided TextArea.
     * 
     * @param txt_id2 TextArea to display the list of files.
     */
    private void pull(TextArea txt_id2) {
        try (BufferedReader readFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File("data/Leecher/List.txt"))))) {
            String line;
            while ((line = readFile.readLine()) != null) {
                txt_id2.appendText(line + "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message via UDP to the remote address.
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
     * Receives a response from the server and returns it as a String.
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
     * Continuously listens for incoming messages on the UDP socket.
     * Processes file requests and sends files or file lists based on the received command.
     */
    private void receiveMessage() {
    	 new Thread(() -> {
             byte[] buffer = new byte[2048];
             DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
             while (true) {
                 try {
                     socket.receive(packet);
                     String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                     System.out.println("Message received: " + receivedMessage);
                     String receivearray[] = receivedMessage.split(" ");
                     
                     // Handle LIST command
                     if ("LIST".equals(receivearray[0])) {
                         File listFile = new File("data/sender/List.txt");
                         if (listFile.exists()) {
                             try (BufferedReader reader = new BufferedReader(new FileReader(listFile))) {
                                 String line;
                                 while ((line = reader.readLine()) != null) {
                                     String response = line;
                                     DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.getBytes().length, packet.getAddress(), packet.getPort());
                                     socket.send(responsePacket);
                                 }
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         } 
                           
                     // Handle FILE request
                     } else if ("FILE".equals(receivearray[0])) {
                         String ID = receivearray[1];
                         try (BufferedReader reader = new BufferedReader(new FileReader(new File("data/sender/List.txt")))) {
                             String line;
                             while ((line = reader.readLine()) != null) {
                                 if (line.contains(ID)) {
                                     String[] tempArray = line.split(" ");
                                     String fileName = tempArray[1];

                                     byte[] fileNameBytes = fileName.getBytes();
                                     DatagramPacket fileNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, packet.getAddress(), packet.getPort());
                                     socket.send(fileNamePacket);  

                                     File imageFile = new File("data/sender/" + fileName);

                                     try (FileInputStream imageFis = new FileInputStream(imageFile)) {
                                         byte[] bufferfile = new byte[1024]; 
                                         int bytesRead;

                                         while ((bytesRead = imageFis.read(bufferfile)) != -1) {
                                             DatagramPacket filePacket = new DatagramPacket(bufferfile, bytesRead, packet.getAddress(), packet.getPort());
                                             socket.send(filePacket); 
                                         }

                                         byte[] eofMessage = "EOF".getBytes();
                                         DatagramPacket eofPacket = new DatagramPacket(eofMessage, eofMessage.length, packet.getAddress(), packet.getPort());
                                         socket.send(eofPacket);
                                     }
                                     break;
                                 }
                             }
                         } catch (FileNotFoundException e) {
                             e.printStackTrace();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }

                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }).start();
    }

    /**
     * The entry point of the application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
