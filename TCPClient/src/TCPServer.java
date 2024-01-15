import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TCPServer {
    public static void main(String args[]) {
        Socket so = null;
        ServerSocket ss;

        try {
            ss = new ServerSocket(9999);
            so = ss.accept();
            System.out.print("OOK");
            OutputStream out = so.getOutputStream();

            PrintStream ps = new PrintStream(out);

            while(true) {
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                //System.out.print(so.getLocalPort());
                ps.println(input);
                out.flush();
                ps.flush();

            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

}
