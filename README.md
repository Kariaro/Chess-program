# Chess-program
A small chess engine calculating the board and what moves a piece can make

This program does not include the Window class for the GUI.
ChessBoard.java is a engine calculating all the moves the pieces can make.

## How to use

First you create a instance of ChessBoard.
``` java
ChessBoard chess = new ChessBoard();
/* To setup the board you can use a string like this
 * "rnbqkbnrpppppppp................................PPPPPPPPRNBQKBNR"
 * 
 * r = Rook, n = Knight, b = Bishop, q = Queen, k = King, p = Pawn
 * 
 * The dots are blank spaces, you can use whatever character
 * you want except "rnbqkpRNBQKP"
 * 
 * The lowercase characters represents the white team
 * and the uppercase the black team.
 *
 *
 * You can use this method with a empty string for the
 * default setup or with a string for a custom game.
 * 
 * This method is needed to initialize the game.
 */
ResetBoard();
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

// The UI Window.java uses resembels the looks of lichess.org
Window window = new Window(chess);
window.start();
```
