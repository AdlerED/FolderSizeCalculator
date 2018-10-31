import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.*;

public class MainWindow extends JFrame implements ActionListener {
    /*
    一小段说明:
    函数compatibleMode : 开启兼容模式后,不会设置软件字体,便于在Linux上调试
    作者: Adler
    联系方式: WX/QQ:1101635162
     */
    boolean compatibleMode = false;
    //实例化窗口控件
    NaivelyConcurrentTotalFileSize n = new NaivelyConcurrentTotalFileSize();
    JTable jt = new JTable() {
        public boolean isCellEditable(int row, int column) {
            return false;
        }//表格不允许被编辑

        public Class getColumnClass(int column) {
            Class returnValue;
            if ((column >= 0) && (column < getColumnCount())) {
                returnValue = getValueAt(0, column).getClass();
            } else {
                returnValue = Object.class;
            }
            return returnValue;
        }
    };
    DefaultTableModel tablemo = (DefaultTableModel) jt.getModel();
    TableRowSorter trs = new TableRowSorter<TableModel>(tablemo);
    JScrollPane jsp = new JScrollPane(jt);
    JButton jb = new JButton("选择目录并分析");
    JFileChooser jfc;
    JTextField jtf = new JTextField("请点击右侧按钮选择目录,我们会分析目录下的文件(夹)大小...");
    //设置超时reg
    JButton set = new JButton("设定");
    String[] nums = new String[]{"1秒(最快,无法爬取较复杂目录)", "3秒(一般,可爬取大部分目录)",
            "5秒(较慢,可爬取绝大部分目录)", "10秒", "30秒", "100秒", "500秒", "2000秒",
            "5000秒", "10000秒", "100000秒"};
    JComboBox jc = new JComboBox(nums);
    //设置线程数reg
    JButton setT = new JButton("设定");
    String[] sThrs = new String[]{"200(完全不影响正常使用)", "1000(性能要求较低)",
            "3000(推荐,性能要求一般)",
            "20000(性能要求较高)", "30000(性能要求高)",
            "40000(万元电脑推荐使用)", "50000(作死推荐使用)"};
    JComboBox jcThrs = new JComboBox(sThrs);
    //等待界面args
    JLabel jl = new JLabel("正在分析目录...请稍候");
    JLabel jl2 = new JLabel("小提示:你可以点击表格的标题栏进行排序!");
    JLabel jl3 = new JLabel();
    JButton about = new JButton("关于...");
    //如何设置?
    JButton jbSh = new JButton("我应该如何设置?");
    JLabel jlPx = new JLabel("设置目录爬行超时:");
    JLabel jlXc = new JLabel("设置分析线程数量:");

    DecimalFormat df = new DecimalFormat("######0.00"); //这东西能保留两位小数

    JProgressBar jpgb = new JProgressBar();

    static int timeOut;
    static int thrs;
    int count = 0;

    public void run() {
        setTitle("加载中...");
        add(jpgb);
        jpgb.setBounds(0, 0, 210, 40);
        jpgb.setValue(25);
        创建读取配置文件();
        jpgb.setStringPainted(true);
        setSize(250, 40);
        Setup(); //配置JFrame=====
        jpgb.setValue(50);
        //执行方法=====
        设置所有控件的字体();
        jpgb.setValue(55);
        设置超时按钮();
        jpgb.setValue(60);
        设置线程数();
        //执行方法=====
        jpgb.setValue(65);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        jpgb.setValue(100);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //准备:创建一个空文件夹用于刷新两次表格先 这样可以去除报错bug
        计算目录(System.getProperty("user.dir"));
        计算目录(System.getProperty("user.dir"));
        setSize(500, 162);
        jpgb.setVisible(false);
        setLocationRelativeTo(null);
        setTitle("FolderSizeCalculator (查查哪个文件夹在掏空你的硬盘?)");
    }

