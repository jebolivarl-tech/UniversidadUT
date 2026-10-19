package universidad.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import universidad.dao.CRUD;

/**
 * Ventana CRUD genérica del cliente de escritorio: se construye con una {@link Definicion}
 * (columnas de la tabla, campos del formulario y controles del módulo) y habla directo con la
 * base de datos local.
 */
public class FormularioCRUD<T> extends javax.swing.JFrame {

    /** Editor del formulario: convierte la entidad a texto y el texto de vuelta a la entidad. */
    public static final class Campo<T> {
        final String etiqueta;
        final Function<T, String> lectura;
        final BiConsumer<T, String> escritura;
        final boolean clave;
        final Supplier<List<OptionItem>> opciones;

        private Campo(String etiqueta, Function<T, String> lectura, BiConsumer<T, String> escritura,
                      boolean clave, Supplier<List<OptionItem>> opciones) {
            this.etiqueta = etiqueta;
            this.lectura = lectura;
            this.escritura = escritura;
            this.clave = clave;
            this.opciones = opciones;
        }

        public static <T> Campo<T> texto(String etiqueta, Function<T, String> lectura, BiConsumer<T, String> escritura) {
            return new Campo<>(etiqueta, lectura, escritura, false, null);
        }

        /** Campo ofuscado: se usa para la contraseña de las cuentas. */
        public static <T> Campo<T> clave(String etiqueta, BiConsumer<T, String> escritura) {
            return new Campo<>(etiqueta, t -> "", escritura, true, null);
        }

        public static <T> Campo<T> lista(String etiqueta, Function<T, String> lectura,
                                         BiConsumer<T, String> escritura, Supplier<List<OptionItem>> opciones) {
            return new Campo<>(etiqueta, lectura, escritura, false, opciones);
        }
    }

    /** Valor de una lista desplegable: entrega el id numérico o el texto según el tipo de opción. */
    public static final class OptionItem {
        private final int id;
        private final String texto;

        public OptionItem(int id, String texto) {
            this.id = id;
            this.texto = texto;
        }

        /** Opción fija (estados, sí/no) donde el valor guardado es el propio texto. */
        public static OptionItem texto(String texto) {
            return new OptionItem(-1, texto);
        }

