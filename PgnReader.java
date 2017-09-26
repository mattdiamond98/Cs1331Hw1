import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PgnReader {

    /**
     * LOWERCASE IS BLACK
     * UPPERCASE IS WHITE
     */
    private static char[][] chessBoard =
    {
/*8*/   {'r','n','b','q','k','b','n','r'},
/*7*/   {'p','p','p','p','p','p','p','p'},
/*6*/   {'0','0','0','0','0','0','0','0'},
/*5*/   {'0','0','0','0','0','0','0','0'},
/*4*/   {'0','0','0','0','0','0','0','0'},
/*3*/   {'0','0','0','0','0','0','0','0'},
/*2*/   {'P','P','P','P','P','P','P','P'},
/*1*/   {'R','N','B','Q','K','B','N','R'},
    //    a   b   c   d   e   f   g   h
    };

    //Convert chess notation to row,col matrix notation
    public static String matrixCoords(String coords) {
        return v(8-(((int)coords.charAt(1))-48),
                            coords.charAt(0)-97);
    }
    //this is like a constructor method
    public static String v(int y, int x) {
        return "" + y + "," + x;
    }

    public static int vY(String v) {
        return Integer.parseInt(v.split(",")[0]);
    }
    public static int vX(String v) {
        return Integer.parseInt(v.split(",")[1]);
    }

    public static String vAdd(String v1, String v2) {
        int vx = vX(v1) + vX(v2);
        int vy = vY(v1) + vY(v2);
        return v(vy,vx);
    }

    //primarily used to flip vectors by multiplying by -1
    public static String vMult(String v, int n) {
        int vx = vX(v) * n;
        int vy = vY(v) * n;
        return v(vy,vx);
    }
    //Not actually normalize, but set components to max |1|
    public static String vNorm(String v) {
        int vY = vY(v);
        int vX = vX(v);
        if (vY != 0)
            vY /= Math.abs(vY);
        if (vX != 0)
            vX /= Math.abs(vX);
        return v(vY,vX);
    }

    public static boolean isDiag(String v) {
        int vY = vY(v);
        int vX = vX(v);
        return vY != 0 && vX != 0;
    }

    public static char pieceAt(String pos) {
        if (!isValidPosition(pos)) return 'x';
        return chessBoard[vY(pos)][vX(pos)];
    }

    public static boolean isEnemy(String pos1, String pos2) {
        return isEnemy(pieceAt(pos1),pieceAt(pos2));
    }

    public static boolean isEnemy(char c1, char c2) {
        return Character.isUpperCase(c1) ?
            Character.isLowerCase(c2) : Character.isUpperCase(c2);
    }

    public static boolean isValidPosition(String v) {
        if (!v.contains(",")) return false;
        int vx = vX(v);
        int vy = vY(v);
        return vx >= 0 && vx <= 7 && vy >= 0 && vy <= 7;
    }

    public static String[] validPositions(String[] moves) {
        String vpos = "";
        for (int i = 0; i < moves.length; i++) {
            if (isValidPosition(moves[i])) {
                vpos += moves[i];
            }
            if (i != moves.length - 1) {
                vpos += ":";
            }
        }
        return vpos.split(":");
    }

    /**
     * Find the tagName tag pair in a PGN game and return its value.
     *
     * @see http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm
     *
     * @param tagName the name of the tag whose value you want
     * @param game a `String` containing the PGN text of a chess game
     * @return the value in the named tag pair
     */
    public static String tagValue(String tagName, String game) {
        int tagIndex = game.indexOf("[" + tagName);
        if (tagIndex < 0) {
            return "NOT GIVEN";
        }
        int startIndex = game.indexOf("\"", tagIndex) + 1;
        int endIndex = game.indexOf("\"", startIndex);
        return game.substring(startIndex, endIndex);
    }

    /**
     * Play out the moves in game and return a String with the game's
     * final position in Forsyth-Edwards Notation (FEN).
     *
     * @see http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm#c16.1
     *
     * @param game a `String` containing a PGN-formatted chess game or opening
     * @return the game's final position in FEN.
     */
    public static String finalPosition(String game) {
        String gameSequence = game.substring(game.lastIndexOf("]")+1)
            .replace(".","").replace("\n"," ").replace("?","").trim();
        String[] moveSets = getMoveSets(gameSequence);
        for (String moveSet : moveSets) {
            doMoveSet(moveSet);
        }
        //TODO: turn this into FEN notation before turning in
        String finalPosition = "";
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard.length; j++) {
                finalPosition += chessBoard[i][j];
            }
            finalPosition += "/";
        }

        String builder = "";
        while (finalPosition.length() > 0) {
            char c = finalPosition.charAt(0);
            if (c == '0') {
                int n = 0;
                while (finalPosition.charAt(0) == '0') {
                    n++;
                    finalPosition = finalPosition.substring(1);
                }
                builder += n;
            } else {
                builder += c;
                finalPosition = finalPosition.substring(1);
            }
        }

        return builder;
    }

    public static String[] getAllPiecesOfType(char c) {
        String builder = "";
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[i].length; j++) {
                if (chessBoard[i][j] == c) {
                    builder += v(i,j) + ":";
                }
            }
        }
        return builder.substring(0,builder.length() - 1).split(":");
    }

    //Returns a set of positions, based on the vector coordinate of a piece
    //that includes all positions the piece can move to
    public static String[] getAllPossibleMoves(String v, boolean capture,
        String dis) {

        int y = vY(v);
        int x = vX(v);
        char pieceType = chessBoard[y][x];
        boolean white = Character.isUpperCase(pieceType);
        if (dis.length() > 0) {
            if (!isValidPiece(v,dis)) {
                return new String[0];
            }
        }
        switch (Character.toLowerCase(pieceType)) {
            case 'p': return getAllMovesPawn(v(y, x), white, capture, dis);
            case 'r': return getAllMovesRook(v(y, x), white, capture, dis);
            case 'b': return getAllMovesBishop(v(y, x), white, capture, dis);
            case 'q': return getAllMovesQueen(v(y, x), white, capture, dis);
            case 'n': return getAllMovesKnight(v(y, x), white, capture, dis);
            case 'k': return getAllMovesKing(v(y, x), white, capture, dis);
            default: return null;
        }
    }

    public static String[] getAllMovesPawn(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        int dir = white ? -1 : 1;
        String moves = "";
        if (capture) {
            for (int i : new int[]{-1,1}) {
                String posNorm = vAdd(pos, v(dir, i));
                if (isEnemy(pos,posNorm) ||
                    isEnemy(pos,vAdd(posNorm,v(dir * -1,0))) &&
                    pieceAt(posNorm) == '0' &&
                    pieceAt(vAdd(posNorm,v(dir,0))) == '0') {
                    moves += posNorm + ":";
                }
            }
            //TODO: code for en passant
        } else {
            String posNorm = vAdd(pos, v(dir,0));
            if (pieceAt(posNorm) == '0') {
                moves += posNorm + ":";

                if (y == (white ? 6 : 1)) {
                    String posDouble = vAdd(pos, v(dir * 2, 0));
                    if (pieceAt(posDouble) == '0') {
                        moves += posDouble + ":";
                    }
                }
            }
        }
        //TODO: en passant capability
        return validPositions(moves.split(":"));
    }

    //TODO: castling handling
    public static String[] getAllMovesRook(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        String moves = "";

        String[] dirs = {"1,0","0,1","-1,0","0,-1"};
        for (String dir : dirs) {
            String nextMove = vAdd(pos,dir);
            while (isValidPosition(nextMove)) {
                if (pieceAt(nextMove) == '0') {
                    if (!capture) {
                        moves += nextMove + ":";
                    }
                    nextMove = vAdd(nextMove,dir);
                } else {
                    if (capture && isEnemy(pos,nextMove)) {
                        moves += nextMove + ":";
                    }
                    break;
                }
            }
        }
        return validPositions(moves.split(":"));
    }

    public static String[] getAllMovesBishop(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        String moves = "";

        String[] dirs = {"1,1","-1,1","1,-1","-1,-1"};
        for (String dir : dirs) {
            String nextMove = vAdd(pos,dir);
            while (isValidPosition(nextMove)) {
                if (pieceAt(nextMove) == '0') {
                    if (!capture) {
                        moves += nextMove + ":";
                    }
                    nextMove = vAdd(nextMove,dir);
                } else {
                    if (capture && isEnemy(pos,nextMove)) {
                        moves += nextMove + ":";
                    }
                    break;
                }
            }
        }
        return validPositions(moves.split(":"));
    }

    public static String[] getAllMovesQueen(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        String moves = "";
        String[] dirs = {"1,1","-1,1","1,-1","-1,-1","1,0","0,1","-1,0","0,-1"};
        for (String dir : dirs) {
            String nextMove = vAdd(pos,dir);
            while (isValidPosition(nextMove)) {
                if (pieceAt(nextMove) == '0') {
                    if (!capture) {
                        moves += nextMove + ":";
                    }
                    nextMove = vAdd(nextMove,dir);
                } else {
                    if (capture && isEnemy(pos,nextMove)) {
                        moves += nextMove + ":";
                    }
                    break;
                }
            }
        }
        return validPositions(moves.split(":"));
    }

    public static String[] getAllMovesKnight(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        String moves = "";

        String[] dirs = {"2,1","1,2","-1,2","-2,1",
            "-2,-1","-1,-2","1,-2","2,-1"};
        for (String dir : dirs) {
            String move = vAdd(pos,dir);
            if (pieceAt(move) == '0' && !capture ||
                isEnemy(pos, move) && capture) {
                moves += move + ":";
            }
        }
        return validPositions(moves.split(":"));
    }

    //TODO: worry about moving into check? maybe not bc only one king ever
    public static String[] getAllMovesKing(String pos, boolean white,
    boolean capture, String disambiguation) {
        int y = vY(pos);
        int x = vX(pos);
        String moves = "";

        String[] dirs = {"1,0","0,1","-1,0","0,-1",
                         "1,1","1,-1","-1,1","-1,-1"};
        for (String dir : dirs) {
            String move = vAdd(pos,dir);
            if (pieceAt(move) == '0' && !capture ||
                isEnemy(pos, move) && capture) {
                moves += move + ":";
            }
        }
        return validPositions(moves.split(":"));
    }

    public static boolean isValidPiece(String pos, String disamb) {
        int y = vY(pos);
        int x = vX(pos);
        if (disamb.contains("Y:")) {
            int dy = Integer.parseInt(""+disamb.charAt(disamb.indexOf("Y:")+2));
            if (dy != y) return false;
        }
        if (disamb.contains("X:")) {
            int dx = Integer.parseInt(""+disamb.charAt(disamb.indexOf("X:")+2));
            if (dx != x) return false;
        }
        return true;
    }


    //here




    public static String[] getMoveSets(String game) {
        game = " " + game;
        String moveSets = "";

        int lastIndex = 0;
        for (int i = 2; lastIndex >= 0; ++i) {
            int nextIndex = game.indexOf(" " + i + " ", lastIndex);
            if (nextIndex > 0) {
                moveSets += game.substring(lastIndex +
                    (""+(i-1)).length() + 2, nextIndex) + ":";
            } else {
                moveSets += game.substring(lastIndex +
                    (""+(i-1)).length() + 2);
            }
            lastIndex = nextIndex;
        }

        return moveSets.split(":");
    }

    public static void doMoveSet(String moveSet) {
        String[] moves = moveSet.split(" ");
        doMove(moves[0], true);
        if (moves.length > 1) {
            doMove(moves[1], false);
        }
    }

    public static void doMove(String move, boolean white) {
        if (move.contains("-")) {
            return;
        }
        char type = move.charAt(0);
        if (type == 'O') {
            //handle castling
            int y = white ? 7 : 0;
            if (move.equals("O-O")) { //Kingside castling
                chessBoard[y][4] = '0';
                chessBoard[y][5] = white ? 'R' : 'r';
                chessBoard[y][6] = white ? 'K' : 'k';
                chessBoard[y][7] = '0';
            } else { //Queenside castling
                chessBoard[y][4] = '0';
                chessBoard[y][3] = white ? 'R' : 'r';
                chessBoard[y][2] = white ? 'K' : 'k';
                chessBoard[y][0] = '0';
            }
            return;
        }
        if (Character.isLowerCase(type)) {
            type = 'P';
        } else {
            move = move.substring(1);
        }
        if (!white) {
            type = Character.toLowerCase(type);
        }
        boolean capture = false;
        if (move.contains("x")) {
            move = move.replace("x","");
            capture = true;
        }
        if (move.contains("#")) {
            //these signify checkmate moves. Saving code spot for later
            move = move.replace("#","");
        }
        if (move.contains("+")) {
            //same thing for checking moves
            move = move.replace("+","");
        }

        //This is a bit of a hack.
        //Basic idea is to append on any extra information that the methods
        //later might need, with a tag to access the info.
        String disambiguation = "";
        char promotion = 'p';
        if (move.contains("=")) {
        //information for pawn promotion
        //disambiguation += "P:" + move.substring(move.indexOf("=")+1) + ";";
            promotion = move.substring(move.indexOf("=") + 1).charAt(0);
            if (white) {
                promotion = Character.toUpperCase(promotion);
            } else {
                promotion = Character.toLowerCase(promotion);
            }
            move = move.replace(move.substring(move.indexOf("=")),"");
        }
        if (move.length() > 2) {
            //information of starting rank/file
            String prefix = move.substring(0,move.length()-2);
            move = move.substring(move.length()-2);
            //for debugging, might leave in because it shouldn't hurt anything
            if (prefix.length() > 2) {
                System.out.println(">>> Warning: received too long prefix");
            }
            while (prefix.length() > 0) {
                char c = prefix.charAt(0);
                if (Character.isDigit(c)) {
                    disambiguation += "Y:" + (8-(((int)c)-48));
                }
                if (Character.isLetter(c)) {
                    disambiguation += "X:" + (((int)c)-97);
                }
                prefix = prefix.substring(1);
            }
        }
        String finalLocation = matrixCoords(move.substring(move.length() - 2));
        String startLocation = "";
        for (String piece : getAllPiecesOfType(type)) {
            for (String possibleMove : getAllPossibleMoves(piece, capture,
                disambiguation)) {
                if (possibleMove.equals(finalLocation)) {
                    if (startLocation.equals("")) {
                        startLocation = piece;
                    } else {
                        //we have access to startLocation and finalLocation
                        //and type
                        String kingPos = getAllPiecesOfType(
                            white ? 'K' : 'k')[0];
                        String dir = vNorm(vAdd(startLocation,
                            vMult(kingPos,-1)));

                        boolean wouldCheck = false;

                        boolean cont = true;
                        for (String nextMove = vAdd(kingPos,dir); cont;
                            nextMove = vAdd(nextMove,dir)) {
                            if (!nextMove.equals(startLocation) &&
                                pieceAt(nextMove) != '0') {
                                if (nextMove.equals(finalLocation)) {
                                    cont = false; //no check
                                } else if (isEnemy(nextMove, kingPos)) {
                                    cont = false;
                                    //in the future, replace with
                                    //check for vectors
                                    switch (Character.toLowerCase(
                                        pieceAt(nextMove))) {
                                        case 'b':
                                            wouldCheck = isDiag(dir);
                                            break;
                                        case 'r':
                                            wouldCheck = !isDiag(dir);
                                            break;
                                        case 'q':
                                            wouldCheck = true;
                                    }
                                } else {
                                    cont = false; //no check
                                }
                            }

                        }

                        if (!wouldCheck) {
                            startLocation = piece;
                        }

                    }
                }
            }
        }
        if (!startLocation.equals("")) {
            if (Character.toLowerCase(type) == 'p' && capture &&
            pieceAt(finalLocation) == '0') {
                int d = white ? 1 : -1;
                chessBoard[vY(finalLocation) + d][vX(finalLocation)] = '0';
            }

            chessBoard[vY(finalLocation)][vX(finalLocation)] = type;
            chessBoard[vY(startLocation)][vX(startLocation)] = '0';

            if (Character.toLowerCase(type) == 'p' &&
                vY(finalLocation) == (white ? 0 : 7)) {
                chessBoard[vY(finalLocation)][vX(finalLocation)] = promotion;
            }

        }
    }

    /**
     * Reads the file named by path and returns its content as a String.
     *
     * @param path the relative or abolute path of the file to read
     * @return a String containing the content of the file
     */
    public static String fileContent(String path) {
        Path file = Paths.get(path);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                // Add the \n that's removed by readline()
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
            System.exit(1);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String game = fileContent(args[0]);
        System.out.format("Event: %s%n", tagValue("Event", game));
        System.out.format("Site: %s%n", tagValue("Site", game));
        System.out.format("Date: %s%n", tagValue("Date", game));
        System.out.format("Round: %s%n", tagValue("Round", game));
        System.out.format("White: %s%n", tagValue("White", game));
        System.out.format("Black: %s%n", tagValue("Black", game));
        System.out.format("Result: %s%n", tagValue("Result", game));
        System.out.println("Final Position:");
        System.out.println(finalPosition(game));
    }
}