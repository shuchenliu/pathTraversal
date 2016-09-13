
import java.io.InputStreamReader;
import com.google.gson.stream.JsonReader;  
import com.google.gson.stream.JsonToken;
import java.util.LinkedList;
import java.util.Scanner;

/**
 *
 * @author Shuchen Liu, Dilks Lab, Emory University. shuchenliu2014@gmail.com
 */


public class pathTraversal {
    
    private static String SESSION_ID;
    
    class ReturnInfo{
        public String message;
        public LinkedList<String> nodeQueue;
        public ReturnInfo(String message, LinkedList<String> nodes) {
            this.message = message;
            this.nodeQueue = nodes;
        }
    }
    
    
    private ReturnInfo executeCMD(String header, String path) {
        
        String cmd = "curl";
        String url = "http://challenge.shopcurbside.com/";
        ReturnInfo ans = new ReturnInfo("", new LinkedList<>());
        String command = cmd + " " + header + " " + url + path;
        
        
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            
            // if we know a sessionID will be returned
            if (header.length() == 0) {
                Scanner scn = new Scanner(p.getInputStream()).useDelimiter("\\A");
                ans.message = scn.next();
                return ans;
            }
            
            
            InputStreamReader isr = new InputStreamReader(p.getInputStream(), "UTF-8");
            JsonReader reader = new JsonReader(isr);
            
            // read in json
            reader.beginObject(); 
            while (reader.hasNext()) {
                
                String tagName = reader.nextName().toLowerCase();
                   
                if (tagName.equals("error")){  //if error dectected
                       reader.nextString();
                       ans.message = "request limit reached";
                } else if (tagName.equals("next")) { // or we got the address(es)
                    
                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                             ans.nodeQueue.offer(reader.nextString());
                        }
                        reader.endArray();
                    } else if (reader.peek() == JsonToken.STRING) {
                           ans.nodeQueue.offer(reader.nextString());
                    }
                    
                } else if (tagName.equals("secret")) { //jackpot, we found one piece of the secret
                    ans.message = reader.nextString();
                } else {
                     reader.skipValue();
                }
            }
            reader.endObject();
            return ans;
            
        } catch (Exception e) {
			    e.printStackTrace();
		    }

            return ans;
    }
    
    private void startTraveral(String path, StringBuilder secretBuffer) {
        String header = "--header Session:";
        ReturnInfo feedback = executeCMD(header + SESSION_ID, path);
        //base case: adding secret to secret buffer
        if (feedback.message.length() == 1) {
            System.out.println("found! The piece is " + feedback.message);
            secretBuffer.append(feedback.message) ;
            //System.out.println(secretBuffer.toString());
            return;
        }

        //check if new seesionID needed. If so, get a new one
        if (feedback.message.equals("request limit reached")) {
            SESSION_ID = getSessionID();
            System.out.println("Oops, need to generate a new pin.");
            startTraveral(path, secretBuffer);
        }

        //depth-first-search if next addresses avaiable
        LinkedList<String> queue = feedback.nodeQueue;
        while (queue != null && !queue.isEmpty()) {
            String next = queue.poll();
            startTraveral(next, secretBuffer);
        }

    }

    private String getSessionID() {
        ReturnInfo temp = executeCMD("", "get-session");
        return temp.message;
    }

    public static void main(String[] args) {
        pathTraversal curbside = new pathTraversal();
        StringBuilder secret = new StringBuilder();
        SESSION_ID = curbside.getSessionID();
        System.out.println("revealing the secret of Curbside...");
        
        curbside.startTraveral("start", secret);
        System.out.println("");
        System.out.println("okay, here's what I got: " + secret.toString());
    }     
}