        public String valor() {
            return id < 0 ? texto : String.valueOf(id);
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    /** Definición completa de un módulo: dao, clave primaria, listado, tabla y formulario. */
    public static final class Definicion<T> {
        final String titulo;
        final CRUD<T> dao;
        final Supplier<T> nueva;
        final Supplier<List<T>> listado;
        final BiConsumer<T, Integer> fijarId;
        final Consumer<T> antesEscribir;
        final List<String> cabeceras;
        final List<Function<T, ?>> columnas;
        final List<Campo<T>> campos;
        final Predicate<T> valido;
        final String mensajeInvalido;
        final boolean soloLectura;

        private Definicion(Builder<T> b) {
            this.titulo = b.titulo;
            this.dao = b.dao;
            this.nueva = b.nueva;
            this.listado = b.listado != null ? b.listado : b.dao::listarTodos;
            this.fijarId = b.fijarId;
            this.antesEscribir = b.antesEscribir;
            this.cabeceras = b.cabeceras;
            this.columnas = b.columnas;
            this.campos = b.campos;
            this.valido = b.valido;
            this.mensajeInvalido = b.mensajeInvalido;
            this.soloLectura = b.soloLectura;
        }

        public static <T> Builder<T> de(String titulo, CRUD<T> dao, Supplier<T> nueva) {
            return new Builder<>(titulo, dao, nueva);
        }

        public static final class Builder<T> {
            private final String titulo;
            private final CRUD<T> dao;
            private final Supplier<T> nueva;
            private Supplier<List<T>> listado;
            private BiConsumer<T, Integer> fijarId = (t, id) -> { };
            private Consumer<T> antesEscribir = t -> { };
            private final List<String> cabeceras = new ArrayList<>();
            private final List<Function<T, ?>> columnas = new ArrayList<>();
            private final List<Campo<T>> campos = new ArrayList<>();
            private Predicate<T> valido = t -> true;
            private String mensajeInvalido = "Revise los campos del formulario.";
            private boolean soloLectura;

            private Builder(String titulo, CRUD<T> dao, Supplier<T> nueva) {
                this.titulo = titulo;
                this.dao = dao;
                this.nueva = nueva;
            }

            public Builder<T> id(BiConsumer<T, Integer> fijarId) {
                this.fijarId = fijarId;
                return this;
            }

            /** Listado propio del módulo: se usa para limitar lo que ve cada rol. */
            public Builder<T> listado(Supplier<List<T>> listado) {
                this.listado = listado;
                return this;
            }

            /** Comprobaciones de integridad antes de registrar o actualizar. */
            public Builder<T> antesEscribir(Consumer<T> control) {
                this.antesEscribir = this.antesEscribir.andThen(control);
                return this;
            }

            public Builder<T> columna(String cabecera, Function<T, ?> lector) {
                cabeceras.add(cabecera);
                columnas.add(lector);
                return this;
            }

            public Builder<T> campo(Campo<T> campo) {
                campos.add(campo);
                return this;
            }

            public Builder<T> valido(Predicate<T> valido, String mensaje) {
                this.valido = valido;
                this.mensajeInvalido = mensaje;
                return this;
            }

            public Builder<T> soloLectura(boolean valor) {
                this.soloLectura = valor;
                return this;
            }

            public Definicion<T> construir() {
                return new Definicion<>(this);
            }
        }
    }

    private final Definicion<T> definicion;
    private final List<Object> editores = new ArrayList<>();
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JPanel panelFormulario = new JPanel(new GridLayout(0, 2, 8, 8));

    public FormularioCRUD(Definicion<T> definicion) {
        this.definicion = definicion;
        this.modelo = new DefaultTableModel(new Object[][] {}, definicion.cabeceras.toArray(new String[0])) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabla = new JTable(modelo);
        construirVentana();
        recargar();
    }

    private void construirVentana() {
        setTitle("Sistema Académico - " + definicion.titulo);
        setSize(940, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel superior = new JPanel();
        superior.setLayout(new javax.swing.BoxLayout(superior, javax.swing.BoxLayout.PAGE_AXIS));

        if (!definicion.soloLectura) {
            for (Campo<T> campo : definicion.campos) {
                panelFormulario.add(new JLabel(campo.etiqueta));
                panelFormulario.add(crearEditor(campo));
            }
            JScrollPane scrollFormulario = new JScrollPane(panelFormulario);
            scrollFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Formulario"));
            superior.add(scrollFormulario);
        }

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (definicion.soloLectura) {
            botones.add(new JLabel("Este módulo es de solo lectura para su rol."));
        } else {
            botones.add(boton("Registrar", evt -> registrar()));
            botones.add(boton("Actualizar", evt -> actualizar()));
            botones.add(boton("Eliminar", evt -> eliminar()));
            botones.add(boton("Limpiar", evt -> limpiar()));
        }
        botones.add(boton("Recargar", evt -> recargar()));
        superior.add(botones);
        add(superior, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                seleccionarFila();
            }
        });
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(javax.swing.BorderFactory.createTitledBorder(definicion.titulo));
        add(scrollTabla, BorderLayout.CENTER);
    }

    private javax.swing.JComponent crearEditor(Campo<T> campo) {
        javax.swing.JComponent editor;
        if (campo.opciones != null) {
            editor = new JComboBox<>(campo.opciones.get().toArray(new OptionItem[0]));
        } else if (campo.clave) {
            editor = new JPasswordField(24);
        } else {
            editor = new JTextField(24);
        }
        editores.add(editor);
        return editor;
    }

    private JButton boton(String texto, java.awt.event.ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(accion);
        return boton;
    }

    private String textoDe(int indice) {
        Object editor = editores.get(indice);
        if (editor instanceof JComboBox) {
            Object seleccion = ((JComboBox<?>) editor).getSelectedItem();
            return seleccion == null ? "" : ((OptionItem) seleccion).valor();
        }
        if (editor instanceof JPasswordField) {
            return new String(((JPasswordField) editor).getPassword()).trim();
        }
        return ((JTextField) editor).getText().trim();
    }

    private T desdeFormulario() {
        T entidad = definicion.nueva.get();
        if (!definicion.soloLectura) {
            definicion.fijarId.accept(entidad, 0);
        }
        for (int i = 0; i < definicion.campos.size(); i++) {
            definicion.campos.get(i).escritura.accept(entidad, textoDe(i));
        }
        return entidad;
    }

