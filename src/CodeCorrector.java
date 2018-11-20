import java.util.Arrays;

public class CodeCorrector {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    private int h;
    private GaloisField gf;

    public CodeCorrector(GaloisField gf, int h){
        this.gf = gf;
        this.h = h;
    }

    int[] encode(int[] msg) throws Exception{
        int[] generator = getGenerator();
        int[] shiftedMsg = new int[msg.length + 2 * h];
        System.arraycopy(msg, 0, shiftedMsg, 2 * h, msg.length);
        int[] controlMsg = polynomialMod(shiftedMsg, generator);
        System.arraycopy(controlMsg, 0, shiftedMsg, 0, controlMsg.length);
        return shiftedMsg;
    }

    int[] decode(int[] codeword) {
        int[] syndromes = new int[2 * h];
        for (int i = 0; i < syndromes.length; i++)
            syndromes[i] = evaluate(codeword, gf.pow(2, i + 1));
        boolean noErrors = true;
        for (int syndrome : syndromes) {
            if(syndrome != 0) {
                noErrors = false;
                break;
            }
        }

        if(noErrors){
            int[] msg = new int[codeword.length - 2 * h];
            System.arraycopy(codeword, 2 * h, msg, 0, msg.length);
            return msg;
        }

        int[] errorLocator = getErrorLocatorPolynom(syndromes);
        int[] locatorVals = new int[gf.getSize() - 1];
        for (int i = 0; i < locatorVals.length; i++)
            locatorVals[i] = evaluate(errorLocator, gf.pow(2, i));

        int[] betas = new int[h];
        int[] positions = new int[h];
        int j = 0;
        for (int i = 0; i < locatorVals.length; i++) {
            if(locatorVals[i] == 0){
                if(j >= betas.length) return null;

                betas[j] = gf.inverse(gf.pow(2, i));
                int pos = 0;
                while(gf.pow(2, pos) != betas[j])
                    pos++;
                positions[j] = pos;
                if(pos >= codeword.length) return null;
                j++;
            }
        }

        int[][] ksiSystem = new int[h][h + 1];
        for (int i = 0; i < h; i++)
            for (int k = 0; k < ksiSystem[i].length - 1; k++)
                ksiSystem[i][k] = gf.pow(betas[k], i + 1);

        for (int i = 0; i < h; i++)
            ksiSystem[i][h] = syndromes[i];

        int[] ksis = getRoots(ksiSystem);
        if(ksis == null) return null;

        int[] repaired = Arrays.copyOf(codeword, codeword.length);

        for (int i = 0; i < h; i++) {
            int pos = positions[i];
            repaired[pos] = gf.add(repaired[pos], ksis[i]);
        }

        printErrors(codeword, positions, ksis);
        int[] msg = new int[repaired.length - 2 * h];
        System.arraycopy(repaired, 2 * h, msg, 0, msg.length);
        return msg;
    }

    int[] getGenerator(){
        int[] result = new int[2 * h + 1];
        int gen = 2;
        result[0] = 1;
        int aPow = gen;
        for (int i = 0; i < 2 * h; i++) {
            for(int j = 2 * h; j >= 0; j--) {
                result[j] = gf.multiply(result[j], aPow);
                if(j > 0) result[j] = gf.add(result[j], result[j - 1]);
            }
            aPow = gf.multiply(aPow, gen);
        }
        return result;
    }

    int[] polynomialMod(int[] dividend, int[] divisor) throws Exception{
        if(dividend.length < divisor.length)
            return dividend;

        if(divisor[divisor.length - 1] != 1)
            throw new Exception("Wrong divisor format");

        int[] remainder = Arrays.copyOf(dividend, dividend.length);
        int diff = remainder.length - divisor.length;
        while (diff >= 0) {
            int factor = remainder[remainder.length - 1];
            for (int j = 0; j < divisor.length; j++)
                remainder[j + diff] = gf.add(remainder[j + diff], gf.multiply(divisor[j], factor));

            remainder = Arrays.copyOf(remainder, remainder.length - 1);
            diff--;
        }
        return remainder;
    }

