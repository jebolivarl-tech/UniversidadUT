package universidad;

import com.formdev.flatlaf.FlatDarkLaf;

public class App {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        java.awt.EventQueue.invokeLater(() -> {
            LoginForm ventana = new LoginForm();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
