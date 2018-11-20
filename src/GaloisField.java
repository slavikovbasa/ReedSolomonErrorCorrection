public class GaloisField {

    private final int modulus;

    private final int size;

    GaloisField(int mod) {
        int degree = 31 - Integer.numberOfLeadingZeros(mod);
        modulus = mod;
        size = 1 << degree;
    }

    public int add(int x, int y) {
        return x ^ y;
    }

    public int multiply(int x, int y) {
        int result = 0;
        while(y != 0){
            if ((y & 1) != 0)
                result ^= x;
            x <<= 1;
            if (x >= size)
                x ^= modulus;
            y >>>= 1;
        }
        return result;
    }

    public int inverse(int x) {
        return pow(x, size - 2);
    }

    public int pow(int x, int pow){
        if(pow == 0)
            return 1;
        else if(pow % 2 == 0)
            return pow(multiply(x, x), pow / 2);
        else
            return multiply(pow(multiply(x, x), pow / 2), x);
    }

    public int getSize(){
        return size;
    }
}