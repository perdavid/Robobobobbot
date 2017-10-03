import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

// Jar file for JSON support

/**
 * TestRobot interfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 -> Lokarria(Robulab) -> Core -> MRDS4
 * 
 * @author thomasj, id15den, oi14mes
 */


public class TestRobot {

   private String host;                // host and port numbers
   private int port;
   private TestRobot robot;
   public ObjectMapper mapper = new ObjectMapper();
   public Collection<Map<String, Object>> data;


   /**
    * Create a robot connected to host "host" at port "port"
    * @param host normally http://130.239.42.49
    * @param port normally 50000
    */
   public TestRobot(String host, int port) throws IOException {
      this.host = host;
      this.port = port;
      this.robot = this;



      mapper = new ObjectMapper();

   }


   /**
    * This simple main program creates a robot, sets up some speed and turning rate and
    * then displays angle and position for 16 seconds.
    * @param args         not used
    * @throws Exception   not caught
    */
   public static void main(String[] args) throws Exception {

       //File pathFile = new File("/Users/David/IdeaProjects/Robobobobbot/Path-around-table-and-back.json");
       File pathFile = new File(args[0]);
       System.out.println(args[0]);
       BufferedReader in = new BufferedReader(new InputStreamReader( new FileInputStream(pathFile)));
       ObjectMapper mapper = new ObjectMapper();
       // read the path from the file
       Collection<Map<String, Object>> data;
           data = (Collection<Map<String, Object>>) mapper.readValue(in, Collection.class);
       Queue<Position> queue = new LinkedList<>();
       for (Map<String, Object> point : data)
       {
           Map<String, Object> pose = (Map<String, Object>)point.get("Pose"); Map<String, Object> aPosition = (Map<String, Object>)pose.get("Position");
           double x = (Double)aPosition.get("X");
           double y = (Double)aPosition.get("Y");
           queue.add(new Position(x,y));

       }


      System.out.println("Creating Robot");
      //TestRobot robot = new TestRobot("http://130.239.42.70", 50000);
      TestRobot robot = new TestRobot(args[1],50000);

      System.out.println("Creating request");
      DifferentialDriveRequest dr = new DifferentialDriveRequest();

       System.out.println("Creating response");
       LocalizationResponse lr = new LocalizationResponse();

       long starttime = System.nanoTime();
       System.out.println("Start to move robot");
while (!queue.isEmpty()) {
    double error;

    try {
        Thread.sleep(0);
    } catch (InterruptedException ex) {
    }

    robot.getResponse(lr);
    dr.setLinearSpeed(1);


    double robotAngle = robot.getHeadingAngle(lr);
    Position robotposition = new Position(robot.getPosition(lr));
    Position carrot = robot.getCarrot(lr, queue, 0.5);
    if (queue.isEmpty()){
        long stoptime = System.nanoTime();
        long totaltime = stoptime - starttime;
        double seconds = ((double)totaltime/(Math.pow(10,9)));
        System.out.println("Stop robot");
        System.out.println("The robot finished the path in " + seconds +" seconds");
        break;
    }

    error = (robotposition.getBearingTo(carrot) - robotAngle);
    dr.setAngularSpeed(error);

    if(Math.abs(error) > 0.1){
        if(error > Math.PI) {
            dr.setLinearSpeed(0);
            error = (2 * Math.PI) - error;
            error = 0 - error;
            dr.setAngularSpeed(error*6/Math.PI);

        }
        else if (error < -(Math.PI)){
            dr.setLinearSpeed(0);
            error = (2 * Math.PI) + error;
            dr.setAngularSpeed(error*6/Math.PI);
        }
        else {
            dr.setLinearSpeed(0);
            dr.setAngularSpeed(error*6 / Math.PI);
        }
    }

    robot.putRequest(dr);

    }
       dr.setLinearSpeed(0);
       dr.setAngularSpeed(0);
       robot.putRequest(dr);
}


    /**
     * Find a suitable carrotpoint depending on the distance.
     * @param lr
     * @param queue
     * @param distance
     * @return position used as "carrot"
     */

   Position getCarrot(LocalizationResponse lr, Queue<Position> queue, double distance){

       Position roboto = new Position(lr.getPosition());

       while (!queue.isEmpty()) {

           Position temp = queue.peek();
           if (roboto.getDistanceTo(temp) > distance) {
               return temp;
           } else
           queue.poll();
       }
       return null;
   }
    /**
     * Extract the robot heading from the response
     * @param lr
     * @return angle in degrees
     */
   double getHeadingAngle(LocalizationResponse lr)
   {


       return lr.getHeadingAngle();


   }

   /**
    * Extract the position
    * @param lr
    * @return coordinates
    */
   double[] getPosition(LocalizationResponse lr)
   {
      return lr.getPosition();
   }
   

   /**
    * Send a request to the robot.
    * @param r request to send
    * @return response code from the connection (the web server)
    * @throws Exception
    */
   public int putRequest(Request r) throws Exception
   {
      URL url = new URL(host + ":" + port + r.getPath());

      HttpURLConnection connection = (HttpURLConnection)url.openConnection();

      connection.setDoOutput(true);

      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setUseCaches (false);

      OutputStreamWriter out = new OutputStreamWriter(
            connection.getOutputStream());

      // construct a JSON string
      String json = mapper.writeValueAsString(r.getData());

      // write it to the web server
      out.write(json);
      out.close();

      // wait for response code
      int rc = connection.getResponseCode();

      return rc;
   }

   /**
    * Get a response from the robot
    * @param r response to fill in
    * @return response same as parameter
    * @throws Exception
    */
   public Response getResponse(Response r) throws Exception
   {
      URL url = new URL(host + ":" + port + r.getPath());
      System.out.println(url);

      // open a connection to the web server and then get the resulting data
      URLConnection connection = url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(
            connection.getInputStream()));

      // map it to a Java Map
      Map<String, Object> data = mapper.readValue(in, Map.class);
      r.setData(data);

      in.close();

      return r;
   }

}

