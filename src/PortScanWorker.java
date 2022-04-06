// добавление различных библиотек:

import java.net.*; // сокет реализации взаимодействия программ по протоколу TCP
import java.util.ArrayList; // динамический список
import java.util.List; // список
import java.util.concurrent.BrokenBarrierException; // заставляет основной поток ждать выполнения подпотоки, прежде чем выполнить down класс исключений
import java.util.concurrent.CyclicBarrier; // заставляет основной поток ждать выполнения подпотоки, прежде чем выполнить down
import java.io.IOException; // исключения

public class PortScanWorker implements Runnable { // Класс, который имплементит реализацию потока через Runnable
    static int globalId = 1; // целая переменная класса

    // поля
    private int id; // целое поле
    private List<Integer> ports; // список портов только с целыми параметрами, Integer - это дженерик
    private List<Integer> openPorts; // список открытых портов
    private InetAddress inetAddress; // ip адреса
    private int timeout = 200; // время выхода
    CyclicBarrier barrier; // используем барьер методом композиции

    // конструктор. Всегда вызывается при создании объекта, даже неявно. Если он неявный, значит пустой.
    public PortScanWorker() {
        id = globalId++;
    }

    // сеттеры и геттеры. Это инкапсуляция приватных полей,
    // реализуются через методы. Сеттеры назначают в метод приватные поля,
    // геттеры вызывают эти приватные поля
    public int getId() { // геттер id
        return id;
    }

    public void setBarrier(CyclicBarrier barrier) { // сеттер барьера
        this.barrier = barrier;
    }

    public int getTimeout() { // геттер времени
        return timeout;
    }

    public void setTimeout(int timeout) { // сеттер времени
        this.timeout = timeout;
    }

    public List<Integer> getOpenPorts() { // геттер списка открытых портов
        return openPorts;
    }

    public void setInetAddress(InetAddress inetAddress) { // сеттер ip адрессов
        this.inetAddress = inetAddress;
    }

    public void setPorts(List<Integer> ports) { // сеттер списка портов
        this.ports = ports;
    }

    // метод run, в котор описывается логика потока класса
    public void run() {
        //System.out.println("Started thread with id = " + id);
        scan(inetAddress); // выполение метода scan в методе run. сканируем ip адресса
        try { //бросаем исключение. в потоках всегда они обрабатываются
            barrier.await(); // ждем, пока завершатся подпотоки.
        } catch (InterruptedException | BrokenBarrierException ex) {
            return;
        }
    }

    // основной метод сканирования
    void scan(InetAddress inetAddress) { // принимает на вход ip адреса
        openPorts = new ArrayList<Integer>(); // создали динамический массив открытх портов
        //System.out.println("scanning ports: ");
        for (Integer port : ports) { // прошлись циклом форейч. принимаются только целые значения из списка ports в переменную port
            //System.out.print(port);
            // обработка исключения
            try {
                InetSocketAddress isa = new InetSocketAddress(inetAddress, port); // создали сущность из библиотеки net. с данными на вход ip и port
                Socket socket = new Socket(); // создали сущность (объект) сокет (гнездо)
                socket.connect(isa, timeout); // подключаемся
                System.out.println("Found opened port: " + port); // вывод в консоль открытого порта + его порт
                openPorts.add(port); // добавляем этот порт в список открытых портов
                socket.close(); // закрываем сокет
            } catch (IOException ioe) { // бросаем исключение
                //System.out.println("");
            }
        }
        //System.out.println("FINISH, id = " + id);
    }

}


