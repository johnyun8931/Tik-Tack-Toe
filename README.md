# Networked Tic-Tac-Toe Game - Project Report

## 1. Project Overview

This project implements a **multi-player networked Tic-Tac-Toe game** using Java socket programming and multithreading. The system consists of a central server that manages game state and multiple clients that can connect and play simultaneously. The architecture follows a client-server model where:

- **Server (TTTServer.java)**: Listens for incoming client connections on port 8080 and manages the shared game board
- **Client (TTTClient.java)**: Connects to the server and provides a user interface for players to make moves
- **Service Handler (TTTService.java)**: Each client connection spawns a new thread that handles communication with that specific client
- **Game Logic (Grid.java)**: Encapsulates the Tic-Tac-Toe board state, move validation, and win/draw detection

The implementation leverages Java's `Socket`, `ServerSocket`, and `Thread` classes to enable concurrent gameplay with real-time updates broadcast to all connected players.

---

## 2. Features Available in This Game

### Core Gameplay Features:
- **Multi-player Support**: Multiple clients can connect to the same server and play the same game
- **Turn-based System**: Players alternate turns (Player 0 uses 'O', Player 1 uses 'X')
- **Real-time Board Updates**: All connected clients receive immediate updates when any player makes a move
- **Win Detection**: Automatically detects when a player achieves three-in-a-row (horizontal, vertical, or diagonal)
- **Draw Detection**: Identifies when the board is full with no winner
- **Thread-safe Operations**: Uses `ReentrantLock` to prevent race conditions when multiple threads access the game board
- **Visual Board Display**: Displays the board in a user-friendly format with separators
- **Input Validation**: Prevents players from playing in already-occupied cells
- **Quit Option**: Players can type 'Q' to quit the game early

### Technical Features:
- **Broadcasting Mechanism**: Server broadcasts board state changes to all connected clients
- **Concurrent Client Handling**: Each client runs in a separate thread for independent communication
- **Non-blocking Input**: Client uses a separate thread for reading user input while listening for server messages
- **Error Handling**: Try-catch blocks handle IOException and proper socket cleanup

---

## 3. How to Play the Game

### Starting the Server:
1. Compile all Java files:
   ```bash
   javac *.java
   ```
2. Start the server:
   ```bash
   java TTTServer
   ```
3. The server will display: `[SERVER] Waiting for players to connect...`

### Connecting as a Client:
1. In a new terminal, run:
   ```bash
   java TTTClient
   ```
2. The client displays a reference board showing cell numbers 1-9:
   ```
   1|2|3
   -+-+-
   4|5|6
   -+-+-
   7|8|9
   ```
3. You will receive a welcome message: `Welcome Player 0` (or Player 1, 2, etc.)

### Making Moves:
1. Wait for your turn (the server will indicate whose turn it is)
2. Type a number from **1 to 9** corresponding to the cell where you want to place your symbol
3. Press Enter to submit your move
4. The updated board will be broadcast to all players
5. Players alternate turns automatically

### Game End Conditions:
- **Win**: When three symbols align horizontally, vertically, or diagonally
  - Message displayed: `Game over! Player X wins!`
- **Draw**: When all 9 cells are filled with no winner
  - Message displayed: `Touche! Draw game`
- **Quit**: Any player can type `Q` to end the game early

### Example Gameplay:
```
Select a cell to play from the board:
1|2|3
-+-+-
4|5|6
-+-+-
7|8|9

Welcome Player 0
************************************************************
 | | 
-+-+-
 | | 
-+-+-
 | | 

Player 0's turn
5
************************************************************
 | | 
-+-+-
 |O| 
-+-+-
 | | 

Player 1's turn
```

---

## 4. Overview of the Code and How Server and Client Work

### Architecture Diagram:
```
┌─────────────┐          ┌──────────────┐          ┌─────────────┐
│  TTTClient  │◄────────►│  TTTServer   │◄────────►│  TTTClient  │
│  (Player 0) │          │              │          │  (Player 1) │
└─────────────┘          └──────┬───────┘          └─────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │     Grid     │
                         │ (Game State) │
                         └──────────────┘
                                │
                        ┌───────┴───────┐
                        ▼               ▼
                  ┌──────────┐    ┌──────────┐
                  │TTTService│    │TTTService│
                  │(Thread 1)│    │(Thread 2)│
                  └──────────┘    └──────────┘
```

### Component Breakdown:

#### **TTTServer.java** (Server Main)
```java
public class TTTServer{
    public static void main(String[] args) throws IOException{
        final int S_PORT = 8080;
        ServerSocket server = new ServerSocket(S_PORT);
        Grid grid = new Grid();
        List<TTTService> clients = new ArrayList<>();
        int player = 0;
        
        while(true){
            Socket s = server.accept();
            TTTService service = new TTTService(s, grid, player, clients);
            clients.add(service);
            player++;
            Thread t = new Thread(service);
            t.start();
        }
    }
}
```

