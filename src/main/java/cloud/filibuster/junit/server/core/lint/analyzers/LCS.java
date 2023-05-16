package cloud.filibuster.junit.server.core.lint.analyzers;

public class LCS {
    private LCS() {

    }

    /** function lcs, from <a href="https://www.sanfoundry.com/java-program-longest-common-substring-algorithm/">...</a> **/
    public static String computeLCS(String str1, String str2)
    {
        int l1 = str1.length();
        int l2 = str2.length();

        int[][] arr = new int[l1 + 1][l2 + 1];
        int len = 0;
        int pos = -1;

        for (int x = 1; x < l1 + 1; x++) {
            for (int y = 1; y < l2 + 1; y++) {
                if (str1.charAt(x - 1) == str2.charAt(y - 1)) {
                    arr[x][y] = arr[x - 1][y - 1] + 1;
                    if (arr[x][y] > len) {
                        len = arr[x][y];
                        pos = x;
                    }
                }
                else {
                    arr[x][y] = 0;
                }
            }
        }
        int beginIndex = pos - len;
        if (beginIndex < 0) {  // To cover cases where the LCS is empty
            return "";
        }
        return str1.substring(beginIndex, pos);
    }
}
