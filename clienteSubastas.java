import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class clienteSubastas {

    private static final String HOST = "localhost";
    private static final int PUESTO = 50000;

    public static void main(String[] args) {
        
        System.out.println("Conectando con la subasta");
        
        /*
        Tratamos de establecer conexión con la clase de servidorSubastas.java
        y en caso de conseguirlo, avisamos por la terminal y si no, se lanza una excepción.
        También creamos los conductos de salida y escucha de la clase.
        */
        try (Socket socket = new Socket("localhost", 50000);
                Scanner sc = new Scanner(System.in)) {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Éxito en la conexión");

            // El hilo de escucha. Se encarga de recibir lo que entra por la tuería de llegada y lo muestra
            new Thread(() -> {
            try {
                while (true) {
                    Object mensaje = in.readObject();
                    System.out.println(mensaje); 
                }
            } catch (Exception e) {
            }
        }).start();

        /*
        Bucle de escritura. Lee todo lo que introducimos por teclado y lo envía por la tubería de salida
        Muy importante que solo se pueden escribir números o saltará una excepción.
        */
        while (true) {
            String linea = sc.nextLine();
            
            try {
                int puja = Integer.parseInt(linea);

                out.writeObject(puja);
                out.flush();
            } catch (NumberFormatException e) {
                System.out.println("Error, solo se pueden introducir números");
            }
            
        }
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        
    }
    
}