    public void Setup() { //设置窗口参数
        //固定窗口大小
        setResizable(false);
        setVisible(true);
        setLocationRelativeTo(null);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(jl); //等待文本框
        jl.setBounds(160, 90, 200, 30);
        jl.setForeground(Color.red);
        jl.setVisible(false);
        //选择文件按钮
        add(jb);
        jb.setBounds(355, 10, 110, 29);
        jb.addActionListener(this);
        add(jtf);
        jtf.setEditable(false);
        jtf.setBounds(20, 10, 332, 30);
        add(jbSh);
        jbSh.setBounds(340, 95, 125, 20);
        jbSh.addActionListener(this);
        add(jl2);
        jl2.setVisible(false);
        jl2.setBounds(125, 82, 250, 60);
        add(about);
        about.setBounds(20, 95, 80, 20);
        about.addActionListener(this);
        if (compatibleMode == false) {
            // 设置按钮显示效果
            UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("微软雅黑", Font.PLAIN, 13)));
            // 设置文本显示效果
            UIManager.put("OptionPane.messageFont", new FontUIResource(new Font("微软雅黑", Font.PLAIN, 13)));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (jb == e.getSource()) {
            setSize(500, 162);
            setLocationRelativeTo(null);
            jl3.setVisible(false);
            jl2.setVisible(false);
            jtf.setText("");
            tablemo.setRowCount(0); //清空表格
            //我也不知道为什么 只有第三次点击按钮的时候再清空表格才不会卡
            jfc = new JFileChooser();
            jl.setVisible(true);
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jfc.showDialog(new JLabel(), "选择文件夹");
            File file = jfc.getSelectedFile();
            try {
                jtf.setText(file.getPath());
            } catch (NullPointerException npe) {
                jl.setVisible(false);
                jtf.setText("错误!请选择一个文件夹...");
            }
            计算目录(file.getPath());
            jl.setVisible(false);
        }
        if (set == e.getSource()) {
            if (jc.getSelectedItem() == "1秒(最快,无法爬取较复杂目录)") {
                System.out.println("1s");
                setTimeOut(1);
            }
            if (jc.getSelectedItem() == "3秒(一般,可爬取大部分目录)") {
                System.out.println("3s");
                setTimeOut(3);
            }
            if (jc.getSelectedItem() == "5秒(较慢,可爬取绝大部分目录)") {
                System.out.println("5s");
                setTimeOut(5);
            }
            if (jc.getSelectedItem() == "10秒") {
                System.out.println("10s");
                setTimeOut(10);
            }
            if (jc.getSelectedItem() == "30秒") {
                System.out.println("30s");
                setTimeOut(30);
            }
            if (jc.getSelectedItem() == "100秒") {
                System.out.println("100s");
                setTimeOut(100);
            }
            if (jc.getSelectedItem() == "500秒") {
                System.out.println("500s");
                setTimeOut(500);
            }
            if (jc.getSelectedItem() == "2000秒") {
                System.out.println("2000s");
                setTimeOut(2000);
            }
            if (jc.getSelectedItem() == "5000秒") {
                System.out.println("5000s");
                setTimeOut(5000);
            }
            if (jc.getSelectedItem() == "10000秒") {
                System.out.println("10000s");
                setTimeOut(10000);
            }
            if (jc.getSelectedItem() == "100000秒") {
                System.out.println("100000s");
                setTimeOut(100000);
            }
            JOptionPane.showInternalMessageDialog(null, "超时阈值已设置为:" + getTimeOut() + "秒.", "设置成功", JOptionPane.INFORMATION_MESSAGE);

        }
        if (setT == e.getSource()) {
            if (jcThrs.getSelectedItem() == "200(完全不影响正常使用)") {
                System.out.println("200ths");
                setThrs(200);
            }
            if (jcThrs.getSelectedItem() == "1000(性能要求较低)") {
                System.out.println("1000ths");
                setThrs(1000);
            }
            if (jcThrs.getSelectedItem() == "3000(推荐,性能要求一般)") {
                System.out.println("3000ths");
                setThrs(3000);
            }
            if (jcThrs.getSelectedItem() == "20000(性能要求较高)") {
                System.out.println("20000ths");
                setThrs(20000);
            }
            if (jcThrs.getSelectedItem() == "30000(性能要求高)") {
                System.out.println("30000ths");
                setThrs(30000);
            }
            if (jcThrs.getSelectedItem() == "40000(万元电脑推荐使用)") {
                System.out.println("40000ths");
                setThrs(40000);
            }
            if (jcThrs.getSelectedItem() == "50000(作死推荐使用)") {
                System.out.println("50000ths");
                setThrs(50000);
            }
            JOptionPane.showInternalMessageDialog(null, "分析线程已设置为:" + getThrs() + "线程.", "设置成功", JOptionPane.INFORMATION_MESSAGE);
        }
        if (jbSh == e.getSource()) {
            JOptionPane.showInternalMessageDialog(null, "扫描结果大小出现-1是由于您的超时设定过低,快速扫描无法成功扫描结构过于复杂的目录.\n无论如何,都建议你将线程数设置到电脑能承受的尽可能多的数量\n其次,如果想快速扫描且不扫描部分比较大并且比较复杂的文件夹,可以将超时设低\n如果想细致扫描,可以将超时设置到尽可能高(耗时可能会增加)\n推荐组合: (快速扫描)超时1秒+50000线程\n(细致扫描)超时100000秒+30000线程\n请注意:设置计算机难以处理的过多线程可能会起到相反作用.", "不清楚怎么设置?", JOptionPane.INFORMATION_MESSAGE);
        }
        if (about == e.getSource()) {
            JOptionPane.showInternalMessageDialog(null, "这原本是个C++(QT)项目\n后来由于种种BUG在Java重写\n仅用于学习交流,请勿用于商业用途\n本软件造成的任何风险使用者自负\n作者:Adler WX/QQ:1101635162\n小提示:你可以修改config.ini来更改默认的超时和线程数\n修改配置文件后软件开启默认使用配置文件设置,但不会显示在下拉框内\n在你使用软件修改选项后,配置文件暂时失效.\n( PS: CPU及内存爆炸作者不予负责XD )", "关于作者", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int getTimeOut) {
        timeOut = getTimeOut;
    }

    public static int getThrs() {
        return thrs;
    }

    public void setThrs(int getThrs) {
        thrs = getThrs;
    }

    public void 计算目录(String path) {
        final long start = System.nanoTime();
        File file = new File(path);
        String[] colName = {"类型", "文件名", "大小(MB)"};
        DefaultTableModel contactTableModel = (DefaultTableModel) jt.
                getModel();
        contactTableModel.setColumnIdentifiers(colName);
        jt.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(jsp);
        jsp.setBounds(20, 125, 450, 400);
        jt.setModel(tablemo);
        ArrayList dic = new ArrayList();
        int fileS;
        for (File temp : file.listFiles()) {
            if (temp.isDirectory()) {
                for (int i = 0; i < 5; i++) {
                    if (i == 0) {
                        dic.add("文件夹");
                    }
                    if (i == 1) {
                        dic.add(temp.getName());
                    }
                    if (i == 2) {
                        ExecutorService pool = Executors.newFixedThreadPool(1);
                        NaivelyConcurrentTotalFileSize n = new NaivelyConcurrentTotalFileSize();
                        n.setVar(temp.getAbsolutePath());
                        Callable c = n;
                        Future f = pool.submit(c);
                        try {
                            System.out.println(f.get().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        pool.shutdown();
                        try {
                            String s = f.get().toString();
                            System.out.println("got" + s);
                            Long l = Long.parseLong(s);
                            dic.add(l);
                        } catch (Exception ex) {
                            dic.add("-1");
                        }
                        f.cancel(true);
                    }
                    if (i == 3) {
                        tablemo.addRow(dic.toArray());
                    }
                    if (i == 4) {
                        dic.clear();
                    }
                }
            }
            if (temp.isFile()) {
                for (int i = 0; i < 5; i++) {
                    if (i == 0) {
                        dic.add("文件");
                    }
                    if (i == 1) {
                        dic.add(temp.getName());
                    }
                    if (i == 2) {
                        Long t = temp.length();
                        Long calc;
                        calc = t / 1024 / 1024;
                        fileS = calc.intValue();
                        dic.add(fileS);
                    }
                    if (i == 3) {
                        tablemo.addRow(dic.toArray());
                    }
                    if (i == 4) {
                        dic.clear();
                    }
                }
            }
        }
        trs.setComparator(2, new Comparator<Object>() {
            //哈哈哈哈哈终于能排序了
            public int compare(Object arg0, Object arg1) {
                try {
                    int a = Integer.parseInt(arg0.toString());
                    int b = Integer.parseInt(arg1.toString());
                    return a - b;
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });
        //设置固定列宽
        TableColumn firstCol = jt.getColumnModel().getColumn(0);
        firstCol.setPreferredWidth(80);
        firstCol.setMaxWidth(80);
        firstCol.setMinWidth(80);
        TableColumn secCol = jt.getColumnModel().getColumn(1);
        secCol.setPreferredWidth(260);
        secCol.setMaxWidth(260);
        secCol.setMinWidth(260);
        jt.setRowSorter(trs); //设置排序
        setSize(500, 575);
        setLocationRelativeTo(null);
        final long end = System.nanoTime();
        double all = (end - start) / 1.0e9;
        String getAll = df.format(all);
        count++;
        if (count >= 3) {
            jl2.setVisible(true);
            jl3.setVisible(true);
            add(jl3);
            jl3.setBounds(163,70,250,60);
        }
        jl3.setText("本次分析花费时间:" + getAll + "秒.");
        System.out.println("All time taken: " + getAll);
        System.out.println("All working done.");
    }

    public void 设置所有控件的字体() {
        if (compatibleMode == false) {
            jl2.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            jl3.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            jb.setFont(new Font("微软雅黑", Font.BOLD, 10));
            jtf.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            set.setFont(new Font("微软雅黑", Font.BOLD, 12));
            setT.setFont(new Font("微软雅黑", Font.BOLD, 12));
            jc.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            jcThrs.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            jl.setFont(new Font("微软雅黑", Font.BOLD, 14));
            jbSh.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            jlPx.setFont(new Font("微软雅黑", Font.BOLD, 12));
            jlXc.setFont(new Font("微软雅黑", Font.BOLD, 12));
            about.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            jt.setFont(new Font("微软雅黑", Font.BOLD, 11));

        }
    }

    public void 创建读取配置文件() {
        Properties pro = new Properties();
        try {
            pro.load(new BufferedInputStream(new FileInputStream("config.ini")));
        } catch (FileNotFoundException e) {
            pro.put("timeOut", "3");
            pro.put("thrs", "3000");
            try {
                pro.store(new BufferedOutputStream(new FileOutputStream("config.ini")), "Save Configs File.");
            } catch (FileNotFoundException f) {
                f.printStackTrace();
            } catch (IOException i) {
                i.printStackTrace();
            }
        } catch (IOException i) {
            i.printStackTrace();
        } finally {
            String get1 = pro.getProperty("timeOut");
            String get2 = pro.getProperty("thrs");
            timeOut = Integer.parseInt(get1);
            thrs = Integer.parseInt(get2);
        }
        //开始判断并且设置下拉框内容
        //超时
        //    String[] nums = new String[] {"1秒(最快,无法爬取较复杂目录)", "3秒(一般,可爬取大部分目录)",
        //            "5秒(较慢,可爬取绝大部分目录)", "10秒", "30秒", "100秒", "500秒", "2000秒",
        //            "5000秒", "10000秒", "100000秒"};
        //    String[] sThrs = new String[] {"200(完全不影响正常使用)", "1000(性能要求较低)",
        //            "3000(推荐,性能要求一般)",
        //            "20000(性能要求较高)", "30000(性能要求高)",
        //            "40000(万元电脑推荐使用)", "50000(作死推荐使用)"};
        //暂时不写了
    }

    public void 设置超时按钮() {
        add(jlPx);
        jlPx.setBounds(20, 40, 150, 30);
        add(jc);
        jc.setBounds(140, 47, 250, 20);
        add(set);
        set.setBounds(405, 47, 60, 20);
        set.addActionListener(this);
        jc.setSelectedItem("3秒(一般,可爬取大部分目录)");
    }

    public void 设置线程数() {
        add(jlXc);
        jlXc.setBounds(20, 60, 150, 30);
        add(jcThrs);
        jcThrs.setBounds(140, 67, 250, 20);
        add(setT);
        setT.setBounds(405, 67, 60, 20);
        setT.addActionListener(this);
        jcThrs.setSelectedItem("3000(推荐,性能要求一般)");
    }

}
