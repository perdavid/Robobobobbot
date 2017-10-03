import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collection;
import java.util.Map;

/**
 * Created by David on 2017-09-05.
 */
public class  ReadPath {

    public Position[] path;



    public ReadPath() throws IOException {
        File pathFile = new File("Path-around-table.json");
        BufferedReader in = new BufferedReader(new InputStreamReader( new FileInputStream(pathFile)));
        ObjectMapper mapper = new ObjectMapper();
        // read the path from the file
        Collection<Map<String, Object>> data;
        data = (Collection<Map<String, Object>>) mapper.readValue(in, Collection.class);
        int nPoints = data.size();
        Position[] path = new Position[nPoints];
        int index = 0;
        for (Map<String, Object> point : data)
        {
            Map<String, Object> pose = (Map<String, Object>)point.get("Pose"); Map<String, Object> aPosition = (Map<String, Object>)pose.get("Position");
            double x = (Double)aPosition.get("X");
            double y = (Double)aPosition.get("Y");
            path[index] = new Position(x, y);
            index++;
        }
    }

}

