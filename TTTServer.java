/*
For this project, you will create a networked Tic-Tac-Toe game.

You will write a server program that handles all games.  Also write a client program that connects to the server.

When a client connects, it should be able to join a game, make moves on the board, and receive updates from the server about the current board, whose turn it is, and whether the game ends in a win, loss, or draw.


You must write a report that includes Introduction, body and conclusion. In the body part of the report, show the source code and explain the code.
Note that you must provide the code and sample results in your report as well. 
*/

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TTTServer{
    public static void main(String[] args) throws IOException{
        final int S_PORT = 8080;
        ServerSocket server = new ServerSocket(S_PORT);
        System.out.println("[SERVER] Waiting for players to connect...");
        Grid grid = new Grid();
        List<TTTService> clients = new ArrayList<>();
        int player = 0;
        
        while(true){
            Socket s = server.accept();
            System.out.printf("[SERVER] Player %d connected!\n", player + 1);
            TTTService service = new TTTService(s, grid, player, clients);
            clients.add(service);
            player++;
            Thread t = new Thread(service);
            t.start();
        }
    }
}