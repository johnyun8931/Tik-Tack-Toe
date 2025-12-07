import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class TTTService implements Runnable{
    private Socket s;
    private Scanner in;
    private PrintWriter out;
    private int player;
    private Grid grid;
    private List<TTTService> clients;

    public TTTService(Socket aSocket, Grid aGrid, int playerNum, List<TTTService> clientList){
        s = aSocket;
        grid = aGrid;
        player = playerNum;
        clients = clientList;
    }
    
    public void sendUpdate(String message){
        out.println("*".repeat(60));
        out.println(message);
        out.flush();
    }
    
    private void broadcast(String message){
        for(TTTService client : clients){
            client.sendUpdate(message);
        }
    }

    public void run(){
        try{
            try{
                in = new Scanner(s.getInputStream());
                out = new PrintWriter(s.getOutputStream());
                
                out.println("Welcome Player " + player);
                // run through game receiving inputs from user
                // show the board before user input
                while(!grid.isOver() && in.hasNext()){
                    
                    String command = in.next();
                    if(grid.getTurn() == player){
                        doService(command);
                    }
                    else out.print("Type a command\n");
                }

                out.println("Goodbye");
                out.flush();
            }
            finally{
                s.close();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void doService(String command) {
        if(command.equals("Q")){
            grid.finish();
            broadcast("Player " + player + " quit. Great Game!");
        }
        
        else{
            int num = Integer.parseInt(command);
            int info = grid.play(num, player);
            if(info == 0) {
                out.println("Already a symbol played there, enter a new spot");
                out.flush();
                return;
            }
            
            // Broadcast the updated board to all clients
            String boardUpdate = "\n" + grid.toString() + "\n";
            if(grid.isTerminated()){
                broadcast(boardUpdate + "Game over! Player " + player + " wins!");
            }
            else if(grid.isDraw()){
                broadcast(boardUpdate + "Touche! Draw game");
            }
            else{
                broadcast(boardUpdate + "Player " + grid.getTurn() + "'s turn");
            }
        }
    }
}