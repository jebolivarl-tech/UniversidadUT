package universidad;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import universidad.dao.UsuarioDAO;
import universidad.dao.UsuarioDAOImpl;
import universidad.modelo.Usuario;

public class LoginForm extends JFrame {

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtContrasena = new JPasswordField(20);
    private final JButton btnIngresar = new JButton("Ingresar");

    public LoginForm() {
        construirFormulario();
    }

    private void construirFormulario() {
        setTitle("Inicio de sesión - Sistema Universitario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        setContentPane(panel);

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 6, 6, 6);
        restricciones.anchor = GridBagConstraints.LINE_START;

        JLabel lblTitulo = new JLabel("Iniciar sesión");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(20f));
        restricciones.gridx = 0;
        restricciones.gridy = 0;
        restricciones.gridwidth = 2;
        panel.add(lblTitulo, restricciones);

        restricciones.gridwidth = 1;
        restricciones.gridy++;
        panel.add(new JLabel("Usuario:"), restricciones);
        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        panel.add(txtUsuario, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy++;
        restricciones.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Contraseña:"), restricciones);
        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        panel.add(txtContrasena, restricciones);

        JPanel botones = new JPanel();
        JButton btnSalir = new JButton("Salir");
        botones.add(btnSalir);
        botones.add(btnIngresar);
        restricciones.gridx = 0;
        restricciones.gridy++;
        restricciones.gridwidth = 2;
        restricciones.anchor = GridBagConstraints.LINE_END;
        restricciones.fill = GridBagConstraints.NONE;
        panel.add(botones, restricciones);

        btnIngresar.addActionListener(evento -> ingresar());
        btnSalir.addActionListener(evento -> dispose());
        getRootPane().setDefaultButton(btnIngresar);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        String username = txtUsuario.getText().trim();
        char[] password = txtContrasena.getPassword();

        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String passwordValue = new String(password);
        Arrays.fill(password, '\0');
        txtContrasena.setText("");
        btnIngresar.setEnabled(false);

        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() {
                return usuarioDAO.login(username, passwordValue);
            }

            @Override
            protected void done() {
                btnIngresar.setEnabled(true);
                try {
                    Usuario usuario = get();
                    if (usuario == null) {
                        JOptionPane.showMessageDialog(LoginForm.this,
                                "El usuario o la contraseña son incorrectos.",
                                "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                        txtContrasena.requestFocusInWindow();
                        return;
                    }

                    FlatDarkLaf.setup();
                    MenuPrincipal menu = new MenuPrincipal(usuario);
                    menu.setLocationRelativeTo(null);
                    menu.setVisible(true);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    mostrarErrorDeConexion();
                } catch (ExecutionException exception) {
                    mostrarErrorDeConexion(exception.getCause());
                }
            }
        }.execute();
    }

    private void mostrarErrorDeConexion() {
        JOptionPane.showMessageDialog(this,
                "No fue posible validar el acceso. Verifique la conexión con la base de datos.",
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorDeConexion(Throwable error) {
        Throwable causa = error;
        while (causa != null && causa.getCause() != null) {
            causa = causa.getCause();
        }

        String detalle = causa == null || causa.getMessage() == null
                ? "No se recibió detalle del controlador JDBC."
                : causa.getMessage();
        JOptionPane.showMessageDialog(this,
                "No fue posible validar el acceso.\nDetalle: " + detalle,
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
    }
}