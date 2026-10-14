package universidad;

import com.formdev.flatlaf.FlatLightLaf;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import universidad.dao.CarreraDAO;
import universidad.dao.CarreraDAOImpl;
import universidad.modelo.Carrera;

public class CarreraForm extends javax.swing.JFrame {

    // 1. Modelo de la interfaz gráfica
    private DefaultTableModel modeloTabla;
    private final CarreraDAO carreraDAO = new CarreraDAOImpl();

    // 2. Componentes visuales declarados manualmente
    private javax.swing.JLabel lblNombre, lblIdFacultad;
    private javax.swing.JTextField txtNombre, txtIdFacultad;
    private javax.swing.JButton btnRegistrar, btnActualizar, btnEliminar;
    private javax.swing.JTable tablaCarreras;
    private javax.swing.JScrollPane scrollTabla;

    public CarreraForm() {
        construirVentana();    // Diseña la interfaz manualmente en coordenadas exactas
        inicializarTabla();    // Configura la JTable con sus columnas
        listarEnTablaDirecto(); // Carga de forma autónoma las carreras desde MySQL
    }

    private void construirVentana() {
        this.setTitle("Sistema Académico - Gestión de Carreras");
        this.setSize(750, 600);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE); // Cierra solo esta ventana
        this.getContentPane().setLayout(null); // Layout absoluto libre de bloqueos

        // --- Etiquetas (Labels) ---
        lblNombre = new javax.swing.JLabel("Nombre de Carrera:");
        lblNombre.setBounds(30, 30, 150, 25);
        this.getContentPane().add(lblNombre);

        lblIdFacultad = new javax.swing.JLabel("ID Facultad:");
        lblIdFacultad.setBounds(30, 70, 100, 25);
        this.getContentPane().add(lblIdFacultad);

        // --- Cajas de Texto (TextFields) ---
        txtNombre = new javax.swing.JTextField();
        txtNombre.setBounds(180, 30, 200, 25);
        this.getContentPane().add(txtNombre);

        txtIdFacultad = new javax.swing.JTextField();
        txtIdFacultad.setBounds(180, 70, 200, 25);
        this.getContentPane().add(txtIdFacultad);

        // --- Botones ---
        btnRegistrar = new javax.swing.JButton("Registrar");
        btnRegistrar.setBounds(420, 30, 120, 30);
        btnRegistrar.addActionListener(evt -> btnRegistrarActionPerformed(evt));
        this.getContentPane().add(btnRegistrar);

        btnActualizar = new javax.swing.JButton("Actualizar");
        btnActualizar.setBounds(420, 80, 120, 30);
        btnActualizar.addActionListener(evt -> btnActualizarActionPerformed(evt));
        this.getContentPane().add(btnActualizar);

        btnEliminar = new javax.swing.JButton("Eliminar");
        btnEliminar.setBounds(420, 130, 120, 30);
        btnEliminar.addActionListener(evt -> btnEliminarActionPerformed(evt));
        this.getContentPane().add(btnEliminar);

        // --- Tabla ---
        tablaCarreras = new javax.swing.JTable();
        tablaCarreras.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tablaCarrerasMouseClicked(evt);
            }
        });
        
        scrollTabla = new javax.swing.JScrollPane(tablaCarreras);
        scrollTabla.setBounds(30, 200, 680, 330);
        this.getContentPane().add(scrollTabla);
    }

    private void inicializarTabla() {
        modeloTabla = new DefaultTableModel(
            new Object[][] {},
            new String[] { "ID Carrera", "Nombre de Carrera", "ID Facultad" }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaCarreras.setModel(modeloTabla);
    }

    public void listarEnTablaDirecto() {
        modeloTabla.setRowCount(0);
        try {
            List<Carrera> carreras = carreraDAO.listarTodos();
            for (Carrera carrera : carreras) {
                modeloTabla.addRow(new Object[]{
                    carrera.getIdCarrera(),
                    carrera.getNombre(),
                    carrera.getIdFacultad()
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Error al listar carreras en interfaz: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtIdFacultad.setText("");
    }

    // --- ACCIONES DE LOS BOTONES (EVENTOS DIRECTOS) ---

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {                                             
        try {
            if (txtNombre.getText().trim().isEmpty() || txtIdFacultad.getText().trim().isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "⚠️ Todos los campos son obligatorios.");
                return;
            }
            
            String nombre = txtNombre.getText().trim();
            int idFacultad = Integer.parseInt(txtIdFacultad.getText().trim());
            if (carreraDAO.registrar(new Carrera(nombre, idFacultad)) == 0) {
                throw new IllegalStateException("No se registró la carrera.");
            }

            listarEnTablaDirecto();
            limpiarCampos();
            javax.swing.JOptionPane.showMessageDialog(this, " ¡Carrera registrada con éxito!");
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, " Error al registrar: " + ex.getMessage());
        }
    }                                            

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {                                             
        int fila = tablaCarreras.getSelectedRow();
        if (fila == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, " Selecciona una carrera de la tabla para editar.");
            return;
        }
        try {
            int idCarrera = Integer.parseInt(tablaCarreras.getValueAt(fila, 0).toString());
            String nombre = txtNombre.getText().trim();
            int idFacultad = Integer.parseInt(txtIdFacultad.getText().trim());
            if (carreraDAO.actualizar(new Carrera(idCarrera, nombre, idFacultad)) == 0) {
                javax.swing.JOptionPane.showMessageDialog(this, "No hubo cambios en los datos de la carrera.");
                return;
            }

            listarEnTablaDirecto();
            limpiarCampos();
            javax.swing.JOptionPane.showMessageDialog(this, "🔄 ¡Carrera actualizada!");
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "❌ Error al modificar: " + ex.getMessage());
        }
    }                                            

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {                                           
        int fila = tablaCarreras.getSelectedRow();
        if (fila == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "⚠️ Selecciona una carrera de la tabla para eliminar.");
            return;
        }
        int confirma = javax.swing.JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar esta carrera?", "Confirmación", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirma == javax.swing.JOptionPane.YES_OPTION) {
            try {
                int id = Integer.parseInt(tablaCarreras.getValueAt(fila, 0).toString());
                if (carreraDAO.eliminar(id) == 0) {
                    javax.swing.JOptionPane.showMessageDialog(this, "La carrera ya no existe.");
                    return;
                }

                listarEnTablaDirecto();
                limpiarCampos();
                javax.swing.JOptionPane.showMessageDialog(this, "🗑️ Carrera eliminada.");
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this, "❌ No se puede eliminar la carrera. Verifique que no tenga estudiantes asociados.");
            }
        }
    }                                          

    private void tablaCarrerasMouseClicked(java.awt.event.MouseEvent evt) {                                              
        int fila = tablaCarreras.getSelectedRow();
        if (fila != -1) {
            txtNombre.setText(tablaCarreras.getValueAt(fila, 1).toString());
            txtIdFacultad.setText(tablaCarreras.getValueAt(fila, 2).toString());
        }
    }                                             

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            FlatLightLaf.setup();
            CarreraForm frame = new CarreraForm();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
