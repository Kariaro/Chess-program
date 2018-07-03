package scripters;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Window extends JFrame {
	private static final long serialVersionUID = 1L;
	private final Random random = new Random(0);
	
	public ChessBoard board;
	
	public BufferedImage[] Images = new BufferedImage[12];
	public BufferedImage[] Ghost  = new BufferedImage[12];
	public MouseAdapter    Mouse;
	public boolean flip_board = false;
	public boolean running  = false;
	public boolean computer = false;
	
	public Window(ChessBoard board) {
		setTitle("Brute Chess 2");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(640 + 6, 512 + 29);
		setResizable(false);
		
		try { // TODO: Make this less spagetti coded.
			BufferedImage bi = ImageIO.read(Window.class.getResource("/Chess.png"));
			/** Fill the array */
			for(int x = 0; x < 6; x++) for(int y = 0; y < 2; y++) Images[x + y * 6] = bi.getSubimage(x * 133, y * 133, 133, 133);
			for(int i = 0; i < 12; i++) { /** Rezise the image to 64x64 */
				BufferedImage resizedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = resizedImage.createGraphics();
				g.setComposite(AlphaComposite.Src);
				g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g.setRenderingHint(RenderingHints.KEY_RENDERING    , RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING , RenderingHints.VALUE_ANTIALIAS_ON);

				g.drawImage(Images[i], 0, 0, 64, 64, null);
				g.dispose();
				Images[i] = resizedImage;
			}
			for(int i = 0; i < 12; i++) { /** Make the images compatable with the screen */
				GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice device   = env.getDefaultScreenDevice();
				/* CREATE GHOST VERSION */ {
					BufferedImage buff = device.getDefaultConfiguration().createCompatibleImage(64, 64, Transparency.TRANSLUCENT);
					Graphics2D g2 = (Graphics2D)buff.getGraphics();
					
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
					g2.drawImage(Images[i], 0, 0, null);
					Ghost[i] = buff;
				}
				BufferedImage buff      = device.getDefaultConfiguration().createCompatibleImage(64, 64, Transparency.TRANSLUCENT);
				Graphics g = buff.getGraphics();
				g.drawImage(Images[i], 0, 0, null);
				Images[i] = buff;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		this.board = board;
	}
	
	public void start() {
		if(running) return;
		running = true;
		
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						Thread.sleep(10);
						if(bs != null) render();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		Mouse = new MouseAdapter() {
			public int SX, SY;
			public void mousePressed(MouseEvent e) {
				int x = x(e.getX()), y = y(e.getY()), b = e.getButton();
				if(x < 0 || x > 7 || y < 0 || y > 7) return;
				int id = x + y * 8;
				
				if(b == 1) {
					if(!flip_board) id = Flip(id);
					
					if(Click > -1 && moves != null) {
						if(board.IsPromoting()) {
							if(y == 4 && x > 1 && x < 6) {
								Move(Click, x - 1);
								Click = -1;
							}
							return;
						}
						for(Integer i : moves) {
							if(id == (i & 63)) {
								Move(Click, id);
								//System.out.println("Finished 1\n" + (id & 7) + ", " + (id >> 3));
								if(!board.IsPromoting()) Click = -1;
								break;
							}
						}
					}
					if(board.IsPromoting()) return;
					Click = id;
					
					SX = x * 64 + 32;
					SY = y * 64 + 32;
					
					moves = board.CurrentMoves.get(Click);
				} else flip_board = !flip_board;
			}
			public void mouseReleased(MouseEvent e) {
				SX = SY = -1;
				MouseX = MouseY = 0;
				
				int x = x(e.getX()), y = y(e.getY()), b = e.getButton();
				if(x < 0 || x > 7 || y < 0 || y > 7) return;
				int id = x + y * 8;
				
				if(b == 1) {
					if(!flip_board) id = Flip(id);
					if(moves != null) for(Integer i : moves) {
						if(id == (i & 63)) {
							Move(Click, id);
							//System.out.println("Finished 2\n" + (id & 7) + ", " + (id >> 3) + " R");
							if(!board.IsPromoting()) Click = -1;
							break;
						}
					}
				}
			}
			public void mouseDragged(MouseEvent e) {
				if(SX < 0) return;
				MouseX  = e.getX() -  3 - SX;
				MouseY  = e.getY() - 26 - SY;
				
				HoverX = x(e.getX()); HoverY = y(e.getY());
			}
			public void mouseMoved(MouseEvent e) {
				HoverX = x(e.getX()); HoverY = y(e.getY());
			}
			
			public int x(int i) { return (i -  3) >> 6; }
			public int y(int i) { return (i - 26) >> 6; }
			public void Move(int SQ, int NSQ) {
				if(computer) return;
				
				board.Move(SQ, NSQ);
				if(board.IsPromoting()) {
					moves = board.CurrentMoves.get(Click);
				}
			}
		};
		addMouseListener(Mouse);
		addMouseMotionListener(Mouse);
		
		setVisible(true);
	}
	
	private int Flip(int i) { return (i & 7) + (56 - (i & 56)); }
	public int MouseX = 0, MouseY = 0;
	public int HoverX = 0, HoverY = 0;
	public List<Integer> moves = null;
	
	public int Click = -1;
	
	public long currentSeed = 0;
	public boolean init = false;
	public long LAST = 0;
	public int mo = 0;
	public void render() {
		if(!init) {
			LAST = System.currentTimeMillis();
			init = true;
			random.setSeed(currentSeed);
			play(0);
			computer = true;
		}
		if(!ThreadRunning) {
			String type = "";
			switch(board.GetStatus()) {
				case ChessBoard.STALEMATE: type = "Stalemate"; break;
				case ChessBoard.CHECKMATE: type = "Checkmate"; break;
				case ChessBoard.RESIGN   : type = "Resign"; break;
				case ChessBoard.FIFTY_MOVE_RULE: type = "Fifty Move Rule"; break;
				case ChessBoard.THREEFOLD_REPETITION: type = "Threefold repetition"; break;
				case ChessBoard.TIME_CONTROL: type = "TimeControl"; break;
				default: type = "Unknown"; break;
			}
			//System.out.println("Game Won By [" + (board.GetTurn() == Board.WHITE ? "WHITE":"BLACK") + "]:[" + board.GetMove() + "] By {" + type + "}");
			mo += board.GetMove();
			board.ResetBoard();
			
			random.setSeed(++currentSeed);
			play(0);
		}
		if(System.currentTimeMillis() - LAST > 1000) {
			LAST += 1000;
			System.out.println("Moves / second == " + mo);
			mo = 0;
		}
		
		Graphics2D g = (Graphics2D)bs.getDrawGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Color[] C = new Color[] {
			new Color(0xF0D9B5), // Light Squares
			new Color(0xB58863), // Dark  Squares
			
			new Color(0x829769), // Light Selected
			new Color(0x646F40), // Dark  Selected
			
			new Color(0xAEB187),
			new Color(0x84794E),
		};
		
		for(int i = 0; i < 64; i++) {
			int x = i & 7, y = i >> 3;
			if(flip_board) y = (y - 7) * -1;
			
			g.setColor(C[(i + (i / 8)) & 1]);
			g.fillRect(x * 64 + 3, y * 64 + 26, 64, 64);
		}
		
		for(int i = 0; i < 64; i++) {
			int x = i & 7, y = 7 - (i >> 3);
			if(flip_board) y = (y - 7) * -1;
			
			int piece = board.board[x + (i >> 3) * 8] & 15;
			if(piece == ChessBoard.NONE) continue;
			
			if((piece & ChessBoard.TEAM) > 0) {
				piece &= 7;
				piece += 6;
			}
			
			g.drawImage(Images[piece], 3 + x * 64, 26 + y * 64, null);
		}
		
		if(Click > -1 && Click < 64 && moves != null && !board.IsPromoting()) {
			for(Integer num : moves) {
				int i = num & 63;
				
				int x = i & 7, y = i >> 3;
				if(!flip_board) y = (y - 7) * -1;
				g.setColor(C[4 + (y + x + (flip_board ? 1:0)) % 2]);
				
				if(HoverX == x && HoverY == y)
					g.fillRect(x * 64 + 3, y * 64 + 26, 64, 64);
				else g.fillOval(x * 64 + 27, y * 64 + 50, 16, 16);
			}
		}
		
		if(moves != null && Click > -1) {
			int piece = board.board[Click] & 15;
			
			if((piece & ChessBoard.TEAM) > 0) {
				piece &= 7;
				piece += 6;
			}
			
			int x = Click & 7, y = Click >> 3;
			if(!flip_board) y = (y - 7) * -1;
			g.setColor(C[2 + (x + y) % 2]);
			g.fillRect(x * 64 + 3, y * 64 + 26, 64, 64);
			
			g.drawImage(Ghost [piece], 3 + x * 64, 26 + y * 64, null);
			g.drawImage(Images[piece], 3 + x * 64 + MouseX, 26 + y * 64 + MouseY, null);
		}
		
		if(board.IsPromoting()) {
			int team = board.GetTurn();
			g.setColor(new Color(0x44000000, true));
			g.fillRect(3, 26, 512, 512);
			
			for(int i = 0; i < 4; i++) {
				if((HoverX - 2) == i && HoverY == 4) {
					g.fillRect(131 + i * 64, 282, 64, 64);
				}
			}
			g.drawImage(Images[ChessBoard.QUEEN  + team * 6], 3 + 128, 26 + 256, null);
			g.drawImage(Images[ChessBoard.BISHOP + team * 6], 3 + 192, 26 + 256, null);
			g.drawImage(Images[ChessBoard.ROOK   + team * 6], 3 + 256, 26 + 256, null);
			g.drawImage(Images[ChessBoard.KNIGHT + team * 6], 3 + 320, 26 + 256, null);
		}
		
		bs.show();
	}
	
	public void DrawPieces(Graphics2D g) {
		Color[] C = new Color[] {
			new Color(0xF0D9B5), // Light Squares
			new Color(0xB58863), // Dark  Squares
			
			new Color(0x829769), // Light Selected
			new Color(0x646F40), // Dark  Selected
			
			new Color(0xAEB187),
			new Color(0x84794E),
		};
		
		for(int i = 0; i < 64; i++) {
			int x = i & 7, y = i >> 3;
			if(flip_board) y = (y - 7) * -1;
			
			g.setColor(C[(i + (i / 8)) & 1]);
			g.fillRect(x * 64 + 3, y * 64 + 26, 64, 64);
		}
		
		for(int i = 0; i < 64; i++) {
			int x = i & 7, y = 7 - (i >> 3);
			if(flip_board) y = (y - 7) * -1;
			
			int piece = board.board[x + (i >> 3) * 8] & 15;
			if(piece == ChessBoard.NONE) continue;
			
			if((piece & ChessBoard.TEAM) > 0) {
				piece &= 7;
				piece += 6;
			}
			
			g.drawImage(Images[piece], 3 + x * 64, 26 + y * 64, null);
		}
	}
	
	private boolean RestartGame = false;
	private boolean ThreadRunning = false;
	public void play(final int ms) {
		if(ThreadRunning) return;
		ThreadRunning = true;
		
		new Thread(new Runnable() {
			public void run() {
				int i = 0;
				for(;i < 1024; i++) {
					if(board.GetStatus() != ChessBoard.PLAYING) break;
					
					DoRandomMove();
					
					try {
						Thread.sleep(ms);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				ThreadRunning = false;
				RestartGame = true;
			}
		}).start();
	}
	
	public void DoRandomMove() {
		if(board.GetStatus() != ChessBoard.PLAYING) return;
		Integer[] squares = board.CurrentMoves.keySet().toArray(new Integer[1]);
		Integer in = squares[(random.nextInt() & 63) % squares.length];
		if(in == null) return;
		
		int square = in;
		
		List<Integer> moves = board.CurrentMoves.get(square);
		int move = moves.get((random.nextInt() & 63) % moves.size()) & 63;
		
		board.Move(square, move);
		
		//System.out.println(UtilsText.ToCoord(square) + "->" + UtilsText.ToCoord(move) + ", " + board.GetMove());
	}
	
	public BufferStrategy bs;
	public void paint(Graphics g) {
		if(bs == null) {
			createBufferStrategy(2);
			bs = getBufferStrategy();
			return;
		}
	}
}