    int[] getRoots(int[][] matrix){
        int rows = matrix.length;
        int cols =  matrix[0].length;

        int currRow = 0;
        for (int j = 0; j < cols && currRow < rows; j++) {
            int pivotRow = currRow;
            while (pivotRow < rows && matrix[pivotRow][j] == 0)
                pivotRow++;
            if (pivotRow == rows)
                continue;
            swapRows(matrix, currRow, pivotRow);
            pivotRow = currRow;
            currRow++;

            for (int k = cols - 1; k >= 0; k--)
                matrix[pivotRow][k] = gf.multiply(matrix[pivotRow][k], gf.inverse(matrix[pivotRow][j]));

            for (int l = pivotRow + 1; l < rows; l++)
                for (int k = cols - 1; k >= 0; k--)
                    matrix[l][k] = gf.add(matrix[l][k], gf.multiply(matrix[pivotRow][k], matrix[l][j]));

        }

        for (int i = currRow - 1; i >= 0; i--) {
            int pivotCol = 0;
            while (pivotCol < cols && matrix[i][pivotCol] == 0)
                pivotCol++;
            if (pivotCol == cols)
                continue;

            for (int l = i - 1; l >= 0; l--)
                for (int j = cols - 1; j >= 0; j--)
                    matrix[l][j] = gf.add(matrix[l][j], gf.multiply(matrix[i][j], matrix[l][pivotCol]));
        }

        int[] roots = new int[matrix.length];
        for (int[] row : matrix) {
            int j = 0;
            while (j < row.length && row[j] == 0)
                j++;
            if (j == row.length - 1)
                return null;
            if (j >= row.length)
                continue;
            roots[j] = row[row.length - 1];
        }
        return roots;
    }

    void swapRows(int[][] matrix, int i1, int i2){
        if(i1 == i2)
            return;
        int[] temp = Arrays.copyOf(matrix[i1], matrix[i1].length);
        matrix[i1] = Arrays.copyOf(matrix[i2], matrix[i2].length);
        matrix[i2] = temp;
    }

    int[] getErrorLocatorPolynom(int[] syndromes){
        int[][] sigmaSystem = new int[h][h + 1];
        for (int i = 0; i < h; i++)
            System.arraycopy(syndromes, i, sigmaSystem[i], 0, h);

        for (int i = 0; i < h; i++)
            sigmaSystem[i][h] = syndromes[h + i];

        int[] sigmas = getRoots(sigmaSystem);
        if(sigmas == null) return new int[]{1};

        int[] poly = new int[h + 1];
        poly[0] = 1;
        for (int i = 1; i < poly.length; i++)
            poly[i] = sigmas[sigmas.length - i];
        return poly;
    }

    int evaluate(int[] poly, int val){
        int res = 0;
        for (int i = 0; i < poly.length; i++)
            res = gf.add(res, gf.multiply(poly[i], gf.pow(val, i)));

        return res;
    }

    void printErrors(int[] codeword, int[] positions, int[] ksis){
        System.out.println("Found errors: ");
        outer:
        for (int i = 0; i < codeword.length; i++) {
            StringBuilder str = new StringBuilder(Integer.toBinaryString(codeword[codeword.length - 1 - i]));
            while(str.length() < 4)
                str.insert(0, "0");
            for (int j = 0; j < h; j++) {
                if(codeword.length - i - 1 == positions[j]){
                    String block = Integer.toBinaryString(ksis[j]);
                    while(block.length() < 4)
                        block = new StringBuffer(block).insert(0, "0").toString();
                    for (int k = 0; k < block.length(); k++) {
                        if(block.charAt(k) == '0')
                            System.out.print(str.charAt(k));
                        else
                            System.out.print(ANSI_RED + str.charAt(k) + ANSI_RESET);
                    }
                    System.out.print(" ");
                    continue outer;
                }
            }
            System.out.print(str.toString() + " ");
        }
        System.out.println();
    }
}
