package scripters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copyright (c) 2018 Victor Axberg
  * Last edited 2018-07-03
  */
public class ChessBoard {
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
	public static final int NONE   = 6;
	
	/* STATUS ID */
	public static final int PLAYING              = 0;
	public static final int STALEMATE            = 1;
	public static final int CHECKMATE            = 2;
	public static final int RESIGN               = 3;
	public static final int FIFTY_MOVE_RULE      = 4;
	public static final int THREEFOLD_REPETITION = 5;
	public static final int TIME_CONTROL         = 6;
	
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
	private boolean promoting = false;
	private int Status = 0;
	
	/** "IR" is the check for the rule
	  * Threefold repetition
	  *
	  * "IF" is the check for the rule 
	  * Fifty Moves without captures or check
	  */
	private int IR = 0, IF = 0, LM = -1;
	private int MOVE = 0;
	
	private int[] hit  = new int[64];
	public int[] board = new int[64];
	
	public ChessBoard() {
		CurrentMoves = new HashMap<Integer, List<Integer>>();
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
		
		if(moves_map.size() == 0) {
			int[] hit = GetMovesFromTeam(team == WHITE ? BLACK:WHITE, board);
			boolean attack = false;
			for(int i = 0; i < 64; i++) {
				if(Piece(board[i]) == KING && Team(board[i]) == team) {
					if(hit[i] > 0) attack = true;
					break;
				}
			}
			
			Status = attack ? CHECKMATE:STALEMATE;
			
			//System.out.println("Team: [" + (team == WHITE ? "WHITE":"BLACK") + "] got {" + (attack ? "Checkmated":"Stalemated") + "}");
		}
		
		return moves_map;
	}
	
	private void CalculateMove(int cs, int sq, int flags, int[] board, int[] out) {
		for(int i = 0; i < 64; i++) out[i] = board[i];
		
		if(flags == CASTLING) {
			int side = (sq % 56) - 2;
			int piece = board[cs];
			piece |= 0b10000; // Set Last Move bit
			
			out[cs] = NONE;
			out[sq] = piece;
			
			if(side == 0) {
				int rook = out[sq - 2];
				out[sq - 2] = NONE;
				out[sq + 1] = rook | 16;
			} else {
				int rook = out[sq + 1];
				out[sq + 1] = NONE;
				out[sq - 1] = rook | 16;
			}
		} else if(flags == ENPASSANT) {
			int piece = board[cs];
			piece |= 0b10000; // Set Last Move bit
			
			if((piece & TEAM) == WHITE)
				board[sq - 8] = NONE;
			else
				board[sq + 8] = NONE;
			
			out[cs] = NONE;
			out[sq] = piece;
		} else if(flags == PROMOTION) {
			int piece = board[cs];
			piece |= 0b10000; // Set Last Move bit
			out[cs] = NONE;
			
			out[sq] = piece;
		} else {
			int piece = board[cs];
			piece |= 0b10000; // Set Last Move bit
			
			out[cs] = NONE;
			out[sq] = piece;
		}
	}
	
