
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

// основной класс main, в котором происходит запуск программы
public class Main {
    // поля класса
    public static final int MIN_PORTS_PER_THREAD = 20; // минимальное количество работающих потоков
    public static final int MAX_THREADS = 0xFF; // максимальное количество работающих потоков

    static InetAddress inetAddress; // ip
    static List<Integer> allPorts; // список открытых портов

    static List<Integer> allOpenPorts = new ArrayList<Integer>(); // динамический массив всех открытх портов
    static List<PortScanWorker> workers = new ArrayList<PortScanWorker>(MAX_THREADS); // наши потоки

    static Date startTime; // время начала исполнения программы
    static Date endTime; // время завершения исполнения программы

    public static void main(String[] args) {
        startTime = new Date(); // старт времени

        processArgs(args); // метод разбиения аргументов

        // ограничение минималного количества портов на один поток
        if (allPorts.size() / MIN_PORTS_PER_THREAD > MAX_THREADS) {
            final int PORTS_PER_THREAD = allPorts.size() / MAX_THREADS;

            // ----------создаем список потоков----------------------------------
            List<Integer> threadPorts = new ArrayList<Integer>();
            for (int i = 0, counter = 0; i < allPorts.size(); i++, counter++) {
                if (counter < PORTS_PER_THREAD) {
                    threadPorts.add(allPorts.get(i));
                } else {
                    PortScanWorker psw = new PortScanWorker();
                    psw.setInetAddress(inetAddress);
                    psw.setPorts(new ArrayList<Integer>(threadPorts));
                    workers.add(psw);
                    threadPorts.clear();
                    counter = 0;
                }
            }
            PortScanWorker psw = new PortScanWorker();
            psw.setInetAddress(inetAddress);
            psw.setPorts(new ArrayList<Integer>(threadPorts));
            workers.add(psw);
        } else {
            List<Integer> threadPorts = new ArrayList<Integer>();
            for (int i = 0, counter = 0; i < allPorts.size(); i++, counter++) {
                if (counter < MIN_PORTS_PER_THREAD) {
                    threadPorts.add(allPorts.get(i));
                } else {
                    PortScanWorker psw = new PortScanWorker();
                    psw.setInetAddress(inetAddress);
                    psw.setPorts(new ArrayList<Integer>(threadPorts));
                    workers.add(psw);
                    threadPorts.clear();
                    counter = 0;
                }
            }
            PortScanWorker psw = new PortScanWorker();
            psw.setInetAddress(inetAddress);
            psw.setPorts(new ArrayList<Integer>(threadPorts));
            workers.add(psw);
        }
        //-----------------------------------------------------------------------


        System.out.println("Ports to scan: " + allPorts.size()); // статистика сканирвоания всех портов
        System.out.println("Threads to work: " + workers.size()); // количество потоков, которые это будут делать

        // запускается после выполнения всех остальных потоков
        Runnable summarizer = new Runnable() {
            public void run() {
                System.out.println("Scanning stopped...");

                for (PortScanWorker psw : workers) { // проход по списку потоков
                    List<Integer> openPorts = psw.getOpenPorts();
                    allOpenPorts.addAll(openPorts); // добавление всех элементов из allOpenPorts в open ports
                }

                Collections.sort(allOpenPorts); // сортируем

                // выводим
                System.out.println("List of opened ports:");
                for (Integer openedPort : allOpenPorts) {
                    System.out.println(openedPort);
                }

                endTime = new Date(); // конец времени программы

                System.out.println("Time of run: " + (endTime.getTime() - startTime.getTime()) + " ms"); // вывод времени исполнения программы
            }
        };

        //----------------------создаем барьер---------------------------------------------------
        CyclicBarrier barrier = new CyclicBarrier(workers.size(), summarizer);

        for (PortScanWorker psw : workers) { // барьер ставится на все сканеры
            psw.setBarrier(barrier);
        }

        System.out.println("Start scanning...");

        for (PortScanWorker psw : workers) {
            new Thread(psw).start(); // здесь наши потоки запускаются
        }
    }
    //------------------------------------------------------------------------------------------------
    //--разбиение аргументов. Тоисть это те параметры, которые мы вводим изначально в консоли--
    static void processArgs(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }

        String host = args[0];
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (IOException ioe) {
            System.out.println("Error when resolving host!");
            System.exit(2);
        }

        System.out.println("Scanning host " + host);

        int minPort = 0;
        int maxPort = 0x10000 - 1;

        if (args.length == 2) {
            if (args[1].contains("-")) {
                // range of ports pointed out
                String[] ports = args[1].split("-");
                try {
                    minPort = Integer.parseInt(ports[0]);
                    maxPort = Integer.parseInt(ports[1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("Wrong ports!");
                    System.exit(3);
                }
            } else {
                // one port pointed out
                try {
                    minPort = Integer.parseInt(args[1]);
                    maxPort = minPort;
                } catch (NumberFormatException nfe) {
                    System.out.println("Wrong port!");
                    System.exit(3);
                }
            }
        }
        //----------------------------------------------------------------------------------

        allPorts = new ArrayList<Integer>(maxPort - minPort + 1);

        for (int i = minPort; i <= maxPort; i++) {
            allPorts.add(i);
        }
    }

    // гайд как пользоваться
    static void usage() {
        System.out.println("Java Port Scanner usage: ");
        System.out.println("java Main host port");
        System.out.println("Examples:");
        System.out.println("java Main 192.168.1.1 1-1024");
        System.out.println("java Main 192.168.1.1 1099");
        System.out.println("java Main 192.168.1.1 (this scans all ports from 0 to 65535)");
    }

}