**Key Responsibilities:**
- Creates a `ServerSocket` listening on port 8080
- Maintains a **single shared `Grid` object** that all clients interact with
- Keeps a `List<TTTService>` to track all connected clients for broadcasting
- Accepts incoming client connections in an infinite loop
- For each connection, creates a `TTTService` instance and spawns a new thread
- Assigns sequential player numbers (0, 1, 2, ...)

---

#### **TTTClient.java** (Client)
```java
public class TTTClient {
    public static void main(String [] args) throws IOException{
        Socket s = new Socket("localhost", S_PORT);
        Scanner in = new Scanner(s.getInputStream());
        PrintWriter out = new PrintWriter(s.getOutputStream());
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
        
        Thread t = new Thread(new InputRunnable());
        t.start();
        
        while(in.hasNextLine()){
            String response = in.nextLine();
            System.out.println(response);
        }
    }
}
```

**Key Responsibilities:**
- Connects to the server using a `Socket` on localhost:8080
- Creates two I/O streams: one for sending (`PrintWriter`) and one for receiving (`Scanner`)
- **Dual-thread design**:
  - **Main thread**: Continuously reads messages from the server and displays them
  - **Input thread**: Reads user input from console and sends it to the server
- This design prevents blocking: the user can see server messages while typing
- Displays a reference board at startup showing cell numbers 1-9

---

#### **TTTService.java** (Service Handler)
```java
public class TTTService implements Runnable{
    private Socket s;
    private Scanner in;
    private PrintWriter out;
    private int player;
    private Grid grid;
    private List<TTTService> clients;

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
        in = new Scanner(s.getInputStream());
        out = new PrintWriter(s.getOutputStream());
        out.println("Welcome Player " + player);
        
        while(!grid.isOver() && in.hasNext()){
            String command = in.next();
            if(grid.getTurn() == player){
                doService(command);
            }
        }
    }

    private void doService(String command) {
        if(command.equals("Q")){
            grid.finish();
            broadcast("Player " + player + " quit.");
        } else {
            int num = Integer.parseInt(command);
            int info = grid.play(num, player);
            
            if(info == 0) {
                out.println("Already taken, try again");
                return;
            }
            
            String boardUpdate = "\n" + grid.toString() + "\n";
            if(grid.isTerminated()){
                broadcast(boardUpdate + "Game over! Player " + player + " wins!");
            } else if(grid.isDraw()){
                broadcast(boardUpdate + "Draw game");
            } else {
                broadcast(boardUpdate + "Player " + grid.getTurn() + "'s turn");
            }
        }
    }
}
```

**Key Responsibilities:**
- Each instance handles communication with one client in a separate thread
- **Turn validation**: Only processes commands when it's that player's turn
- **Broadcasting**: When a move is made, sends updates to ALL connected clients via the `clients` list
- **Move processing**: Calls `grid.play()` to update the game state
- **Game state checking**: After each move, checks for win/draw conditions
- **Error handling**: Validates that cells aren't already occupied

---

#### **Grid.java** (Game Logic)
```java
public class Grid {
    final int ROWS = 3;
    final int COLS = 3;
    private char[][] grid;
    private boolean isOver;
    private int turn;
    private Lock gridLock;

    Grid(){
        gridLock = new ReentrantLock();
        grid = new char[ROWS][COLS];
        isOver = false;
        turn = 0;
    }

    public int play(int x, int player){
        if(player != turn) return 2;
        
        char symbol = (player == 0) ? 'O' : 'X';

        gridLock.lock();
        try{
            if(grid[(x-1)/3][(x-1)%3] == 0){
                grid[(x-1)/3][(x-1)%3] = symbol;
                turn = (turn == 1) ? 0 : 1;
            } else {
                return 0;  // Cell occupied
            }
        } finally {
            gridLock.unlock();
        }
        return 1;  // Success
    }

    public boolean isTerminated(){
        // Check horizontal, vertical, and diagonal wins
        // Returns true if three-in-a-row found
    }

    public boolean isDraw(){
        // Check if board is full with no winner
    }

    public String toString(){
        // Returns formatted board representation
    }
}
```

**Key Responsibilities:**
- **State Management**: Stores the 3x3 grid as a `char[][]` array
- **Thread Safety**: Uses `ReentrantLock` to ensure only one thread modifies the grid at a time
- **Move Validation**: 
  - Checks if it's the correct player's turn
  - Converts cell numbers (1-9) to array indices using `(x-1)/3` and `(x-1)%3`
  - Verifies the cell is empty (value is 0)
- **Win Detection**: Checks all 8 possible win conditions (3 horizontal, 3 vertical, 2 diagonal)
- **Draw Detection**: Checks if board is full with no three-in-a-row
- **Display Formatting**: Converts the grid to a string with pipes and dashes

### Communication Flow:

