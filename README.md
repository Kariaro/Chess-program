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

You can do whatever you want that follows the rules of chess.

### Getting the moves
``` java
/* The first integer defines from what square you want the moves
 * and the List of integers, the squares it can go to.
 *
 * On a empty board. If a i have the Rook stand on a1 the possible moves
 * by the Rook will be { a2, a3, a4, a5, a6, a7, a8, b1, c1, d1, e1, f1, h1 }
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
UtilsText.PrintMoves(chess.GetMoves(<x>, <y>, chess.board), "0123456789");
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
