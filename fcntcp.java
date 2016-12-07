import java.text.SimpleDateFormat;
import java.util.Calendar;

public class fcntcp {
    public int findIndex(){

        return 0;
    }

    public static void main(String[] args) throws Exception {
        String file_path = "blank", ipaddress = "";
        boolean quiet_client = false, quiet_server = false;

        int sport = 0, cport = 0, timeout = 1000;
        //time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

        //new Thread(new Server(time, 5000, 5001, "", "")).start();
        //new Client("TCP", time, "", "", 70000, 5001, 2);
        if(args[0].equals("-s") || args[0].equals("--server")){

            for(int i = 1; i < args.length; i++){
                switch(args[i]){
                    case "-q":
                    case "--quiet":
                        quiet_server = true;
                        break;
                }
            }
            sport = Integer.parseInt(args[args.length-1]);
            //System.out.println("Server " + sport + " " + quiet_server);
            new Thread(new Server(sport, quiet_server)).start();
        }

        else if(args[0].equals("-c") || args[0].equals("--client")){
            for(int i = 1; i < args.length; i++){
                switch(args[i]){
                    case "-t":
                    case "--timeout":
                        timeout = Integer.parseInt(args[i+1]);
                        break;
                    case "-f":
                    case "--file":
                        file_path = args[i+1];
                        break;
                    case "-q":
                    case "--quiet":
                        quiet_client = true;
                        break;
                }
                ipaddress = args[args.length - 2];
                cport = Integer.parseInt(args[args.length-1]);
            }
            //System.out.println("Client " + timeout + " " + file_path + " " + quiet_client + " " + ipaddress + cport);
            new Thread(new Client(ipaddress, cport, file_path, timeout, quiet_client)).start();
        }
        else{

        }
    }

}
