import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Grid {
    final int ROWS = 3;
    final int COLS = 3;
    private char [][] grid;
    private boolean isOver;
    private int turn;
    private Lock gridLock;

    Grid(){
        gridLock = new ReentrantLock();
        grid = new char[ROWS][COLS];
        isOver = false;
        turn = 0;               //starting at player 0
    }

    public Grid(char [][] grid){
        this.grid = grid;
        gridLock = new ReentrantLock();
        isOver = false;
        turn = 0;               //starting at player 0
    }

    /**
     * plays a x or o on the grid depending on whose turn it is.
     * flips the turn to the opposing player once turn is made
     * 
     * @param x : index of the cell to be played
     * @return 1 if successfully played, 0 if cell already taken, 2 if not your turn
     */
    public int play(int x, int player){
        if(player != turn) return 2;
        
        char symbol;
        if(player == 0) symbol = 'O';
        else symbol = 'X';

        gridLock.lock();
        try{
            if(grid[(x-1)/3][(x-1)%3] == 0){
                grid[(x-1)/3][(x-1)%3] = symbol;
                if(turn==1) turn = 0;
                else turn = 1;
            }
            else{
                return 0;
            }
        }
        finally{
            gridLock.unlock();
        }
        return 1;
    }
    
    public int getTurn(){
        return turn;
    }

    public boolean isOver(){
        return isOver;
    }
    public void finish(){
        isOver = true;
    }

    public boolean isTerminated(){
        boolean found = false;
        //check horizontal
        for(int i=0; i<ROWS; i++){
            if(grid[i][0]==grid[i][1] && grid[i][1]==grid[i][2] && grid[i][0]!=0){
                found = true;
            }
        }
        //check vertical
        for(int j=0; j<COLS; j++){
            if(grid[0][j]==grid[1][j] && grid[1][j]==grid[2][j] && grid[0][j]!=0){
                found = true;
            }
        }
        //check diagonals
        if(grid[0][0]==grid[1][1] && grid[1][1]==grid[2][2] && grid[0][0]!=0){
            found = true;
        }
        if(grid[0][2]==grid[1][1] && grid[1][1]==grid[2][0] && grid[0][2]!=0){
            found = true;
        }
        
        isOver = found;
        return isOver;
    }

    public boolean isDraw(){
        boolean found = false;
        // check if unfinished grid
        for (char[] row : grid){
            for (char cell : row){
                if(cell == 0)
                    return false;
            }
        }
        //check horizontal
        for(int i=0; i<ROWS; i++){
            if(grid[i][0]==grid[i][1] && grid[i][1]==grid[i][2] && grid[i][0]!=0){
                found = true;
            }
        }
        //check vertical
        for(int j=0; j<COLS; j++){
            if(grid[0][j]==grid[1][j] && grid[1][j]==grid[2][j] && grid[0][j]!=0){
                found = true;
            }
        }
        //check diagonals
        if(grid[0][0]==grid[1][1] && grid[1][1]==grid[2][2] && grid[0][0]!=0){
            found = true;
        }
        if(grid[0][2]==grid[1][1] && grid[1][1]==grid[2][0] && grid[0][2]!=0){
            found = true;
        }
        
        isOver = true;
        return !found;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < ROWS; i++){
            for(int j = 0; j < COLS; j++){
                if(grid[i][j] == 0)
                    sb.append(" ");
                else sb.append(grid[i][j]);
                if(j < COLS - 1){
                    sb.append("|");
                }
            }
            if(i < ROWS - 1){
                sb.append("\n-+-+-\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}
