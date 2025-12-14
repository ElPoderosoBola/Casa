import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class servidorSubastas {
    /*
    El puerto por el que se escucha
     */
    private static final int PUERTO = 50000;
    /*
    Aquí declaramos con el enum los posibles estados de la subasta, se verán más adelante en
    un switch */
    private enum Estado {
        ESPERANDO_JUGADORES, SUBASTA_INICIADA, TIEMPO_FINALIZADO, ADJUDICADO
    }

    /*
    Aquí creamos una variable static volatile para que todas las clases puedan leerla y
    para que pueda cambiar. Esto servirá para cambiar el estado de la subasta y todas las
    clases puedan verlo.
    */
    private static volatile Estado estadoSubasta = Estado.ESPERANDO_JUGADORES;
    private static int precioActual = 0;
    private static String ganadorActual = "Nadie";
    private static final Object cerrojo = new Object(); // Aquí creamos el primer lock

    public static void main(String[] args) {
        System.out.println("Iniciando el servidor");

        /*
        Con esta parte abrimos el proceso. El try es with-resource para que llame solo a close
        */
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor escuchando en el puerto " + PUERTO);

            /*
            Con el executor manejaremos el pool de hilos
            */
            ExecutorService executor = Executors.newCachedThreadPool();
            /*
            Este hilo es el que se encarga de que el switch de handleClient vaya cambiando de fase.
            handleCliente tiene la lógica de la función y este hilo mide el tiempo y da algunos avisos.
            */
            new Thread(() -> {
                try {
                    // Fase 1
                    System.out.println("10 segundos para que se apunten los participantes");
                    Thread.sleep(10000);

                    // Fase 2
                    estadoSubasta = Estado.SUBASTA_INICIADA;
                    System.out.println("Subasta iniciada. 30 segundos para pujar");

                    Thread.sleep(30000);

                    // Fase 3
                    estadoSubasta = Estado.TIEMPO_FINALIZADO;
                    System.out.println("Tiempo de subasta finalizado");
                    Thread.sleep(2000);

                    // Fase 4
                    estadoSubasta = Estado.ADJUDICADO;
                    System.out.println("Subasta adjudicada a: " + ganadorActual);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            // Ponemos el bucle infinito para que siempre esté atento de si llega alguien a quien atender
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Se conectó el cliente: " + clientSocket.getInetAddress());

                // Asiganamos la tarea al hilo que llegue
                executor.execute(() -> handleClient(clientSocket));
                
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Este método es para crear la lógica del hilo que acaba de llegar
    */
    private static void handleClient(Socket clientSocket) {
        try {
            /*
            Aquí creamos las tuberías para mandar y recibir información y buffers, como en el
            ejercicio de los números aleatorios.
             */
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            /*
            Esta parte sirve para leer lo que manda la clase cliente. El método readObject se encargar
            de recibir los bytes del stream y reconvertirlos en un objeto Java.
            */
            Object objetoRecibido = in.readObject();

            while (!clientSocket.isClosed()) {
                /*
                Con este switch decidimos qué se hace según el estado de la subasta. Va como por´
                niveles que van cambiando según el estado de la subasta.
                */
                switch (estadoSubasta) {
                    case ESPERANDO_JUGADORES:
                        /*
                        Este mensaje saldrá en caso de que el cliente quiera pujar antes de tiempo.
                        Se usa el out que creamos anteriomente para crear la tubería por la que
                        los datos viajarán hasta el cliente.
                        Muy importante usar el flush para que el mensaje no se atasque.
                        */
                        out.writeObject("Aún no se puede jugar");
                        out.flush();
                        
                        break;

                    case SUBASTA_INICIADA:
                        /*
                        Primero se recoje la cantidad enviada. Los mensajes de los clientes
                        van entrando de uno en uno (con el lock) para evitar que se produzca la 
                        condición de carrera ni se mezclen datos. Si la puja es válida, avisamos al 
                        cliente que la envió y actualizamos la puja de manera pública y si no es válida, solo se envía
                        un mensaje al cliente que la hizo.
                        */
                        int puja = (Integer) objetoRecibido;

                        synchronized (cerrojo) {
                            if (puja > precioActual) {
                                precioActual = puja;
                                ganadorActual = "Cliente " + clientSocket.getInetAddress();
                                System.out.println("Nueva puja máxima: " + precioActual);

                                out.writeObject("Puja aceptada");
                            } else {
                                out.writeObject("Puja inferior a " + precioActual);
                            }

                            }
                        
                        out.flush();
                        break;

                    case TIEMPO_FINALIZADO:
                        System.out.println("Finalizó el tiempo de subasta. Ya no se puede pujar");
                        out.writeObject("Subasta finalizada. Ya no se aceptan más pujas");
                        out.flush();
                        break;

                    case ADJUDICADO:
                        System.out.println("Ganó el cliente " + ganadorActual + " por " + precioActual);
                        out.writeObject(("Apuesta finalizada por " + precioActual));
                        out.flush();
                        break;
                
                }

                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}