package scripters;

import static scripters.ChessBoard.*;

import java.util.List;
import java.util.Map;

public class UtilsText {
    public static void PrintMoves(int[] array) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 64; i++) {
            char c = '´';
            int s = i < array.length ? array[i]:0;
            if(     (s & PROMOTION) > 0) c = '4';
            else if((s & ENPASSANT) > 0) c = '3';
            else if((s & CASTLING ) > 0) c = '2';
            else if((s & VALID    ) > 0) c = '1';
            builder.append(c).append(" ");
            if(i % 8 == 7) builder.append("\n");
        }
        System.out.println(builder.toString());
    }
    
    public static void PrintMoves(int[] array, String str) {
        char[] set = str.toCharArray();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 64; i++) {
            int s = i < array.length ? (array[i] & 0b1111):12;
            builder.append(set[s >= set.length ? set.length - 1:s]).append(" ");
            if(i % 8 == 7) builder.append("\n");
        }
        System.out.println(builder.toString());
    }
    
    public static void PrintBoard(int[] array) {
        char[] set = "kqbnrp´KQBNRP".toCharArray();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 64; i++) {
            int ID = i < array.length ? array[i]:NONE;
            builder.append(set[(ID & 7) + ((ID & 8) >> 3) * 7]).append(" ");
            if(i % 8 == 7) builder.    append("\n");
        }
        System.out.println(builder.toString());
    }
    
    public static void PrintMoveMap(Map<Integer, List<Integer>> map) {
        if(map.keySet().size() < 1) return;
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for(Integer i : map.keySet()) {
            builder.append((char)((i & 7) + 97)).append((char)(i / 8 + 49)).append("=[");
            List<Integer> moves = map.get(i);
            for(int j = 0; j < moves.size(); j++) {
                int val = moves.get(j);
                int pos = val & 63;
                builder.append((char)((pos & 7) + 97)).append((char)(pos / 8 + 49));
                if(j < moves.size() - 1) builder.append(", ");
            }
            
            builder.append("], ");
        }
        builder.replace(builder.length() - 2, builder.length(), "}");
        System.out.println(builder.toString());
    }
    
    public static String ToCoord(int i) {
        return (char)((i & 7) + 97) + "" + (char)(i / 8 + 49);
    }
    
    /*public static void PrintMoves(int[] array) {
        char[] set = "0123".toCharArray();
        StringBuilder builder = new StringBuilder();
        //builder.append("   A B C D E F G H   \n\n");
        for(int i = 0; i < 64; i++) {
            //if(i % 8 == 0) builder.append(i / 8 + 1).append(" ");
            char c = '´';
            int s = array[i];
            if((s & 0b0001) > 0) c = '1';
            if((s & 0b0010) > 0) c = '2';
            if((s & 0b0100) > 0) c = '3';
            if((s & 0b1000) > 0) c = '4';
            builder.append(c).append(" ");
            if(i % 8 == 7) builder.//append("  ").append(i / 8 + 1).
            append("\n");
        }
        //builder.append("\n   A B C D E F G H");
        System.out.println(builder.toString());
    }*/
}
