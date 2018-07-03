# Chess-program

This is program that calculates what moves each side can make and returning them as a list.
You can then use that list to do your move.


## Inspiration
I like challanges and Chess is definetly one of them.
Chess is complicated game with a set of complicated rules.
Not that they are hard to comprehend but to recreate in a programable way.
My mission was to create a chess game that i could play.

I hope you can learn or find ways to improve from my code or create your own code as thats one of my goals.

## How to use

You start by create a instance of the class ChessBoard.
``` java
ChessBoard chess = new ChessBoard();
/* To setup the board you can use a string like this
 * "rnbqkbnrpppppppp................................PPPPPPPPRNBQKBNR"
 * r = Rook, n = Knight, b = Bishop, q = Queen, k = King, p = Pawn
 * 
 * All characters except "rnbqkpRNBQKP" will be empty squares.
 * Lowercase is the white team and uppercase, the black team.
 *
 * If you do not specify a string the default chess setup will be used.
 */
ResetBoard();
```

You can do whatever you want that follows the rules of chess.

### Getting the moves
``` java
/* The first integer defines from what square you want the moves
 * and the List of integers, the squares it can go to.
 *
 * On a empty board. If a i have the Rook stand on a1 the possible moves
 * by the Rook will be { a2, a3, a4, a5, a6, a7, a8, b1, c1, d1, e1, f1, h1 }
 */
Map<Integer, List<Integer>> moves = chess.CurrentMoves;

// To visualize the moves you can use a couple of methods from UtilsText

/* UtilsText.PrintMoveMap(moves) will print the list with
 *  a chess like format.
 *
 * That rook's moves whould be outputed like this
 *   {a1=[a2, a3, a4, a5, a6, a7, a8, b1, c1, d1, e1, f1, h1]}
 */
System.out.println("UtilsText.PrintMoveMap");
UtilsText.PrintMoveMap(moves);

System.out.println("UtilsText.PrintMoves");
UtilsText.PrintMoves(chess.GetMoves(x, y, chess.board), "0123456789");
```

### Doing a random move

Well that header says it all.
I made a litle code to do random moves and with that i check the speed of my program.
This code completes around 8000 moves a second on my computer.

``` java
// I created this check just because its gonna get weird with more than
// one thread doing moves at a time.
private Random random = new Random();
private boolean ThreadRunning = false;
public void play(final int ms) {
    if(ThreadRunning) return;
    ThreadRunning = true;
    
    new Thread(new Runnable() {
        public void run() {
            int max_moves = 1024;
            for(int i = 0; i < max_moves; i++) {
                if(chess.GetStatus() != ChessBoard.PLAYING) break;
                
                DoRandomMove();
                // UtilsText.PrintBoard(chess.board);
                // UtilsText.PrintMoveMap(chess.CurrentMoves);
                try {
                    Thread.sleep(ms);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            
            ThreadRunning = false;
        }
    }).start();
}

public void DoRandomMove() {
    if(chess.GetStatus() != ChessBoard.PLAYING) return;
    Integer[] squares = chess.CurrentMoves.keySet().toArray(new Integer[1]);
    Integer in = squares[(random.nextInt() & 63) % squares.length];
    if(in == null) return;
    
    int square = in;
    
    List<Integer> moves = chess.CurrentMoves.get(square);
    int move = moves.get((random.nextInt() & 63) % moves.size()) & 63;
    
    chess.Move(square, move);
    
    //System.out.println(UtilsText.ToCoord(square) + "->" + UtilsText.ToCoord(move) + ", " + chess.GetMove());
}
```

### Status

The very last thing i made was that i implemented a Status variable wich indicates whats going on right now.

``` java
/* This will return one of the following
 * PLAYING              : This is default state returned
 * STALEMATE            : The king is not attacked no valid moves exist
 * CHECKMATE            : The king is attacked with no valid moves to make
 * FIFTY_MOVE_RULE      : Fifty moves without any captures or pawn moves
 * THREEFOLD_REPETITION : Three identical moves has been made by both players.
 */
int Status = chess.GetStatus();

// This will return true if one of the pawns has reached it's last rank.
boolean promote = chess.IsPromoting();
```

## GUI

You can either use the class Window.java i with your own set of pieces
or by using the class UtilsText.java with the method PrintBoard.

``` java
/* This is gonna print the board to the console.
 *
 * The output will look like this.
 * r n b q k b n r
 * p p p p p p p p
 * ´ ´ ´ ´ ´ ´ ´ ´
 * ´ ´ ´ ´ ´ ´ ´ ´
 * ´ ´ ´ ´ ´ ´ ´ ´
 * ´ ´ ´ ´ ´ ´ ´ ´
 * P P P P P P P P
 * R N B Q K B N R
 */
UtilsText.PrintBoard(chess.board);

/* The UI Window.java uses resembels the looks of lichess.org
 *
 * I recommend that you do not use this class as it is not intended to be
 * a part of the source code and that you instead create your own version of it.
 * 
 * This class is a Template showing how i made a program GUI using the
 * ChessBoard class
 */
Window window = new Window(chess);
window.start();
```
