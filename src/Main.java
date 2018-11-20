import java.io.*;
import java.util.ArrayList;

public class Main {

    private static final GaloisField GF = new GaloisField(0b10011);
    private static int code;
    private static final int m = 4;
    private static final int h = 2;

    public static void main(String[] args) throws Exception {
        CodeCorrector corrector = new CodeCorrector(GF, h);
        if(Integer.toBinaryString(code).length() >= Math.pow(2, m) * m){
            System.out.println("Message is too large");
            return;
        }
        int[] codeArray = readFromConsole();
        int[] msg = corrector.encode(codeArray);
        if(msg == null) {
            System.out.println("Too many errors!!!");
            return;
        }
        System.out.println("Original message: ");
        writeToConsole(msg, 2, ' ');
    }

    private static int[] split(int i, int blockSize){
        int degree = 32 - Integer.numberOfLeadingZeros(i);
        int blocks = degree / blockSize;
        int modulus = 1 << blockSize;
        if(degree % blockSize != 0) blocks++;
        int[] res = new int[blocks];
        for (int j = 0; j < blocks; j++) {
            res[j] = i % modulus;
            i >>>= blockSize;
        }
        return res;
    }

    private static int[] readFromConsole() throws IOException {
        System.out.println("Input message: ");
        String str;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            str = br.readLine();
        }
        String[] blocks = str.split(" ");
        int[] ints = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++)
            ints[i] = Integer.parseInt(blocks[blocks.length - 1 - i], 2);

        return ints;
    }

    private static int[] readFromFile(String path) throws IOException {
        String str;
        try(BufferedReader br = new BufferedReader(new FileReader(path))){
            str = br.readLine();
        }
        String[] blocks = str.split(" ");
        int[] ints = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++)
            ints[i] = Integer.parseInt(blocks[blocks.length - 1 - i], 2);

        return ints;
    }

    public static void writeToConsole(int[] msg, int radix, char spliterator){
        if(radix == 2)
            for (int i = 0; i < msg.length; i++) {
                StringBuilder str = new StringBuilder(Integer.toBinaryString(msg[msg.length - 1 - i]));
                while(str.length() < 4)
                    str.insert(0, "0");
                System.out.print(str.toString() + spliterator);
            }
        else
            for (int i = 0; i < msg.length; i++)
                System.out.print(msg[msg.length - 1 - i] + "" + spliterator);
        System.out.println();
    }

    private static void writeToFile(String path, int[] msg, int radix, char spliterator) throws IOException {
        StringBuilder res = new StringBuilder();
        if(radix == 2) {
            for (int i = 0; i < msg.length; i++) {
                StringBuilder str = new StringBuilder(Integer.toBinaryString(msg[msg.length - 1 - i]));
                while (str.length() < 4)
                    str.insert(0, "0");
                res.append(str.toString()).append(spliterator);
            }
        } else {
            for (int i = 0; i < msg.length; i++)
                res.append(msg[msg.length - 1 - i]).append(spliterator);
        }
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(path))){
            bw.write(res.toString());
        }
    }
}
