package universidad;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DatePickerSettings.DateArea;
import universidad.dao.CarreraDAO;
import universidad.dao.CarreraDAOImpl;
import universidad.dao.EstudianteDAO;
import universidad.dao.EstudianteDAOImpl;
import universidad.modelo.Carrera;
import universidad.modelo.Estudiante;

/** Ventana de escritorio del catálogo y registro de estudiantes, con tabla filtrable y KPIs. */
public class EstudianteForm extends javax.swing.JFrame {

    private static final Color AZUL = new Color(23, 37, 84);
    private static final Color AZUL_MEDIO = new Color(37, 68, 160);
    private static final Color AZUL_CLARO = new Color(226, 234, 252);
    private static final Color NARANJA = new Color(217, 119, 6);
    private static final Color ROJO = new Color(220, 38, 38);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color FONDO = new Color(244, 246, 250);
    private static final Color BORDE = new Color(226, 232, 240);
    private static final Color TEXTO = new Color(30, 41, 59);
    private static final Font FUENTE = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FUENTE_B = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FUENTE_S = new Font("Segoe UI", Font.BOLD, 11);

    // Colores de las etiquetas de carrera (fondo, texto)
    private static final Color[][] PILLS = {
        {new Color(219, 234, 254), new Color(30, 64, 175)},
        {new Color(209, 250, 229), new Color(4, 120, 87)},
        {new Color(254, 226, 226), new Color(153, 27, 27)},
        {new Color(254, 243, 199), new Color(146, 64, 14)},
        {new Color(237, 233, 254), new Color(91, 33, 182)},
        {new Color(207, 250, 254), new Color(14, 116, 144)}
    };

    private final EstudianteDAO estudianteDAO = new EstudianteDAOImpl();
    private final CarreraDAO carreraDAO = new CarreraDAOImpl();
    private final Map<Integer, String> nombresCarrera = new HashMap<>();

    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTable tblEstudiantes;
    private JLabel lblContador;

    private JLabel kpiTotal, kpiCarreras, kpiVisibles, kpiEdad;

    private CampoConPista txtId, txtNombre, txtApellido, txtEmail;
    private DatePicker dpFechaNacimiento;
    private JComboBox<Carrera> cbxCarreraForm;

    private CampoConPista txtBuscar, txtDesde, txtHasta;
    private JComboBox<String> cbxBuscarEn;
    private JComboBox<Object> cbxFiltroCarrera;

    public EstudianteForm() {
        initComponentsCustom();
        cargarComboCarreras();
        cargarDatosTabla();
        setLocationRelativeTo(null);
    }

    static class PanelTarjeta extends JPanel {
        private final int arc;

