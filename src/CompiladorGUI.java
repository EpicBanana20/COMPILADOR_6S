import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.Element;
import java.awt.*;

public class CompiladorGUI extends JFrame {

    // Paleta de colores
    private final Color BG_EDITOR = new Color(30, 30, 30);
    private final Color BG_PANEL = new Color(37, 37, 38);
    private final Color TEXTO_CLARO = new Color(220, 220, 220);
    private final Color BORDES = new Color(55, 55, 55);
    private final Color BG_SELECCION = new Color(40, 70, 100);
    private final Color BG_BOTON = new Color(25, 100, 150);
    private final Color SCROLL_THUMB = new Color(65, 65, 65);

    // Componentes de la interfaz
    private JTextArea editorCodigo;
    private JTable tablaTokens;
    private JTable tablaErrores;
    private JTable tablaPila; 
    private JLabel labelRutaArchivo;
    
    // Botones
    private JButton btnAbrir;
    private JButton btnCompilar;
    private JButton btnCrearXls;

    // Modelos de tabla
    private DefaultTableModel modeloTokens;
    private DefaultTableModel modeloErrores;
    private DefaultTableModel modeloPila;

    public CompiladorGUI() {
        super("Mi Compilador - Entorno de Desarrollo");
        UIManager.put("Panel.background", BG_PANEL); 
        inicializarComponentes();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void inicializarComponentes() {
        // --- 1. Panel Superior (Toolbar) ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        panelNorte.setBorder(new MatteBorder(0, 0, 1, 0, BORDES)); 
        
        btnAbrir = crearBotonSuave("Abrir Archivo");
        btnCompilar = crearBotonSuave("Compilar (Ejecutar)");
        btnCrearXls = crearBotonSuave("Crear .xls");
        
        labelRutaArchivo = new JLabel("Ningún archivo abierto");
        labelRutaArchivo.setForeground(new Color(150, 150, 150));
        labelRutaArchivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panelNorte.add(btnAbrir);
        panelNorte.add(btnCompilar);
        panelNorte.add(btnCrearXls);
        panelNorte.add(Box.createHorizontalStrut(20));
        panelNorte.add(labelRutaArchivo);

        // --- 2. Panel Central (Editor de código) ---
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBorder(new EmptyBorder(10, 10, 10, 10)); 
        
        editorCodigo = new JTextArea();
        editorCodigo.setFont(new Font("Consolas", Font.PLAIN, 16));
        editorCodigo.setBackground(BG_EDITOR);
        editorCodigo.setForeground(TEXTO_CLARO);
        editorCodigo.setCaretColor(Color.WHITE);
        editorCodigo.setMargin(new Insets(10, 10, 10, 10)); 
        
        JScrollPane scrollEditor = new JScrollPane(editorCodigo);
        scrollEditor.setBorder(BorderFactory.createEmptyBorder()); 
        aplicarScrollOscuro(scrollEditor); 
        
        TextLineNumber tln = new TextLineNumber(editorCodigo);
        scrollEditor.setRowHeaderView(tln);
        panelCentro.add(scrollEditor, BorderLayout.CENTER);

        // --- 3. Panel Inferior (Tablas) ---
        modeloTokens = new DefaultTableModel(new String[]{"Token", "Lexema", "Linea"}, 0);
        tablaTokens = crearTablaModerna(modeloTokens);
        
        modeloErrores = new DefaultTableModel(new String[]{"Token Error", "Descripción", "Lexema", "Tipo", "Linea"}, 0);
        tablaErrores = crearTablaModerna(modeloErrores);

        modeloPila = new DefaultTableModel(new String[]{"Clasificación", "Elemento", "Cantidad"}, 0);
        tablaPila = crearTablaModerna(modeloPila);

        JSplitPane splitDerecho = crearSplitPaneOscuro(
            crearPanelTabla("Tabla de Errores", tablaErrores, 250), 
            crearPanelTabla("Contadores", tablaPila, 250)
        );
        splitDerecho.setDividerLocation(300);

        JSplitPane splitPrincipal = crearSplitPaneOscuro(
            crearPanelTabla("Tokens Correctos", tablaTokens, 300), 
            splitDerecho
        );
        splitPrincipal.setDividerLocation(350);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setBorder(new EmptyBorder(0, 10, 10, 10)); 
        panelSur.setPreferredSize(new Dimension(0, 300));
        panelSur.add(splitPrincipal, BorderLayout.CENTER);

        // --- 4. Ensamblar todo ---
        Container cp = getContentPane();
        cp.add(panelNorte, BorderLayout.NORTH);
        cp.add(panelCentro, BorderLayout.CENTER);
        cp.add(panelSur, BorderLayout.SOUTH);
        
        // NOTA: Aquí ya no hay Event Listeners (lógica).
    }

    // --- MÉTODOS VISUALES (Se mantienen igual) ---
    private JSplitPane crearSplitPaneOscuro(Component izq, Component der) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setContinuousLayout(true);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(BG_PANEL);

        split.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(BG_PANEL); 
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        g.setColor(BORDES); 
                        g.drawLine(getSize().width / 2, 0, getSize().width / 2, getSize().height);
                    }
                };
            }
        });
        return split;
    }

    private JPanel crearPanelTabla(String titulo, JTable tabla, int minWidth) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 5, 0, 5)); 
        panel.setMinimumSize(new Dimension(minWidth, 100));
        
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(new Color(180, 180, 180));
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitulo.setBorder(new EmptyBorder(5, 10, 10, 5));
        
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder()); 
        scroll.getViewport().setBackground(BG_EDITOR);
        aplicarScrollOscuro(scroll); 
        
        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void aplicarScrollOscuro(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new ScrollBarPersonalizada());
        scroll.getHorizontalScrollBar().setUI(new ScrollBarPersonalizada());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        JPanel corner = new JPanel();
        corner.setBackground(BG_EDITOR);
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
    }

    private JTable crearTablaModerna(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setBackground(BG_EDITOR);
        tabla.setForeground(TEXTO_CLARO);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(30); 
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(BORDES);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(BG_SELECCION);
        tabla.setSelectionForeground(Color.WHITE);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10)); 
                return this;
            }
        };
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(45, 45, 45));
        header.setForeground(new Color(200, 200, 200));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 35)); 
        
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, BORDES), new EmptyBorder(0, 10, 0, 10)));
                setHorizontalAlignment(JLabel.LEFT);
                return this;
            }
        };
        header.setDefaultRenderer(headerRenderer);
        return tabla;
    }

    private JButton crearBotonSuave(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(BG_BOTON);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15)); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // --- CLASES AUXILIARES VISUALES ---
    public class TextLineNumber extends JPanel {
        private javax.swing.text.JTextComponent component;
        public TextLineNumber(javax.swing.text.JTextComponent component) {
            this.component = component;
            setBackground(BG_EDITOR); 
            setForeground(new Color(110, 110, 110)); 
            setFont(new Font("Consolas", Font.PLAIN, 14));
            setBorder(new EmptyBorder(10, 10, 10, 15)); 
            component.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
            });
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(component.getFont());
            g.setColor(getForeground());
            Element root = component.getDocument().getDefaultRootElement();
            int lineCount = root.getElementCount();
            for (int i = 0; i < lineCount; i++) {
                Element line = root.getElement(i);
                int startOffset = line.getStartOffset();
                try {
                    int lineY = component.modelToView(startOffset).y + component.getFontMetrics(component.getFont()).getAscent();
                    String lineNumber = String.valueOf(i + 1);
                    int stringWidth = g.getFontMetrics().stringWidth(lineNumber);
                    g.drawString(lineNumber, getWidth() - stringWidth - getInsets().right, lineY);
                } catch (Exception e) {}
            }
        }
        @Override public Dimension getPreferredSize() { return new Dimension(45, component.getPreferredSize().height); }
    }

    private class ScrollBarPersonalizada extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            this.thumbColor = SCROLL_THUMB; 
            this.trackColor = BG_EDITOR;    
        }
        @Override protected JButton createDecreaseButton(int orientation) { return crearBotonInvisible(); }
        @Override protected JButton createIncreaseButton(int orientation) { return crearBotonInvisible(); }
        private JButton crearBotonInvisible() {
            JButton boton = new JButton();
            boton.setPreferredSize(new Dimension(0, 0));
            return boton;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(BG_EDITOR);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
    }

    // =========================================================
    // GETTERS: La forma en que otras clases interactúan con la GUI
    // =========================================================
    
    public JTextArea getEditorCodigo() { return editorCodigo; }
    public JLabel getLabelRutaArchivo() { return labelRutaArchivo; }
    
    public JButton getBtnAbrir() { return btnAbrir; }
    public JButton getBtnCompilar() { return btnCompilar; }
    public JButton getBtnCrearXls() { return btnCrearXls; }
    
    public DefaultTableModel getModeloTokens() { return modeloTokens; }
    public DefaultTableModel getModeloErrores() { return modeloErrores; }
    public DefaultTableModel getModeloPila() { return modeloPila; }

    // Ya no necesitamos el método main() aquí, lo pondremos en tu clase principal o controlador.
}