**Client Makes a Move:**
```
1. Client types "5" and presses Enter
2. InputRunnable thread sends "5" to server via PrintWriter
3. Server's TTTService thread receives "5"
4. TTTService validates it's the player's turn
5. Calls grid.play(5, player)
6. Grid locks, updates cell, unlocks
7. TTTService broadcasts updated board to ALL clients
8. All clients' main threads receive and display the update
```

**Thread Safety:**
- The `Grid` class uses a `ReentrantLock` to prevent race conditions
- When `play()` is called, it acquires the lock, modifies the grid, then releases it
- This ensures that even if two players try to move simultaneously, only one will succeed at a time

---

## 5. Conclusion and Areas of Improvement

### Summary
This project successfully demonstrates a **functional networked Tic-Tac-Toe game** with the following accomplishments:
- ✅ Multiple players can connect and play simultaneously
- ✅ Real-time board updates are broadcast to all clients
- ✅ Thread-safe game state management prevents data corruption
- ✅ Win and draw conditions are accurately detected
- ✅ Clean separation of concerns (server, client, service, game logic)

The implementation showcases important concepts in **network programming**, **concurrent programming**, and **object-oriented design**. The use of Java's `Socket` API enables TCP/IP communication, while multithreading allows for non-blocking I/O and concurrent client handling.

---

### Areas of Improvement

#### 1. **Game Room Management**
**Current Issue**: All players join the same game, even if there are already 2 players.

**Improvement**: 
- Create a lobby system where players can join different game rooms
- Limit each game to 2 players (X and O)
- Implement a matchmaking system that pairs players automatically
```java
class GameRoom {
    Grid grid;
    List<TTTService> players; // Max size: 2
    boolean isFull() { return players.size() >= 2; }
}
```

#### 2. **Player Disconnection Handling**
**Current Issue**: If a player disconnects, the game may hang or crash.

**Improvement**:
- Detect when a client socket closes
- Notify the other player and end the game gracefully
- Remove disconnected clients from the broadcast list
```java
if(socket.isClosed() || !socket.isConnected()) {
    clients.remove(this);
    broadcast("Player " + player + " disconnected. Game over.");
}
```

#### 3. **Input Validation and Error Messages**
**Current Issue**: Invalid inputs (non-numeric, out of range) may cause crashes.

**Improvement**:
- Add try-catch for `NumberFormatException`
- Validate that input is between 1-9
- Provide helpful error messages
```java
try {
    int num = Integer.parseInt(command);
    if(num < 1 || num > 9) {
        out.println("Error: Please enter a number between 1 and 9");
        return;
    }
} catch(NumberFormatException e) {
    out.println("Error: Invalid input. Enter a number 1-9 or Q to quit");
    return;
}
```

#### 4. **Enhanced User Interface**
**Current Issue**: Console output can be cluttered and hard to follow.

**Improvement**:
- Clear the screen before displaying each updated board
- Add colors using ANSI escape codes (X in red, O in blue)
- Show whose turn it is more prominently
- Display move history

#### 5. **Spectator Mode**
**Current Improvement**: 
- Allow additional clients to connect as spectators (read-only)
- Spectators receive board updates but cannot make moves
- Useful for teaching or observing games

#### 6. **Game Statistics and Replay**
**Improvement**:
- Track wins/losses for each player
- Log all moves to a file for later replay
- Implement a "rematch" feature

#### 7. **Security Enhancements**
**Current Issue**: No authentication or encryption.

**Improvement**:
- Add player authentication (username/password)
- Use SSL/TLS for encrypted communication
- Prevent malicious inputs (SQL injection, buffer overflow attempts)

#### 8. **Scalability**
**Current Issue**: Uses a single-threaded server model (one thread per client).

**Improvement**:
- Use a thread pool with `ExecutorService` to limit resource usage
- Implement connection pooling for better performance
```java
ExecutorService threadPool = Executors.newFixedThreadPool(10);
threadPool.execute(new TTTService(s, grid, player, clients));
```

#### 9. **GUI Implementation**
**Major Enhancement**: Replace console interface with a graphical interface using JavaFX or Swing
- Click cells to make moves instead of typing numbers
- Visual board with images for X and O
- Menu system for starting new games, viewing stats, etc.

#### 10. **AI Opponent**
**Feature Addition**: Implement a computer player using minimax algorithm
- Allow single-player mode against an AI
- Different difficulty levels (easy, medium, hard)

---

### Final Thoughts

This project provides a solid foundation for understanding networked game development. The modular architecture makes it relatively easy to add the improvements listed above. The most critical enhancements would be:
1. **Proper game room management** (to support multiple concurrent games)
2. **Disconnect handling** (for robustness)
3. **Input validation** (for stability)

With these improvements, the system would be production-ready for a small-scale online Tic-Tac-Toe platform. The current implementation successfully demonstrates core concepts and provides an engaging multiplayer gaming experience.
