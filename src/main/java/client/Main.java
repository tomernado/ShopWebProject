package client;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws Exception {
        ServerConnection connection = new ServerConnection("localhost", 5000);
        SwingUtilities.invokeLater(() -> new MainFrame(connection).setVisible(true));
    }
}
