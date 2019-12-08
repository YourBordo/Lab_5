

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;


import static java.lang.Math.abs;

public class GraphicsDisplay extends JPanel {

    private BasicStroke deltaStroke;
    private double[][] viewport = new double[2][2];
    private Font axisFont;
    private ArrayList<double[][]> undoHistory = new ArrayList(5);
    private Font labelsFont;
    private int selectedMarker = -1;
    private Double[][] graphicsData;
    private Double[][] originalData;// список коорд точек для построения графика
    private BasicStroke axisStroke;
    private BasicStroke graphicsStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;
    private static DecimalFormat formatter=(DecimalFormat) NumberFormat.getInstance();
    private boolean scaleMode = false;
    private boolean changeMode = false;
    private double[] originalPoint = new double[2];
    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();
    // границы диапазона пространства,подлежащего отображению
    private double maxX;
    private double maxY;
    private double minX;
    private double minY;
    private double scaleX;
    private double scale;
    private double scaleY;
    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showTurn = true;

    public GraphicsDisplay() {

        setBackground(Color.WHITE);
        graphicsStroke = new BasicStroke(4.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 10.0f, new float[]{40, 10, 10, 10, 20, 10, 10, 10, 20, 40}, 0.0f);
        axisStroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[] { 10, 10 }, 0.0F);
        markerStroke = new BasicStroke(2.0f, BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        axisFont = new Font("Serif", Font.BOLD, 36);
        addMouseMotionListener(new MouseMotionHandler());
        addMouseListener(new MouseHandler());
        labelsFont = new java.awt.Font("Serif",0,10);
    }


    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData;
        this.originalData = new Double[graphicsData.length][];
        int i = 0;
        for (Double[] point : graphicsData){

            Double x = Double.valueOf(point[0]);
            Double y = Double.valueOf(point[1]);
            originalData[i++] = new Double[] {x, y};
        }