        PanelTarjeta(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class BotonRedondeado extends JButton {
        BotonRedondeado(String texto, Color fondo, Color letra) {
            super(texto);
            setBackground(fondo);
            setForeground(letra);
            setFont(FUENTE_B);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isPressed() ? getBackground().darker() : getBackground();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class CampoConPista extends JTextField {
        private final String hint;

        CampoConPista(String hint) {
            this.hint = hint;
            setFont(FUENTE);
            setForeground(TEXTO);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDE),
                    new EmptyBorder(4, 10, 4, 10)));
            setPreferredSize(new Dimension(100, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (hint != null && getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    static class EtiquetaCarrera extends JLabel {
        private Color fondo = AZUL_CLARO;

        EtiquetaCarrera() {
            setFont(FUENTE_S);
            setBorder(new EmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        void setColores(Color bg, Color fg) {
            fondo = bg;
            setForeground(fg);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void initComponentsCustom() {
        setTitle("Sistema Académico - Gestión de Estudiantes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(1150, 720));
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());

        add(crearSidebar(), BorderLayout.WEST);
        add(crearContenido(), BorderLayout.CENTER);
    }

    private JPanel crearSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(230, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, BORDE), new EmptyBorder(20, 16, 16, 16)));

        JLabel logo = new JLabel("Universidad del Tolima");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setForeground(AZUL);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("SISTEMA ACADÉMICO");
        sub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sub.setForeground(GRIS);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);
        side.add(sub);
        side.add(Box.createVerticalStrut(28));

        side.add(crearItemMenu("Panel de Control", false, e -> dispose()));
        side.add(Box.createVerticalStrut(6));
        side.add(crearItemMenu("Gestión de Estudiantes", true, null));
        side.add(Box.createVerticalStrut(6));
        side.add(crearItemMenu("Carreras y Facultades", false, e -> {
            FlatLightLaf.setup();
            CarreraForm cf = new CarreraForm();
            cf.setLocationRelativeTo(null);
            cf.setVisible(true);
        }));
        side.add(Box.createVerticalGlue());

        JLabel bd = new JLabel("Base de datos: MySQL");
        bd.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bd.setForeground(GRIS);
        bd.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(bd);
        return side;
    }

    private JButton crearItemMenu(String texto, boolean activo, java.awt.event.ActionListener accion) {
        BotonRedondeado boton = new BotonRedondeado(texto, activo ? AZUL_CLARO : Color.WHITE, activo ? AZUL_MEDIO : TEXTO);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setBorder(new EmptyBorder(0, 14, 0, 0));
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        boton.setPreferredSize(new Dimension(200, 40));
        if (accion != null) {
            boton.addActionListener(accion);
        }
        return boton;
    }

    private JPanel crearContenido() {
        JPanel cont = new JPanel(new BorderLayout());
        cont.setOpaque(false);
        cont.add(crearTopBar(), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 16));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel arriba = new JPanel();
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.setOpaque(false);

        JLabel titulo = new JLabel("Registro y Catálogo de Estudiantes");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titulo.setForeground(AZUL);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel desc = new JLabel("Consulta, registra y actualiza estudiantes, con búsqueda y filtros por carrera y fecha.");
        desc.setFont(FUENTE);
        desc.setForeground(GRIS);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        arriba.add(titulo);
        arriba.add(Box.createVerticalStrut(2));
        arriba.add(desc);
        arriba.add(Box.createVerticalStrut(14));

        kpiTotal = new JLabel("0");
        kpiCarreras = new JLabel("0");
        kpiVisibles = new JLabel("0");
        kpiEdad = new JLabel("-");
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        kpis.add(crearKpi("TOTAL ESTUDIANTES", kpiTotal, "Registrados en el sistema"));
        kpis.add(crearKpi("CARRERAS VINCULADAS", kpiCarreras, "Programas disponibles"));
        kpis.add(crearKpi("RESULTADOS FILTRADOS", kpiVisibles, "Coinciden con la búsqueda"));
        kpis.add(crearKpi("EDAD PROMEDIO", kpiEdad, "Según fecha de nacimiento"));
        arriba.add(kpis);

        cuerpo.add(arriba, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(crearPanelFormulario(), BorderLayout.WEST);
        centro.add(crearPanelTabla(), BorderLayout.CENTER);
        cuerpo.add(centro, BorderLayout.CENTER);

        cont.add(cuerpo, BorderLayout.CENTER);
        return cont;
    }

    private JPanel crearTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDE), new EmptyBorder(14, 24, 14, 24)));
        JLabel izq = new JLabel("Sistema Académico  /  Gestión de Estudiantes");
        izq.setFont(FUENTE_B);
        izq.setForeground(GRIS);
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale.forLanguageTag("es-CO")));
        JLabel der = new JLabel(hoy);
        der.setFont(FUENTE);
        der.setForeground(GRIS);
        bar.add(izq, BorderLayout.WEST);
        bar.add(der, BorderLayout.EAST);
        return bar;
    }