    private void seleccionarFila() {
        if (definicion.soloLectura) {
            return;
        }
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Integer id = idDeFila(fila);
        if (id == null) {
            return;
        }
        T entidad = definicion.dao.leerPorId(id);
        if (entidad == null) {
            avisar("El registro ya no existe en la base de datos.");
            recargar();
            return;
        }
        for (int i = 0; i < definicion.campos.size(); i++) {
            Campo<T> campo = definicion.campos.get(i);
            Object editor = editores.get(i);
            if (editor instanceof JComboBox) {
                String valor = campo.lectura.apply(entidad);
                JComboBox<OptionItem> combo = (JComboBox<OptionItem>) editor;
                for (int j = 0; j < combo.getItemCount(); j++) {
                    if (combo.getItemAt(j).valor().equals(valor)) {
                        combo.setSelectedIndex(j);
                        break;
                    }
                }
            } else if (editor instanceof JPasswordField) {
                ((JPasswordField) editor).setText("");
            } else {
                ((JTextField) editor).setText(campo.lectura.apply(entidad));
            }
        }
    }

    private Integer idDeFila(int fila) {
        try {
            return Integer.valueOf(String.valueOf(modelo.getValueAt(fila, 0)).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void registrar() {
        try {
            T entidad = desdeFormulario();
            if (!definicion.valido.test(entidad)) {
                avisar(definicion.mensajeInvalido);
                return;
            }
            definicion.antesEscribir.accept(entidad);
            if (definicion.dao.registrar(entidad) == 0) {
                throw new IllegalStateException("La base de datos no creó ningún registro.");
            }
            limpiar();
            recargar();
            avisar("Registro creado.");
        } catch (Exception ex) {
            avisar("No se pudo registrar: " + causa(ex));
        }
    }

    private void actualizar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            avisar("Seleccione un registro de la tabla para modificarlo.");
            return;
        }
        try {
            T entidad = desdeFormulario();
            definicion.fijarId.accept(entidad, idDeFila(fila));
            if (!definicion.valido.test(entidad)) {
                avisar(definicion.mensajeInvalido);
                return;
            }
            definicion.antesEscribir.accept(entidad);
            if (definicion.dao.actualizar(entidad) == 0) {
                avisar("No hubo cambios en el registro.");
                return;
            }
            limpiar();
            recargar();
            avisar("Registro actualizado.");
        } catch (Exception ex) {
            avisar("No se pudo actualizar: " + causa(ex));
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            avisar("Seleccione un registro de la tabla para eliminarlo.");
            return;
        }
        int confirmado = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este registro? Los datos vinculados pueden eliminarse en cascada.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmado != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (definicion.dao.eliminar(idDeFila(fila)) == 0) {
                avisar("El registro ya no existe.");
                recargar();
                return;
            }
            limpiar();
            recargar();
            avisar("Registro eliminado.");
        } catch (Exception ex) {
            avisar("No se pudo eliminar: " + causa(ex));
        }
    }

    private void limpiar() {
        for (Object editor : editores) {
            if (editor instanceof JComboBox) {
                if (((JComboBox<?>) editor).getItemCount() > 0) {
                    ((JComboBox<?>) editor).setSelectedIndex(0);
                }
            } else if (editor instanceof JPasswordField) {
                ((JPasswordField) editor).setText("");
            } else {
                ((JTextField) editor).setText("");
            }
        }
        tabla.clearSelection();
    }

    private void recargar() {
        modelo.setRowCount(0);
        try {
            for (T entidad : definicion.listado.get()) {
                Object[] fila = new Object[definicion.columnas.size()];
                for (int i = 0; i < definicion.columnas.size(); i++) {
                    Object valor = definicion.columnas.get(i).apply(entidad);
                    fila[i] = valor instanceof Date ? valor.toString() : valor;
                }
                modelo.addRow(fila);
            }
        } catch (Exception ex) {
            avisar("No se pudo consultar la base de datos: " + causa(ex));
        }
    }

    private static String causa(Throwable ex) {
        Throwable raiz = ex;
        while (raiz.getCause() != null) {
            raiz = raiz.getCause();
        }
        return raiz.getMessage() == null ? ex.toString() : raiz.getMessage();
    }

    private void avisar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }
}