        this.minX = graphicsData[0][0];
        this.maxX = graphicsData[graphicsData.length - 1][0];
        this.minY = graphicsData[0][1];
        this.maxY = this.minY;
        for ( i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < this.minY) {
                this.minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > this.maxY) {
                this.maxY = graphicsData[i][1];
            }
        }
        zoomToRegion(minX, maxY, maxX, minY);
    }
    protected void paintDelta(Graphics2D canvas){
        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);
        Double step_x = (maxX - minX)/100;
        Double step_y = (maxY - minY)/100;
        int i = 0;
        canvas.setStroke(deltaStroke);
        canvas.setColor(Color.BLACK);
        canvas.setPaint(Color.BLACK);
        for (Double temp = minX; temp < maxX; temp += step_x){
            if (i % 5 == 0){
                canvas.draw(new Line2D.Double(xyToPoint(temp, step_y), xyToPoint(temp, -step_y)));
            }
            else{
                canvas.draw(new Line2D.Double(xyToPoint(temp, step_y/2), xyToPoint(temp, -step_y/2)));
            }
            i++;
        }
        i = 0;
        for (Double temp = minY; temp < maxY; temp += step_y){
            if (i % 5 == 0){
                canvas.draw(new Line2D.Double(xyToPoint(step_x, temp), xyToPoint(-step_x, temp)));
            }
            else{
                canvas.draw(new Line2D.Double(xyToPoint(step_x/2, temp), xyToPoint(-step_x/2, temp)));
            }
            i++;
        }
    }


    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + x / this.scaleX, this.viewport[0][1] - y / this.scaleY};
    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        // Вычисляем смещение X от самой левой точки (minX)
        double deltaX = x - viewport[0][0];
        double deltaY = viewport[0][1] - y;
        return new Point2D.Double(deltaX * scaleX, deltaY * scaleY);
    }

    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        // Инициализировать новый экземпляр точки
        Point2D.Double dest = new Point2D.Double();
        // Задать еѐ координаты как координаты существующей точки +
        // заданные смещения
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }

    protected void paintGraphics(Graphics2D canvas) {
        // Выбрать линию для рисования графика
        canvas.setStroke(graphicsStroke);
        canvas.setColor(Color.PINK);
        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            // Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (i > 0) {
                // Не первая итерация – вести линию в точку point
                graphics.lineTo(point.getX(), point.getY());
            } else {
                // Первая итерация - установить начало пути в точку point
                graphics.moveTo(point.getX(), point.getY());
            }
        }
        // Отобразить график
        canvas.draw(graphics);
    }

    protected void paintAxis(Graphics2D canvas) {
        // Шаг 1 – установить необходимые настройки рисования
        // Установить особое начертание для осей
        canvas.setStroke(axisStroke);
        // Оси рисуются чѐрным цветом
        canvas.setColor(Color.BLACK);
        // Стрелки заливаются чѐрным цветом
        canvas.setPaint(Color.BLACK);
        // Подписи к координатным осям делаются специальным шрифтом
        canvas.setFont(axisFont);
        // Создать объект контекста отображения текста - для получения
        // характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
        // Шаг 2 - Определить, должна ли быть видна ось Y на графике
        if (minX <= 0.0 && maxX >= 0.0) {
            // Она видна, если левая граница показываемой области minX<=0.0,
            // а правая (maxX) >= 0.0
            // Шаг 2а - ось Y - это линия между точками (0, maxY) и (0, minY)
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));
            // Шаг 2б - Стрелка оси Y
            GeneralPath arrow = new GeneralPath();
            // Установить начальную точку ломаной точно на верхний конец оси Y
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            // Вести левый "скат" стрелки в точку с относительными
            // координатами (5,20)
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5, arrow.getCurrentPoint().getY() + 20);
            // Вести нижнюю часть стрелки в точку с относительными
            // координатами (-10, 0)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10, arrow.getCurrentPoint().getY());
            // Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
            // Шаг 2в - Нарисовать подпись к оси Y
            // Определить, сколько места понадобится для надписи “y”
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
            // Вывести надпись в точке с вычисленными координатами
            canvas.drawString("y", (float) labelPos.getX() + 10,
                    (float) (labelPos.getY() - bounds.getY()));
        }
        // Шаг 3 - Определить, должна ли быть видна ось X на графике
        if (minY <= 0.0 && maxY >= 0.0) {
            // Она видна, если верхняя граница показываемой области max)>=0.0,
            // а нижняя (minY) <= 0.0
            // Шаг 3а - ось X - это линия между точками (minX, 0) и (maxX, 0)
            canvas.draw(new Line2D.Double(xyToPoint(minX, 0),
                    xyToPoint(maxX, 0)));
            // Шаг 3б - Стрелка оси X
            GeneralPath arrow = new GeneralPath();
            // Установить начальную точку ломаной точно на правый конец оси X
            Point2D.Double lineEnd = xyToPoint(maxX, 0);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            // Вести верхний "скат" стрелки в точку с относительными
            // координатами (-20,-5)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 20,
                    arrow.getCurrentPoint().getY() - 5);
            // Вести левую часть стрелки в точку
            // с относительными координатами (0, 10)
            arrow.lineTo(arrow.getCurrentPoint().getX(),
                    arrow.getCurrentPoint().getY() + 10);
            // Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
            // Шаг 3в - Нарисовать подпись к оси X
            // Определить, сколько места понадобится для надписи “x”
            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = xyToPoint(maxX, 0);
            Point2D.Double labelPos2 = xyToPoint(1, 0);
            canvas.drawString("",
                    (float) (labelPos2.getX()),
                    (float) (labelPos2.getY()));

            // Вывести надпись в точке с вычисленными координатами
            canvas.drawString("x",
                    (float) (labelPos.getX() - bounds.getWidth() - 10),
                    (float) (labelPos.getY() + bounds.getY()) - 10);
        }
    }

    protected int findSelectedPoint(int x, int y) {
        if (graphicsData == null) return -1;
        int pos = 0;
        for (Double[] point : graphicsData) {
            Point2D.Double screenPoint = xyToPoint(point[0], point[1]);
            double distance = (screenPoint.getX() - x) * (screenPoint.getX() - x) + (screenPoint.getY() - y) * (screenPoint.getY() - y);
            if (distance < 100) return pos;
            pos++;
        }
        return -1;
    }


    protected void paintMarkers(Graphics2D canvas) {
        // Шаг 1 - Установить специальное перо для черчения контуров маркеров

        // Шаг 2 - Организовать цикл по всем точкам графика
        for (Double[] point : graphicsData) {

            boolean check = false;
            Double value = point[1];
            value = Math.abs(value - Math.floor(value));
            if (value <= 0.1 || value >= 0.9){
                check = true;
            }

            if (check){
                canvas.setColor(Color.GREEN);
                canvas.setPaint(Color.GREEN);
            }
            else{
                canvas.setColor(Color.RED);
                canvas.setPaint(Color.RED);
            }


            if (check) {
                canvas.setColor(Color.GREEN);
                // Выбрать  цвет для закрашивания маркеров внутри
                canvas.setPaint(Color.GREEN);
              /*  canvas.setStroke(markerStroke);
                GeneralPath path = new GeneralPath();
                canvas.setStroke(markerStroke);
                Point2D.Double center = xyToPoint(point[0], point[1]);
                canvas.draw(new Line2D.Double(shiftPoint(center, -11, 11), shiftPoint(center, 11, 11)));
                canvas.draw(new Line2D.Double(shiftPoint(center, -11, -11), shiftPoint(center, 11, -11)));
                canvas.draw(new Line2D.Double(shiftPoint(center, 11, 11), shiftPoint(center, 11, -11)));
                canvas.draw(new Line2D.Double(shiftPoint(center, -11, -11), shiftPoint(center, -11, 11)));
                canvas.draw(new Line2D.Double(shiftPoint(center, 11, 11), shiftPoint(center, -11, -11)));
                canvas.draw(new Line2D.Double(shiftPoint(center, -11, 11), shiftPoint(center, 11, -11)));
*/
            }
            else {
                canvas.setColor(Color.RED);
                // Выбрать  цвет для закрашивания маркеров внутри
                canvas.setPaint(Color.RED);
            }
            canvas.setStroke(markerStroke);
            GeneralPath path = new GeneralPath();
            canvas.setStroke(markerStroke);
            Point2D.Double center = xyToPoint(point[0], point[1]);
            canvas.draw(new Line2D.Double(shiftPoint(center, -11, 11), shiftPoint(center, 11, 11)));
            canvas.draw(new Line2D.Double(shiftPoint(center, -11, -11), shiftPoint(center, 11, -11)));
            canvas.draw(new Line2D.Double(shiftPoint(center, 11, 11), shiftPoint(center, 11, -11)));
            canvas.draw(new Line2D.Double(shiftPoint(center, -11, -11), shiftPoint(center, -11, 11)));
            canvas.draw(new Line2D.Double(shiftPoint(center, 11, 11), shiftPoint(center, -11, -11)));
            canvas.draw(new Line2D.Double(shiftPoint(center, -11, 11), shiftPoint(center, 11, -11)));


        }
    }
    private void paintLabels(Graphics2D canvas){
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context=canvas.getFontRenderContext();

        if (selectedMarker >= 0)
        {
            Point2D.Double point = xyToPoint(graphicsData[selectedMarker][0],
                    graphicsData[selectedMarker][1]);
            String label = "x= " + formatter.format(graphicsData[selectedMarker][0]) +
                    ", y= " + formatter.format(graphicsData[selectedMarker][1]);
            Rectangle2D bounds = labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLACK);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }
    }
    public void paintComponent(Graphics g) {

        super.paintComponent(g);

        scaleX=this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        scaleY=this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        if (graphicsData == null || graphicsData.length == 0) return;



        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();

        if (!showTurn) {
            AffineTransform at = AffineTransform.getRotateInstance(-Math.PI / 2, getSize().getWidth() / 2, getSize().getHeight() / 2);
            at.concatenate(new AffineTransform(getSize().getHeight() / getSize().getWidth(), 0.0, 0.0, getSize().getWidth() / getSize().getHeight(),
                    (getSize().getWidth() - getSize().getHeight()) / 2, (getSize().getHeight() - getSize().getWidth()) / 2));
            canvas.setTransform(at);


        }
        // Шаг 8 - В нужном порядке вызвать методы отображения элементов графика
        // Порядок вызова методов имеет значение, т.к. предыдущий рисунок будет
        // затираться последующим
        // Первым (если нужно) отрисовываются оси координат.

        if (showAxis)
        {paintAxis(canvas);
            paintLabels(canvas);
        }
        paintGraphics(canvas);
        // Затем (если нужно) отображаются маркеры точек графика.
        if (showMarkers) paintMarkers(canvas);
        // Шаг 9 - Восстановить старые настройки холста
        paintSelection(canvas);
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);

    }

    public static void main(String[] args)
    {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void paintSelection(Graphics2D canvas) {
        if (!scaleMode) return;
        canvas.setStroke(selectionStroke);
        canvas.setColor(Color.BLACK);
        canvas.draw(selectionRect);
    }



    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }



    public void reset() {
        showGraphics(this.originalData);
    }


    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }

        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (undoHistory.size() > 0) {
                    viewport = ((double[][]) undoHistory.get(undoHistory.size() - 1));

                    undoHistory.remove(undoHistory.size() - 1);
                } else {
                    zoomToRegion(minX, maxY, maxX, minY);
                }
                repaint();
            }
        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() != 1) return;
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            originalPoint = translatePointToXY(ev.getX(), ev.getY());
            if (selectedMarker >= 0) {
                changeMode = true;
                setCursor(Cursor.getPredefinedCursor(8));
            } else {
                scaleMode = true;
                setCursor(Cursor.getPredefinedCursor(5));
                selectionRect.setFrame(ev.getX(), ev.getY(), 1.0D, 1.0D);
            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() != 1) return;

            setCursor(Cursor.getPredefinedCursor(0));
            if (changeMode) {
                changeMode = false;
            } else {
                scaleMode = false;
                double[] finalPoint = translatePointToXY(ev.getX(), ev.getY());
                undoHistory.add(viewport);
                viewport = new double[2][2];
                zoomToRegion(originalPoint[0], originalPoint[1], finalPoint[0], finalPoint[1]);
                repaint();
            }
        }
    }


    public class MouseMotionHandler implements MouseMotionListener {

        public void mouseDragged(MouseEvent ev) {
            if (changeMode) {

                double[] currentPoint = translatePointToXY(ev.getX(), ev.getY());
                double newY =  graphicsData[selectedMarker][1] +
                        currentPoint[1] - graphicsData[selectedMarker][1];
                if (newY > viewport[0][1]) {
                    newY = viewport[0][1];
                }
                if (newY < viewport[1][1]) {
                    newY = viewport[1][1];
                }
                graphicsData[selectedMarker][1] = Double.valueOf(newY);
                repaint();
            } else {
                double width = ev.getX() - selectionRect.getX();
                if (width < 5.0D) {
                    width = 5.0D;
                }
                double height = ev.getY() - selectionRect.getY();
                if (height < 5.0D) {
                    height = 5.0D;
                }
                selectionRect.setFrame(selectionRect.getX(), selectionRect.getY(), width, height);
                repaint();
            }
        }

        public void mouseMoved(MouseEvent ev) {
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            if (selectedMarker >= 0)
                setCursor(Cursor.getPredefinedCursor(8));
            else {
                setCursor(Cursor.getPredefinedCursor(0));
            }
            repaint();
        }

    }
}