	private void FixAttacked(int[] board) {
		int[] moves = GetMovesFromTeam((MOVE & 1) == WHITE ? BLACK:WHITE, board);
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
			board[i] = id & 0b111111;
		}
		return board;
	}
	public String GetBoardID() { return GetBoardID(board); }
	public String GetBoardID(int[] board) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < 64; i++) builder.append(BOARD_CHARACTERS.charAt(board[i] & 0b111111));
		return builder.toString();
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
		}
		return new int[64];
	}
	
	public void Move(int cs, int sq) { Move(cs, sq, board); }
	public void Move(int cs, int sq, int[] board) {
		if(!CurrentMoves.containsKey(cs)) return;
		List<Integer> moves = CurrentMoves.get(cs);
		
		for(int i : moves) {
			if((i & 63) != sq) continue;
			
			// Remove Set Move Bit
			for(int j = 0; j < board.length; j++) {
				if((board[j] & WAS_MOVED) > 0) {
					board[j] &= ~(WAS_MOVED);
					board[j] |= HAS_MOVED;
				}
			}
			
			int flags = (i >> 6) & ~(VALID | SAFE);
			//System.out.println("Flags: " + flags);
			
			if(flags == CASTLING) {
				int side = (sq % 56) - 2;
				int piece = board[cs];
				piece |= 0b10000; // Set Last Move bit
				
				board[cs] = NONE;
				board[sq] = piece;
				
				if(side == 0) {
					int rook = board[sq - 2];
					board[sq - 2] = NONE;
					board[sq + 1] = rook | 16;
				} else {
					int rook = board[sq + 1];
					board[sq + 1] = NONE;
					board[sq - 1] = rook | 16;
				}
			} else if(flags == ENPASSANT) {
				int piece = board[cs];
				piece |= 0b10000; // Set Last Move bit
				
				if((piece & TEAM) == WHITE)
					board[sq - 8] = NONE;
				else
					board[sq + 8] = NONE;
				
				board[cs] = NONE;
				board[sq] = piece;
				
				IF = MOVE + 1;
			} else if(flags == PROMOTION) {
				CurrentMoves.clear();
				List<Integer> list = new ArrayList<Integer>();
				list.add(QUEEN  + ((VALID | SAFE | PROMOTION2) << 6) + (sq << 12));
				list.add(BISHOP + ((VALID | SAFE | PROMOTION2) << 6) + (sq << 12));
				list.add(ROOK   + ((VALID | SAFE | PROMOTION2) << 6) + (sq << 12));
				list.add(KNIGHT + ((VALID | SAFE | PROMOTION2) << 6) + (sq << 12));
				CurrentMoves.put(cs, list);
				
				promoting = true;
				break;
			} else if((flags & 63) == PROMOTION2) {
				int id = i & 63;
				int piece = (board[cs] & 0b111000) + id;
				piece |= 0b10000; // Set Last Move bit
				board[cs] = NONE;
				board[i >> 12] = piece;
				
				promoting = false;
			} else {
				int piece = board[cs];
				piece |= 0b10000; // Set Last Move bit
				
				if(board[sq] != NONE || Piece(piece) == PAWN) IF = MOVE + 1;
				board[cs] = NONE;
				board[sq] = piece;
			}
			
			/** If a move has been made */ {
				MOVE++;
				FixAttacked(board);
				
				CurrentMoves.clear();
				CurrentMoves.putAll(CalculateValidMoves(GetBoardID(), MOVE & 1));
				
				if(MOVE - IF > 50) {
					Status = FIFTY_MOVE_RULE;
					return;
				}
				
				if(LM < 0) {
					LM = cs + (sq << 6);
					IR = 1;
				} else {
					if(IR == 1) {
						LM |= (cs + (sq << 6)) << 12;
						IR = 2;
					} else if(IR > 1) {
						int sel = (IR & 1) == 0 ? 0:12;
						int id = (LM >> sel) & 4095;
						int d0 = id & 63;
						int d1 = id >> 6;
						
						if(d0 == sq && d1 == cs) {
							IR++;
							LM &= ~(0b111111111111 << sel);
							LM |= (d1 | (d0 << 6)) << sel;
							
							if(IR > 5) {
								//System.out.println("Draw by Threefold Repetition");
								Status = THREEFOLD_REPETITION;
								return;
							}
						} else { LM = cs + (sq << 6); IR = 1; }
					}
				}
			}
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
			if(di_0[i] > 0 && (d0 + i) > -1 && (d0 + i) < 8) moves[i + (d0 + i) * 8] = 1;
			if(di_1[i] > 0 && (d1 - i) > -1 && (d1 - i) < 8) moves[i + (d1 - i) * 8] = 1;
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
			if(di_0[i] > 0 && (d0 + i) > -1 && (d0 + i) < 8) moves[i + (d0 + i) * 8] = 1;
			if(di_1[i] > 0 && (d1 - i) > -1 && (d1 - i) < 8) moves[i + (d1 - i) * 8] = 1;
		}
		
		return moves;
	}
	
	public void ResetBoard() {
		for(int i = 0; i < 64; i++) hit[i] = 0;
		
		SetBoard(CHESS_BOARD);
		IR = IF = 0;
		Status  = 0;
		MOVE    = 0;
		
		CurrentMoves.clear();
		CurrentMoves.putAll(CalculateValidMoves(GetBoardID(), 0));
	}
	public void ResetBoard(String ID) {
		SetBoard(ID);
		FixAttacked(board);
		IR = IF = 0;
		Status  = 0;
		MOVE    = 0;
		
		CurrentMoves.clear();
		CurrentMoves.putAll(CalculateValidMoves(GetBoardID(), 0));
	}
	
	public int GetStatus() { return Status; }
	public void Resign(int team) {
		Status = RESIGN;
	}
	
	public boolean IsPromoting() { return promoting; }
	public int GetTurn() { return MOVE & 1; }
	public int GetMove() { return MOVE; }
	
	/** 000 0 0 0
	  * |   | | |
	  * |   | | |
	  * |   | | ^----------< has moved once or more
	  * |   | ^------------< was moved this turn
	  * |   ^--------------< team
	  * ^------------------< piece ID
	  */
	public int Team(int i) { if(i == NONE) return -1; return (i & TEAM) >> 3; }
	public int Piece(int i) { return i & PIECE_ID; }
	public boolean WasMoved(int i) { return (i & WAS_MOVED) > 0; }
	public boolean HasMoved(int i) { return (i & HAS_MOVED) > 0; }
	
	// TODO: Quick Bookmark
	
	/* ============================================= */
	
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
		int team = Team(piece), f = team<1?1:-1;
		boolean last_rank = (team == WHITE) ? (y == 6):(y == 1);
		boolean enpassant = (team == WHITE) ? (y == 4):(y == 3);
		
		/* BASIC forward once or twice */
		if(!last_rank) {
			int P0 = board[x + (y + f) * 8];
			if(Piece(P0) == NONE) { // Move forward
				array[x] = 1 | SAFE;
				if(!HasMoved(piece)) { // Move forward two steps
					//TODO: This could be faulty if a custom setup is played
					int P1 = board[x + (y + f * 2) * 8];
					if(Piece(P1) == NONE) array[x + 8] = 1 | SAFE;
				}
			}
			if(x > 0) { // Left piece
				int LP = board[x - 1 + y * 8];
				int P1 = board[x - 1 + (y + f) * 8];
				if(Piece(P1) != NONE && Team(P1) != team) array[x - 1] = 1;
				if(Piece(LP) == PAWN && Team(LP) != team && !HasMoved(LP) && WasMoved(LP) && enpassant) array[x - 1] = 1 | ENPASSANT | SAFE;
			}
			if(x < 7) { // Right piece
				int RP = board[x + 1 + y * 8];
				int P1 = board[x + 1 + (y + f) * 8];
				if(Piece(P1) != NONE && Team(P1) != team) array[x + 1] = 1;
				if(Piece(RP) == PAWN && Team(RP) != team && !HasMoved(RP) && WasMoved(RP) && enpassant) array[x + 1] = 1 | ENPASSANT | SAFE;
			}
		} else { // Middle piece
			int MP = board[x + (y + f) * 8];
			if(Piece(MP) == NONE) array[x] = 1 | PROMOTION | SAFE;

			if(x > 0) { // Left piece
				int LP = board[(x - 1) + (y + f) * 8];
				if(Piece(LP) != NONE && Team(LP) != team) array[x - 1] = 1 | PROMOTION | SAFE;
			}
			if(x < 7) { // Right piece
				int RP = board[(x + 1) + (y + f) * 8];
				if(Piece(RP) != NONE && Team(RP) != team) array[x + 1] = 1 | PROMOTION | SAFE;
			}
		}

		return array;
	}
	public int[] GetKingAdvanced(int x, int y, int[] board) {
		int[] array = new int[11];
		int piece = board[x + y * 8];
		int team = Team(piece);
		
		/* Normal moves */ {
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
}
