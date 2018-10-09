import util.OperatingSystem;
import util.OperationResult;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        //Create a new instance of an OperatingSystem wrapper
        OperatingSystem os = new OperatingSystem();

        try {
            //Execute a simple command, and print the results
            OperationResult dateResult = os.runCommand("date;");
            System.out.println(dateResult.getOutput().get(0));

            //Execute a sequence of commands in order, and print their results
            List<String> getAccessRules = new ArrayList<>();
            getAccessRules.add("/sbin/iptables -L INPUT -n --line-numbers |  sed 's/--//g' | sed -r 's/(\\s+)/|/'  | sed 's/  \\+/|/g';");
            getAccessRules.add("/sbin/ip6tables -L INPUT -n --line-numbers |  sed 's/--//g' | sed -r 's/(\\s+)/|/'  | sed 's/  \\+/|/g';");
            OperationResult accessResult = os.runScript(getAccessRules);

            for(String rule : accessResult.getOutput()) {
                System.out.println(rule);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
