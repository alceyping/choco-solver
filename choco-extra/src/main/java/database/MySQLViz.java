/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package database;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.jdbc.JDBCCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 05/06/13
 */
public class MySQLViz {

    private static final String QUERY1 = "SELECT t1.PID, t1.%s - t2.%s FROM RESOLUTIONS as t1, RESOLUTIONS as t2 " +
            "WHERE t1.BID=%s AND t2.BID=%s AND t1.PID=t2.PID order by t1.PID";

    private static final String QUERY2 = "SELECT t1.BID, t1.%s FROM RESOLUTIONS as t1, BENCHMARKS as t2 " +
            "WHERE t1.PID=%s AND t1.BID=t2.BID order by t2.DATE";


    private Properties properties = new Properties();

    private final String url, dbname, user, pwd;

    private Connection connection;

    public MySQLViz(File mysqlProperties) {
        try {
            properties.load(new FileInputStream(mysqlProperties));
        } catch (Exception e) {
            System.err.println("Unable to load " + mysqlProperties + " file from classpath. " + e);
            System.exit(1);
        }

        url = properties.getProperty("mysql.url");
        dbname = properties.getProperty("mysql.dbname");
        user = properties.getProperty("mysql.user");
        pwd = properties.getProperty("mysql.pwd");
    }

    public void compare() {
        compare(null, null);
    }

    public void compare(String bname) {
        compare(bname, null);
    }

    public void compare(String bname1, String bname2) {
        int bid1 = 1, bid2 = 1;
        // 1. get ids
        if (bname1 != null) {
            // get the last BID
            try {
                PreparedStatement statement = connection.prepareStatement("select BID from BENCHMARKS where NAME = ?;");
                statement.setString(1, bname1);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    bid1 = resultSet.getInt(1);
                } else {
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // get the last BID
            try {
                PreparedStatement statement = connection.prepareStatement("select BID from BENCHMARKS where DATE = " +
                        "(SELECT MAX(DATE) from BENCHMARKS);");
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    bid1 = resultSet.getInt(1);
                } else {
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (bname2 != null) {
            try {
                PreparedStatement statement = connection.prepareStatement("select BID from BENCHMARKS where NAME = ?;");
                statement.setString(1, bname2);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    bid2 = resultSet.getInt(1);
                } else {
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            bid2 = bid1 - 1;
        }

        display(
                createChart(QUERY1, new String[]{"BUILDING_TIME", "BUILDING_TIME", Integer.toString(bid1),
                        Integer.toString(bid2)}, "BUILDING_TIME"),
                createChart(QUERY1, new String[]{"SOLVING_TIME", "SOLVING_TIME", Integer.toString(bid1),
                        Integer.toString(bid2)}, "SOLVING_TIME"),
                createChart(QUERY1, new String[]{"SOLUTION", "SOLUTION", Integer.toString(bid1),
                        Integer.toString(bid2)}, "SOLUTION"),
                createChart(QUERY1, new String[]{"NB_SOL", "NB_SOL", Integer.toString(bid1),
                        Integer.toString(bid2)}, "NB_SOL")
        );
    }

    public void history(String pbname) {
        int pid = 1;
        // get pb id
        try {
            PreparedStatement statement = connection.prepareStatement("select PID from PROBLEMS where NAME = ?;");
            statement.setString(1, pbname);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                pid = resultSet.getInt(1);
            } else {
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        display(createChart(QUERY2, new String[]{"SOLVING_TIME", Integer.toString(pid)}, "SOLVING_TIME"));

    }


    private void display(JFreeChart... chart) {
        JFrame frame = new JFrame("");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        frame.setLayout(new FlowLayout());
        for (JFreeChart c : chart) {
            JLabel label = new JLabel();
            label.setIcon(new ImageIcon(c.createBufferedImage(750, 450)));
            frame.getContentPane().add(label);
        }
        frame.pack();
        frame.setVisible(true);
    }

    public void connect() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            try {
                connection = DriverManager.getConnection("jdbc:mysql://" + url + "/" + dbname, user, pwd);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create pie chart representing percentage of employees in each department
     * that has at least one employee.
     *
     * @return Pie chart; null if no image created.
     */
    public JFreeChart createChart(String query, String[] args, String name) {
        JFreeChart pieChart = null;

        connect();

        final String QUERY = String.format(query, args);
        try {
            JDBCCategoryDataset pieDataset =
                    new JDBCCategoryDataset(connection, QUERY);

            pieChart =
                    ChartFactory.createLineChart(name, // chart title
                            "pb",
                            "stat",
                            pieDataset,
                            PlotOrientation.VERTICAL,
                            true,      // legend displayed
                            true,      // tooltips displayed
                            false);   // no URLs

        } catch (SQLException sqlEx)    // checked exception
        {
            System.err.println("Error trying to acquire JDBCPieDataset.");
            System.err.println("Error Code: " + sqlEx.getErrorCode());
            System.err.println("SQLSTATE:   " + sqlEx.getSQLState());
            sqlEx.printStackTrace();
        }

        return pieChart;
    }

    public static void main(String[] args) {
        MySQLViz ms = new MySQLViz(new File("/Users/cprudhom/Sources/Choco3/choco-parser/src/main/resources/mysql.properties"));
        ms.connect();
        ms.compare();
        ms.compare("MZN20130603");
        ms.compare("MZN20130603", "JACOP");

        ms.history("filter_fir_1_1.fzn");


    }

}