    private JPanel crearKpi(String titulo, JLabel valor, String pie) {
        PanelTarjeta tarjeta = new PanelTarjeta(new BorderLayout(), 16);
        tarjeta.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        etiquetaTitulo.setForeground(GRIS);
        valor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valor.setForeground(AZUL);
        JLabel etiquetaPie = new JLabel(pie);
        etiquetaPie.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        etiquetaPie.setForeground(GRIS);
        tarjeta.add(etiquetaTitulo, BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        tarjeta.add(etiquetaPie, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel crearPanelFormulario() {
        PanelTarjeta pnl = new PanelTarjeta(new GridBagLayout(), 16);
        pnl.setPreferredSize(new Dimension(340, 0));
        pnl.setBorder(new EmptyBorder(16, 18, 16, 18));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel t = new JLabel("Formulario Estudiante");
        t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        t.setForeground(AZUL);
        g.gridy = 0;
        g.insets = new Insets(0, 0, 4, 0);
        pnl.add(t, g);

        txtId = new CampoConPista(null);
        txtId.setEditable(false);
        txtId.setBackground(new Color(241, 245, 249));
        txtNombre = new CampoConPista("Ej: Valeria Sofía");
        txtApellido = new CampoConPista("Ej: Navarro Morales");
        txtEmail = new CampoConPista("correo@ejemplo.com");
        DatePickerSettings configuracionFecha = new DatePickerSettings(Locale.forLanguageTag("es-CO"));
        configuracionFecha.setFormatForDatesCommonEra("dd/MM/uuuu");
        configuracionFecha.setAllowEmptyDates(false);
        configuracionFecha.setFontValidDate(FUENTE);
        configuracionFecha.setColor(DateArea.DatePickerTextValidDate, TEXTO);
        configuracionFecha.setColor(DateArea.TextFieldBackgroundValidDate, Color.WHITE);
        dpFechaNacimiento = new DatePicker(configuracionFecha);
        dpFechaNacimiento.setDate(LocalDate.now());
        dpFechaNacimiento.getComponentDateTextField().setBorder(
                BorderFactory.createEmptyBorder(4, 10, 4, 10));
        dpFechaNacimiento.setBorder(BorderFactory.createLineBorder(BORDE));
        cbxCarreraForm = new JComboBox<>();
        cbxCarreraForm.setFont(FUENTE);

        int fila = 1;
        fila = agregarFila(pnl, g, fila, "ID (automático)", txtId);
        fila = agregarFila(pnl, g, fila, "Nombre(s) *", txtNombre);
        fila = agregarFila(pnl, g, fila, "Apellido(s) *", txtApellido);
        fila = agregarFila(pnl, g, fila, "Correo electrónico", txtEmail);
        fila = agregarFila(pnl, g, fila, "Fecha de nacimiento", dpFechaNacimiento);
        dpFechaNacimiento.setPreferredSize(new Dimension(260, 34));
        fila = agregarFila(pnl, g, fila, "Carrera universitaria *", cbxCarreraForm);

        BotonRedondeado btnGuardar = new BotonRedondeado("Registrar", AZUL, Color.WHITE);
        btnGuardar.addActionListener(e -> accionGuardar());
        BotonRedondeado btnModificar = new BotonRedondeado("Actualizar", NARANJA, Color.WHITE);
        btnModificar.addActionListener(e -> accionModificar());
        BotonRedondeado btnEliminar = new BotonRedondeado("Eliminar", ROJO, Color.WHITE);
        btnEliminar.addActionListener(e -> accionEliminar());
        BotonRedondeado btnNuevo = new BotonRedondeado("Limpiar campos", GRIS, Color.WHITE);
        btnNuevo.addActionListener(e -> limpiarCampos());

        JPanel botones = new JPanel(new GridLayout(2, 2, 8, 8));
        botones.setOpaque(false);
        for (JButton b : new JButton[]{btnGuardar, btnModificar, btnEliminar, btnNuevo}) {
            b.setPreferredSize(new Dimension(100, 38));
            botones.add(b);
        }
        g.gridy = fila;
        g.insets = new Insets(16, 0, 0, 0);
        pnl.add(botones, g);

        g.gridy = fila + 1;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        pnl.add(Box.createVerticalGlue(), g);
        return pnl;
    }

    private int agregarFila(JPanel pnl, GridBagConstraints g, int fila, String etiqueta, JComponent campo) {
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXTO);
        g.gridy = fila;
        g.insets = new Insets(8, 0, 3, 0);
        pnl.add(lbl, g);
        g.gridy = fila + 1;
        g.insets = new Insets(0, 0, 0, 0);
        campo.setPreferredSize(new Dimension(100, 34));
        pnl.add(campo, g);
        return fila + 2;
    }

    private JPanel crearPanelTabla() {
        PanelTarjeta panelTabla = new PanelTarjeta(new BorderLayout(0, 10), 16);
        panelTabla.setBorder(new EmptyBorder(14, 14, 10, 14));
        panelTabla.add(crearPanelFiltros(), BorderLayout.NORTH);

        // La columna 6 (IdCarrera) no se pinta: la tabla la usa para saber qué carrera guardar
        modeloTabla = new DefaultTableModel(
                new Object[]{"ID", "NOMBRE", "APELLIDO", "CORREO", "F. NACIMIENTO", "CARRERA", "IdCarrera"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblEstudiantes = new JTable(modeloTabla);
        tblEstudiantes.setRowHeight(40);
        tblEstudiantes.setFont(FUENTE);
        tblEstudiantes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEstudiantes.setShowGrid(false);
        tblEstudiantes.setIntercellSpacing(new Dimension(0, 0));
        tblEstudiantes.setFillsViewportHeight(true);

        JTableHeader encabezado = tblEstudiantes.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setPreferredSize(new Dimension(100, 34));
        DefaultTableCellRenderer renderEncabezado = new DefaultTableCellRenderer();
        renderEncabezado.setBackground(new Color(241, 245, 249));
        renderEncabezado.setForeground(GRIS);
        renderEncabezado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        renderEncabezado.setBorder(new EmptyBorder(0, 12, 0, 8));
        encabezado.setDefaultRenderer(renderEncabezado);

        tblEstudiantes.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                setBorder(BorderFactory.createCompoundBorder(
                        new MatteBorder(0, 0, 1, 0, BORDE), new EmptyBorder(0, 12, 0, 8)));
                setForeground(TEXTO);
                setBackground(isSelected ? AZUL_CLARO : Color.WHITE);
                return this;
            }
        });

        tblEstudiantes.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            private final EtiquetaCarrera etiquetaCarrera = new EtiquetaCarrera();
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

            {
                panel.add(etiquetaCarrera);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                int modelRow = table.convertRowIndexToModel(row);
                int idCarrera = ((Number) modeloTabla.getValueAt(modelRow, 6)).intValue();
                Color[] colores = PILLS[Math.abs(idCarrera) % PILLS.length];
                etiquetaCarrera.setText(value == null ? "" : value.toString());
                etiquetaCarrera.setColores(colores[0], colores[1]);
                panel.setBackground(isSelected ? AZUL_CLARO : Color.WHITE);
                panel.setBorder(new MatteBorder(0, 0, 1, 0, BORDE));
                return panel;
            }
        });

