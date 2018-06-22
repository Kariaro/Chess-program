package scripters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChessBoard {
	public static final void main(String[] args) {
		System.out.println("MAIN: CHESSBOARD\n");
		new ChessBoard();
	}
	
	/* PRIVATE GLOBALS */
	private static final String BOARD_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+=";
	
	/* GLOBAL PRESETS */
	public static final String CHESS_BOARD = "rnbqkbnrpppppppp                                PPPPPPPPRNBQKBNR";
	public static final String CASTLING_E  = "r   k  r                                                R   K  R";
	
	/* GLOBAL PIECES */
	public static final int KING   = 0;
	public static final int QUEEN  = 1;
	public static final int BISHOP = 2;
	public static final int KNIGHT = 3;
	public static final int ROOK   = 4;
	public static final int PAWN   = 5;
	public static final int NONE   = 6; // TODO: Make this the standard value for a empty square
	
	/* GLOBAL TEAMS */
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	
	/* GLOBAL FLAGS */
	public static final int VALID      = 1 << 0;
	public static final int CASTLING   = 1 << 1;
	public static final int ENPASSANT  = 1 << 2;
	public static final int PROMOTION  = 1 << 3;
	public static final int SAFE       = 1 << 4;
	public static final int PROMOTION2 = 1 << 5;
	
	public static final int PIECE_ID  =  0b111;
	public static final int TEAM      = 1 << 3;
	public static final int WAS_MOVED = 1 << 4;
	public static final int HAS_MOVED = 1 << 5;
	
	/* LOCAL VARIABLES*/
	public Map<Integer, List<Integer>> CurrentMoves;
	public int MOVE = 0;
	
	public int[] hit   = new int[64];
	public int[] moves = new int[64];
	public int[] board = new int[64];
	public Piece2[] pieces = new Piece2[64];
	
	public ChessBoard() {
		SetBoard(
			"        " +
			"        " +
			"        " +
			"        " +
			"        " +
			"        " +
			"Q  p    " +
			"    k   "
		);
		SetBoard(CHESS_BOARD);
		CurrentMoves = CalculateValidMoves(GetBoardID(), 0);
		
		/** 000 0 0 0
		  * |   | | |
		  * |   | | |
		  * |   | | ^----------< has moved once or more
		  * |   | ^------------< was moved this turn
		  * |   ^--------------< team
		  * ^------------------< piece ID
		  */
		
		
		String ID = GetBoardID();
		System.out.println("Current Board: " + ID);
		
		UtilsText.PrintMoveMap(CurrentMoves);
		
		for(int i = 0; i < 256; i++) {
			DoRandomMove();
			UtilsText.PrintBoard(board);
			UtilsText.PrintMoveMap(CurrentMoves);
		}
		
	}
	
	public void play(int ms) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				for(int i = 0; i < 64; i++) {
					if(!can_play) break;
					DoRandomMove();
					UtilsText.PrintBoard(board);
					UtilsText.PrintMoveMap(CurrentMoves);
					
					try {
						Thread.sleep(ms);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();
		
		can_play = true;
	}
	
	public boolean can_play = true;
	public Random r = new Random(9);
	public void DoRandomMove() {
		Integer[] squares = CurrentMoves.keySet().toArray(new Integer[1]);
		Integer in = squares[(r.nextInt() & 63) % squares.length];
		if(in == null || squares.length == 0) {
			can_play = false;
			return;
		}
		int square = in;
		
		List<Integer> moves = CurrentMoves.get(square);
		int move = moves.get((r.nextInt() & 63) % moves.size()) & 63;
		
		Move(square, move, board);
		
		System.out.println(UtilsText.ToCoord(square) + "->" + UtilsText.ToCoord(move) + ", " + MOVE);
	}
	
	public Map<Integer, List<Integer>> CalculateValidMoves(String ID, int team) {
		Map<Integer, List<Integer>> moves_map = new HashMap<Integer, List<Integer>>();
		int[] board = CreateBoard(ID);
		int[] out = new int[64];
		for(int i = 0; i < 64; i++) out[i] = NONE;
		
		for(int i = 0; i < 64; i++) {
			int piece = board[i];
			if(piece == NONE || Team(piece) != team) continue;
			int x_ = i & 7, y_ = i >> 3;
			
			int[] valid_moves = GetMoves(x_, y_, board);
			
			int size = 0;
			for(int j = 0; j < 64; j++) {
				int m = valid_moves[j];
				if(m == 0) continue;
				
				CalculateMove(i, j, m & ~(VALID | SAFE), board, out); {
					int k = 0;
					for(int v = 0; v < 64; v++) if(Piece(out[v]) == KING && Team(out[v]) == team) { k = v; break; }
					
					int[] attacked = GetMovesFromTeam(team == WHITE ? BLACK:WHITE, out);
					if(attacked[k] > 0) valid_moves[j] = 0; else size++;
				}
			}
			
			if(size > 0) {
				List<Integer> list = new ArrayList<Integer>();
				for(int j = 0; j < 64; j++) {
					int m = valid_moves[j];
					if(m == 0) continue;
					
					list.add(j + (m << 6));
				}
				moves_map.put(i, list);
			}
		}
		
		return moves_map;
	}
	
	private void CalculateMove(int cs, int sq, int flags, int[] board, int[] out) {
		for(int i = 0; i < 64; i++) out[i] = board[i];
		
		if(flags == CASTLING) {
			int side = (sq % 56) - 2;
			//System.out.println("{CASTLING}");
			int piece = board[cs];
			piece |= 0b110000; // Set Last Move bit, and HasMoved bit
			
			out[cs] = NONE;
			out[sq] = piece;
			
			if(side == 0) {
				int rook = out[sq - 2];
				out[sq - 2] = NONE;
				out[sq + 1] = rook;
			} else {
				int rook = out[sq + 1];
				out[sq + 1] = NONE;
				out[sq - 1] = rook;
			}
		} else if(flags == ENPASSANT) {
			//System.out.println("{ENPASSANT}");
			int piece = board[cs];
			piece |= 0b110000; // Set Last Move bit, and HasMoved bit
			
			out[sq - 8] = NONE;
			out[cs] = NONE;
			out[sq] = piece;
		} else if(flags == PROMOTION) { // TODO: When Move() is done remember to rewrite this
			//System.out.println("{PROMOTION}");
			int piece = board[cs];
			piece |= 0b110000; // Set Last Move bit, and HasMoved bit
			out[cs] = NONE;
			
			if(cs / 8 == 1)
				out[cs - 8] = piece;
			else
				out[cs + 8] = piece;
		} else {
			int piece = board[cs];
			piece |= 0b110000; // Set Last Move bit, and HasMoved bit
			
			out[cs] = NONE;
			out[sq] = piece;
		}
	}
	
	private void FixAttacked(int[] board) {
		int[] moves = GetMovesFromTeam((MOVE % 2 < 1) ? BLACK:WHITE, board);
		for(int i = 0; i < 64; i++) hit[i] = moves[i];
	}
	
	private static final String NAMES = "kqbnrpKQBNRP";
	public void SetBoard(String s) {
		for(int i = 0; i < 64; i++) {
			int id = NONE;
			if(i < s.length()) {
				id = NAMES.indexOf(s.charAt(i));
				if(id > 5) {
					id -= 6;
					id |= 0b1000;
				}
				if(id < 0) id = NONE;
			}
			board[i] = id;
		}
		
		MOVE = 0;
		FixAttacked(board);
	}
	
	public void LoadBoard(String str) {
		MOVE = 0;
		for(int i = 0; i < 64; i++) {
			int id = NONE;
			if(i < str.length()) {
				int ch = str.charAt(i);
				id = BOARD_CHARACTERS.indexOf(ch);
				if(id < 0) id = NONE;
			}
			if(HasMoved(id) && Team(id) > 0) MOVE = 1;
			board[i] = id & 0b111111;
		}
		FixAttacked(board);
	}
	
	public int[] CreateBoard(String str) {
		int[] board = new int[64];
		for(int i = 0; i < 64; i++) {
			int id = NONE;
			if(i < str.length()) {
				int ch = str.charAt(i);
				id = BOARD_CHARACTERS.indexOf(ch);
				if(id < 0) id = NONE;
			}
			//if(HasMoved(id) && Team(id) > 0) MOVE = 1;
			board[i] = id & 0b111111;
		}
		return board;
	}
	
	public int[] GetMovesFromTeam(int team, int[] board) {
		int[] moves = new int[64];
		for(int y = 0; y < 8; y++) for(int x = 0; x < 8; x++) {
			int piece = board[x + y * 8];
			if(piece == NONE || Team(piece) != team) continue;
			int[] m = GetMoves(x, y, board);
			for(int i = 0; i < 64; i++) moves[i] += m[i] & 1;
		}
		return moves;
	}
	
	public int[] GetMoves(int x, int y, int[] board) {
		int id = Piece(board[x + y * 8]);
		switch(id) {
			case KING  : return King  (x, y, board);
			case QUEEN : return Queen (x, y, board);
			case BISHOP: return Bishop(x, y, board);
			case KNIGHT: return Knight(x, y, board);
			case ROOK  : return Rook(x, y, board);
			case PAWN  : return Pawn(x, y, board);
			default:
				// TODO: Nothing should ever appear here. Debug if thats true / otherwise fix
		}
		return new int[64];
	}
	
	
	
	public int[] GetDiagonal_1(int x, int y, int[] board) {
		int team = (board[x + y * 8] >> 3) & 1;
		int sy = y + x;
		
		int[] array = { 6, 6, 6, 6, 6, 6, 6, 6 };
		for(int i = 0; i < 8; i++) {
			int y_ = sy - i;
			if(y_ < 0 || y_ > 7) continue;
			array[i] = board[i + y_ * 8] & 0b1111;
		}
		
		int l = 0, h = 8;
		for(int i = 0; i < 8; i++) {
			if(i == x || array[i] == NONE) continue;
			int p = array[i] >> 3;
			
			if(i < x) {
				if(p == team) l = i + 1;
				else l = i;
			} else if(p == team) {
				h = i - 1;
				break;
			} else {
				h = i;
				break;
			}
		}
		
		for(int i = 0; i < 8; i++) {
			if(i >= l && i <= h && i != x) array[i] = 1;
			else array[i] = 0;
		}
		return array;
	}
	public int[] GetDiagonal_0(int x, int y, int[] board) {
		int team = (board[x + y * 8] >> 3) & 1;
		int sy = y - x;
		
		int[] array = { 6, 6, 6, 6, 6, 6, 6, 6 };
		for(int i = 0; i < 8; i++) {
			int y_ = sy + i;
			if(y_ < 0 || y_ > 7) continue;
			array[i] = board[i + y_ * 8] & 0b1111;
		}
		
		int l = 0, h = 8;
		for(int i = 0; i < 8; i++) {
			if(i == x || array[i] == NONE) continue;
			int p = array[i] >> 3;
			
			if(i < x) {
				if(p == team) l = i + 1;
				else l = i;
			} else if(p == team) {
				h = i - 1;
				break;
			} else {
				h = i;
				break;
			}
		}
		
		for(int i = 0; i < 8; i++) {
			if(i >= l && i <= h && i != x) array[i] = 1;
			else array[i] = 0;
		}
		return array;
	}
	public int[] GetVertical  (int x, int y, int[] board) {
		int team = (board[x + y * 8] >> 3) & 1;
		int[] array = {
			board[x +  0] & 0b1111,
			board[x +  8] & 0b1111,
			board[x + 16] & 0b1111,
			board[x + 24] & 0b1111,
			board[x + 32] & 0b1111,
			board[x + 40] & 0b1111,
			board[x + 48] & 0b1111,
			board[x + 56] & 0b1111,
		};
		int l = 0, h = 8;
		for(int i = 0; i < 8; i++) {
			if(i == y || array[i] == NONE) continue;
			int p = array[i] >> 3;
			
			if(i < y) {
				if(p == team) l = i + 1;
				else l = i;
			} else if(p == team) {
				h = i - 1;
				break;
			} else {
				h = i;
				break;
			}
		}
		
		for(int i = 0; i < 8; i++) {
			if(i >= l && i <= h && i != y) array[i] = 1;
			else array[i] = 0;
		}
		return array;
	}
	public int[] GetHorizontal(int x, int y, int[] board) {
		int team = (board[x + y * 8] >> 3) & 1;
		int[] array = {
			board[y * 8]     & 0b1111,
			board[1 + y * 8] & 0b1111,
			board[2 + y * 8] & 0b1111,
			board[3 + y * 8] & 0b1111,
			board[4 + y * 8] & 0b1111,
			board[5 + y * 8] & 0b1111,
			board[6 + y * 8] & 0b1111,
			board[7 + y * 8] & 0b1111,
		};
		int l = 0, h = 8;
		for(int i = 0; i < 8; i++) {
			if(i == x || array[i] == NONE) continue;
			int p = array[i] >> 3;
			
			if(i < x) {
				if(p == team) l = i + 1;
				else l = i;
			} else if(p == team) {
				h = i - 1;
				break;
			} else {
				h = i;
				break;
			}
		}
		
		for(int i = 0; i < 8; i++) {
			if(i >= l && i <= h && i != x) array[i] = 1;
			else array[i] = 0;
		}
		return array;
	}
	public int[] GetPawnAdvanced(int x, int y, int[] board) {
		int[] array = new int[16];
		int piece = board[x + y * 8];
		int team = Team(piece);
		
		if(team == WHITE) {
			/* BASIC forward once or twice */
			if(y < 6) {
				int P0 = board[x + (y + 1) * 8];
				if(Piece(P0) == NONE) {
					array[x] = 1 | SAFE; // MOVE FORWARD
					if(y == 1 && !HasMoved(piece)) {
						int P1 = board[x + (y + 2) * 8]; // MOVE FORWARD TWO STEPS
						if(Piece(P1) == NONE) array[x + 8] = 1 | SAFE;
					}
				}
				if(x > 0) {
					int LP = board[x - 1 + y * 8]; // LEFT PIECE
					int P1 = board[x - 1 + (y + 1) * 8];
					if(Piece(P1) != NONE && Team(P1) != team) array[x - 1] = 1;
					if(Piece(LP) != NONE && Team(LP) != team && WasMoved(LP) && Piece(LP) == PAWN) array[x - 1] = 1 | ENPASSANT | SAFE;
				}
				if(x < 7) {
					int RP = board[x + 1 + y * 8]; // RIGHT PIECE
					int P1 = board[x + 1 + (y + 1) * 8];
					if(Piece(P1) != NONE && Team(P1) != team) array[x + 1] = 1;
					if(Piece(RP) != NONE && Team(RP) != team && WasMoved(RP) && Piece(RP) == PAWN) array[x + 1] = 1 | ENPASSANT | SAFE;
				}
			} else if(y == 6) {
				int MP = board[x + (y + 1) * 8]; // MIDDLE PIECE
				if(Piece(MP) == NONE) array[x] = 1 | PROMOTION | SAFE;
				
				if(x > 0) {
					int LP = board[(x - 1) + (y + 1) * 8]; // LEFT PIECE
					if(Piece(LP) != NONE && Team(LP) != team) array[x - 1] = 1 | PROMOTION | SAFE;
				}
				if(x < 7) {
					int RP = board[(x + 1) + (y + 1) * 8]; // RIGHT PIECE
					if(Piece(RP) != NONE && Team(RP) != team) array[x + 1] = 1 | PROMOTION | SAFE;
				}
			}
		}
		if(team == BLACK) {
			/* BASIC forward once or twice */
			if(y > 1) {
				int P0 = board[x + (y - 1) * 8];
				if(Piece(P0) == NONE) {
					array[x] = 1 | SAFE; // MOVE FORWARD
					if(y == 6 && !HasMoved(piece)) {
						int P1 = board[x + (y - 2) * 8]; // MOVE FORWARD TWO STEPS
						if(Piece(P1) == NONE) array[x + 8] = 1 | SAFE;
					}
				}
				if(x > 0) {
					int LP = board[x - 1 + y * 8]; // LEFT PIECE
					int P1 = board[x - 1 + (y - 1) * 8];
					if(Piece(P1) != NONE && Team(P1) != team) array[x - 1] = 1;
					if(Piece(LP) != NONE && Team(LP) != team && WasMoved(LP) && Piece(LP) == PAWN) array[x - 1] = 1 | ENPASSANT | SAFE;
				}
				if(x < 7) {
					int RP = board[x + 1 + y * 8]; // RIGHT PIECE
					int P1 = board[x + 1 + (y - 1) * 8];
					if(Piece(P1) != NONE && Team(P1) != team) array[x + 1] = 1;
					if(Piece(RP) != NONE && Team(RP) != team && WasMoved(RP) && Piece(RP) == PAWN) array[x + 1] = 1 | ENPASSANT | SAFE;
				}
			} else if(y == 1) {
				int MP = board[x + (y - 1) * 8]; // MIDDLE PIECE
				if(Piece(MP) == NONE) array[x] = 1 | PROMOTION | SAFE;
				
				if(x > 0) {
					int LP = board[(x - 1) + (y - 1) * 8]; // LEFT PIECE
					if(Piece(LP) != NONE && Team(LP) != team) array[x - 1] = 1 | PROMOTION | SAFE;
				}
				if(x < 7) {
					int RP = board[(x + 1) + (y - 1) * 8]; // RIGHT PIECE
					if(Piece(RP) != NONE && Team(RP) != team) array[x + 1] = 1 | PROMOTION | SAFE;
				}
			}
		}
		
		return array;
	}
	public int[] GetKingAdvanced(int x, int y, int[] board) {
		int[] array = new int[11];
		int piece = board[x + y * 8];
		int team = Team(piece);
		
		/* NORMAL MOVES */ {
			if(x > 0) {
				if(team != Team(board[x - 1 + y * 8])) array[3] = 1;
				if(y > 0) {
					if(team != Team(board[x - 1 + (y - 1) * 8])) array[0] = 1;
					if(team != Team(board[x     + (y - 1) * 8])) array[1] = 1;
				}
				if(y < 7) {
					if(team != Team(board[x - 1 + (y + 1) * 8])) array[6] = 1;
					if(team != Team(board[x     + (y + 1) * 8])) array[7] = 1;
				}
			}
			if(x < 7) {
				if(team != Team(board[x + 1 + y * 8])) array[5] = 1;
				if(y > 0) {
					if(team != Team(board[x + 1 + (y - 1) * 8])) array[2] = 1;
					if(team != Team(board[x     + (y - 1) * 8])) array[1] = 1;
				}
				if(y < 7) {
					if(team != Team(board[x + 1 + (y + 1) * 8])) array[8] = 1;
					if(team != Team(board[x     + (y + 1) * 8])) array[7] = 1;
				}
			}
		}
		
		if(!HasMoved(piece) && x == 4) { /* CASTLING */
			// TODO: x == 4 / If possible, find a better way to do this
			
			int R0 = board[0 + y * 8]; // Queenside
			if(Piece(R0) == ROOK && !HasMoved(R0)) {
				int[] hori = GetHorizontal(x, y, board);
				int s = team * 56;
				if(hori[1] > 0 && hori[2] > 0 && hori[3] > 0 && (hit[s+2]<1&&hit[s+3]<1&&hit[s+4]<1)) array[9] = 1 | CASTLING | SAFE;
			}
			
			int R1 = board[7 + y * 8]; // Kingside
			if(Piece(R1) == ROOK && !HasMoved(R1)) {
				int[] hori = GetHorizontal(x, y, board);
				int s = team * 56;
				if(hori[5] > 0 && hori[6] > 0 && (hit[s+4]<1&&hit[s+5]<1&&hit[s+6]<1)) array[10] = 1 | CASTLING | SAFE;
			}
		}
		
		return array;
	}
	
	public void Move(int cs, int sq) { Move(cs, sq, board); }
	public void Move(int cs, int sq, int[] board) { // TODO: Fix minor bugs + vvv Pawn PROMOTION
		if(!CurrentMoves.containsKey(cs)) return;
		boolean moved = false;
		List<Integer> moves = CurrentMoves.get(cs);
		
		for(int i : moves) {
			if((i & 63) != sq || moved) continue;
			
			for(int j = 0; j < board.length; board[j++] &= ~(WAS_MOVED)); // Remove Set Move Bit
			
			int flags = (i >> 6) & ~(VALID | SAFE);
			
			System.out.println("Flags: " + flags);
			
			if(flags == CASTLING) {
				int side = (sq % 56) - 2;
				// System.out.println("{CASTLING}");
				int piece = board[cs];
				piece |= 0b110000; // Set Last Move bit, and HasMoved bit
				
				board[cs] = NONE;
				board[sq] = piece;
				
				if(side == 0) {
					int rook = board[sq - 2];
					board[sq - 2] = NONE;
					board[sq + 1] = rook;
				} else {
					int rook = board[sq + 1];
					board[sq + 1] = NONE;
					board[sq - 1] = rook;
				}
				
				MOVE ++;
				FixAttacked(board);
				
				moved = true;
			} else if(flags == ENPASSANT) {
				System.out.println("{ENPASSANT}");
				int piece = board[cs];
				piece |= 0b110000; // Set Last Move bit, and HasMoved bit
				
				board[sq - 8] = NONE;
				board[cs] = NONE;
				board[sq] = piece;
				
				
				MOVE++;
				FixAttacked(board);
				
				moved = true;
			} else if(flags == PROMOTION) {
				System.out.println("{PROMOTION}");
				
				CurrentMoves.clear();
				List<Integer> list = new ArrayList<Integer>();
				list.add(QUEEN  + ((VALID | SAFE | PROMOTION2) << 6));
				list.add(BISHOP + ((VALID | SAFE | PROMOTION2) << 6));
				list.add(ROOK   + ((VALID | SAFE | PROMOTION2) << 6));
				list.add(KNIGHT + ((VALID | SAFE | PROMOTION2) << 6));
				CurrentMoves.put(cs, list);
				break;
			} else if(flags == PROMOTION2) { // TODO: Add pawn PROMOTION
				System.out.println("{PROMOTION2}");
				
				int id = i & 63;
				int piece = (board[cs] & 0b111000) + id;
				piece |= 0b110000; // Set Last Move bit, and HasMoved bit
				board[cs] = NONE;
				
				if(cs / 8 == 1)
					board[cs - 8] = piece;
				else
					board[cs + 8] = piece;
				
				MOVE++;
				FixAttacked(board);
				
				moved = true;
			} else {
				int piece = board[cs];
				piece |= 0b110000; // Set Last Move bit, and HasMoved bit
				
				board[cs] = NONE;
				board[sq] = piece;
				
				MOVE++;
				FixAttacked(board);
				
				moved = true;
			}
		}
		
		if(moved) {
			CurrentMoves = CalculateValidMoves(GetBoardID(), MOVE & 1);
		}
	}
	
	public void PrintInfo(int x, int y) {
		int piece = board[x + y * 8] & 0b111111;
		String S1 = Integer.toBinaryString(piece & 0b111); S1 = "000".substring(S1.length()) + S1;
		System.out.println(
			"ID: [" + S1 + "], " +
			"team{" + ((piece >> 3) & 1) + "}, " +
			"wasmoved{" + ((piece & WAS_MOVED) / WAS_MOVED) + "}, " +
			"hasmoved{" + ((piece & HAS_MOVED) / HAS_MOVED) + "}"
		);
	}
	
	public int[] Knight(int x, int y, int[] board) {
		int[] moves = new int[64];
		int[] p = { -1, 2, 1, 2, 2, 1, 2, -1, 1, -2, -1, -2, -2, -1, -2, 1 };
		int team = Team(board[x + y * 8]);
		for(int i = 0; i < 8; i++) {
			int x_ = p[i*2+0] + x, y_ = p[i*2+1] + y;
			if(x_ < 0 || x_ > 7 || y_ < 0 || y_ > 7) continue;
			if(team != Team(board[x_ + y_ * 8])) moves[x_ + y_ * 8] = 1;
		}
		return moves;
	}
	public int[] King(int x, int y, int[] board) {
		int[] moves = new int[64];
		int[] king = GetKingAdvanced(x, y, board);
		for(int i = 0; i < 9; i++) {
			if(king[i] > 0) moves[(x + (i % 3) - 1) + (y + (i / 3) - 1) * 8] = king[i];
		}
		int team = Team(board[x + y * 8]);
		moves[team * 56 + 2] = king[ 9];
		moves[team * 56 + 6] = king[10];
		
		return moves;
	}
	public int[] Pawn(int x, int y, int[] board) {
		int[] moves = new int[64];
		int[] pawn = GetPawnAdvanced(x, y, board);
		int piece = board[x + y * 8];
		int team  = Team(piece) * -2 + 1;
		
		for(int i = 0; i < pawn.length; i++) {
			int y_ = i / 8 + 1;
			
			if(pawn[i] > 0) moves[(i % 8) + (y + y_ * team) * 8] = pawn[i];
		}
		
		return moves;
	}
	public int[] Rook(int x, int y, int[] board) {
		int[] moves = new int[64];
		int[] hori = GetHorizontal(x, y, board);
		int[] vert = GetVertical  (x, y, board);
		
		for(int i = 0; i < 8; i++) {
			if(hori[i] > 0) moves[i + y * 8] = 1;
			if(vert[i] > 0) moves[x + i * 8] = 1;
		}
		return moves;
	}
	public int[] Bishop(int x, int y, int[] board) {
		int[] moves = new int[64];
		int d0 = y - x, d1 = y + x;
		int[] di_0 = GetDiagonal_0(x, y, board);
		int[] di_1 = GetDiagonal_1(x, y, board);
		
		for(int i = 0; i < 8; i++) {
			if(di_0[i] > 0 && (d0 + i) > -1 && (d0 + i) < 7) moves[i + (d0 + i) * 8] = 1;
			if(di_1[i] > 0 && (d1 - i) > -1 && (d1 - i) < 7) moves[i + (d1 - i) * 8] = 1;
		}
		return moves;
	}
	public int[] Queen(int x, int y, int[] board) {
		int[] moves = new int[64];
		int d0 = y - x, d1 = y + x;
		int[] hori = GetHorizontal(x, y, board);
		int[] vert = GetVertical  (x, y, board);
		int[] di_0 = GetDiagonal_0(x, y, board);
		int[] di_1 = GetDiagonal_1(x, y, board);
		
		for(int i = 0; i < 8; i++) {
			if(hori[i] > 0) moves[i + y * 8] = 1;
			if(vert[i] > 0) moves[x + i * 8] = 1;
			if(di_0[i] > 0 && (d0 + i) > -1 && (d0 + i) < 7) moves[i + (d0 + i) * 8] = 1;
			if(di_1[i] > 0 && (d1 - i) > -1 && (d1 - i) < 7) moves[i + (d1 - i) * 8] = 1;
		}
		return moves;
	}
	
	public String GetBoardID() { return GetBoardID(board); }
	public String GetBoardID(int[] board) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < 64; i++) builder.append(BOARD_CHARACTERS.charAt(board[i] & 0b111111));
		return builder.toString();
	}
	
	// TODO: Make this format the standard
	/** 000 0 0 0
	  * |   | | |
	  * |   | | |
	  * |   | | ^----------< has moved once or more
	  * |   | ^------------< was moved this turn
	  * |   ^--------------< team
	  * ^------------------< piece ID
	  */
	//public boolean LastMove(int i) { return (i & 0b1000) > 0; }
	//public int Move(int i) { return (i >> 5) & 0b11111111111; }
	public int Team(int i) { if(i == NONE) return -1; return (i & TEAM) >> 3; }
	public int Piece(int i) { return i & PIECE_ID; }
	public boolean WasMoved(int i) { return (i & WAS_MOVED) > 0; }
	public boolean HasMoved(int i) { return (i & HAS_MOVED) > 0; }
}
