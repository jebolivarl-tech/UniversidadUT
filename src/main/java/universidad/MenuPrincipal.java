package universidad;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class MenuPrincipal extends javax.swing.JFrame {

    private javax.swing.JButton btnModuloEstudiantes;
    private javax.swing.JButton btnModuloCarreras;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JLabel lblTitulo;

    public MenuPrincipal() {
        construirMenu();
    }

    private void construirMenu() {
        this.setTitle("Sistema Universitario - Panel de Control");
        this.setSize(500, 400);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(null);

        // --- Título del Panel ---
        lblTitulo = new javax.swing.JLabel("PANEL DE CONTROL ACADÉMICO", javax.swing.SwingConstants.CENTER);
        lblTitulo.setFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 18));
        lblTitulo.setBounds(20, 40, 440, 30);
        this.getContentPane().add(lblTitulo);

        // --- Botón para abrir Módulo de Estudiantes ---
        btnModuloEstudiantes = new javax.swing.JButton(" Gestión de Estudiantes");
        btnModuloEstudiantes.setFont(new java.awt.Font("Tahoma", java.awt.Font.PLAIN, 14));
        btnModuloEstudiantes.setBounds(100, 120, 280, 50);
        btnModuloEstudiantes.addActionListener(evt -> {
            FlatLightLaf.setup();
            EstudianteForm ventanaEstudiantes = new EstudianteForm();
            ventanaEstudiantes.setLocationRelativeTo(this); // Centra respecto al menú
            ventanaEstudiantes.setVisible(true);
        });
        this.getContentPane().add(btnModuloEstudiantes);

        // --- Botón para abrir Módulo de Carreras ---
        btnModuloCarreras = new javax.swing.JButton("Gestión de Carreras");
        btnModuloCarreras.setFont(new java.awt.Font("Tahoma", java.awt.Font.PLAIN, 14));
        btnModuloCarreras.setBounds(100, 210, 280, 50);
        btnModuloCarreras.addActionListener(evt -> {
            FlatLightLaf.setup();
            CarreraForm ventanaCarreras = new CarreraForm();
            ventanaCarreras.setLocationRelativeTo(this); // Centra respecto al menú
            ventanaCarreras.setVisible(true);
        });
        this.getContentPane().add(btnModuloCarreras);

        btnCerrarSesion = new javax.swing.JButton("Cerrar Sesión");
        btnCerrarSesion.setBounds(150, 300, 200, 42);
        btnCerrarSesion.addActionListener(evt -> {
            dispose();
            FlatDarkLaf.setup();
            LoginForm login = new LoginForm();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
        this.getContentPane().add(btnCerrarSesion);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            FlatDarkLaf.setup();
            LoginForm login = new LoginForm();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }
}
