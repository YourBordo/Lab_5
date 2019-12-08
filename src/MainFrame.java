
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;


public class MainFrame extends JFrame {

    // Объект диалогового окна для выбора файлов
    private JFileChooser fileChooser;

    // Флаг, указывающий на загруженность данных графика
    private boolean fileLoaded;

    // Компонент-отображатель графика
    private GraphicsDisplay display = new GraphicsDisplay();

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // Пункты меню
    private JMenuItem resetGraphicsMenuItem;
    private JCheckBoxMenuItem showAxisMenuItem;
    private JCheckBoxMenuItem showMarkersMenuItem;
    private JCheckBoxMenuItem shapeturnAction;
    private JMenuItem informationItem;
    private JMenuItem saveToTextMenuItem;

    public MainFrame(){
        super("Построение графиков функций на основе подготовленных файлов");
        setSize(WIDTH, HEIGHT);
        Toolkit kit = Toolkit.getDefaultToolkit();
        setLocation((kit.getScreenSize().width - WIDTH)/2,
                (kit.getScreenSize().height - HEIGHT)/2);
        // Развѐртывание окна на весь экран
        // setExtendedState(MAXIMIZED_BOTH);
        // Создать и установить полосу меню
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        // Добавить пункт меню "Файл"
        JMenu fileMenu = new JMenu("файл");
        menuBar.add(fileMenu);
        JMenu graphicsMenu = new JMenu("График");
        menuBar.add(graphicsMenu);
        // Создать действие по открытию файла
        Action openGraphicsAction = new AbstractAction("Открыть файл с графиком"){
            public void actionPerformed(ActionEvent arg0) {
                if (fileChooser==null) {
                    fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File("."));
                }
                if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION);
                openGraphics(fileChooser.getSelectedFile());
            }
        };
        // Добавить соответствующий элемент меню
        fileMenu.add(openGraphicsAction);
        // Создать пункт меню "График"

        Action showAxisAction = new AbstractAction("Показывать оси координат") {
            public void actionPerformed(ActionEvent e) {
                display.setShowAxis(showAxisMenuItem.isSelected());
            }
        };
        showAxisMenuItem = new JCheckBoxMenuItem(showAxisAction);
        graphicsMenu.add(showAxisMenuItem);
        showAxisMenuItem.setSelected(true);


        // Повторить действия для элемента "Показывать маркеры точек"
        Action showMarkersAction = new AbstractAction("Показывать маркеры точек") {

            public void actionPerformed(ActionEvent e) {
                // по аналогии с showAxisMenuItem
                display.setShowMarkers(showMarkersMenuItem.isSelected());
            }
        };
        showMarkersMenuItem = new JCheckBoxMenuItem(showMarkersAction);
        graphicsMenu.add(showMarkersMenuItem);
        showMarkersMenuItem.setSelected(true);
        graphicsMenu.addMenuListener(new GraphicsMenuListener());
        getContentPane().add(display, BorderLayout.CENTER);
        graphicsMenu.addSeparator();


        Action resetGraphicsAction = new AbstractAction("Отменить все изменения") {
            public void actionPerformed(ActionEvent event)
            { MainFrame.this.display.reset(); }
        };
        resetGraphicsMenuItem = new JMenuItem(resetGraphicsAction);
        graphicsMenu.add(resetGraphicsMenuItem);
        resetGraphicsMenuItem.setEnabled(false);
        graphicsMenu.addMenuListener(new GraphicsMenuListener());
        getContentPane().add(display, BorderLayout.CENTER);


    }

    // Считывание данных графика из существующего файла
    protected void openGraphics(File selectedFile) {
        try {

            DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));

            Double[][] graphicsData = new Double[in.available()/(Double.SIZE/8)/2][];
            int i = 0;
            while (in.available() > 0) {

                Double x = Double.valueOf(in.readDouble());
                Double y = Double.valueOf(in.readDouble());
                graphicsData[i++] = new Double[] {x, y};
            }
            // Шаг 4 - Проверка, имеется ли в списке в результате чтения
            // хотя бы одна пара координат
            if (graphicsData!=null && graphicsData.length>0) {
                fileLoaded = true;
                resetGraphicsMenuItem.setEnabled(true);
                display.showGraphics(graphicsData);
            }
            in.close();
        }catch (FileNotFoundException e){
            JOptionPane.showMessageDialog(MainFrame.this,
                    "Указанный файл не найден", "Ошибка загрузки данных",
                    JOptionPane.WARNING_MESSAGE);
            return;

        }catch (IOException e){
            // В случае ошибки ввода из файлового потока
            // показать сообщение об ошибке
            JOptionPane.showMessageDialog(MainFrame.this,
                    "Ошибка чтения координат точек из файла",
                    "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    // Класс-слушатель событий, связанных с отображением меню
    private class GraphicsMenuListener implements MenuListener {
        // Обработчик, вызываемый перед показом меню
        public void menuSelected(MenuEvent e) {
            // Доступность или недоступность элементов меню "График"
            // определяется загруженностью данных
            showAxisMenuItem.setEnabled(fileLoaded);
            showMarkersMenuItem.setEnabled(fileLoaded);
            shapeturnAction.setEnabled(fileLoaded);
        }

        public void menuDeselected(MenuEvent e) {
        }

        // Обработчик, вызываемый в случае отмены выбора пункта меню
        // (очень редкая ситуация)
        public void menuCanceled(MenuEvent e) {

        }

    }
}
