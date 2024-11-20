import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatParserGUI {

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Chat Parser");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create a button
        JButton selectFileButton = new JButton("Select a .txt file to process");

        // Add an action listener to the button
        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open file chooser
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                // Restrict to .txt files only
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
                    }

                    @Override
                    public String getDescription() {
                        return "Text Files (*.txt)";
                    }
                });

                int returnValue = fileChooser.showOpenDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        // Process the file to JSON
                        processFile(selectedFile.getAbsolutePath());

                        // Load the generated JSON and display the chat
                        JSONArray messagesArray = loadJsonData("results.json");
                        Set<String> authors = getUniqueAuthors(messagesArray);
                        frame.setVisible(false);

                        // DISPLAY CHAT CHECKPOINT-1
                        displayChatInterface(messagesArray, authors);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Add the button to the frame
        frame.add(selectFileButton, BorderLayout.CENTER);

        // Make the frame visible
        frame.setVisible(true);
    }

    // ChatParser logic
    public static void processFile(String inputFile) throws IOException {
        String outputFile = "results.json"; // Output file name
        List<String> lines;
        JSONArray resultArray = new JSONArray();

        lines = Files.readAllLines(Paths.get(inputFile));

        StringBuilder currentMessage = new StringBuilder();
        for (String line : lines) {
            // Replace \u202f with a single space
            line = line.replace('\u202f', ' ');

            // Check if the line starts with a datetime
            if (line.contains(" - ")) {
                // If there's an existing message in the buffer, process it
                if (currentMessage.length() > 0) {
                    processMessage(currentMessage.toString(), resultArray);
                    currentMessage.setLength(0); // Clear the buffer
                }
                // Start a new message
                currentMessage.append(line);
            } else {
                // Append to the current message (multi-line handling)
                currentMessage.append("\n").append(line);
            }
        }

        // Process the last message in the buffer
        if (currentMessage.length() > 0) {
            processMessage(currentMessage.toString(), resultArray);
        }

        // Write to JSON file
        Files.write(Paths.get(outputFile), resultArray.toString(4).getBytes());
    }

    private static void processMessage(String message, JSONArray resultArray) {
        // Split the message into parts using " - "
        String[] parts = message.split(" - ", 2);
        if (parts.length < 2) {
            System.out.println("Skipping invalid message: " + message);
            return;
        }

        String datetime = parts[0].trim();
        String messageBody = parts[1].trim();

        // Check for the system message to skip
        if (messageBody.equals(
                "Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them. Tap to learn more.")) {
            System.out.println("Skipping system message: " + message);
            return;
        }

        // Split the message body by ":" to extract author and message
        String[] msgBody = messageBody.split(":", 2);
        if (msgBody.length < 2) {
            System.out.println("Skipping message without author: " + message);
            return;
        }

        String author = msgBody[0].trim();
        String messageText = msgBody[1].trim();

        // Handle multi-line message text
        messageText = messageText.replace("\n", " ");

        // Create JSON object
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("datetime", datetime);
        jsonObject.put("author", author);
        jsonObject.put("message", messageText);

        resultArray.put(jsonObject);
    }

    private static JSONArray loadJsonData(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return new JSONArray(content);
    }

    private static Set<String> getUniqueAuthors(JSONArray messagesArray) {
        Set<String> uniqueAuthors = new HashSet<>();
        for (int i = 0; i < messagesArray.length(); i++) {
            JSONObject message = messagesArray.getJSONObject(i);
            uniqueAuthors.add(message.getString("author"));
        }
        return uniqueAuthors;
    }

    // author selection page
    private static void displayChatInterface(JSONArray messagesArray, Set<String> authors) {
        JFrame chatFrame = new JFrame("Chat Display");
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(600, 500);
        chatFrame.setLayout(new BorderLayout());

        JLabel label = new JLabel("WhatsApp Chat Preview");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        chatFrame.add(label, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        // Create buttons dynamically for each message author
        for (String author : authors) {
            JButton button = new JButton(author);
            button.addActionListener(new AuthorSelectionListener(author, messagesArray));
            buttonPanel.add(button);
        }

        chatFrame.add(buttonPanel, BorderLayout.SOUTH);

        // Initialize chat panel
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(chatPanel);
        chatFrame.add(scrollPane, BorderLayout.CENTER);

        chatFrame.setVisible(true);
    }

    private static class AuthorSelectionListener implements ActionListener {
        private final String selectedAuthor;
        private final JSONArray messagesArray;

        public AuthorSelectionListener(String selectedAuthor, JSONArray messagesArray) {
            this.selectedAuthor = selectedAuthor;
            this.messagesArray = messagesArray;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame chatFrame = new JFrame("Chat - " + selectedAuthor);
            chatFrame.setSize(600, 500);
            JPanel chatPanel = new JPanel();
            chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(chatPanel);
            chatFrame.add(scrollPane);

            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject message = messagesArray.getJSONObject(i);
                String author = message.getString("author");
                String datetime = message.getString("datetime");
                String text = message.optString("message", "").trim();

                // Replace "<Media omitted>" and skip invalid messages
                if (text.equals("<Media omitted>")) {
                    text = "A document was shared but cannot be displayed.";
                }
                if (text.equals(".") || text.equals("^") || text.isEmpty()) {
                    continue;
                }

                // Create a message panel
                JPanel messagePanel = new JPanel();
                messagePanel.setLayout(new BorderLayout());
                messagePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding

                JPanel messageBox = new JPanel();
                messageBox.setLayout(new BoxLayout(messageBox, BoxLayout.Y_AXIS));
                messageBox.setBackground(
                        author.equals(selectedAuthor) ? new Color(195, 217, 235) : new Color(195, 235, 222));
                messageBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding

                JLabel authorLabel = new JLabel(author);
                authorLabel.setFont(new Font("Arial", Font.BOLD, 12));
                JLabel messageLabel = new JLabel(
                        "<html>" + text + "<br><i style='font-size:10px;'>" + datetime + "</i></html>");

                messageBox.add(authorLabel);
                messageBox.add(messageLabel);

                if (author.equals(selectedAuthor)) {
                    messagePanel.add(messageBox, BorderLayout.EAST);
                } else {
                    messagePanel.add(messageBox, BorderLayout.WEST);
                }

                chatPanel.add(messagePanel);
            }

            chatFrame.setVisible(true);
        }
    }
}