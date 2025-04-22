package frisskyy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class RKProgram extends JFrame {

    interface ThirdOrderODE {
        double calculate(double t, double y, double dy, double d2y);
        String getName();
        double[] exactSolution(double t); // Метод для точного решения
        boolean isExactSolutionAvailable(); // Проверка на точное решение
    }

    private final ThirdOrderODE[] equations = {
            new ThirdOrderODE() {
                public double calculate(double t, double y, double dy, double d2y) {
                    return -d2y - dy - y;
                }
                public String getName() {
                    return "y''' + y'' + y' + y = 0";
                }
                public double[] exactSolution(double t) {
                    // Точное решение для первой задачи
                    double e_t = Math.exp(-t);
                    double cos_t = Math.cos(t);
                    double sin_t = Math.sin(t);
                    double y_exact = 0.5*e_t + 0.5*cos_t + 0.5*sin_t;
                    double dy_exact = -0.5*e_t - 0.5*sin_t + 0.5*cos_t;
                    double d2y_exact = 0.5*e_t - 0.5*cos_t - 0.5*sin_t;
                    return new double[]{y_exact, dy_exact, d2y_exact};
                }
                public boolean isExactSolutionAvailable() {
                    return true;
                }
            },
            new ThirdOrderODE() {
                public double calculate(double t, double y, double dy, double d2y) {
                    return Math.sin(t) - y - dy - d2y;
                }
                public String getName() {
                    return "y''' + y'' + y' + y = sin(t)";
                }
                public double[] exactSolution(double t) {
                    return new double[]{0, 0, 0}; // No exact solution available
                }
                public boolean isExactSolutionAvailable() {
                    return false;
                }
            }
    };

    private JComboBox<String> equationCombo, methodCombo;
    private JTextField y0Field, dy0Field, d2y0Field;
    private JTextField t0Field, tEndField, hField;
    private JButton solveButton, exportButton;
    private ChartPanel chartPanel;
    private JTextArea resultArea;
    private List<double[]> lastSolution;
    private DecimalFormat df = new DecimalFormat("0.######");

    public RKProgram() {
        super("Решение ОДУ 3-го порядка методами Рунге-Кутты");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout(5, 5));

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(9, 2, 5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        controlPanel.add(new JLabel("Уравнение:"));
        equationCombo = new JComboBox<>();
        for (ThirdOrderODE eq : equations) {
            equationCombo.addItem(eq.getName());
        }
        equationCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateInitialConditionsFields();
            }
        });
        controlPanel.add(equationCombo);

        controlPanel.add(new JLabel("Метод:"));
        methodCombo = new JComboBox<>(new String[]{"Эйлер", "Эйлер-Коши (РК2)", "Рунге-Кутта 3 (Гейне)", "Рунге-Кутта 4", "Фельдберг 5(4)"});
        controlPanel.add(methodCombo);

        controlPanel.add(new JLabel("y(0):"));
        y0Field = new JTextField("1.0");
        controlPanel.add(y0Field);

        controlPanel.add(new JLabel("y'(0):"));
        dy0Field = new JTextField("0.0");
        controlPanel.add(dy0Field);

        controlPanel.add(new JLabel("y''(0):"));
        d2y0Field = new JTextField("0.0");
        controlPanel.add(d2y0Field);

        controlPanel.add(new JLabel("Начальное время (t0):"));
        t0Field = new JTextField("0.0");
        controlPanel.add(t0Field);

        controlPanel.add(new JLabel("Конечное время (t_end):"));
        tEndField = new JTextField("1.0");
        controlPanel.add(tEndField);

        controlPanel.add(new JLabel("Шаг (h):"));
        hField = new JTextField("0.1");
        controlPanel.add(hField);

        exportButton = new JButton("Экспорт в TXT");
        exportButton.addActionListener(new ExportButtonListener());
        controlPanel.add(exportButton);

        solveButton = new JButton("Решить");
        solveButton.addActionListener(new SolveButtonListener());
        controlPanel.add(solveButton);

        add(controlPanel, BorderLayout.NORTH);

        // График и результаты
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        // График
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Решение ОДУ", "t", "y(t)",
                new XYSeriesCollection(),
                PlotOrientation.VERTICAL, true, true, false);
        chartPanel = new ChartPanel(chart);
        centerPanel.add(chartPanel);

        // Здесь результаты подсчета
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        centerPanel.add(new JScrollPane(resultArea));

        add(centerPanel, BorderLayout.CENTER);

        // Обновляем поля
        updateInitialConditionsFields();
    }

    private void updateInitialConditionsFields() {
        int selectedIndex = equationCombo.getSelectedIndex();
        if (selectedIndex == 0) { // Задача 1
            y0Field.setText("1.0");
            dy0Field.setText("0.0");
            d2y0Field.setText("0.0");
            y0Field.setEditable(false);
            dy0Field.setEditable(false);
            d2y0Field.setEditable(false);
        } else { // Задача 2
            y0Field.setText("2.0");
            dy0Field.setText("5.0");
            d2y0Field.setText("6.0");
            t0Field.setText("5.0");
            tEndField.setText("6.0");
            y0Field.setEditable(true);
            dy0Field.setEditable(true);
            d2y0Field.setEditable(true);
        }
    }

    // Методы решения
    private List<double[]> solveEuler(ThirdOrderODE equation,
                                      double y0, double dy0, double d2y0,
                                      double t0, double tEnd, double h) {
        List<double[]> results = new ArrayList<>();
        double t = t0;
        double y = y0;
        double dy = dy0;
        double d2y = d2y0;

        while (t <= tEnd + 1e-10) {
            results.add(new double[]{t, y, dy, d2y});
            double d3y = equation.calculate(t, y, dy, d2y);
            y += h * dy;
            dy += h * d2y;
            d2y += h * d3y;
            t += h;
        }
        return results;
    }

    private List<double[]> solveEulerCauchy(ThirdOrderODE equation,
                                            double y0, double dy0, double d2y0,
                                            double t0, double tEnd, double h) {
        List<double[]> results = new ArrayList<>();
        double t = t0;
        double y = y0;
        double dy = dy0;
        double d2y = d2y0;

        while (t <= tEnd) {
            results.add(new double[]{t, y, dy, d2y});

            // Предиктор
            double d3y = equation.calculate(t, y, dy, d2y);
            double yPred = y + h * dy;
            double dyPred = dy + h * d2y;
            double d2yPred = d2y + h * d3y;

            // Корректор
            double d3yCorr = equation.calculate(t + h, yPred, dyPred, d2yPred);
            y += h * (dy + dyPred) / 2;
            dy += h * (d2y + d2yPred) / 2;
            d2y += h * (d3y + d3yCorr) / 2;
            t += h;
        }
        return results;
    }

    private List<double[]> solveRKHeun(ThirdOrderODE equation,
                                       double y0, double dy0, double d2y0,
                                       double t0, double tEnd, double h) {
        List<double[]> results = new ArrayList<>();
        double t = t0;
        double y = y0;
        double dy = dy0;
        double d2y = d2y0;

        while (t <= tEnd + 1e-10) {
            results.add(new double[]{t, y, dy, d2y});

            // Коэффициенты k1
            double k1_y = dy;
            double k1_dy = d2y;
            double k1_d2y = equation.calculate(t, y, dy, d2y);

            // Коэффициенты k2
            double k2_y = dy + h * k1_dy / 3;
            double k2_dy = d2y + h * k1_d2y / 3;
            double k2_d2y = equation.calculate(t + h/3, y + h*k1_y/3, dy + h*k1_dy/3, d2y + h*k1_d2y/3);

            // Коэффициенты k3
            double k3_y = dy + 2*h * k2_dy / 3;
            double k3_dy = d2y + 2*h * k2_d2y / 3;
            double k3_d2y = equation.calculate(t + 2*h/3, y + 2*h*k2_y/3, dy + 2*h*k2_dy/3, d2y + 2*h*k2_d2y/3);

            // Обновление
            y += h * (k1_y + 3*k3_y) / 4;
            dy += h * (k1_dy + 3*k3_dy) / 4;
            d2y += h * (k1_d2y + 3*k3_d2y) / 4;
            t += h;
        }
        return results;
    }

    private List<double[]> solveRK4(ThirdOrderODE equation,
                                    double y0, double dy0, double d2y0,
                                    double t0, double tEnd, double h) {
        List<double[]> results = new ArrayList<>();
        double t = t0;
        double y = y0;
        double dy = dy0;
        double d2y = d2y0;

        while (t <= tEnd + 1e-10) {
            results.add(new double[]{t, y, dy, d2y});

            double k1_y = dy;
            double k1_dy = d2y;
            double k1_d2y = equation.calculate(t, y, dy, d2y);

            double k2_y = dy + h * k1_dy / 2;
            double k2_dy = d2y + h * k1_d2y / 2;
            double k2_d2y = equation.calculate(t + h/2, y + h*k1_y/2, dy + h*k1_dy/2, d2y + h*k1_d2y/2);

            double k3_y = dy + h * k2_dy / 2;
            double k3_dy = d2y + h * k2_d2y / 2;
            double k3_d2y = equation.calculate(t + h/2, y + h*k2_y/2, dy + h*k2_dy/2, d2y + h*k2_d2y/2);

            double k4_y = dy + h * k3_dy;
            double k4_dy = d2y + h * k3_d2y;
            double k4_d2y = equation.calculate(t + h, y + h*k3_y, dy + h*k3_dy, d2y + h*k3_d2y);

            y += h * (k1_y + 2*k2_y + 2*k3_y + k4_y) / 6;
            dy += h * (k1_dy + 2*k2_dy + 2*k3_dy + k4_dy) / 6;
            d2y += h * (k1_d2y + 2*k2_d2y + 2*k3_d2y + k4_d2y) / 6;
            t += h;
        }
        return results;
    }

    private List<double[]> solveFehlberg(ThirdOrderODE equation,
                                         double y0, double dy0, double d2y0,
                                         double t0, double tEnd, double h) {
        List<double[]> results = new ArrayList<>();
        double t = t0;
        double y = y0;
        double dy = dy0;
        double d2y = d2y0;

        while (t <= tEnd + 1e-10) {
            results.add(new double[]{t, y, dy, d2y});

            // Коэффициенты k1
            double k1_y = dy;
            double k1_dy = d2y;
            double k1_d2y = equation.calculate(t, y, dy, d2y);

            // Коэффициенты k2
            double k2_y = dy + h * k1_dy / 4;
            double k2_dy = d2y + h * k1_d2y / 4;
            double k2_d2y = equation.calculate(t + h/4, y + h*k1_y/4, dy + h*k1_dy/4, d2y + h*k1_d2y/4);

            // Коэффициенты k3
            double k3_y = dy + h * (3*k1_dy + 9*k2_dy) / 32;
            double k3_dy = d2y + h * (3*k1_d2y + 9*k2_d2y) / 32;
            double k3_d2y = equation.calculate(t + 3*h/8,
                    y + h*(3*k1_y + 9*k2_y)/32,
                    dy + h*(3*k1_dy + 9*k2_dy)/32,
                    d2y + h*(3*k1_d2y + 9*k2_d2y)/32);

            // Коэффициенты k4
            double k4_y = dy + h * (1932*k1_dy - 7200*k2_dy + 7296*k3_dy) / 2197;
            double k4_dy = d2y + h * (1932*k1_d2y - 7200*k2_d2y + 7296*k3_d2y) / 2197;
            double k4_d2y = equation.calculate(t + 12*h/13,
                    y + h*(1932*k1_y - 7200*k2_y + 7296*k3_y)/2197,
                    dy + h*(1932*k1_dy - 7200*k2_dy + 7296*k3_dy)/2197,
                    d2y + h*(1932*k1_d2y - 7200*k2_d2y + 7296*k3_d2y)/2197);

            // Коэффициенты k5
            double k5_y = dy + h * (439*k1_dy/216 - 8*k2_dy + 3680*k3_dy/513 - 845*k4_dy/4104);
            double k5_dy = d2y + h * (439*k1_d2y/216 - 8*k2_d2y + 3680*k3_d2y/513 - 845*k4_d2y/4104);
            double k5_d2y = equation.calculate(t + h,
                    y + h*(439*k1_y/216 - 8*k2_y + 3680*k3_y/513 - 845*k4_y/4104),
                    dy + h*(439*k1_dy/216 - 8*k2_dy + 3680*k3_dy/513 - 845*k4_dy/4104),
                    d2y + h*(439*k1_d2y/216 - 8*k2_d2y + 3680*k3_d2y/513 - 845*k4_d2y/4104));

            // Коэффициенты k6
            double k6_y = dy + h * (-8*k1_dy/27 + 2*k2_dy - 3544*k3_dy/2565 + 1859*k4_dy/4104 - 11*k5_dy/40);
            double k6_dy = d2y + h * (-8*k1_d2y/27 + 2*k2_d2y - 3544*k3_d2y/2565 + 1859*k4_d2y/4104 - 11*k5_d2y/40);
            double k6_d2y = equation.calculate(t + h/2,
                    y + h*(-8*k1_y/27 + 2*k2_y - 3544*k3_y/2565 + 1859*k4_y/4104 - 11*k5_y/40),
                    dy + h*(-8*k1_dy/27 + 2*k2_dy - 3544*k3_dy/2565 + 1859*k4_dy/4104 - 11*k5_dy/40),
                    d2y + h*(-8*k1_d2y/27 + 2*k2_d2y - 3544*k3_d2y/2565 + 1859*k4_d2y/4104 - 11*k5_d2y/40));

            // Решение 5-го порядка
            y += h * (16*k1_y/135 + 6656*k3_y/12825 + 28561*k4_y/56430 - 9*k5_y/50 + 2*k6_y/55);
            dy += h * (16*k1_dy/135 + 6656*k3_dy/12825 + 28561*k4_dy/56430 - 9*k5_dy/50 + 2*k6_dy/55);
            d2y += h * (16*k1_d2y/135 + 6656*k3_d2y/12825 + 28561*k4_d2y/56430 - 9*k5_d2y/50 + 2*k6_d2y/55);
            t += h;
        }
        return results;
    }

    private class SolveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                ThirdOrderODE equation = equations[equationCombo.getSelectedIndex()];
                double y0 = Double.parseDouble(y0Field.getText());
                double dy0 = Double.parseDouble(dy0Field.getText());
                double d2y0 = Double.parseDouble(d2y0Field.getText());
                double t0 = Double.parseDouble(t0Field.getText());
                double tEnd = Double.parseDouble(tEndField.getText());
                double h = Double.parseDouble(hField.getText());

                String method = (String)methodCombo.getSelectedItem();
                switch(method) {
                    case "Эйлер":
                        lastSolution = solveEuler(equation, y0, dy0, d2y0, t0, tEnd, h);
                        break;
                    case "Эйлер-Коши (РК2)":
                        lastSolution = solveEulerCauchy(equation, y0, dy0, d2y0, t0, tEnd, h);
                        break;
                    case "Рунге-Кутта 3 (Гейне)":
                        lastSolution = solveRKHeun(equation, y0, dy0, d2y0, t0, tEnd, h);
                        break;
                    case "Рунге-Кутта 4":
                        lastSolution = solveRK4(equation, y0, dy0, d2y0, t0, tEnd, h);
                        break;
                    case "Фельдберг 5(4)":
                        lastSolution = solveFehlberg(equation, y0, dy0, d2y0, t0, tEnd, h);
                        break;
                }

                // Update chart
                XYSeries seriesY = new XYSeries("y(t)");
                XYSeries seriesDY = new XYSeries("y'(t)");
                XYSeries seriesD2Y = new XYSeries("y''(t)");
                XYSeries seriesExactY = new XYSeries("Точное y(t)");
                XYSeries seriesExactDY = new XYSeries("Точное y'(t)");
                XYSeries seriesExactD2Y = new XYSeries("Точное y''(t)");

                StringBuilder resultsText = new StringBuilder();

                if (equation.isExactSolutionAvailable()) {
                    resultsText.append(String.format("%-8s %-12s %-12s %-12s %-12s %-12s %-12s\n",
                            "t", "y(t)", "y'(t)", "y''(t)", "Ошибка y", "Ошибка y'", "Ошибка y''"));
                } else {
                    resultsText.append(String.format("%-8s %-12s %-12s %-12s\n",
                            "t", "y(t)", "y'(t)", "y''(t)"));
                }

                for (double[] point : lastSolution) {
                    seriesY.add(point[0], point[1]);
                    seriesDY.add(point[0], point[2]);
                    seriesD2Y.add(point[0], point[3]);

                    if (equation.isExactSolutionAvailable()) {
                        double[] exact = equation.exactSolution(point[0]);
                        seriesExactY.add(point[0], exact[0]);
                        seriesExactDY.add(point[0], exact[1]);
                        seriesExactD2Y.add(point[0], exact[2]);

                        double errorY = Math.abs(point[1] - exact[0]);
                        double errorDY = Math.abs(point[2] - exact[1]);
                        double errorD2Y = Math.abs(point[3] - exact[2]);

                        resultsText.append(String.format("%-8.3f %-12.8f %-12.6f %-12.6f %-12.8f %-12.6f %-12.6f\n",
                                point[0], point[1], point[2], point[3], errorY, errorDY, errorD2Y));
                    } else {
                        resultsText.append(String.format("%-8.3f %-12.8f %-12.6f %-12.6f\n",
                                point[0], point[1], point[2], point[3]));
                    }
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.addSeries(seriesY);
                dataset.addSeries(seriesDY);
                dataset.addSeries(seriesD2Y);

                if (equation.isExactSolutionAvailable()) {
                    dataset.addSeries(seriesExactY);
                    dataset.addSeries(seriesExactDY);
                    dataset.addSeries(seriesExactD2Y);
                }

                JFreeChart chart = ChartFactory.createXYLineChart(
                        String.format("Решение: %s (%s)", equation.getName(), method),
                        "t", "Значения", dataset, PlotOrientation.VERTICAL, true, true, false);
                chartPanel.setChart(chart);

                resultArea.setText(resultsText.toString());

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(RKProgram.this,
                        "Ошибка ввода данных", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ExportButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (lastSolution == null || lastSolution.isEmpty()) {
                JOptionPane.showMessageDialog(RKProgram.this,
                        "Нет данных для экспорта", "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(RKProgram.this) == JFileChooser.APPROVE_OPTION) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                    ThirdOrderODE equation = equations[equationCombo.getSelectedIndex()];

                    if (equation.isExactSolutionAvailable()) {
                        writer.write(String.format("%-8s %-12s %-12s %-12s %-12s %-12s %-12s\n",
                                "t", "y(t)", "y'(t)", "y''(t)", "Ошибка y", "Ошибка y'", "Ошибка y''"));
                    } else {
                        writer.write(String.format("%-8s %-12s %-12s %-12s\n",
                                "t", "y(t)", "y'(t)", "y''(t)"));
                    }

                    for (double[] point : lastSolution) {
                        if (equation.isExactSolutionAvailable()) {
                            double[] exact = equation.exactSolution(point[0]);
                            double errorY = Math.abs(point[1] - exact[0]);
                            double errorDY = Math.abs(point[2] - exact[1]);
                            double errorD2Y = Math.abs(point[3] - exact[2]);

                            writer.write(String.format("%-8.3f %-12.6f %-12.6f %-12.6f %-12.6f %-12.6f %-12.6f\n",
                                    point[0], point[1], point[2], point[3], errorY, errorDY, errorD2Y));
                        } else {
                            writer.write(String.format("%-8.3f %-12.6f %-12.6f %-12.6f\n",
                                    point[0], point[1], point[2], point[3]));
                        }
                    }
                    JOptionPane.showMessageDialog(RKProgram.this,
                            "Данные успешно экспортированы", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RKProgram.this,
                            "Ошибка при экспорте: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new RKProgram().setVisible(true);
        });
    }
}