        tblEstudiantes.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblEstudiantes.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblEstudiantes.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblEstudiantes.getColumnModel().getColumn(3).setPreferredWidth(200);
        tblEstudiantes.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblEstudiantes.getColumnModel().getColumn(5).setPreferredWidth(190);
        tblEstudiantes.removeColumn(tblEstudiantes.getColumnModel().getColumn(6));

        sorter = new TableRowSorter<>(modeloTabla);
        tblEstudiantes.setRowSorter(sorter);

        tblEstudiantes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblEstudiantes.getSelectedRow() != -1) {
                cargarFilaSeleccionada();
            }
        });

        JScrollPane scroll = new JScrollPane(tblEstudiantes);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(Color.WHITE);
        panelTabla.add(scroll, BorderLayout.CENTER);

        lblContador = new JLabel("Mostrando 0 de 0 estudiantes");
        lblContador.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblContador.setForeground(GRIS);
        panelTabla.add(lblContador, BorderLayout.SOUTH);
        return panelTabla;
    }

    private JPanel crearPanelFiltros() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 3, 3, 3);

        txtBuscar = new CampoConPista("Buscar por ID, nombre, apellido o correo...");
        txtBuscar.addActionListener(e -> aplicarFiltros());
        cbxFiltroCarrera = new JComboBox<>();
        cbxFiltroCarrera.setFont(FUENTE);
        cbxFiltroCarrera.addItem("Todas las carreras");
        BotonRedondeado btnBuscar = new BotonRedondeado("Buscar", AZUL, Color.WHITE);
        btnBuscar.addActionListener(e -> aplicarFiltros());
        BotonRedondeado btnReset = new BotonRedondeado("Restablecer", GRIS, Color.WHITE);
        btnReset.addActionListener(e -> restablecerFiltros());

        g.gridy = 0;
        g.gridx = 0;
        g.weightx = 1;
        pnl.add(txtBuscar, g);
        g.gridx = 1;
        g.weightx = 0.5;
        pnl.add(cbxFiltroCarrera, g);
        g.gridx = 2;
        g.weightx = 0;
        btnBuscar.setPreferredSize(new Dimension(95, 34));
        pnl.add(btnBuscar, g);
        g.gridx = 3;
        btnReset.setPreferredSize(new Dimension(110, 34));
        pnl.add(btnReset, g);

        cbxBuscarEn = new JComboBox<>(new String[]{"Buscar en: todos", "Buscar en: ID", "Buscar en: nombre",
            "Buscar en: apellido", "Buscar en: correo"});
        cbxBuscarEn.setFont(FUENTE);
        txtDesde = new CampoConPista("Nacidos desde AAAA-MM-DD");
        txtDesde.addActionListener(e -> aplicarFiltros());
        txtHasta = new CampoConPista("Nacidos hasta AAAA-MM-DD");
        txtHasta.addActionListener(e -> aplicarFiltros());

        g.gridy = 1;
        g.gridx = 0;
        g.weightx = 1;
        pnl.add(cbxBuscarEn, g);
        g.gridx = 1;
        g.weightx = 0.5;
        pnl.add(txtDesde, g);
        g.gridx = 2;
        g.gridwidth = 2;
        g.weightx = 0;
        pnl.add(txtHasta, g);
        return pnl;
    }

    private void aplicarFiltros() {
        final String texto = txtBuscar.getText().trim().toLowerCase();
        final int campo = cbxBuscarEn.getSelectedIndex(); // 0=todos,1=ID,2=Nombre,3=Apellido,4=Correo

        final Integer idCarreraFiltro = (cbxFiltroCarrera.getSelectedItem() instanceof Carrera)
                ? ((Carrera) cbxFiltroCarrera.getSelectedItem()).getIdCarrera() : null;

        final Date desde;
        final Date hasta;
        try {
            desde = txtDesde.getText().trim().isEmpty() ? null : Date.valueOf(txtDesde.getText().trim());
            hasta = txtHasta.getText().trim().isEmpty() ? null : Date.valueOf(txtHasta.getText().trim());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Formato de fecha inválido en los filtros. Use AAAA-MM-DD",
                    "Filtro de fechas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RowFilter<DefaultTableModel, Integer> filtro = new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!texto.isEmpty()) {
                    int[] cols = (campo == 0) ? new int[]{0, 1, 2, 3} : new int[]{campo - 1};
                    boolean coincide = false;
                    for (int c : cols) {
                        Object v = e.getValue(c);
                        if (v != null && v.toString().toLowerCase().contains(texto)) {
                            coincide = true;
                            break;
                        }
                    }
                    if (!coincide) {
                        return false;
                    }
                }
                if (idCarreraFiltro != null) {
                    Object v = e.getValue(6);
                    if (v == null || ((Number) v).intValue() != idCarreraFiltro) {
                        return false;
                    }
                }
                if (desde != null || hasta != null) {
                    Object v = e.getValue(4);
                    if (!(v instanceof java.util.Date)) {
                        return false;
                    }
                    java.util.Date f = (java.util.Date) v;
                    if (desde != null && f.before(desde)) {
                        return false;
                    }
                    if (hasta != null && f.after(hasta)) {
                        return false;
                    }
                }
                return true;
            }
        };

        sorter.setRowFilter(filtro);
        actualizarContador();
    }

    private void restablecerFiltros() {
        txtBuscar.setText("");
        txtDesde.setText("");
        txtHasta.setText("");
        cbxBuscarEn.setSelectedIndex(0);
        cbxFiltroCarrera.setSelectedIndex(0);
        sorter.setRowFilter(null);
        actualizarContador();
    }

    private void actualizarContador() {
        int visibles = tblEstudiantes.getRowCount();
        int total = modeloTabla.getRowCount();
        lblContador.setText("Mostrando " + visibles + " de " + total + " estudiantes");
        kpiTotal.setText(String.valueOf(total));
        kpiCarreras.setText(String.valueOf(nombresCarrera.size()));
        kpiVisibles.setText(String.valueOf(visibles));

        int suma = 0;
        int n = 0;
        for (int i = 0; i < total; i++) {
            Object v = modeloTabla.getValueAt(i, 4);
            if (v instanceof java.sql.Date) {
                suma += Period.between(((java.sql.Date) v).toLocalDate(), LocalDate.now()).getYears();
                n++;
            }
        }
        kpiEdad.setText(n == 0 ? "-" : String.format("%.1f años", suma / (double) n));
    }

    private void cargarComboCarreras() {
        try {
            cbxCarreraForm.removeAllItems();
            nombresCarrera.clear();
            List<Carrera> lista = carreraDAO.listarTodos();
            for (Carrera c : lista) {
                cbxCarreraForm.addItem(c);
                cbxFiltroCarrera.addItem(c);
                nombresCarrera.put(c.getIdCarrera(), c.getNombre());
            }
        } catch (Exception ex) {
            System.err.println("Error al cargar carreras: " + ex.getMessage());
        }
    }

    public void cargarDatosTabla() {
        modeloTabla.setRowCount(0);
        List<Estudiante> lista = estudianteDAO.listarTodos();
        for (Estudiante e : lista) {
            modeloTabla.addRow(new Object[]{
                e.getIdEstudiante(),
                e.getNombre(),
                e.getApellido(),
                e.getEmail(),
                e.getFechaNacimiento(),
                nombresCarrera.getOrDefault(e.getIdCarrera(), "Carrera " + e.getIdCarrera()),
                e.getIdCarrera()
            });
        }
        actualizarContador();
    }

    private void cargarFilaSeleccionada() {
        int vista = tblEstudiantes.getSelectedRow();
        if (vista < 0) {
            return;
        }
        int fila = tblEstudiantes.convertRowIndexToModel(vista);

        txtId.setText(String.valueOf(modeloTabla.getValueAt(fila, 0)));
        txtNombre.setText(String.valueOf(modeloTabla.getValueAt(fila, 1)));
        txtApellido.setText(String.valueOf(modeloTabla.getValueAt(fila, 2)));
        Object correo = modeloTabla.getValueAt(fila, 3);
        txtEmail.setText(correo == null ? "" : correo.toString());
        Object fecha = modeloTabla.getValueAt(fila, 4);
        establecerFechaNacimiento(fecha == null ? null : Date.valueOf(fecha.toString()));

        int idCarrera = ((Number) modeloTabla.getValueAt(fila, 6)).intValue();
        for (int i = 0; i < cbxCarreraForm.getItemCount(); i++) {
            if (cbxCarreraForm.getItemAt(i).getIdCarrera() == idCarrera) {
                cbxCarreraForm.setSelectedIndex(i);
                break;
            }
        }
    }

    private void accionGuardar() {
        try {
            String nom = txtNombre.getText().trim();
            String ape = txtApellido.getText().trim();
            String mail = txtEmail.getText().trim();
            Carrera carrera = (Carrera) cbxCarreraForm.getSelectedItem();

            if (nom.isEmpty() || ape.isEmpty() || carrera == null) {
                JOptionPane.showMessageDialog(this, "Complete los campos obligatorios (*).");
                return;
            }
            Date fecha = obtenerFechaNacimiento();

            Estudiante nuevo = new Estudiante(nom, ape, mail, fecha, carrera.getIdCarrera());
            if (estudianteDAO.registrar(nuevo) == 0) {
                JOptionPane.showMessageDialog(this, "No se pudo registrar el estudiante.");
                return;
            }

            JOptionPane.showMessageDialog(this, "¡Estudiante registrado correctamente!");
            cargarDatosTabla();
            limpiarCampos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar: " + ex.getMessage());
        }
    }

    private void accionModificar() {
        if (txtId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante de la tabla para actualizar.");
            return;
        }
        try {
            int id = Integer.parseInt(txtId.getText());
            String nom = txtNombre.getText().trim();
            String ape = txtApellido.getText().trim();
            String mail = txtEmail.getText().trim();
            Carrera carrera = (Carrera) cbxCarreraForm.getSelectedItem();

            if (nom.isEmpty() || ape.isEmpty() || carrera == null) {
                JOptionPane.showMessageDialog(this, "Complete los campos obligatorios (*).");
                return;
            }
            Date fecha = obtenerFechaNacimiento();

            Estudiante est = new Estudiante(nom, ape, mail, fecha, carrera.getIdCarrera());
            est.setIdEstudiante(id);
            if (estudianteDAO.actualizar(est) == 0) {
                JOptionPane.showMessageDialog(this, "No hubo cambios en los datos del estudiante.");
                return;
            }

            JOptionPane.showMessageDialog(this, "¡Estudiante actualizado correctamente!");
            cargarDatosTabla();
            limpiarCampos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage());
        }
    }

    private void accionEliminar() {
        if (txtId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un estudiante de la tabla para eliminar.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de eliminar al estudiante seleccionado?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = Integer.parseInt(txtId.getText());
                if (estudianteDAO.eliminar(id) == 0) {
                    JOptionPane.showMessageDialog(this, "El estudiante ya no existe.");
                    return;
                }
                JOptionPane.showMessageDialog(this, "Estudiante eliminado.");
                cargarDatosTabla();
                limpiarCampos();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage());
            }
        }
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        establecerFechaNacimiento(null);
        if (cbxCarreraForm.getItemCount() > 0) {
            cbxCarreraForm.setSelectedIndex(0);
        }
        tblEstudiantes.clearSelection();
    }

    private Date obtenerFechaNacimiento() {
        return Date.valueOf(dpFechaNacimiento.getDate());
    }

    private void establecerFechaNacimiento(Date fecha) {
        dpFechaNacimiento.setDate(fecha == null ? LocalDate.now() : fecha.toLocalDate());
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> {
            new EstudianteForm().setVisible(true);
        });
    }
}