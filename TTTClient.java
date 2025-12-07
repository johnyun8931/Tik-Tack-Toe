import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
public class TTTClient {
    public static void main(String [] args) throws IOException{
        final int S_PORT = 8080;
        Socket s = new Socket("localhost", S_PORT);
        InputStream input = s.getInputStream();
        OutputStream output = s.getOutputStream();
        Scanner in = new Scanner(input);
        PrintWriter out = new PrintWriter(output);
        Scanner console = new Scanner(System.in);

        class InputRunnable implements Runnable{
            public void run(){
                while (!Thread.interrupted()){
                    String line = console.nextLine();
                    out.println(line);
                    out.flush();
                }
            }
        }
        
        char[][] board = {{'1','2','3'},{'4','5','6'},{'7','8','9'}};
        Grid sample = new Grid(board);
        System.out.println("Select a cell to play from the board: ");
        System.out.println(sample);
        
        InputRunnable runnable = new InputRunnable();
        Thread t = new Thread(runnable);
        t.start();
        
        while(in.hasNextLine()){
            String response = in.nextLine();
            System.out.println(response);
            
        }
        t.interrupt();
        s.close();
        in.close();
        console.close();
    }

}
