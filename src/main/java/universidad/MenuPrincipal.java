package universidad;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import universidad.modelo.Usuario;
import universidad.swing.ModulosSwing;
import universidad.swing.ModulosSwing.Modulo;

/** Panel de control del cliente de escritorio: muestra solo los módulos permitidos para el rol. */
public class MenuPrincipal extends javax.swing.JFrame {

    private static final int BOTONES_POR_FILA = 2;

    private final Usuario usuario;

    public MenuPrincipal(Usuario usuario) {
        this.usuario = usuario;
        construirMenu();
    }

    private void construirMenu() {
        setTitle("Sistema Universitario - Panel de Control");
        setSize(620, 520);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 12));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBorder(new EmptyBorder(18, 20, 8, 20));
        JLabel lblTitulo = new JLabel("PANEL DE CONTROL ACADÉMICO", javax.swing.SwingConstants.CENTER);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        JLabel lblUsuario = new JLabel(usuario.getUsername() + " · " + usuario.getRol(),
                javax.swing.SwingConstants.CENTER);
        lblUsuario.setForeground(new java.awt.Color(170, 175, 180));
        encabezado.add(lblTitulo, BorderLayout.NORTH);
        encabezado.add(lblUsuario, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        add(crearBotonera(), BorderLayout.CENTER);
        add(crearPie(), BorderLayout.SOUTH);
    }

    private javax.swing.JScrollPane crearBotonera() {
        List<Modulo> modulos = ModulosSwing.modulos(usuario.getRol(), usuario.getIdUsuario(),
                usuario.getIdEstudiante(), usuario.getIdProfesor());
        JPanel panel = new JPanel(new GridLayout(0, BOTONES_POR_FILA, 12, 12));
        panel.setBorder(new EmptyBorder(6, 20, 6, 20));
        for (Modulo modulo : modulos) {
            JButton boton = new JButton(modulo.titulo());
            boton.setMargin(new java.awt.Insets(14, 10, 14, 10));
            boton.addActionListener(evt -> abrir(modulo));
            panel.add(boton);
        }
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel crearPie() {
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.setBorder(new EmptyBorder(4, 20, 16, 20));
        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.addActionListener(evt -> cerrarSesion());
        pie.add(btnCerrarSesion);
        return pie;
    }

    private void abrir(Modulo modulo) {
        java.awt.EventQueue.invokeLater(() -> {
            javax.swing.JFrame ventana = modulo.crearVentana();
            ventana.setLocationRelativeTo(this);
            ventana.setVisible(true);
        });
    }

    private void cerrarSesion() {
        dispose();
        FlatDarkLaf.setup();
        LoginForm login = new LoginForm();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
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
