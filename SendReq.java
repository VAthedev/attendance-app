import java.net.*;
import java.io.*;
public class SendReq {
    public static void main(String[] a) throws Exception {
        Socket s = new Socket("localhost", 9999);
        PrintWriter w = new PrintWriter(s.getOutputStream(), true);
        w.println("{\"type\":\"FORGOT_PASSWORD\",\"payload\":{\"email\":\"nicezoe25@gmail.com\"},\"nonce\":\"123\"}");
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
        System.out.println(r.readLine());
    }
}
